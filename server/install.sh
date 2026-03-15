#!/usr/bin/env bash

set -euo pipefail

APP_IMAGE_DEFAULT="dmitri1000/aleapp:latest"
INSTALL_DIR_DEFAULT="$(pwd)"
PUBLIC_HOST_DEFAULT="localhost"
HTTP_PORT_DEFAULT="3000"
TURN_PORT_DEFAULT="3478"
TURN_REALM_DEFAULT="callapp"
SERVER_NAME_DEFAULT="CallApp Server"
SERVER_USERNAME_DEFAULT="@callapp"
SERVER_DESCRIPTION_DEFAULT="Self-hosted CallApp backend"
SERVER_IMAGE_URL_DEFAULT=""
BOOTSTRAP_LABEL_DEFAULT="Initial admin invite"
BOOTSTRAP_MAX_USES_DEFAULT="1"
AUTO_GENERATE_LABEL="auto"

if [[ "$(uname -s)" != "Linux" ]]; then
  echo "Этот скрипт рассчитан на Linux-сервер."
  exit 1
fi

if [[ "${EUID}" -eq 0 ]]; then
  SUDO=""
else
  SUDO="sudo"
fi

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

read_from_tty() {
  local answer
  local prompt_text="$1"
  read -r -p "$prompt_text" answer </dev/tty
  printf '%s' "$answer"
}

print_to_tty() {
  printf '%s\n' "$1" >/dev/tty
}

prompt() {
  local label="$1"
  local default_value="${2-}"
  local answer

  if [[ -n "$default_value" ]]; then
    answer="$(read_from_tty "$label (Enter = $default_value): ")"
    if [[ -z "$answer" ]]; then
      answer="$default_value"
    fi
  else
    answer="$(read_from_tty "$label: ")"
  fi

  printf '%s' "$answer"
}

random_alnum() {
  local length="$1"

  if command_exists openssl; then
    openssl rand -base64 64 | tr -dc 'A-Za-z0-9' | head -c "$length"
    return
  fi

  tr -dc 'A-Za-z0-9' </dev/urandom | head -c "$length"
}

random_hex() {
  local length="$1"

  if command_exists openssl; then
    openssl rand -hex "$((length / 2))" | head -c "$length"
    return
  fi

  tr -dc 'a-f0-9' </dev/urandom | head -c "$length"
}

random_uuid() {
  if command_exists uuidgen; then
    uuidgen | tr '[:upper:]' '[:lower:]'
    return
  fi

  local hex
  hex="$(random_hex 32)"
  printf '%s-%s-%s-%s-%s' \
    "${hex:0:8}" "${hex:8:4}" "${hex:12:4}" "${hex:16:4}" "${hex:20:12}"
}

normalize_username() {
  local username="$1"
  if [[ "$username" != @* ]]; then
    username="@$username"
  fi
  printf '%s' "$username"
}

env_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

ensure_docker() {
  if command_exists docker && docker compose version >/dev/null 2>&1; then
    echo "Docker и docker compose уже доступны."
    return
  fi

  if ! command_exists apt; then
    echo "Docker не найден, а apt в системе недоступен."
    echo "Установите Docker вручную и запустите скрипт повторно."
    exit 1
  fi

  echo "Docker не найден. Устанавливаю через apt."
  $SUDO apt update
  $SUDO apt install -y ca-certificates curl
  $SUDO install -m 0755 -d /etc/apt/keyrings
  $SUDO curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  $SUDO chmod a+r /etc/apt/keyrings/docker.asc

  cat <<EOF | $SUDO tee /etc/apt/sources.list.d/docker.sources >/dev/null
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Signed-By: /etc/apt/keyrings/docker.asc
EOF

  $SUDO apt update
  $SUDO apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  $SUDO systemctl enable --now docker
  $SUDO systemctl status docker --no-pager || true
}

autogen_hint() {
  printf 'Enter = %s' "$AUTO_GENERATE_LABEL"
}

prompt_or_auto() {
  local label="$1"
  local answer
  answer="$(read_from_tty "$label (Enter = сгенерировать автоматически): ")"
  printf '%s' "$answer"
}

installation_exists() {
  local compose_dir="$1"
  [[ -f "$compose_dir/docker-compose.yml" || -f "$compose_dir/.env" || -d "$compose_dir/data" ]]
}

prompt_existing_install_action() {
  local choice

  print_to_tty ""
  print_to_tty "Найдена уже существующая установка CallApp."
  print_to_tty "1) Обновить контейнеры до версии из этого скрипта с сохранением текущих данных."
  print_to_tty "2) Полностью снести старый сервер и установить заново."

  while true; do
    choice="$(read_from_tty "Выберите вариант (1/2): ")"
    case "$choice" in
      1|2)
        printf '%s' "$choice"
        return
        ;;
      *)
        print_to_tty "Введите 1 или 2."
        ;;
    esac
  done
}

confirm_full_reinstall() {
  local confirmation

  print_to_tty ""
  print_to_tty "ВНИМАНИЕ: это полностью удалит все данные старого сервера."
  print_to_tty "Будут удалены база данных, invite-токены и конфигурация старой установки."
  confirmation="$(read_from_tty "Чтобы подтвердить полную переустановку, введите DELETE: ")"

  if [[ "$confirmation" != "DELETE" ]]; then
    echo "Переустановка отменена."
    exit 1
  fi
}

set_env_value() {
  local env_file="$1"
  local key="$2"
  local value="$3"
  local escaped_value

  escaped_value="$(printf '%s' "$value" | sed 's/[\/&]/\\&/g')"

  if grep -q "^${key}=" "$env_file"; then
    $SUDO sed -i "s/^${key}=.*/${key}=${escaped_value}/" "$env_file"
  else
    printf '%s=%s\n' "$key" "$value" | $SUDO tee -a "$env_file" >/dev/null
  fi
}

update_existing_installation() {
  local compose_dir="$1"
  local compose_file="$compose_dir/docker-compose.yml"
  local env_file="$compose_dir/.env"
  local app_image_value

  if [[ ! -f "$compose_file" || ! -f "$env_file" ]]; then
    echo "Для обновления нужны $compose_file и $env_file."
    echo "Если этих файлов нет, запустите полную переустановку."
    exit 1
  fi

  app_image_value="\"$(env_escape "$APP_IMAGE_DEFAULT")\""
  set_env_value "$env_file" "APP_IMAGE" "$app_image_value"

  echo "Обновляю контейнеры с сохранением данных."
  $SUDO docker compose -f "$compose_file" --env-file "$env_file" pull
  $SUDO docker compose -f "$compose_file" --env-file "$env_file" up -d --remove-orphans

  echo
  echo "Обновление завершено."
  echo "Каталог установки: $compose_dir"
  echo "Backend image: $APP_IMAGE_DEFAULT"
  exit 0
}

remove_existing_installation() {
  local compose_dir="$1"
  local compose_file="$compose_dir/docker-compose.yml"
  local env_file="$compose_dir/.env"

  if [[ -f "$compose_file" && -f "$env_file" ]]; then
    $SUDO docker compose -f "$compose_file" --env-file "$env_file" down --remove-orphans || true
  fi

  $SUDO rm -rf "$compose_dir/data"
  $SUDO rm -f "$compose_dir/.env" "$compose_dir/docker-compose.yml" "$compose_dir/turnserver.conf"
}

handle_existing_installation() {
  local compose_dir="$1"
  local existing_action

  existing_action="$(prompt_existing_install_action)"
  if [[ "$existing_action" == "1" ]]; then
    update_existing_installation "$compose_dir"
  fi

  confirm_full_reinstall
  remove_existing_installation "$compose_dir"
}

echo "=== CallApp backend installer ==="

ensure_docker

if installation_exists "$INSTALL_DIR_DEFAULT"; then
  install_dir="$INSTALL_DIR_DEFAULT"
  handle_existing_installation "$install_dir"
else
  install_dir="$(prompt "Куда установить backend" "$INSTALL_DIR_DEFAULT")"
  if installation_exists "$install_dir"; then
    handle_existing_installation "$install_dir"
  fi
fi

public_host="$(prompt "Публичный домен или IP сервера (без http://)" "$PUBLIC_HOST_DEFAULT")"
public_host="${public_host#http://}"
public_host="${public_host#https://}"
public_host="${public_host%/}"

http_port="$(prompt "HTTP порт backend" "$HTTP_PORT_DEFAULT")"
turn_port="$(prompt "TURN порт" "$TURN_PORT_DEFAULT")"
server_name="$SERVER_NAME_DEFAULT"
server_username="$(normalize_username "$SERVER_USERNAME_DEFAULT")"
server_description="$SERVER_DESCRIPTION_DEFAULT"
server_image_url="$SERVER_IMAGE_URL_DEFAULT"
turn_realm="$TURN_REALM_DEFAULT"
app_image="$APP_IMAGE_DEFAULT"

jwt_secret="$(prompt_or_auto "JWT secret")"
if [[ -z "$jwt_secret" ]]; then
  jwt_secret="$(random_hex 64)"
fi

turn_secret="$(prompt_or_auto "TURN secret")"
if [[ -z "$turn_secret" ]]; then
  turn_secret="$(random_hex 64)"
fi

server_id="$(prompt_or_auto "SERVER_ID")"
if [[ -z "$server_id" ]]; then
  server_id="$(random_uuid)"
fi

bootstrap_admin_token="$(prompt_or_auto "Админ invite token code")"
if [[ -z "$bootstrap_admin_token" ]]; then
  bootstrap_admin_token="$(random_alnum 10)"
fi

bootstrap_admin_label="$(prompt "Подпись для стартового админского инвайта" "$BOOTSTRAP_LABEL_DEFAULT")"
bootstrap_admin_max_uses="$(prompt "Сколько раз можно использовать стартовый админский инвайт" "$BOOTSTRAP_MAX_USES_DEFAULT")"

compose_dir="$install_dir"
data_dir="$compose_dir/data"

$SUDO mkdir -p "$data_dir"

cat <<EOF | $SUDO tee "$compose_dir/.env" >/dev/null
APP_ENV=production
HTTP_PORT=$http_port
DB_PATH=/app/data/callapp.db
JWT_SECRET=$jwt_secret
JWT_ISSUER=callapp-server
JWT_AUDIENCE=callapp-clients
TURN_SECRET=$turn_secret
TURN_HOST="$(env_escape "$public_host")"
TURN_PORT=$turn_port
TURN_REALM="$(env_escape "$turn_realm")"
SERVER_ID="$(env_escape "$server_id")"
SERVER_NAME="$(env_escape "$server_name")"
SERVER_USERNAME="$(env_escape "$server_username")"
SERVER_DESCRIPTION="$(env_escape "$server_description")"
SERVER_IMAGE_URL="$(env_escape "$server_image_url")"
BOOTSTRAP_ADMIN_TOKEN="$(env_escape "$bootstrap_admin_token")"
BOOTSTRAP_ADMIN_TOKEN_LABEL="$(env_escape "$bootstrap_admin_label")"
BOOTSTRAP_ADMIN_TOKEN_MAX_USES=$bootstrap_admin_max_uses
APP_IMAGE="$(env_escape "$app_image")"
EOF

cat <<EOF | $SUDO tee "$compose_dir/docker-compose.yml" >/dev/null
services:
  app:
    image: \${APP_IMAGE}
    restart: unless-stopped
    ports:
      - "\${HTTP_PORT}:3000"
    env_file:
      - .env
    volumes:
      - ./data:/app/data
    depends_on:
      - coturn

  coturn:
    image: coturn/coturn:4.6.3
    restart: unless-stopped
    network_mode: host
    command: ["-c", "/etc/coturn/turnserver.conf"]
    volumes:
      - ./turnserver.conf:/etc/coturn/turnserver.conf:ro
EOF

cat <<EOF | $SUDO tee "$compose_dir/turnserver.conf" >/dev/null
listening-port=$turn_port
fingerprint
use-auth-secret
static-auth-secret=$turn_secret
realm=$turn_realm
total-quota=100
bps-capacity=0
stale-nonce=600
no-cli
no-tls
no-dtls
log-file=stdout
EOF

echo "Пулю контейнеры и запускаю стек."
$SUDO docker compose -f "$compose_dir/docker-compose.yml" --env-file "$compose_dir/.env" pull
$SUDO docker compose -f "$compose_dir/docker-compose.yml" --env-file "$compose_dir/.env" up -d

admin_invite="$public_host:$http_port/$bootstrap_admin_token"

echo
echo "Установка завершена."
echo "Каталог установки: $compose_dir"
echo "Backend image: $app_image"
echo "Админский invite token: $admin_invite"
echo
echo "Сохраните этот invite token: через него создаётся первый администратор."
