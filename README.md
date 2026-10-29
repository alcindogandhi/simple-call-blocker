# Bloqueador de Chamadas

App Android simples que recusa chamadas indesejadas: números específicos, prefixos (como o `0303` do telemarketing), números ocultos e, opcionalmente, quem não está nos contatos. Funciona sem internet; nada sai do aparelho.

A especificação completa está em [SPEC.md](SPEC.md).

## Idiomas

Português do Brasil (padrão, `res/values`) e inglês (`res/values-en`). O app segue o idioma do aparelho; no Android 13+ o idioma do app também pode ser escolhido em Configurações › Apps › Bloqueador de Chamadas › Idioma. Para incluir outro idioma, crie `res/values-<código>/strings.xml` com as mesmas chaves: a lista de idiomas do app é gerada no build.

Para testar outro idioma no emulador sem mudar o sistema:

```sh
adb shell cmd locale set-app-locales io.github.alcindogandhi.simplecallblocker --locales pt-BR   # "" volta ao do sistema
```

## Requisitos

- Android 10 (API 29) ou mais novo no aparelho.
- JDK 17+ e Android SDK com a plataforma 37 para compilar. O Gradle é baixado pelo wrapper.

## Compilar e instalar

```sh
./gradlew assembleDebug        # APK de debug
./gradlew installDebug         # instala no aparelho/emulador conectado
./gradlew assembleRelease      # APK de release (minificado, não assinado)
```

Na primeira abertura o app pede para ser o **app de identificação de chamadas e spam** (papel `ROLE_CALL_SCREENING`). Sem isso o sistema não consulta o app. Ao conceder o papel, o Android também concede a permissão de notificações.

## Testes

```sh
./gradlew testDebugUnitTest          # unitários (JVM): normalização, decisão, configurações, países
./gradlew connectedDebugAndroidTest  # instrumentados (emulador/aparelho): banco Room
./gradlew lintDebug
```

`connectedDebugAndroidTest` desinstala o app ao terminar; rode `installDebug` depois, em um comando separado.

### Teste de ponta a ponta no emulador

O build de debug inclui um receptor de comandos, aceito só a partir do `adb shell`, para preparar cenários sem tocar na tela:

```sh
P=io.github.alcindogandhi.simplecallblocker
adb shell cmd role add-role-holder android.app.role.CALL_SCREENING $P
adb shell am broadcast -p $P -a $P.debug.SET --es key country --es value BR
adb shell am broadcast -p $P -a $P.debug.ADD_RULE --es type PREFIX --es pattern 0303
adb shell am broadcast -p $P -a $P.debug.SET --es key block_unknown --ez value true

adb emu gsm call 03031234567    # simula uma chamada recebida
adb emu gsm cancel 03031234567
```

Chaves de `SET`: `blocking_enabled`, `block_hidden`, `block_unknown`, `notify`, `country`.

O chip do emulador informa país **US**: escolha o Brasil em Configurações › País (ou pelo comando acima) para que números sem DDI sejam interpretados como brasileiros.

## Estrutura

```
app/src/main/java/com/simplecallblocker/
  domain/      regras de negócio puras: NumberNormalizer, CallDecisionEngine, modelos, países
  data/        Room (regras e histórico), DataStore (configurações), repositórios
  screening/   CallScreeningService, consulta de contatos, notificação, papel de triagem
  ui/          Compose: onboarding, Início, Regras, Histórico, Configurações
app/src/debug/ receptor de comandos para testes via adb
app/schemas/   esquema exportado do Room (base para migrações)
```

As dependências são criadas uma única vez no `AppContainer` (`SimpleCallBlockerApp.kt`), sem framework de injeção.

## Limitações conhecidas

- **Registro de chamadas:** no Android 10+ chamadas recusadas pela triagem sempre aparecem no registro do telefone, como *bloqueadas*. Não é possível ocultá-las.
- **SMS** não é filtrado (exigiria ser o app de SMS padrão).

## Licença

Copyright (C) 2026 Alcindo Gandhi

Este programa é software livre: você pode redistribuí-lo e/ou modificá-lo sob os termos da
[GNU General Public License](LICENSE), conforme publicada pela Free Software Foundation, na versão 3
da licença ou (a seu critério) qualquer versão posterior.

Este programa é distribuído na esperança de que seja útil, mas SEM NENHUMA GARANTIA, nem mesmo a
garantia implícita de COMERCIALIZAÇÃO ou ADEQUAÇÃO A UMA FINALIDADE ESPECÍFICA. Veja a licença
completa no arquivo [LICENSE](LICENSE).
