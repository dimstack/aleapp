# CallApp — Decentralized P2P Calling Application

## Communication
- Communicate with the user in **Russian** (explanations, comments, descriptions).
- If the user writes `КИП`, it means: perform `git commit` and `git push` to GitHub.
- For every new feature, write tests whenever possible.
- After making changes, verify compilation and run the tests.
- In PowerShell, do not use `&&` as a command separator; use `;` or run commands separately.

## Project Overview

CallApp is a decentralized voice and video calling application for Android built on the **BYOS (Bring Your Own Server)** architecture. Each user (or group admin) deploys their own server instance via a Docker container on a VPS, creating small trusted communities for censorship-resistant communication. Think of it as "WhatsApp calling, but each group runs on its own server."

### Core Philosophy
- **Decentralized**: No central authority. Each server is independent.
- **Censorship-resistant**: Anyone can spin up a server; there's no single point to block.
- **Small trusted groups**: Server admin controls access via invite tokens (with optional approval).
- **BYOS**: Bring Your Own Server — users deploy their own infrastructure.
- **Easy deployment**: One-click Docker install, similar to Amnezia VPN's approach.

### Design Reference

The old `design-reference/` prototype has been removed from the repository. Use the existing Android Compose implementation, current theme tokens, and shipped UI behavior as the source of truth for future screen changes.

---

## Architecture

### High-Level Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Container (VPS)                │
│                                                         │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │   coturn     │  │  Signaling   │  │   REST API    │  │
│  │ TURN/STUN    │  │  Server      │  │   (Ktor)      │  │
│  │ :3478        │  │  WebSocket   │  │   :3000       │  │
│  └─────────────┘  │  :8080       │  └───────┬───────┘  │
│                    └──────────────┘          │           │
│                                      ┌──────┴───────┐   │
│                                      │   SQLite DB   │   │
│                                      └──────────────┘   │
└─────────────────────────────────────────────────────────┘
         ▲                    ▲                ▲
         │ TURN/STUN          │ WebSocket      │ HTTP
         │                    │ signaling      │ REST
    ┌────┴────────────────────┴────────────────┴────┐
    │           Android Client (Kotlin)              │
    │  ┌──────────┐ ┌──────────┐ ┌───────────────┐  │
    │  │ WebRTC   │ │ Jetpack  │ │  ViewModel +  │  │
    │  │ (P2P)    │ │ Compose  │ │  StateFlow    │  │
    │  └──────────┘ └──────────┘ └───────────────┘  │
    └────────────────────────────────────────────────┘
```

### Server Components (Docker Container)

1. **coturn** — TURN/STUN server for NAT traversal. Enables P2P connections even behind strict NATs (~15% of connections need TURN relay).
2. **Signaling Server** — WebSocket-based server for SDP offer/answer and ICE candidate exchange during call setup. Built with Ktor.
3. **REST API** — Manages users, servers, join requests, favorites. Built with Ktor + Kotlin.
4. **SQLite** — Lightweight database for user profiles, server metadata, join requests, and contacts.

### Android Client

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **WebRTC Library**: Stream WebRTC Android (`io.getstream:stream-webrtc-android`)
- **Architecture**: MVVM with ViewModel + StateFlow
- **Networking**: OkHttp (WebSocket), Ktor Client (REST API)
- **Serialization**: Kotlinx Serialization or Gson

---

## User Flows & Screens

### 1. Server Connection (Onboarding) — Invite Token System

**Connection is ONLY possible via invite tokens.** No direct IP/port entry.

**How it works (new account):**
1. Admin generates an invite token on the server (via admin panel or API)
2. Admin shares the token with the user (messenger, in person, etc.)
3. User pastes the token into the app → app parses server address + token code
4. Server validates token (not expired, not exhausted, not revoked)
5. User chooses: **"Создать аккаунт"** or **"Войти в существующий"**
6. **Create**: User fills profile (name, username, password) → account created
7. **Login**: User enters username + password → authenticated into existing account
8. If token has `requireApproval: true` (create only) → join request is sent, user waits for admin approval
9. If token has `requireApproval: false` (create only) → user is immediately added as a member

**Token format:** `server.example.com:3000/ABCD1234` (server address + short alphanumeric code)

**Note:** Invite token is required for BOTH creating and logging into accounts (protects against brute-force). Login does NOT consume token uses (currentUses not incremented).

**Password requirements:** Minimum 8 characters. Hashed server-side with BCrypt (cost factor 12). Rate limiting: 5 failed login attempts → 15 min lockout.

**Screen: "Подключение к серверу"**
- Single input field for invite token
- On submit: parse token → connect to server → validate → show auth choice

**Screen: "Добро пожаловать" (Auth Choice)**
- Server name displayed at top
- Two card-buttons:
  - "Создать аккаунт" → CreateProfileScreen
  - "Войти в существующий" → LoginScreen

**Screen: "Вход в аккаунт" (Login)**
- Username field (@-prefix)
- Password field (with visibility toggle)
- "Войти" button
- Error handling: wrong password, user not found, too many attempts

**Screen: "Заявка отправлена"** (only if token requires approval, create flow only)
- Confirmation that the request was sent, user waits for admin approval

**Admin: Token Generation (in Server Management)**
- Token name/label (for admin's reference, e.g., "Для команды дизайна")
- Max uses (how many people can join via this token; null = unlimited)
- Role granted: ADMIN or MEMBER
- Require admin approval: yes/no (protection against token leaks)
- Generated token displayed with copy/share buttons
- List of all tokens with usage stats, ability to revoke

### 2. Main Screen (Home)

**Screen: "CallApp" (Main)**

Two sections:
- **Избранные (Favorites)**: Quick-access contacts with online status indicators (green dot) and call button. Shows count badge.
- **Серверы (Servers)**: List of servers user is connected to, with server avatar, name, and username. Shows count badge. FAB button (+) to add/connect to new server.

### 3. Server Detail

**Screen: Server detail (e.g., "Tech Community")**

- Server info: avatar, name, username, description
- Admin badge ("Вы администратор") if current user is admin
- Search bar for members
- Member list with online status and call buttons
- Edit button (pencil icon) → enters delete mode for removing members (admin only)
- Navigation icons in toolbar: member requests, settings, add member

### 4. Join Requests Management (Admin Only)

**Screen: "Заявки на вступление"**

- List of pending requests with user avatar, username, date/time
- Accept (green "Принять") / Decline (red "Отклонить") buttons per request
- Count badge for new requests

### 5. Server Management (Admin Only)

**Screen: "Управление сервером"**

- Edit server avatar (camera icon overlay)
- Fields: server name*, username*, description*
- Server image URL field
- Save / Cancel buttons
- Danger zone (red): delete server

### 6. User Profile

**Screen: "Мой профиль"**

- Avatar, display name, username
- "Редактировать профиль" button
- Info card: name, username
- Server card: current server name + role (Администратор/Участник)

### 7. Other User Profile

**Screen: "Профиль пользователя"**

- Avatar, display name, username
- Add/Remove from favorites button (star icon)
- Server and role info

### 8. Outgoing / Active Call

**Screen: Call (dark navy background `#1A1F28`)**

- Top-left: call status ("Звонок...") + timer (00:23)
- Center: callee avatar (gold ring border) + name + status text
- Bottom bar: microphone toggle, video toggle, camera switch, end call (warm brown `#8B6F47`)
- Top-right: info button (connection quality)
- **TODO**: Active video call with remote video fullscreen + local video PiP

### 8b. Incoming Call

**Screen: Incoming call (dark navy background `#1A1F28`)**

- Center: caller avatar (gold ring border) + caller name
- Server name below the name (e.g. "Game Dev Hub")
- Call type badge: "Видеозвонок" (video call) or "Аудиозвонок" (audio call)
- Status text: "Входящий вызов..." (Incoming call...)
- Two action buttons at bottom:
  - **Отклонить** (Decline) — warm brown circle (`#8B6F47`) with crossed phone icon
  - **Принять** (Accept) — muted green circle (`#4A7C59`) with phone icon
- Helper text: "Нажмите кнопку для ответа на вызов"

### 9. Notifications

**Screen: "Уведомления"**

- Count of unread notifications
- "Очистить все" (Clear all) button
- Notification types with icons:
  - Заявка отклонена (declined) — red X
  - Заявка одобрена (approved) — green checkmark
  - Заявка отправлена (sent) — orange clock
- Each shows: server name + timestamp

### 10. Settings

**Screen: "Настройки"**

- Theme: Light "Old Money" (default) / Dark "Evening" — both fully implemented
- Status: В сети (Online), Не беспокоить (Do Not Disturb), Невидимый (Invisible)
- About: app name + version + description + GitHub link

---

## Data Models

### Server
```
Server {
    id: String (UUID)
    name: String
    username: String (unique, @-prefixed)
    description: String
    imageUrl: String?
    adminApiKey: String
    createdAt: Timestamp
}
```

### User (per server)
```
User {
    id: String (UUID)
    name: String
    username: String (unique per server, @-prefixed)
    passwordHash: String (BCrypt, cost 12)
    avatarUrl: String?
    role: Enum (ADMIN, MEMBER)
    status: Enum (ONLINE, DO_NOT_DISTURB, INVISIBLE)
    serverId: String (FK)
    createdAt: Timestamp
}
```

### InviteToken
```
InviteToken {
    id: String (UUID)
    token: String (unique, 8-12 chars, base62)
    label: String? (admin's note, e.g., "Для команды дизайна")
    serverId: String (FK)
    createdBy: String (FK → User, admin who created it)
    maxUses: Int? (null = unlimited)
    currentUses: Int (default 0)
    grantedRole: Enum (ADMIN, MEMBER) (default MEMBER)
    requireApproval: Boolean (default false)
    expiresAt: Timestamp? (null = never expires)
    isRevoked: Boolean (default false)
    createdAt: Timestamp
}
```

### JoinRequest
```
JoinRequest {
    id: String (UUID)
    username: String
    inviteTokenId: String (FK → InviteToken)
    serverId: String (FK)
    status: Enum (PENDING, APPROVED, DECLINED)
    createdAt: Timestamp
}
```

### Favorite
```
Favorite {
    userId: String (FK)
    favoriteUserId: String (FK)
}
```

### Notification
```
Notification {
    id: String (UUID)
    userId: String (FK)
    type: Enum (REQUEST_SENT, REQUEST_APPROVED, REQUEST_DECLINED, INCOMING_CALL, MISSED_CALL)
    serverName: String
    message: String
    isRead: Boolean
    createdAt: Timestamp
}
```

---

## API Endpoints (REST)

### Authentication
```
POST   /api/connect              — Connect to server via invite token { token: "ABCD1234" }
POST   /api/auth/login           — Login to existing account { invite_token, username, password } → AuthResponse
POST   /api/users                — Create user profile on server { name, username, password, avatar_url? }
```

### Invite Tokens (Admin Only)
```
POST   /api/invite-tokens        — Create invite token (label, maxUses, grantedRole, requireApproval, expiresAt)
GET    /api/invite-tokens        — List all tokens with usage stats
DELETE /api/invite-tokens/{id}   — Revoke token
```

### Users
```
GET    /api/users                — List all users on server
GET    /api/users/{id}           — Get user profile
PUT    /api/users/{id}           — Update user profile
DELETE /api/users/{id}           — Remove user from server (admin only)
```

### Server Management
```
GET    /api/server               — Get server info
PUT    /api/server               — Update server info (admin only)
DELETE /api/server               — Delete server (admin only)
```

### Join Requests
```
POST   /api/join-requests        — Submit join request
GET    /api/join-requests        — List pending requests (admin only)
PUT    /api/join-requests/{id}   — Approve/decline request (admin only)
```

### Favorites
```
GET    /api/favorites            — Get user's favorites
POST   /api/favorites/{userId}   — Add to favorites
DELETE /api/favorites/{userId}   — Remove from favorites
```

### Notifications
```
GET    /api/notifications        — Get user notifications
PUT    /api/notifications/read   — Mark all as read
DELETE /api/notifications        — Clear all notifications
```

---

## WebSocket Signaling Protocol

### Connection
```
ws://{server_ip}:8080/ws?token={sessionToken}
wss://{server_ip}:8080/ws?token={sessionToken}   # when server uses HTTPS
```

### Message Types
```json
// Call initiation
{ "type": "call_offer", "targetUserId": "...", "sdp": "..." }

// Call answer
{ "type": "call_answer", "targetUserId": "...", "sdp": "..." }

// ICE candidate exchange
{ "type": "ice_candidate", "targetUserId": "...", "candidate": "...", "sdpMid": "...", "sdpMLineIndex": 0 }

// Call control
{ "type": "call_end", "targetUserId": "..." }
{ "type": "call_decline", "targetUserId": "..." }
{ "type": "call_busy", "targetUserId": "..." }

// Presence
{ "type": "status_update", "userId": "...", "status": "online|dnd|invisible" }
```

---

## WebRTC Configuration

### ICE Servers
```kotlin
val iceServers = listOf(
    // Free STUN (NAT discovery)
    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),

    // Self-hosted TURN (relay fallback, from the same Docker container)
    PeerConnection.IceServer.builder("turn:{SERVER_IP}:3478")
        .setUsername("callapp")
        .setPassword("{generated_password}")
        .createIceServer()
)
```

### Call Flow
```
Caller                    Signaling Server                   Callee
  │                              │                              │
  │──── call_offer (SDP) ───────>│──── call_offer (SDP) ──────>│
  │                              │                              │
  │                              │<──── call_answer (SDP) ─────│
  │<──── call_answer (SDP) ─────│                              │
  │                              │                              │
  │──── ice_candidate ──────────>│──── ice_candidate ─────────>│
  │<──── ice_candidate ─────────│<──── ice_candidate ──────────│
  │                              │                              │
  │<═══════════ P2P Connection Established ═══════════════════>│
  │          (audio/video flows directly)                       │
```

---

## Tech Stack

### Server (Docker Container)
| Component | Technology |
|-----------|-----------|
| Runtime | JVM (Kotlin) |
| HTTP Framework | Ktor |
| WebSocket | Ktor WebSockets |
| Database | SQLite (via Exposed or SQLDelight) |
| TURN/STUN | coturn |
| Containerization | Docker (eclipse-temurin + gradle base) |

### Android Client
| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| Min SDK | 26 (Android 8.0) |
| UI | Jetpack Compose |
| WebRTC | io.getstream:stream-webrtc-android-compose |
| Navigation | Compose Navigation |
| State | ViewModel + StateFlow |
| HTTP Client | Ktor Client or Retrofit |
| WebSocket | OkHttp |
| DI | ServiceLocator (MVP), Hilt planned for Phase 3 |
| Image Loading | Coil |
| Serialization | Kotlinx Serialization |

---

## Project Structure

aleapp/               
├── android/          ← Android клиент (Jetpack Compose)
├── server/           ← Ktor signaling + REST API
├── docker/           ← Dockerfile, docker-compose, coturn конфиги
├── AGENTS.md
└── README.md


### Server
```
server/
├── Dockerfile
├── docker-compose.yml
├── build.gradle.kts
├── src/main/kotlin/
│   ├── Application.kt
│   ├── plugins/
│   │   ├── Routing.kt
│   │   ├── Serialization.kt
│   │   └── WebSockets.kt
│   ├── routes/
│   │   ├── UserRoutes.kt
│   │   ├── ServerRoutes.kt
│   │   ├── InviteTokenRoutes.kt
│   │   ├── JoinRequestRoutes.kt
│   │   ├── FavoriteRoutes.kt
│   │   └── NotificationRoutes.kt
│   ├── models/
│   │   ├── User.kt
│   │   ├── Server.kt
│   │   ├── InviteToken.kt
│   │   ├── JoinRequest.kt
│   │   └── Notification.kt
│   ├── database/
│   │   └── DatabaseFactory.kt
│   └── signaling/
│       └── SignalingManager.kt
├── turnserver.conf
└── data/
    └── callapp.db
```

### Android Client 
```

android/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   └── kotlin/com/callapp/android/
│       ├── CallApp.kt
│       ├── di/
│       │   └── AppModule.kt
│       ├── data/
│       │   ├── remote/
│       │   │   ├── ApiService.kt
│       │   │   └── SignalingClient.kt
│       │   ├── model/
│       │   │   ├── User.kt
│       │   │   ├── Server.kt
│       │   │   ├── InviteToken.kt
│       │   │   ├── JoinRequest.kt
│       │   │   └── Notification.kt
│       │   └── repository/
│       │       ├── UserRepository.kt
│       │       ├── ServerRepository.kt
│       │       └── CallRepository.kt
│       ├── webrtc/
│       │   ├── WebRTCManager.kt
│       │   ├── AudioManager.kt
│       │   └── CallState.kt
│       ├── ui/
│       │   ├── theme/
│       │   │   ├── Theme.kt
│       │   │   ├── Color.kt
│       │   │   └── Type.kt
│       │   ├── navigation/
│       │   │   └── NavGraph.kt
│       │   ├── screens/
│       │   │   ├── home/
│       │   │   │   ├── HomeScreen.kt
│       │   │   │   └── HomeViewModel.kt
│       │   │   ├── connect/
│       │   │   │   ├── ConnectScreen.kt
│       │   │   │   ├── AuthChoiceScreen.kt
│       │   │   │   ├── LoginScreen.kt
│       │   │   │   └── ConnectViewModel.kt
│       │   │   ├── server/
│       │   │   │   ├── ServerDetailScreen.kt
│       │   │   │   ├── ServerManageScreen.kt
│       │   │   │   └── ServerViewModel.kt
│       │   │   ├── joinrequest/
│       │   │   │   ├── JoinRequestsScreen.kt
│       │   │   │   └── JoinRequestViewModel.kt
│       │   │   ├── profile/
│       │   │   │   ├── MyProfileScreen.kt
│       │   │   │   ├── UserProfileScreen.kt
│       │   │   │   └── ProfileViewModel.kt
│       │   │   ├── call/
│       │   │   │   ├── OutgoingCallScreen.kt
│       │   │   │   ├── IncomingCallScreen.kt
│       │   │   │   ├── ActiveCallScreen.kt
│       │   │   │   └── CallViewModel.kt
│       │   │   ├── notifications/
│       │   │   │   ├── NotificationsScreen.kt
│       │   │   │   └── NotificationsViewModel.kt
│       │   │   └── settings/
│       │   │       ├── SettingsScreen.kt
│       │   │       └── SettingsViewModel.kt
│       │   └── components/
│       │       ├── UserCard.kt
│       │       ├── ServerCard.kt
│       │       ├── StatusIndicator.kt
│       │       └── CallControls.kt
│       └── util/
│           ├── Constants.kt
│           └── Extensions.kt
```

---

## Design System — "Old Money" Aesthetic

### Theme: Light (Old Money)
| Token | Value | Usage |
|-------|-------|-------|
| Background | `#F8F6F1` | Warm cream, main background |
| Foreground | `#2C3E50` | Dark navy graphite, primary text |
| Card | `#FFFFFF` | Cards, sections |
| Primary | `#1B3A52` | Deep navy — buttons, links, accents |
| Secondary | `#E8E3D5` | Light beige — secondary surfaces |
| Muted | `#D4CCBB` | Muted beige — disabled states |
| Accent | `#C9B896` | Gold-beige — highlights, badges |
| Destructive | `#8B4513` | Restrained brown — danger zone |
| InputBg | `#F5F1E8` | Warm input field backgrounds |
| StatusOnline | `#4A7C59` | Muted green |
| StatusBusy | `#8B6F47` | Muted brown |
| StatusOffline | `#8B8B8B` | Gray |
| CallAccept | `#4A7C59` | Elegant green |
| CallDecline | `#8B6F47` | Elegant brown |
| Border | `rgba(44, 62, 80, 0.12)` | Subtle borders |

### Theme: Dark (Evening)
| Token | Value | Usage |
|-------|-------|-------|
| Background | `#1A1F28` | Deep dark blue |
| Foreground | `#E8E3D5` | Light beige text |
| Card | `#232933` | Dark with blue tint |
| Primary | `#C9B896` | Gold-beige — buttons, accents |
| Secondary | `#2A313D` | Dark blue surfaces |
| Muted | `#3A4250` | Medium blue — disabled |
| Accent | `#A89968` | Muted gold — highlights |
| Destructive | `#9D7C5A` | Warm brown — danger |
| InputBg | `#2A313D` | Dark input backgrounds |
| StatusOnline | `#5A9670` | Green |
| StatusBusy | `#A89968` | Gold |
| StatusOffline | `#6B7280` | Gray |
| CallAccept | `#5A9670` | Green |
| CallDecline | `#9D7C5A` | Warm brown |
| Border | `rgba(201, 184, 150, 0.15)` | Subtle gold borders |

### Android Color Mapping (Material 3)
```kotlin
// Light theme
val md_theme_light_primary = Color(0xFF1B3A52)
val md_theme_light_onPrimary = Color(0xFFF8F6F1)
val md_theme_light_background = Color(0xFFF8F6F1)
val md_theme_light_surface = Color(0xFFFFFFFF)
val md_theme_light_onSurface = Color(0xFF2C3E50)
val md_theme_light_secondary = Color(0xFFE8E3D5)
val md_theme_light_accent = Color(0xFFC9B896)

// Dark theme
val md_theme_dark_primary = Color(0xFFC9B896)
val md_theme_dark_onPrimary = Color(0xFF1A1F28)
val md_theme_dark_background = Color(0xFF1A1F28)
val md_theme_dark_surface = Color(0xFF232933)
val md_theme_dark_onSurface = Color(0xFFE8E3D5)
val md_theme_dark_secondary = Color(0xFF2A313D)
val md_theme_dark_accent = Color(0xFFA89968)
```

### Typography
- Font: System default (Roboto on Android)
- Base size: 16sp
- Headers: Medium weight (500), 18-24sp
- Body: Normal weight (400), 14-16sp
- Caption: Normal weight, 12sp (usernames, timestamps)

### Component Patterns
- Cards with rounded corners (12dp radius), no harsh shadows — subtle `rgba` borders
- Online status: green dot (12dp) positioned bottom-left of avatar
- Avatars: circular, 48dp (list items), 96dp (profile), 120dp (call screen)
- Call screen avatars have gold ring border (accent color)
- Buttons: rounded (12dp radius), filled primary or outlined
- No bright/neon colors — all colors are muted and warm
- Call screen uses dark navy background (`#1A1F28`) regardless of current theme
- Incoming call: two large circular buttons (decline brown, accept green) with icons

---

## Docker Deployment

### docker-compose.yml structure
```yaml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "3000:3000"
      - "8080:8080"
    volumes:
      - ./data:/app/data
    environment:
      - ADMIN_API_KEY=${ADMIN_API_KEY}
      - TURN_SECRET=${TURN_SECRET}

  coturn:
    image: coturn/coturn
    ports:
      - "3478:3478"
      - "3478:3478/udp"
      - "49152-65535:49152-65535/udp"
    volumes:
      - ./turnserver.conf:/etc/turnserver.conf
```

### Installation (target UX)
```bash
curl -sSL https://callapp.example/install.sh | bash
```

---

## Development Priorities

### Phase 1 — MVP
1. Server: REST API (users, server info, join requests)
2. Server: WebSocket signaling
3. Server: Docker container with coturn
4. Client: Connect to server screen
5. Client: Profile creation
6. Client: Home screen (servers + favorites)
7. Client: Server detail with member list
8. Client: Audio calling (WebRTC)
9. Client: Incoming/outgoing call screens

### Phase 2 — Core Features
1. Video calling
2. Join request management (admin)
3. Server management (admin)
4. Notifications
5. Favorites system
6. User status (online/DND/invisible)

### Phase 3 — Polish
1. Dark theme
2. Push notifications (via FCM or WebSocket)
3. Call history
4. Connection quality indicator
5. Auto-reconnect on network change
6. End-to-end encryption

---

## Key Technical Decisions

- **No central registration**: Users create profiles per-server. No global accounts.
- **Server-scoped identity**: Username uniqueness is per-server, not global.
- **Cross-server contacts**: Users can be on multiple servers. Favorites aggregate contacts across servers.
- **API key = first admin**: Whoever has the API key (generated at server deploy) gets initial admin rights. Further users join only via invite tokens.
- **Invite tokens only**: No direct IP connection. Admin generates tokens with configurable: label, max uses, granted role (admin/member), require approval flag. Protects against token leaks via optional approval step.
- **SQLite over Postgres**: Lightweight, no extra service. Sufficient for small groups (10-50 users per server).
- **coturn in same container**: Simplifies deployment. Single Docker container = complete server.

---

## Important Constraints

- Target audience: tech-savvy users who can rent a VPS and run Docker
- Server expected group size: 5-50 members
- Languages: UI is in Russian (primary), English support planned
- Android only (no iOS planned for MVP)
- No message/chat functionality — this is a calling-only app

## Code Conventions

- **Previews**: Every Compose UI component and screen MUST include `@Preview` composables (light + dark theme at minimum). Add previews for individual components, not just full screens.
- **Commit messages**: Use Conventional Commits format — `type: description`. Types: `feat`, `fix`, `refactor`, `style`, `docs`, `chore`, `test`. No `Co-Authored-By`. Description in English, body as bullet list of changes.

## Docker Image Publishing

- When the user asks to build and push the backend image, always build and push `dmitri1000/aleapp:latest`.
- Unless the user asks otherwise, the image should be built from `server/Dockerfile`.
- When backend code changes are made, rebuild and push the backend container image even if the user did not explicitly remind about `latest`.
- Do not create or push version tags unless the user explicitly asks for a specific additional tag.

---

## Actual Repository State (March 2026)

This document above mostly describes the target product vision. The repository itself already contains a more concrete implementation with some important differences and additions that should be taken into account before making changes.

### Current Top-Level Structure

The actual repository layout is currently:

```text
aleapp/
├── android/                 # Android Studio project
│   └── app/                 # Main Android application module
├── server/                  # Ktor backend + Docker deployment assets
├── AGENTS.md
├── CLAUDE.md
└── README.md
```

There is no separate top-level `docker/` directory in the current repo state. Docker-related files live inside `server/`.

### Android Client: What Is Actually Implemented

- Android app module path: `android/app`
- Package namespace: `com.callapp.android`
- Build setup: AGP `9.1.0`, Kotlin `2.2.10`, Compose BOM `2024.09.00`
- SDK levels: `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`
- Networking: Ktor Client `3.1.1` with OkHttp engine
- WebRTC: `io.getstream:stream-webrtc-android` and `stream-webrtc-android-compose`
- Image loading: Coil 3
- Navigation: `androidx.navigation.compose`

The implemented Android source tree differs from the older idealized example in this document. Important actual packages:

- `android/app/src/main/java/com/callapp/android/data` — repositories, session store, service locator
- `android/app/src/main/java/com/callapp/android/network` — REST client, DTOs, server connection manager
- `android/app/src/main/java/com/callapp/android/network/signaling` — WebSocket signaling client and message models
- `android/app/src/main/java/com/callapp/android/ui/screens` — Compose screens
- `android/app/src/main/java/com/callapp/android/calling` — foreground service and boot receiver for incoming calls
- `android/app/src/main/java/com/callapp/android/webrtc` — WebRTC factory and manager

Implemented screens/routes currently include:

- Home
- Add server / token input
- Auth choice
- Create profile
- Login
- Pending approval
- Server detail
- Join requests
- Server management
- Invite tokens management
- My profile
- User profile
- Notifications
- Settings
- Outgoing/active call
- Incoming call

### Android Runtime Behavior

- The app stores multiple server sessions locally via `SessionStore`
- `ServiceLocator` tracks the active server and current user
- There is a foreground service `CallAvailabilityService` that restores saved signaling sessions and listens for incoming calls across connected servers
- `CallBootReceiver` restarts call availability handling after device reboot
- Incoming calls are surfaced through high-priority/full-screen notifications
- App manifest already requests the permissions needed for internet, audio, camera, notifications, foreground service, boot completed, and Bluetooth call scenarios

### Backend: What Is Actually Implemented

The backend is already more than a scaffold. It contains:

- Ktor `3.1.1`
- Kotlin/JVM `2.2.10`
- JVM toolchain `21`
- SQLite via `org.xerial:sqlite-jdbc`
- HikariCP connection pooling
- JWT auth via `com.auth0:java-jwt`
- BCrypt password hashing
- WebSocket signaling
- Database migrations in `server/src/main/resources/db/migration`

Important backend packages:

- `com.callapp.server.config` — config mapping from `application.conf`
- `com.callapp.server.database` — datasource, migrations, bootstrap, health probe
- `com.callapp.server.auth` — JWT services and principals
- `com.callapp.server.repository` — JDBC repositories
- `com.callapp.server.service` — onboarding, management, invite token, password, TURN credentials
- `com.callapp.server.signaling` — signaling manager and WebSocket message models
- `com.callapp.server.plugins` — Ktor plugin wiring

### Backend Endpoints That Exist Right Now

In addition to the product-level API list above, the current server implementation exposes:

- `GET /` — simple service status payload
- `GET /health` — health endpoint with database connectivity status
- `GET /api/turn-credentials` — temporary TURN credentials for authenticated users
- `GET /ws?token=...` — authenticated signaling WebSocket

Important current backend behavior differences:

- `POST /api/connect` returns a guest session JWT for a valid invite token
- `POST /api/users` currently requires that guest JWT in `Authorization: Bearer ...`
- `POST /api/users` returns `200 OK` with a user DTO for immediate join, or `202 Accepted` with auth/session payload when approval is required
- `DELETE /api/server` is intentionally disabled in the current build and returns `410 Gone`
- `POST /api/join-requests` is deprecated in the current build; pending approval is handled through `POST /api/users`
- Signaling WebSocket currently accepts only authenticated user tokens, not guest tokens

### Current Auth / Session Model

The repo now uses a two-step onboarding/auth model:

1. `POST /api/connect` validates invite token and returns a guest session token.
2. That guest token is then used to create a new user via `POST /api/users`.
3. Existing users log in via `POST /api/auth/login` and receive a user session token.
4. Authenticated user session tokens are required for management APIs and WebSocket signaling.

This is important: the Android client already assumes JWT-based sessions, not just raw invite-token flows.

### TURN / Calling Notes

- TURN credentials are generated server-side by `TurnCredentialsService`
- Android `CallRepository` requests TURN credentials dynamically from `/api/turn-credentials`
- Signaling tests confirm SDP relay between connected users
- When the callee is offline, the server currently records a `MISSED_CALL` notification for that user

### Deployment Reality

Current deployment assets live in `server/`:

- `server/Dockerfile`
- `server/docker-compose.yml`
- `server/turnserver.conf`
- `server/install.sh`
- `server/.env.example`

The install script is Linux-oriented and:

- installs Docker if needed
- writes `.env`, `docker-compose.yml`, and `turnserver.conf`
- pulls `dmitri1000/aleapp:latest`
- generates bootstrap secrets automatically when omitted
- creates and prints the initial admin invite token
- supports update-in-place or destructive reinstall with explicit confirmation

### Test Deployment Server

- Test backend deployments may be performed on the dedicated server `144.31.181.69` over SSH port `12234`.
- SSH user for this server is `ivanzolo`.
- From this Windows workspace, prefer running SSH through WSL because the configured key lives there.
- Preferred connection pattern:

```bash
wsl ssh -p 12234 ivanzolo@144.31.181.69
```

- Docker Engine and Docker Compose plugin are already installed on that server from the official Docker Ubuntu repository.
- User `ivanzolo` is already in the `docker` group, so Docker commands should work without `sudo` in a fresh SSH session.
- Before deploying backend changes, first verify the backend locally when feasible:
  - on Windows: `cd server` then `gradlew.bat test` and `gradlew.bat build`
- For backend deployment requests, unless the user says otherwise:
  - build and push `dmitri1000/aleapp:latest` from `server/Dockerfile`
  - connect to `ivanzolo@144.31.181.69:12234`
  - pull the latest image on the server
  - restart the backend using the deployment assets in `server/`
  - verify the container is healthy after restart
- After remote deployment, always perform a basic smoke check, at minimum:
  - `docker ps`
  - backend container logs
  - health check against `/health` when the service is reachable
- If a deployment requires secrets or env changes that are not present locally, inspect the existing server-side deployment files first and avoid destructive replacement unless the user explicitly requests reinstall/reset.

### Tests Already Present

Server tests currently cover:

- health/application startup
- onboarding flow
- management routes
- signaling routes
- bootstrap admin invite token seeding
- JWT service
- TURN credentials service

Android unit tests currently cover:

- API error mapping in `ApiClient`
- ConnectViewModel error mapping

### Practical Commands

Useful commands for working in the current repo:

```bash
# Android
cd android
./gradlew testDebugUnitTest
./gradlew assembleDebug

# Server
cd server
./gradlew test
./gradlew run
./gradlew build
```

On Windows in this workspace, use `gradlew.bat` instead of `./gradlew`.

### Guidance For Future Edits

- Prefer the actual file/package layout over the older illustrative tree in this document
- Treat the design reference as the visual source of truth, but treat `android/app` and `server/src/main` as the implementation source of truth
- When documenting or changing the onboarding flow, account for guest JWT sessions and the current `POST /api/users` behavior
- When documenting server APIs, include `/health`, `/api/turn-credentials`, and the currently disabled/deprecated endpoints
