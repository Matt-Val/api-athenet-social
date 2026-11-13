# api-athenet-social

Microservicio de interacciones sociales de Athenet: el perfil, los "me gusta" y los
equipos favoritos de los **usuarios públicos** que se registran por su cuenta en
`web-athenet`.

Es el primer servicio de la plataforma con una entidad de usuario propia. Los otros
cinco microservicios autentican contra **Microsoft Entra ID** (usuarios internos:
admin, director, delegado); este autentica **solo contra AWS Cognito**. Ningún
servicio valida los dos emisores a la vez.

| | |
|---|---|
| Puerto | `8085` |
| Base de datos | `social_db` (PostgreSQL) |
| Identidad | AWS Cognito — se valida el **access token** |
| Esquema | Flyway (`db/migration`), Hibernate solo valida |

## Endpoints

Las rutas `/api/me/**` exigen un access token de Cognito con el grupo `CLIENTE`.
Las de `/api/public/**` responden sin token.

| Método | Ruta | Notas |
|---|---|---|
| `GET` | `/api/me/profile` | Crea el perfil la primera vez, leyendo el correo y el nombre de `/oauth2/userInfo` |
| `PATCH` | `/api/me/profile` | Solo `displayName` |
| `GET` | `/api/me/likes` | Admite `?targetType=EVENT` |
| `POST` | `/api/me/likes` | `201` si se creó, `200` si ya existía — es idempotente |
| `DELETE` | `/api/me/likes/{targetType}/{targetId}` | `204`, también si no existía |
| `GET` | `/api/me/favorite-teams` | |
| `POST` | `/api/me/favorite-teams` | `201` / `200`, idempotente |
| `DELETE` | `/api/me/favorite-teams/{teamId}` | `204` |
| `GET` | `/api/public/likes/count?targetType=&targetId=` | **Anónimo.** Solo el total, nunca quién |
| `GET` | `/api/public/likes/counts?targetType=&targetIds=a,b,c` | **Anónimo.** Por lote; los que no tienen ninguno vuelven en `0` |
| `GET` | `/actuator/health` | Anónimo, para los health checks de Docker |

`targetType` ∈ `EVENT | MATCH | COMPETITION | INSTITUTION | PUBLICATION`.
`targetId` es **texto**: para eventos es el `internalId` (`EVT-42`), no el id numérico.

Los errores siguen el contrato del proyecto: `{ timestamp, status, error, message }`.

## Variables de entorno

| Variable | Por defecto | Para qué |
|---|---|---|
| `SERVER_PORT` | `8085` | |
| `DB_URL` | `jdbc:postgresql://localhost:5435/social_db` | En producción, el RDS con `?sslmode=require` |
| `DB_USERNAME` | `social_user` | |
| `DB_PASSWORD` | **sin valor** | Si falta, la aplicación no arranca — a propósito |
| `COGNITO_ISSUER_URI` | **sin valor** | `https://cognito-idp.<region>.amazonaws.com/<userPoolId>` |
| `COGNITO_CLIENT_ID` | **sin valor** | El App Client; se compara contra el claim `client_id` |
| `COGNITO_USERINFO_URI` | **sin valor** | `https://<dominio>/oauth2/userInfo` |
| `CORS_PUBLIC_ORIGINS` | `http://localhost:5173` | Origen de las rutas anónimas |
| `CORS_USER_ORIGINS` | `http://localhost:5173` | Origen de `/api/me/**`; aquí sí viaja `Authorization` |
| `JPA_SHOW_SQL` | `false` | |
| `LOG_LEVEL_SOCIAL` | `INFO` | |

Las tres sin valor por defecto son deliberadas: es mejor fallar al arrancar que a
mitad de una petición, y así ninguna contraseña termina escrita en el repositorio.

En local se cargan desde un `.env` (ignorado por git) gracias a
`spring.config.import=optional:file:.env[.properties]`. En la EC2 no existe ese
archivo y llegan por `docker --env-file`. Plantillas: `.env.example` y
`production.env.example`.

## Levantarlo en local

```bash
docker compose up -d          # PostgreSQL en el 5435
cp .env.example .env          # y completar los valores de Cognito
./mvnw spring-boot:run
```

Flyway crea las tablas al arrancar. Para empezar de cero: `docker compose down -v`.

## Tests

```bash
docker compose up -d          # SocialApplicationTests necesita la base
./mvnw test
```

- `CognitoJwtConfigTest` — que `cognito:groups` se traduzca a `ROLE_CLIENTE` y que la identidad sea el `sub`.
- `SecurityConfigTest` — 401 sin token, **403 con token válido sin el grupo**, 200 con `ROLE_CLIENTE`, rutas públicas anónimas y `denyAll` en lo no declarado.
- `SocialApplicationTests` — arranca el contexto completo: Flyway aplica y Hibernate valida que cada entidad calce con su tabla. Es el que caza un nombre de columna mal escrito.

## Decisiones que conviene conocer

**Se valida el access token, no el id token.** El access token de Cognito no trae
`aud` sino `client_id`, así que se comprueban `iss`, `client_id` y `token_use=access`.
Ese último cierra la confusión de audiencia: los tres tipos de token de Cognito van
firmados con la misma clave.

**El `sub` nunca llega desde el cliente.** Sale del token validado, los DTOs de
petición no tienen campo de identidad, y `fail-on-unknown-properties=true` convierte
un cuerpo con `cognitoSub` en un `400` en vez de ignorarlo.

**La idempotencia la garantiza la base.** No hay ningún `if` que compruebe si ya
diste like: el constraint `uk_user_likes_once` rechaza el duplicado y el servicio
traduce ese rechazo en un `200`.

**`teamName` e `institutionName` vuelven `null`.** Todavía no hay cliente de
MS-Instituciones; el frontend ya tiene el catálogo de equipos cargado en sesión.

## Pendiente

- Trigger Lambda *Post confirmation* en Cognito para que el grupo `CLIENTE` se
  asigne solo al registrarse (hoy se agrega a mano).
- Cliente de MS-Instituciones para completar los nombres de los equipos.
