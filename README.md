# CodeLens: analizador de código basado en microservicios

Primer previo, Microservicios (UFPS 2026). Carlos Alejandro Quiñones Martínez (1151805).

**Documentación:** la presentación del proyecto (análisis del sistema, arquitectura y
microservicios) está en [`docs/CodeLensProMax_Primer_Previo_Microservicios.pdf`](docs/CodeLensProMax_Primer_Previo_Microservicios.pdf).

CodeLens es una plataforma web que analiza el código de un proyecto, detecta fallos y
vulnerabilidades y muestra dónde mejorar. El código entra por tres vías: un **.zip**, una
**carpeta local** o un **repositorio Git**. El resultado es un panel con la nota, los puntajes,
los hallazgos (archivo y línea) y un plan de acción priorizado.

## Arquitectura

```
                    ┌──────────────────────────┐
  Navegador ───────▶│ Frontend Angular (:4200) │  SPA; en Docker la sirve nginx
                    └────────────┬─────────────┘
                                 │ /api/**  (Bearer JWT)
                    ┌────────────▼─────────────┐
                    │   API Gateway (:8080)    │  Spring Cloud Gateway
                    │ ruteo · JWT · CORS       │  valida el token y agrega X-User-Id
                    └──┬──────┬──────┬──────┬──┘
          /auth,/users │      │      │      │ /reports
          ┌────────────▼┐ ┌───▼────┐ │ ┌────▼──────────┐
          │ user-service│ │ source │ │ │ report-service│
          │   :8084     │ │ :8081  │ │ │    :8083      │
          └─────┬───────┘ └───▲────┘ │ └──────┬────────┘
                │   /analyses │  ┌───▼────────▼──┐
              H2 (users)      └──┤analysis :8082 │  (report → analysis → source
                                 └───────────────┘   se llaman directo, sin gateway)
```

| Servicio | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| `gateway` | 8080 | Punto único de entrada. Enruta, valida el JWT y centraliza CORS. | — |
| `user-service` | 8084 | Registro, login (BCrypt) y emisión del token JWT. | H2 `user-db` |
| `source-service` | 8081 | Ingesta: clona el repo git (JGit), descomprime el .zip o la carpeta y hace el inventario de archivos. | H2 `source-db` + workspaces |
| `analysis-service` | 8082 | Motor de reglas: seguridad (inyección, secretos, criptografía), dependencias vulnerables, complejidad, duplicación y deuda técnica. | H2 `analysis-db` |
| `report-service` | 8083 | Veredicto, resumen ejecutivo y recomendaciones priorizadas (P0–P3). Exporta a Markdown y HTML. | H2 `report-db` |

- Hay **una base de datos por servicio** y ninguna tabla es compartida.
- Cada servicio tiene su propio `Dockerfile`.
- Todos los errores usan el mismo formato: `{timestamp, status, error, message, path}`.
- **Seguridad:** el gateway es el único que ve el token. Borra cualquier `X-User-Id` que mande el
  cliente y lo reemplaza por el del token, así que nadie puede suplantar a otro usuario. Cada
  servicio guarda el `ownerId` y el historial solo muestra lo del usuario autenticado.

**Stack:** Java 21, Spring Boot 3.3, Spring Cloud Gateway, JPA/H2, JGit, jjwt y Angular 18 (standalone + signals).

## Requisitos

| Herramienta | Versión | Instalar (macOS) |
|---|---|---|
| Java | **21** (Spring Boot 3.3 no arranca bien con JDKs más nuevos) | `brew install openjdk@21` |
| Maven | 3.9+ | `brew install maven` |
| Node.js | 20+ | `brew install node@20` |
| Docker | opcional | Docker Desktop o `brew install colima docker docker-compose` |

## Correr en local

### Opción A: con scripts (recomendada)

```bash
# 1) Backend: compila y levanta los 5 microservicios en segundo plano (usa Java 21 automáticamente)
./scripts/run-local.sh            # la primera vez tarda en bajar dependencias
#    ./scripts/run-local.sh --skip-build   para arrancar sin recompilar

# 2) Frontend (otra terminal)
cd frontend
npm install
npm start                         # http://localhost:4200  (proxy /api -> gateway :8080)
```

Abre <http://localhost:4200>, crea una cuenta y analiza un proyecto.

- Logs: `logs/<servicio>.log`.
- Datos: `data/` (las bases H2 y el código importado).
- Para detener: `./scripts/stop-local.sh` y `Ctrl+C` en la terminal del frontend.

### Opción B: servicio por servicio (una terminal por cada uno)

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # o /opt/homebrew/opt/openjdk@21
mvn -DskipTests package
java -jar user-service/target/user-service.jar
java -jar source-service/target/source-service.jar
java -jar analysis-service/target/analysis-service.jar
java -jar report-service/target/report-service.jar
java -jar gateway/target/gateway.jar
```

### Opción C: Docker Compose (un contenedor por servicio)

```bash
cp .env.example .env              # cambia JWT_SECRET
docker compose up --build         # frontend http://localhost:4200 · gateway http://localhost:8080
docker compose down               # (agrega -v para borrar los volúmenes de datos)
```

## Cómo probar

### 1. Prueba automática de punta a punta

Con los servicios arriba:

```bash
./scripts/smoke-test.sh
SMOKE_GIT_URL=https://github.com/octocat/Hello-World ./scripts/smoke-test.sh   # incluye clonado git
```

La prueba recorre todo a través del gateway:

1. `401` sin token o con token falso.
2. Registro, `409` si el usuario ya existe y `401` con la contraseña incorrecta.
3. Login y `/users/me`.
4. Importa un proyecto de ejemplo con fallos conocidos, como `.zip` y como carpeta.
5. Lo analiza (nota F, hallazgos de SQLi, `eval`, secretos, MD5, `shell=True` y lodash vulnerable).
6. Genera el reporte (JSON, Markdown y HTML).
7. Verifica que otro usuario no ve ese historial, ni aunque falsifique `X-User-Id`.

### 2. En la interfaz

1. Crea una cuenta en `/register`.
2. **Nuevo análisis**, en cualquiera de las tres pestañas:
   - **Archivo .zip:** arrastra o elige un zip.
   - **Carpeta local:** elige una carpeta. El navegador la empaqueta y deja fuera `node_modules`, `.git`, `target`, `dist`…
   - **Repositorio Git:** pega la URL pública, por ejemplo `https://github.com/octocat/Hello-World`.
3. Mira el pipeline: cada paso muestra qué microservicio está trabajando.
4. En el reporte revisa la nota, los puntajes, el resumen, el plan de acción y los hallazgos
   (filtrables). Puedes descargar el `.md` o abrir el HTML imprimible.
5. En **Historial** aparecen solo tus reportes. **Salir** cierra la sesión.

### 3. A mano con curl

```bash
API=http://localhost:8080/api/v1
TOKEN=$(curl -s -X POST $API/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"alejo","email":"alejo@ufps.edu.co","fullName":"Alejo","password":"secreto123"}' | jq -r .token)
# si ya existe: curl -s -X POST $API/auth/login -H 'Content-Type: application/json' -d '{"username":"alejo","password":"secreto123"}'

SRC=$(curl -s -X POST $API/sources/git -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"repositoryUrl":"https://github.com/octocat/Hello-World"}' | jq -r .id)
AN=$(curl -s -X POST $API/analyses -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"sourceId\":\"$SRC\"}" | jq -r .id)
curl -s -X POST $API/reports -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"analysisId\":\"$AN\"}" | jq '{verdict, grade, overallScore}'
```

## API (a través del gateway, `http://localhost:8080/api/v1`)

Todo exige `Authorization: Bearer <token>`, excepto `/auth/**`.

| Método | Ruta | Servicio | Descripción |
|---|---|---|---|
| POST | `/auth/register` | user | Crea la cuenta. Responde `201 {token, user}`. |
| POST | `/auth/login` | user | Acepta usuario o correo. Responde `{token, expiresAt, user}`. |
| GET | `/users/me` | user | Usuario autenticado. |
| POST | `/sources/git` | source | `{repositoryUrl, branch?, accessToken?}` |
| POST | `/sources/zip` | source | multipart `file` (.zip, hasta 200 MB) |
| POST | `/sources/folder` | source | multipart `file` (carpeta empaquetada) + `name` |
| GET | `/sources`, `/sources/{id}`, `/sources/{id}/files` | source | Snapshots del usuario e inventario de archivos. |
| POST | `/analyses` | analysis | `{sourceId}`. Ejecuta el análisis y lo devuelve completo. |
| GET | `/analyses`, `/analyses/{id}`, `/analyses/{id}/findings`, `/analyses/{id}/detail` | analysis | Análisis y hallazgos. |
| GET | `/rules`, `/rules/info`, `/languages` | analysis | Catálogo del motor. |
| POST | `/reports` | report | `{analysisId}`. Genera veredicto y recomendaciones. |
| GET | `/reports`, `/reports/{id}`, `/reports/{id}/markdown`, `/reports/{id}/html` | report | Historial y exportaciones. |

Cada servicio expone `GET /actuator/health` y una consola H2 en `/h2-console` (solo para desarrollo).

## Configuración (variables de entorno)

| Variable | Dónde | Por defecto |
|---|---|---|
| `JWT_SECRET` | gateway y user-service (debe ser el mismo, 32+ caracteres) | secreto de desarrollo |
| `JWT_TTL` | user-service | `8h` |
| `CORS_ALLOWED_ORIGINS` | gateway (un origen fuera de la lista recibe **403** sin cuerpo) | `http://localhost:*,http://127.0.0.1:*` |
| `USER_SERVICE_URI`, `SOURCE_SERVICE_URI`, `ANALYSIS_SERVICE_URI`, `REPORT_SERVICE_URI` | gateway | `http://localhost:808x` |
| `SOURCE_SERVICE_URL` | analysis-service | `http://localhost:8081` |
| `ANALYSIS_SERVICE_URL` | report-service | `http://localhost:8082` |
| `GATEWAY_URL` | frontend (nginx en Docker) | `http://gateway:8080` |

## Estructura

```
primer-previo-microserv/
├── pom.xml                 # POM padre (Spring Boot 3.3 + Spring Cloud 2023)
├── gateway/                # API Gateway + filtro JWT
├── user-service/           # usuarios y tokens
├── source-service/         # ingesta git / zip / carpeta
├── analysis-service/       # motor de reglas (reglas en resources/rules/*.json)
├── report-service/         # reportes y playbooks de recomendaciones
├── frontend/               # Angular 18 (pages/, core/, shared/) + Dockerfile nginx
├── docs/                   # presentación del proyecto (PDF)
├── docker-compose.yml
└── scripts/                # run-local.sh · stop-local.sh · smoke-test.sh
```
