#!/usr/bin/env bash
# Detiene los microservicios levantados con run-local.sh.
set -uo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

shopt -s nullglob
pid_files=(logs/*.pid)
if [[ ${#pid_files[@]} -eq 0 ]]; then
  echo "No hay servicios registrados en logs/*.pid"
  exit 0
fi

for pid_file in "${pid_files[@]}"; do
  name="$(basename "$pid_file" .pid)"
  pid="$(cat "$pid_file")"
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" && echo "  $name detenido (pid $pid)"
  else
    echo "  $name no estaba corriendo"
  fi
  rm -f "$pid_file"
done
