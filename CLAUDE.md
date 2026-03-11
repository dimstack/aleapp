# CallApp — Decentralized P2P Calling Application

## Communication
- Общение с пользователем ведётся на **русском языке** (пояснения, комментарии, описания).

## Project Overview

CallApp is a decentralized voice and video calling application for Android built on the **BYOS (Bring Your Own Server)** architecture. Each user (or group admin) deploys their own server instance via a Docker container on a VPS, creating small trusted communities for censorship-resistant communication. Think of it as "WhatsApp calling, but each group runs on its own server."

### Core Philosophy
- **Decentralized**: No central authority. Each server is independent.
- **Censorship-resistant**: Anyone can spin up a server; there's no single point to block.
- **Small trusted groups**: Server admin controls access via invite tokens (with optional approval).
- **BYOS**: Bring Your Own Server — users deploy their own infrastructure.
- **Easy deployment**: One-click Docker install, similar to Amnezia VPN's approach.

### Design Reference

The repository includes a web-based design prototype in the `design-reference/` folder, built with React + TypeScript + Tailwind CSS. **All UI screens must closely follow these mockups** — layout, spacing, colors, typography. When implementing any screen, always reference the prototype first.

**Screen components** (`design-reference/src/app/components/`):
- `AddServerScreen.tsx` — connect to server form
- `CallScreen.tsx` — outgoing/active call
- `IncomingCallScreen.tsx` — incoming call with accept/decline
- `ContactCard.tsx` — favorite contact card
- `AuthChoiceScreen.tsx` — choose create account or login
- `CreateProfileScreen.tsx` — profile creation on server (with password)
- `LoginScreen.tsx` — login to existing account (username + password)
- `Header.tsx` — top app bar
- `JoinRequestsScreen.tsx` — admin join request management
- `NotificationsScreen.tsx` — notifications list
- `PendingRequestScreen.tsx` — application submitted confirmation
- `ProfileScreen.tsx` — my profile
- `ServerCard.tsx` — server list item
- `ServerManagementScreen.tsx` — server settings (admin)
- `ServerScreen.tsx` — server detail with members
- `SettingsScreen.tsx` — app settings (theme, status, about)
- `UserProfileScreen.tsx` — other user's profile

**Theme** (`design-reference/src/styles/`):
- `theme.css` — complete color scheme as CSS variables for both Light ("Old Money") and Dark ("Evening") themes

**UI components** (`design-reference/src/app/components/ui/`):
- Full set of reusable shadcn/ui components (button, card, input, avatar, badge, switch, etc.)

**Guidelines** (`design-reference/guidelines/`):
- `Guidelines.md` — design guidelines document

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
ws://{server_ip}:8080/ws?userId={userId}
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
| DI | Hilt or Koin |
| Image Loading | Coil |
| Serialization | Kotlinx Serialization |

---

## Project Structure

aleapp/               
├── android/          ← Android клиент (Jetpack Compose)
├── server/           ← Ktor signaling + REST API
├── docker/           ← Dockerfile, docker-compose, coturn конфиги
├── design-reference/ ← TSX из Figma
├── CLAUDE.md
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
│   └── kotlin/com/callapp/
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