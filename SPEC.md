# Bloqueador de Chamadas — Especificação

Documento de referência do comportamento e das decisões técnicas do app. Para compilar, testar e contribuir, veja o [README](README.md).

## 1. Visão geral

App Android simples para recusar chamadas indesejadas: telemarketing, números específicos, números ocultos e, opcionalmente, quem não está nos contatos.

- **Plataforma:** Android 10+ (`minSdk 29`), Kotlin, Jetpack Compose com Material 3.
- **Mecanismo:** [`CallScreeningService`](https://developer.android.com/reference/android/telecom/CallScreeningService) com o papel `ROLE_CALL_SCREENING` ("app de identificação de chamadas e spam"). É a API oficial e não exige que o app seja o discador padrão.
- **Alcance:** apenas chamadas de voz recebidas. SMS não é filtrado.
- **Privacidade:** sem backend, sem conta e sem permissão de internet. Regras, histórico e configurações ficam só no aparelho.
- **Idiomas:** português do Brasil (padrão) e inglês.

## 2. Funcionalidades

| # | Funcionalidade | Comportamento |
|---|---|---|
| F1 | Interruptor geral | Liga/desliga todo o bloqueio sem apagar regras. |
| F2 | Número exato | Bloqueia um número específico, com descrição opcional. |
| F3 | Prefixo | Bloqueia números que começam com o prefixo: `0303` (telemarketing no Brasil), um DDD (`21`) ou um DDI (`+1`). |
| F4 | Números ocultos | Recusa chamadas sem número identificado. |
| F5 | Fora dos contatos | Só aceita chamadas de quem está nos contatos (requer `READ_CONTACTS`). Inclui F4: enquanto F5 está ativa, F4 aparece ligada e travada e números ocultos são sempre recusados. Ao ligar F5, F4 é gravada como ligada; ao desligar F5, F4 volta a ser editável e continua ligada. Sem a permissão de contatos, F5 é tratada como desligada, na tela e no serviço. |
| F6 | Histórico | Lista local dos bloqueios com número, data/hora e motivo. Ações: copiar o número, remover a regra que bloqueou (com desfazer) e limpar o histórico. Mantém os 500 registros mais recentes. |
| F7 | Notificação | Notificação discreta "Chamada de X bloqueada", substituída a cada novo bloqueio. Pode ser desligada. |
| F9 | País | Automático (chip → rede → Brasil) ou escolhido pelo usuário. Define como números sem DDI são interpretados, nas regras e nas chamadas. |

O identificador F8 foi usado por "ocultar do registro de chamadas", removido por limitação da plataforma (ver §10).

**Fora do escopo:** bloqueio de SMS, lista de permitidos, agendamento por horário, importação/exportação de regras, base de spam online, verificação STIR/SHAKEN e widgets.

## 3. Lógica de decisão

Implementada em `CallDecisionEngine`, na ordem:

1. Chamada **não é recebida** → permitir.
2. Interruptor geral (F1) **desligado** → permitir.
3. Número **oculto** (`handle` nulo, número vazio ou `handlePresentation != PRESENTATION_ALLOWED`) → recusar se F4 **ou** F5 estiver ativa; senão, permitir.
4. Casa com uma **regra exata** ativa → recusar (motivo: número bloqueado).
5. Casa com um **prefixo** ativo → recusar (motivo: prefixo bloqueado).
6. F5 ativa e o número **não está nos contatos** → recusar (motivo: fora dos contatos).
7. Caso contrário → permitir.

Regras valem mesmo para números salvos nos contatos. A consulta aos contatos só é feita no passo 6, quando necessária.

**Falha aberta:** qualquer exceção, ou demora acima de 3 s, resulta em chamada permitida. Um defeito nunca deve impedir uma chamada legítima.

## 4. Números de telefone

Toda a interpretação de números usa a [libphonenumber](https://github.com/google/libphonenumber), isolada em `NumberNormalizer` (Kotlin puro, testável na JVM), com o país de F9.

**Cadastro de regras**

| Tipo | Entrada aceita | Como é guardada |
|---|---|---|
| Exata | 3 a 15 dígitos, com ou sem máscara | E.164 (`+5511999990000`) se o número for válido para o país; senão, só os dígitos (`4004`). |
| Prefixo | 1 a 15 dígitos, com ou sem `+` | `+` seguido de dígitos (`+1`, `+5511`) ou só dígitos (`0303`, `21`). |

Regras repetidas são rejeitadas mesmo em outro formato (`11999990000` e `+55 11 99999-0000` são a mesma regra). Mudar o país não altera regras existentes.

**Comparação com a chamada recebida**

- Regra com `+` → comparada com a forma E.164 do número recebido.
- Regra só com dígitos → comparada com as formas nacionais do número: os dígitos como chegaram (quando não vêm com `+`) e, se o número for do país configurado, o número nacional (`11999990000`) e os dígitos do formato nacional, que preservam o `0` de códigos especiais (`+553031234567` → `03031234567`).

Assim, `21` bloqueia o DDD 21 tanto em `21988887777` quanto em `+5521988887777`, mas não um número americano `+1 212…`.

**Exibição:** formato nacional para números do país configurado (`(11) 99999-0000`, `0303 123 4567`) e internacional para os demais (`+1 212-555-0100`). O histórico guarda o E.164 de números válidos e o número como chegou nos demais casos.

## 5. Resposta ao sistema

```kotlin
CallResponse.Builder()
    .setDisallowCall(true)
    .setRejectCall(true)        // recusa: quem liga ouve ocupado ou cai na caixa postal
    .setSkipNotification(true)  // sem notificação de "chamada perdida"
    .build()
```

Chamadas permitidas recebem `CallResponse.Builder().build()`, sem alterações.

O serviço lê regras de um cache em memória (`StateFlow` alimentado pelo Room) e responde antes de gravar o histórico e mostrar a notificação. Como o processo costuma ser iniciado pela própria chamada, a `Application` pré-carrega as configurações e os metadados da libphonenumber.

## 6. Dados

**Room** (`call_blocker.db`, esquema exportado em `app/schemas/`):

```kotlin
@Entity(tableName = "block_rules", indices = [Index(value = ["type", "pattern"], unique = true)])
data class BlockRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: RuleType,       // EXACT, PREFIX
    val pattern: String,      // normalizado conforme §4
    val label: String? = null,
    val enabled: Boolean = true,
    val createdAt: Long = 0,
)

@Entity(tableName = "blocked_calls", indices = [Index("timestamp")])
data class BlockedCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String?,      // null = oculto
    val timestamp: Long,
    val reason: BlockReason,  // RULE_EXACT, RULE_PREFIX, HIDDEN, UNKNOWN
    val ruleId: Long? = null, // não é chave estrangeira: o registro sobrevive à exclusão da regra
)
```

**DataStore Preferences:** `blockingEnabled`, `blockHidden`, `blockUnknown`, `notifyOnBlock` e `countryIso` (ausente = automático).

## 7. Telas

1. **Boas-vindas:** explica o app e pede o papel `ROLE_CALL_SCREENING`. Se o usuário recusar, orienta e oferece abrir Configurações › Apps padrão. A tela reaparece sempre que o papel for revogado.
2. **Início:** cartão do interruptor geral, contadores "bloqueadas hoje" e "regras ativas", os 5 últimos bloqueios e atalho para o histórico.
3. **Regras:** lista com interruptor e exclusão (com desfazer) por regra. O diálogo de nova regra tem seletor de tipo, campo que aceita só dígitos e `+-(). `, exemplo de número do país configurado, prévia de como a regra será gravada e mensagens de erro. Com o país Brasil, sugere em um toque o prefixo `0303`.
4. **Histórico:** lista cronológica. Tocar em um registro abre as ações de F6.
5. **Configurações:** F4, F5 e F7 (pedindo a permissão necessária ao ligar e refletindo revogações), seletor de país pesquisável por nome ou DDI, atalho para trocar o app de identificação de chamadas e versão.

## 8. Permissões

| Permissão | Uso |
|---|---|
| `ROLE_CALL_SCREENING` (papel) | Concedido pelo usuário na tela de boas-vindas. Sem ele o sistema não consulta o app. |
| `BIND_SCREENING_SERVICE` | Protege o `BlockerScreeningService`; só o sistema pode se ligar a ele. |
| `READ_CONTACTS` | Pedida apenas ao ligar F5. |
| `POST_NOTIFICATIONS` | Android 13+. Concedida automaticamente junto com o papel de triagem; a tela só pede se o usuário a revogar. |

O app **não** usa `READ_CALL_LOG`, `CALL_PHONE` nem `INTERNET`. As duas primeiras são restritas pela Play Store e não são necessárias para a triagem.

## 9. Arquitetura

Módulo único `app`, pacote `io.github.alcindogandhi.simplecallblocker`:

```
domain/      regras de negócio sem dependência do Android
  Models.kt               modelos (também entidades do Room)
  NumberNormalizer.kt     interpretação, comparação e formatação de números (§4)
  CallDecisionEngine.kt   lógica de decisão (§3)
  Countries.kt            países suportados e seus nomes
data/        Room (db/), DataStore (settings/), RulesRepository, HistoryRepository
screening/   BlockerScreeningService, ContactsLookup, BlockNotifier, ScreeningRole
ui/          AppRoot (boas-vindas × navegação), telas com ViewModel, componentes e tema
debug/       (só no build de debug) receptor de comandos via adb para testes de ponta a ponta
```

As dependências são criadas uma vez no `AppContainer` (`SimpleCallBlockerApp.kt`), sem framework de injeção. O build usa Gradle Kotlin DSL com catálogo de versões (`gradle/libs.versions.toml`); o build de release é minificado com R8.

## 10. Decisões de projeto e limitações

- **libphonenumber em vez de `PhoneNumberUtils`:** a API do Android não roda em teste unitário puro, e a libphonenumber é a mesma base, com tratamento completo das regras brasileiras (prefixo `0` de longa distância, códigos de operadora, números `0300`/`0303`/`0800`).
- **Falha aberta:** erros e demoras sempre permitem a chamada (§3).
- **F5 inclui F4:** quem só aceita contatos não tem como conferir um número oculto. A regra vale também no motor de decisão, para cobrir configurações gravadas por versões anteriores.
- **Registro de chamadas (F8 removida):** no Android 10+ chamadas recusadas pela triagem sempre aparecem no registro do sistema como *bloqueadas*, independentemente de `setSkipCallLog`; o discador não as trata como perdidas. A opção de ocultá-las foi removida por não ter efeito.
- **Entidades do Room no domínio:** os modelos de `domain/` levam as anotações do Room para evitar uma camada de mapeamento; as anotações não impedem os testes na JVM.
- **Dica do campo de número:** vem do exemplo de celular da libphonenumber para o país configurado, não do idioma.
- **Ponto em aberto:** confirmar em aparelhos reais se o sistema consulta o serviço de triagem para números que estão nos contatos (algumas versões podem não consultar). Isso não afeta F5, que só precisa recusar quem não é contato.

## 11. Verificação

- **Unitários** (`./gradlew testDebugUnitTest`): normalização e comparação de números, ordem e casos da lógica de decisão (formatos, `0303`, DDD, DDI, ocultos, contatos, F5 incluindo F4, interruptor geral, chamada de saída, falha aberta), configurações e países.
- **Instrumentados** (`./gradlew connectedDebugAndroidTest`): repositórios com Room em memória (normalização no cadastro, duplicatas, país, histórico limitado a 500).
- **Ponta a ponta** no emulador, com chamadas simuladas por `adb emu gsm call` e cenários preparados pelos comandos de debug descritos no README: regras exatas e de prefixo, fora dos contatos com e sem permissão, contato salvo, interruptor geral, notificação e histórico.
- **Release:** o APK minificado é testado pela interface, sem os comandos de debug.
