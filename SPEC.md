# Especificação — Simple Call Blocker (Android)

## Contexto
O usuário quer um app Android **simples** para bloquear chamadas indesejadas (telemarketing, números específicos, números ocultos). O diretório `/home/gandhi/src/simple-call-blocker` está vazio, então o projeto começa do zero. Esta especificação define o escopo da v1, as decisões técnicas e a ordem de implementação.

Decisões assumidas como padrão (podem ser alteradas na revisão):
- **Kotlin + Jetpack Compose (Material 3)**, `minSdk 29` (Android 10), `targetSdk` mais recente estável.
- Bloqueio via **`CallScreeningService` + papel `ROLE_CALL_SCREENING`** — API oficial; não exige que o app seja o discador padrão.
- Somente chamadas de voz recebidas. **SMS fica fora** (exigiria ser o app de SMS padrão).
- Sem backend, sem conta, sem internet: tudo local no aparelho.

---

## 1. Escopo da v1

### Funcionalidades
| # | Funcionalidade | Descrição |
|---|---|---|
| F1 | Interruptor geral | Liga/desliga todo o bloqueio sem apagar regras. |
| F2 | Bloquear número exato | Usuário cadastra um número (com rótulo opcional). |
| F3 | Bloquear por prefixo | Ex.: `0303` (telemarketing BR), `+55 11 4…`, `+1` (DDI). |
| F4 | Bloquear números ocultos/privados | Chamadas sem número identificado. |
| F5 | Bloquear desconhecidos | Números que não estão nos contatos (requer `READ_CONTACTS`). |
| F6 | Histórico de bloqueios | Lista local: número, data/hora, motivo (regra que bloqueou). Ação rápida "desbloquear"/"remover regra". |
| F7 | Notificação de bloqueio (opcional) | Notificação discreta "Chamada de X bloqueada". Desligável. |
| ~~F8~~ | ~~Ocultar do registro do sistema~~ | **Removida.** No Android 10+ chamadas recusadas pela triagem são sempre registradas como `BLOCKED_TYPE`, independentemente de `setSkipCallLog` (confirmado no emulador). O discador as mostra como bloqueadas, não como perdidas. |
| F9 | País | Automático (chip → rede → `BR`) ou escolhido pelo usuário. Define como números sem DDI são interpretados. |

### Fora do escopo (v2+)
Bloqueio de SMS, lista de permitidos explícita, agendamento por horário, importação/exportação, base de spam colaborativa/online, verificação STIR/SHAKEN, widgets.

---

## 2. Comportamento ao bloquear
Resposta enviada ao sistema em `onScreenCall`:
```kotlin
CallResponse.Builder()
    .setDisallowCall(true)
    .setRejectCall(true)              // recusa (quem liga ouve ocupado/caixa postal)
    .setSkipNotification(true)        // sem notificação de "chamada perdida" do sistema
    .build()
```
Chamadas permitidas: `CallResponse.Builder().build()` (sem alterações).

## 3. Lógica de decisão (ordem de avaliação)
1. Chamada **não é recebida** (`callDirection != INCOMING`) → permitir.
2. Bloqueio geral **desligado** → permitir.
3. Número **oculto** (`handle == null`, número vazio ou `handlePresentation != PRESENTATION_ALLOWED`) → bloquear se F4 ativo, senão permitir.
4. Normalizar o número (ver §4).
5. Casa com alguma **regra exata** ativa → bloquear (motivo = regra).
6. Casa com algum **prefixo** ativo → bloquear (motivo = regra).
7. F5 ativo **e** número não está nos contatos → bloquear (motivo = "desconhecido").
8. Caso contrário → permitir.

Requisitos não funcionais do serviço:
- Responder em **< 1 s** (o sistema tolera ~5 s). Regras ficam em cache em memória (Flow do Room → `StateFlow`), sem I/O pesado no caminho crítico.
- Em qualquer exceção → **permitir** a chamada (falhar aberto; nunca perder chamada legítima por bug).
- Gravar o log de bloqueio **depois** de responder (coroutine em `Dispatchers.IO`).

## 4. Normalização de números
- Usar a **libphonenumber** (Java puro, testável na JVM; é a mesma base do `PhoneNumberUtils` do Android) com o país de F9: o escolhido nas configurações ou, se automático, `TelephonyManager.simCountryIso` → `networkCountryIso` → `"BR"`.
- Mudar o país não altera regras existentes: exatas já estão em E.164 e prefixos são guardados como digitados.
- Regras **exatas** são armazenadas em E.164 já normalizadas no cadastro → comparação por igualdade.
- Regras de **prefixo** comparam contra duas formas: só dígitos do número cru (para `0303…`, que não é E.164 válido) e a forma E.164 (para `+55…`, `+1…`).
- Lógica isolada em uma classe pura (`NumberNormalizer`) para testes unitários.

## 5. Modelo de dados (Room)
```kotlin
@Entity data class BlockRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: RuleType,          // EXACT, PREFIX
    val pattern: String,         // E.164 (EXACT) ou dígitos/“+dígitos” (PREFIX)
    val label: String?,          // ex.: "Telemarketing"
    val enabled: Boolean = true,
    val createdAt: Long
)

@Entity data class BlockedCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String?,         // null = oculto
    val timestamp: Long,
    val reason: BlockReason,     // RULE_EXACT, RULE_PREFIX, HIDDEN, UNKNOWN
    val ruleId: Long?
)
```
Configurações em **DataStore Preferences**: `blockingEnabled`, `blockHidden`, `blockUnknown`, `notifyOnBlock`, `countryIso` (null = automático).
Histórico: manter no máximo os últimos 500 registros (limpeza ao inserir).

## 6. Telas (Compose, navegação com bottom bar)
1. **Onboarding / Permissões** — explica o app e pede o papel `ROLE_CALL_SCREENING` via `RoleManager.createRequestRoleIntent`. Reaparece se o papel for revogado.
2. **Início** — interruptor geral grande, status ("Proteção ativa" / "Papel não concedido"), contagem de bloqueios hoje, últimos 5 bloqueios.
3. **Regras** — lista de regras (exatas e prefixos) com switch por regra, botão de excluir com "Desfazer", FAB "Adicionar" (diálogo: tipo, número/prefixo, rótulo; validação e prévia do número normalizado).
4. **Histórico** — lista cronológica; ao tocar: "Copiar número", "Remover regra que bloqueou", "Limpar histórico".
5. **Configurações** — switches F4, F5 (pede `READ_CONTACTS` ao ativar), F7 (pede `POST_NOTIFICATIONS` no Android 13+); seletor de país (F9, "Automático (BR)" + lista); "Sobre".

## 7. Permissões e manifesto
- Serviço:
```xml
<service android:name=".screening.BlockerScreeningService"
    android:permission="android.permission.BIND_SCREENING_SERVICE"
    android:exported="true">
    <intent-filter><action android:name="android.telecom.CallScreeningService"/></intent-filter>
</service>
```
- Papel: `ROLE_CALL_SCREENING` (concedido pelo usuário, sem permissão no manifesto).
- `READ_CONTACTS` — apenas se F5 for ativado (pedida em tempo de execução).
- `POST_NOTIFICATIONS` — apenas se F7 for ativado (Android 13+).
- **Não** usar `READ_CALL_LOG`/`CALL_PHONE` (evita restrições da Play Store).
- Ponto a validar em aparelho real: comportamento do `CallScreeningService` para números que **estão** nos contatos (em algumas versões o sistema pode não invocar o serviço para contatos). Isso não afeta o caso de uso principal (bloquear desconhecidos), mas deve ser documentado.

## 8. Arquitetura e estrutura do projeto
Módulo único `app`, pacote `com.simplecallblocker` (ajustável). Camadas simples, DI manual via uma classe `AppContainer` na `Application` (sem Hilt, para manter simples).
```
app/src/main/java/.../
  SimpleCallBlockerApp.kt         // Application + AppContainer
  MainActivity.kt
  data/
    db/AppDatabase.kt, BlockRuleDao.kt, BlockedCallDao.kt, entities
    settings/SettingsRepository.kt  // DataStore
    RulesRepository.kt, HistoryRepository.kt
  domain/
    NumberNormalizer.kt             // puro, testável
    CallDecisionEngine.kt           // puro: (CallInfo, rules, settings, isContact) -> Decision
  screening/
    BlockerScreeningService.kt      // adapta Call.Details -> CallInfo, chama engine, responde
    ContactsLookup.kt               // PhoneLookup
    BlockNotifier.kt
  ui/
    onboarding/, home/, rules/, history/, settings/, theme/, navigation/
app/src/test/...                    // testes unitários de domain/
app/src/androidTest/...             // testes de DAO
```
Build: Gradle Kotlin DSL + `libs.versions.toml`. Dependências: Compose BOM, Material 3, Navigation Compose, Lifecycle/ViewModel, Room (KSP), DataStore, Coroutines. Testes: JUnit, kotlinx-coroutines-test, Room testing.

`CallDecisionEngine` e `NumberNormalizer` concentram as regras e não dependem do Android framework — é onde ficam a maior parte dos testes.

## 9. Etapas de implementação
1. Esqueleto do projeto (Gradle, tema, `MainActivity`, navegação vazia).
2. `NumberNormalizer` + `CallDecisionEngine` com testes unitários.
3. Room (entidades, DAOs, repositórios) + DataStore de configurações.
4. `BlockerScreeningService` + manifesto + log de bloqueio + notificação.
5. Onboarding com solicitação de `ROLE_CALL_SCREENING`.
6. Telas Regras, Histórico, Início, Configurações.
7. Polimento: estados vazios, validação de entrada, ícone, strings em pt-BR (com `values/strings.xml` em pt-BR e espaço para en).

## 10. Verificação
- **Unitários**: `./gradlew test` — casos: número exato (com/sem `+55`, com máscara), prefixo `0303`, prefixo DDI, oculto, desconhecido vs. contato, bloqueio geral desligado, chamada de saída, exceção → permitir.
- **Instrumentados**: `./gradlew connectedAndroidTest` — DAOs e limpeza do histórico.
- **Ponta a ponta no emulador** (API 29 e API mais recente):
  1. Instalar (`./gradlew installDebug`), conceder o papel no onboarding.
  2. Cadastrar regra `0303` e número `+5511999990000`.
  3. Simular chamadas: `adb emu gsm call 03031234567`, `adb emu gsm call 11999990000`, `adb emu gsm call 11888880000`.
  4. Esperado: as duas primeiras são rejeitadas sem tocar e aparecem no Histórico com o motivo correto; a terceira toca normalmente.
  5. Ativar "bloquear desconhecidos", adicionar um contato e repetir para contato vs. não contato.
  6. Desligar o interruptor geral e confirmar que nada é bloqueado.
- **Aparelho real**: validar o ponto de contatos do §7 e o tempo de resposta.
