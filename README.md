<p align="center">
  <img src="favicon.ico" alt="PicStar Icon" width="72" height="72" />
</p>

<h1 align="center">PicStar</h1>

<p align="center">
  <strong>Um cliente alternativo moderno para Pixiv em Kotlin &amp; Jetpack Compose</strong><br />
  Inspirado na arquitetura de APIs do PixEz Flutter, com interface própria e foco em descoberta de artistas.
</p>

---

## Visão Geral

**PicStar** é um aplicativo Android nativo desenvolvido em **Kotlin** com **Jetpack Compose** e **Material 3**, consumindo diretamente a API oficial do Pixiv (`app-api.pixiv.net`) com autenticação **OAuth2 PKCE**.

O projeto nasceu com o objetivo de oferecer uma experiência visual moderna, fluida e fiel às principais funcionalidades de descoberta do Pixiv — com destaque para o fluxo **"Other artists to follow"** sempre que você segue um criador.

---

## Principais Funcionalidades

### Autenticação & Sessão
- **Login OAuth2 PKCE** integrado via WebView (`pixiv://account/login?code=...`), com renovação automática de *refresh token* e *access token*.
- **Gate de autenticação**: o acesso ao restante do app é liberado apenas após login válido.

### Home (`For You` & `Following`)
- **For You**:
  - Carrossel de **Pixiv Highlights / Pixivision**.
  - Carrossel de **Artistas recomendados** (limitado a 10 cards no carrossel, com opção **View more** para ver a lista completa), exibindo foto de perfil à direita e as **5 últimas ilustrações** dentro do card.
  - Descoberta **Other artists to follow** (`/v1/user/related`) ao seguir qualquer artista.
  - Feed de **ilustrações recomendadas** com rolagem infinita.
- **Following**:
  - Alternância entre **últimas ilustrações** dos artistas seguidos e listagem **By followed artists**.

### Popular
- Rankings do Pixiv divididos por categoria:
  - `Daily`, `For Male`, `For Female`, `Original`, `Rookie`, `Weekly`, `Monthly`, `AI`, `R-18`, `R-18 AI`, `R-18 Weekly`, `R-18G`.

### Favorites
- Lista de ilustrações curtidas/favoritadas pelo usuário, com filtro por tags dinâmicas.

### Busca Avançada
- Botão flutuante de busca disponível nas abas principais, com **histórico recente**, **tags recomendadas** e **autocomplete** após 2+ caracteres.
- Resultados navegáveis por ordenação: `Newest`, `Popular ⌄` (com dropdown para *All*, *Male* e *Female*) e `Oldest`.
- Filtros avançados em modal flutuante nos resultados:
  - **Work type** (Illustrations, Manga, Ugoira, Novels e combinações)
  - **Posting date** (Any time, 24h, week, month, 6 months, year)
  - **Like counts**, **AI generated** e **Mature content (R-18)**.

### Post & Perfil do Criador
- **Detalhes do Post**:
  - Visualizador em **tela cheia** com suporte a múltiplas páginas e zoom por pinça/duplo toque (1 clique).
  - **Download por clique longo** (imagens e Ugoira `.zip`) condicionado à pasta configurada em **Settings > Download settings** (via SAF / `OpenDocumentTree`).
  - Reprodução nativa de animações **Ugoira** frame-a-frame.
  - **Tags traduzidas** (`translated_name`) clicáveis para pesquisa rápida.
  - **Bookmarks clicáveis** para visualizar os usuários que favoritaram o post.
  - **Comentários em dropdown expansível**, com navegação direta ao perfil de quem comentou.
  - **Posts recomendados** relacionados com rolagem infinita.
- **Perfil do Artista / Usuário**:
  - Bio, contadores, links sociais clicáveis (**Website**, **Twitter/X**) e botão de Follow com sugestões relacionadas.
  - Abas: **Works** (filtrável por `All`, `Illustrations`, `Manga` e `Manga Series`), **Likes**, **Following**, **MyPixiv** e **Followers**.

### Settings
- **Profile**, **Preferences**, **Download settings** (pasta de destino SAF, qualidade da imagem, padrão de nome e proxy/host `pximg`), **Theme** e **About**.

---

## Stack & Estrutura do Projeto

- **Linguagem**: Kotlin (JVM 17)
- **UI**: Jetpack Compose + Material 3 (`compileSdk = 35`, `minSdk = 26`)
- **Rede & Imagens**: `HttpURLConnection` com headers do cliente Pixiv (`Referer: https://app-api.pixiv.net/`, `X-Client-Time`, `X-Client-Hash`), decodificação direta de bitmaps e `ZipInputStream` para Ugoira.
- **Persistência**: `SharedPreferences` + Storage Access Framework (`DocumentsContract`) para downloads.

```text
app/src/main/java/com/picstar/app/
├── api/          # PixEzApiClient (OAuth2 PKCE + App-API Pixiv)
├── data/         # PicStarViewModel e PicStarRepository
├── model/        # Modelos de domínio, filtros e estado de UI
├── ui/
│   ├── components/ # Cards, PximgNetworkImage, UgoiraAnimationPlayer
│   ├── screens/    # Home, Popular, Favorites, Search, Settings, Modais
│   └── theme/      # Temas (Midnight, etc.) e tipografia
└── MainActivity.kt
```

---

## Como Compilar

```bash
# Rodar testes unitários e gerar APK de debug
./gradlew testDebugUnitTest assembleDebug --no-daemon

# Instalar no aparelho conectado via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
