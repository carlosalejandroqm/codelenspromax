#!/usr/bin/env bash
# Prueba de punta a punta contra el API Gateway (requiere los servicios arriba).
#
#   ./scripts/smoke-test.sh
#   GATEWAY=http://localhost:8080 SMOKE_GIT_URL=https://github.com/usuario/repo ./scripts/smoke-test.sh
#
# Recorre: auth -> ingesta (zip, carpeta y opcionalmente git) -> analisis ->
# reporte -> historial por usuario. Necesita curl, jq y zip.
set -euo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
API="$GATEWAY/api/v1"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

for tool in curl jq zip; do
  command -v "$tool" >/dev/null || { echo "Falta la herramienta '$tool'" >&2; exit 1; }
done

pass() { printf '  \033[32mOK\033[0m   %s\n' "$1"; }
fail() { printf '  \033[31mFALLO\033[0m %s\n' "$1" >&2; exit 1; }

# request METODO URL [curl args...] -> deja el cuerpo en $BODY y el status en $STATUS
request() {
  local method="$1" url="$2"; shift 2
  local out
  out="$(curl -sS -X "$method" "$url" -w '\n%{http_code}' "$@")"
  STATUS="${out##*$'\n'}"
  BODY="${out%$'\n'*}"
}

expect_status() {
  [[ "$STATUS" == "$1" ]] || fail "$2 (esperaba $1, llego $STATUS): $BODY"
  pass "$2"
}

echo "== Gateway $GATEWAY"
request GET "$GATEWAY/actuator/health"
expect_status 200 "gateway responde /actuator/health"

echo "== Autenticacion (user-service)"
request GET "$API/reports"
expect_status 401 "sin token la API responde 401"

request GET "$API/reports" -H "Authorization: Bearer token-falso"
expect_status 401 "con token invalido la API responde 401"

SUFFIX="$(date +%s)$RANDOM"
USER_A="smoke_a_$SUFFIX"
request POST "$API/auth/register" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_A\",\"email\":\"$USER_A@codelens.dev\",\"fullName\":\"Usuario Prueba\",\"password\":\"secreto123\"}"
expect_status 201 "registro de $USER_A"

request POST "$API/auth/register" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_A\",\"email\":\"otro_$USER_A@codelens.dev\",\"fullName\":\"Duplicado\",\"password\":\"secreto123\"}"
expect_status 409 "registro duplicado responde 409"

request POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_A\",\"password\":\"incorrecta\"}"
expect_status 401 "login con contrasena incorrecta responde 401"

request POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_A\",\"password\":\"secreto123\"}"
expect_status 200 "login de $USER_A"
TOKEN_A="$(jq -r .token <<<"$BODY")"
USER_A_ID="$(jq -r .user.id <<<"$BODY")"
AUTH_A=(-H "Authorization: Bearer $TOKEN_A")

request GET "$API/users/me" "${AUTH_A[@]}"
expect_status 200 "GET /users/me devuelve $(jq -r .username <<<"$BODY")"

echo "== Ingesta (source-service)"
# Proyecto de ejemplo con fallos conocidos para que el motor encuentre algo.
mkdir -p "$WORK/demo-app/src"
cat > "$WORK/demo-app/src/db.js" <<'JS'
const password = "SuperSecreta123!";
const AWS_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";

function findUser(db, req) {
  // TODO: validar la entrada
  return db.query("SELECT * FROM users WHERE name = '" + req.query.name + "'");
}

function run(code) {
  return eval(code);
}

module.exports = { findUser, run };
JS
cat > "$WORK/demo-app/src/app.py" <<'PY'
import hashlib
import subprocess

def hash_password(value):
    return hashlib.md5(value.encode()).hexdigest()

def ping(host):
    return subprocess.call("ping -c 1 " + host, shell=True)
PY
cat > "$WORK/demo-app/package.json" <<'JSON'
{ "name": "demo-app", "version": "1.0.0", "dependencies": { "lodash": "4.17.15", "express": "4.16.0" } }
JSON
(cd "$WORK" && zip -qr demo-app.zip demo-app)

request POST "$API/sources/zip" "${AUTH_A[@]}" -F "file=@$WORK/demo-app.zip"
expect_status 201 "importar .zip ($(jq -r '.analyzableFiles' <<<"$BODY") archivos analizables)"
SOURCE_ID="$(jq -r .id <<<"$BODY")"

request POST "$API/sources/folder" "${AUTH_A[@]}" -F "file=@$WORK/demo-app.zip" -F "name=demo-app"
expect_status 201 "importar carpeta local (tipo $(jq -r .type <<<"$BODY"))"

if [[ -n "${SMOKE_GIT_URL:-}" ]]; then
  request POST "$API/sources/git" "${AUTH_A[@]}" -H 'Content-Type: application/json' \
    -d "{\"repositoryUrl\":\"$SMOKE_GIT_URL\"}"
  expect_status 201 "clonar $SMOKE_GIT_URL"
else
  echo "  --   git omitido (define SMOKE_GIT_URL para probarlo)"
fi

request GET "$API/sources" "${AUTH_A[@]}"
[[ "$(jq length <<<"$BODY")" -ge 2 ]] || fail "el listado de snapshots no incluye los del usuario"
pass "GET /sources lista los snapshots del usuario"

echo "== Analisis (analysis-service)"
request POST "$API/analyses" "${AUTH_A[@]}" -H 'Content-Type: application/json' -d "{\"sourceId\":\"$SOURCE_ID\"}"
expect_status 201 "analisis del snapshot"
ANALYSIS_ID="$(jq -r .id <<<"$BODY")"
[[ "$(jq -r .status <<<"$BODY")" == "COMPLETED" ]] || fail "el analisis no quedo COMPLETED: $BODY"
FINDINGS="$(jq '.criticalCount + .highCount + .mediumCount + .lowCount + .infoCount' <<<"$BODY")"
pass "analisis COMPLETED: nota $(jq -r .grade <<<"$BODY"), puntaje $(jq -r .overallScore <<<"$BODY"), $FINDINGS hallazgos"
[[ "$FINDINGS" -gt 0 ]] || fail "el motor no encontro hallazgos en el proyecto de ejemplo"

request GET "$API/analyses/$ANALYSIS_ID/findings" "${AUTH_A[@]}"
expect_status 200 "GET /analyses/{id}/findings ($(jq length <<<"$BODY") hallazgos, reglas: $(jq -r '[.[].ruleId] | unique | .[0:4] | join(", ")' <<<"$BODY")...)"

echo "== Reporte (report-service)"
request POST "$API/reports" "${AUTH_A[@]}" -H 'Content-Type: application/json' -d "{\"analysisId\":\"$ANALYSIS_ID\"}"
expect_status 201 "generar reporte"
REPORT_ID="$(jq -r .id <<<"$BODY")"
pass "veredicto $(jq -r .verdict <<<"$BODY") con $(jq '.recommendations | length' <<<"$BODY") recomendaciones"

request GET "$API/reports/$REPORT_ID/markdown" "${AUTH_A[@]}"
expect_status 200 "reporte en Markdown"

request GET "$API/reports/$REPORT_ID/html" "${AUTH_A[@]}"
expect_status 200 "reporte en HTML"

request GET "$API/reports" "${AUTH_A[@]}"
[[ "$(jq --arg id "$REPORT_ID" '[.[] | select(.id == $id)] | length' <<<"$BODY")" == "1" ]] \
  || fail "el historial de $USER_A no contiene su reporte"
pass "el historial de $USER_A contiene el reporte"

echo "== Aislamiento entre usuarios"
USER_B="smoke_b_$SUFFIX"
request POST "$API/auth/register" -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER_B\",\"email\":\"$USER_B@codelens.dev\",\"fullName\":\"Otro Usuario\",\"password\":\"secreto123\"}"
expect_status 201 "registro de $USER_B"
TOKEN_B="$(jq -r .token <<<"$BODY")"

request GET "$API/reports" -H "Authorization: Bearer $TOKEN_B"
[[ "$(jq length <<<"$BODY")" == "0" ]] || fail "$USER_B ve reportes ajenos: $BODY"
pass "$USER_B no ve el historial de $USER_A"

request GET "$API/reports" -H "Authorization: Bearer $TOKEN_B" -H "X-User-Id: $USER_A_ID"
[[ "$(jq length <<<"$BODY")" == "0" ]] || fail "el gateway dejo pasar un X-User-Id falsificado"
pass "el gateway ignora un X-User-Id falsificado"

echo
echo "Todo OK. Reporte de ejemplo: $REPORT_ID"
