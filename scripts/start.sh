#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

current_port=$(docker compose port frontend 80 2>/dev/null | sed -n 's/.*:\([0-9][0-9]*\)$/\1/p' || true)

port_available() {
  [ "$1" = "$current_port" ] || ! (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

if [ -z "${FRONTEND_PORT:-}" ]; then
  if port_available 4200; then
    FRONTEND_PORT=4200
  elif port_available 4300; then
    FRONTEND_PORT=4300
  else
    echo "Ports 4200 and 4300 are both in use; set FRONTEND_PORT to a free port." >&2
    exit 1
  fi
fi

export FRONTEND_PORT
docker compose up -d --build "$@"
echo "Frontend: http://localhost:$FRONTEND_PORT"
