# Lumen

Lumen e um MVP de leitor Android para PDF e EPUB com foco em calma, imersao e reflexao.

O app foi desenhado para transmitir a ideia:

> Leia no seu ritmo.

Ele nao usa metas agressivas, streaks ou pressao de produtividade. O progresso existe, mas aparece de forma discreta.

## O que ja existe neste projeto

- Biblioteca com visual escuro, capas grandes e progresso leve.
- Importacao de arquivos PDF e EPUB.
- Leitor de PDF usando recursos nativos do Android.
- Leitor de EPUB offline usando WebView.
- Salvamento local de progresso.
- Reflexoes pessoais ligadas ao livro.
- Feed contextual simulado por livro.
- Favoritos, filtros e busca.
- Tema AMOLED, tema sepia e ajuste simples de brilho.
- Banco local SQLite para livros e anotacoes.

## Como abrir sendo iniciante

1. Instale o Android Studio:
   https://developer.android.com/studio

2. Abra o Android Studio.

3. Escolha `Open`.

4. Selecione esta pasta:
   `C:\Users\Ferna\Documents\Codex\2026-05-26\crie-um-aplicativo-android-moderno-e`

5. Espere o Android Studio baixar e configurar o Gradle.

6. Conecte um celular Android com modo desenvolvedor ativado, ou crie um emulador.

7. Clique em `Run`.

## Como gerar APK

### Opcao recomendada: pelo GitHub

Este projeto ja vem com um gerador automatico de APK para GitHub Actions.

1. Crie um repositorio novo no GitHub.
2. Envie todos os arquivos desta pasta para o repositorio.
3. Entre no repositorio pelo navegador.
4. Abra a aba `Actions`.
5. Clique em `Gerar APK do Lumen`.
6. Clique em `Run workflow`.
7. Espere terminar.
8. Baixe o arquivo chamado `lumen-apk-debug`.
9. Dentro dele estara o APK `app-debug.apk`.

Esse APK e uma versao de teste. O Android pode avisar que o app veio de fonte externa; isso e normal para APK gerado fora da Play Store.

### Pelo Android Studio

No Android Studio:

1. Abra o menu `Build`.
2. Escolha `Build Bundle(s) / APK(s)`.
3. Clique em `Build APK(s)`.
4. Quando terminar, o Android Studio mostrara um link para localizar o APK.

O APK normalmente ficara em:

`app\build\outputs\apk\debug\app-debug.apk`

## Observacao importante

Este computador, neste momento, nao tem Java, Gradle nem Android SDK configurados no terminal. Por isso eu consegui criar o projeto, mas nao consegui gerar o APK diretamente daqui ainda.

Existem dois caminhos:

- Abrir este projeto no Android Studio, que baixa e configura tudo por interface visual.
- Usar GitHub Actions, que gera o APK em um servidor online.

Para este notebook, o caminho com GitHub Actions e o mais tranquilo.

## Estrutura

```text
app/
  src/main/
    AndroidManifest.xml
    java/com/lumen/reader/
      MainActivity.java
      data/LocalStore.java
      model/
      reader/
    res/
build.gradle
settings.gradle
```

## Estado do MVP

Este e um MVP funcional de base. Algumas partes foram feitas de forma propositalmente simples para evitar dependencia de servicos externos:

- O feed social ainda e simulado.
- O suporte EPUB e basico, suficiente para muitos EPUBs simples, mas pode evoluir para um motor dedicado.
- O suporte PDF usa o renderizador nativo do Android.
- A arquitetura ja separa dados, modelos e leitores para facilitar evolucao.
