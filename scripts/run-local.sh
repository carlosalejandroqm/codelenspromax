#!/usr/bin/env bash
# Compila y levanta los 5 microservicios de CodeLens en segundo plano.
#
#   ./scripts/run-local.sh              compila y arranca
#   ./scripts/run-local.sh --skip-build arranca con los jars ya compilados
#
# Logs en logs/<servicio>.log, datos (H2 + workspaces) en data/.
# Para detenerlos: ./scripts/stop-local.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Spring Boot 3.3 / Hibernate requieren Java 21; JDKs mas nuevos fallan al arrancar.
java_major() { "$1/bin/java" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/'; }
if [[ -z "${JAVA_HOME:-}" || "$(java_major "$JAVA_HOME")" != "21" ]]; then
  for candidate in \
    /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
    /usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
    "$(/usr/libexec/java_home -v 21 2>/dev/null || true)"; do
    if [[ -n "$candidate" && -x "$candidate/bin/java" ]]; then
      export JAVA_HOME="$candidate"
      break
    fi
  done
fi
if [[ -z "${JAVA_HOME:-}" || "$(java_major "$JAVA_HOME")" != "21" ]]; then
  echo "No se encontro Java 21. Instala con: brew install openjdk@21" >&2
  exit 1
fi
export PATH="$JAVA_HOME/bin:$PATH"
echo "Usando Java 21 en $JAVA_HOME"

if [[ "${1:-}" != "--skip-build" ]]; then
  echo "Compilando los modulos (mvn package)..."
  mvn -q -DskipTests package
fi

mkdir -p logs data
JAVA_OPTS="${JAVA_OPTS:--Xms64m -Xmx512m}"

# servicio:puerto, en orden de dependencia (el gateway al final).
SERVICES=(user-service:8084 source-service:8081 analysis-service:8082 report-service:8083 gateway:8080)

for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  pid_file="logs/$name.pid"
  if [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
    echo "  $name ya esta corriendo (pid $(cat "$pid_file"))"
    continue
  fi
  # shellcheck disable=SC2086
  nohup java $JAVA_OPTS -jar "$name/target/$name.jar" > "logs/$name.log" 2>&1 &
  echo $! > "$pid_file"
  echo "  $name arrancando (pid $!)"
done

echo "Esperando a que respondan /actuator/health..."
for entry in "${SERVICES[@]}"; do
  name="${entry%%:*}"
  port="${entry##*:}"
  for _ in $(seq 1 90); do
    if curl -fsS "http://localhost:$port/actuator/health" 2>/dev/null | grep -q '"UP"'; then
      printf '  %-17s UP  http://localhost:%s\n' "$name" "$port"
      continue 2
    fi
    if ! kill -0 "$(cat "logs/$name.pid")" 2>/dev/null; then
      echo "  $name se detuvo. Revisa logs/$name.log" >&2
      tail -20 "logs/$name.log" >&2
      exit 1
    fi
    sleep 2
  done
  echo "  $name no respondio a tiempo. Revisa logs/$name.log" >&2
  exit 1
done

echo
echo "Listo. API Gateway en http://localhost:8080"
echo "Frontend: cd frontend && npm install && npm start  ->  http://localhost:4200"
