<p align="center">
  <img src="docs/img/banner.png" alt="Athenet Banner" width="100%">
</p>

<p align="center">
Microservicio de interacciones sociales de <strong>Athenet</strong>: el perfil, los «me gusta» y los equipos favoritos de los usuarios públicos que se registran por su cuenta en el sitio, autenticados contra AWS Cognito.
</p>

<h4 align="center"><strong>
Athenet ORG - <a href="https://github.com/fabetabilo/web-athenet">Athenet Público</a>
</br>
<a href="#control-de-acceso-y-roles">Control de Acceso y Roles</a> - <a href="#desarrollo-local">Desarrollo Local</a> - <a href="#despliegue">Despliegue</a>
</strong></h4>

### Stack

<div align="center">

[![Java](https://img.shields.io/badge/Java_21-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_4-%236DB33F.svg?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-%23336791.svg?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-%23CC0200.svg?style=for-the-badge&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![AWS Cognito](https://img.shields.io/badge/AWS_Cognito-%23DD344C.svg?style=for-the-badge&logo=amazoncognito&logoColor=white)](https://aws.amazon.com/cognito/)
[![Docker](https://img.shields.io/badge/Docker-%232496ED.svg?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-%232088FF.svg?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/features/actions)

</div>

* **Framework backend:** [Spring Boot 4.1](https://spring.io/projects/spring-boot) (Spring Web MVC, Spring Data JPA).
* **Lenguaje:** Java 21.
* **Persistencia:** PostgreSQL, mapeado con Hibernate/JPA (`spring-boot-starter-data-jpa`).
* **Esquema:** Flyway (`db/migration`). Hibernate corre con `ddl-auto=validate`: no crea ni modifica nada, solo verifica que cada entidad calce con su tabla.
* **Autenticación:** `spring-boot-starter-oauth2-resource-server`, validando el **access token** emitido por AWS Cognito.
* **Validación:** Bean Validation (`spring-boot-starter-validation`) sobre los DTO de escritura.
* **Cliente HTTP:** `RestClient` contra `/oauth2/userInfo` de Cognito, con timeouts explícitos.
* **Build:** Maven (via wrapper `./mvnw`, no requiere instalación global).
* **Contenerización y despliegue:** Docker (multi-stage build) + GitHub Actions (test → build de imagen → push a Docker Hub → deploy por SSH).

### ms-social

Este microservicio guarda lo que un visitante del sitio público hace con el contenido de Athenet: su perfil, los recursos a los que les dio «me gusta» y los equipos que marcó como favoritos.

Es el primer servicio de la plataforma con una **entidad de usuario propia**. Los otros cinco autentican contra **Microsoft Entra ID** (usuarios internos: admin, director, delegado); este autentica **solo contra AWS Cognito** (usuarios públicos que se registran por su cuenta). Ningún servicio valida los dos emisores a la vez: cada ruta tiene una única fuente de identidad, y eso es deliberado.

Expone dos superficies bien diferenciadas:

* **Del usuario (`/api/me/**`):** perfil, «me gusta» y equipos favoritos. Exige un access token de Cognito con el grupo `CLIENTE`. **El dueño del recurso es el sujeto del token**, por eso ninguna ruta lleva identificador de dueño: no hay dónde inyectar un `sub` ajeno.
* **Pública (`/api/public/**`):** contadores agregados de «me gusta». Sin autenticación, y solo el total, nunca quién.

| Método | Endpoint | Acceso | Descripción |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/me/profile` | `CLIENTE` | Perfil propio; lo crea la primera vez leyendo correo y nombre de `/oauth2/userInfo` |
| `PATCH` | `/api/me/profile` | `CLIENTE` | Actualiza solo `displayName` |
| `GET` | `/api/me/likes` | `CLIENTE` | «Me gusta» propios; admite `?targetType=EVENT` |
| `POST` | `/api/me/likes` | `CLIENTE` | `201` si se creó, `200` si ya existía — idempotente |
| `DELETE` | `/api/me/likes/{targetType}/{targetId}` | `CLIENTE` | `204`, también si no existía |
| `GET` | `/api/me/favorite-teams` | `CLIENTE` | Equipos favoritos propios |
| `POST` | `/api/me/favorite-teams` | `CLIENTE` | `201` / `200`, idempotente |
| `DELETE` | `/api/me/favorite-teams/{teamId}` | `CLIENTE` | `204` |
| `GET` | `/api/public/likes/count?targetType=&targetId=` | Público | Total de «me gusta» de un recurso |
| `GET` | `/api/public/likes/counts?targetType=&targetIds=a,b,c` | Público | Por lote; los que no tienen ninguno vuelven en `0` |
| `GET` | `/actuator/health` | Público | Health check de Docker y del workflow de deploy |

El consumidor es `web-athenet` (el sitio público). Dos detalles del contrato:

* **`targetType`** ∈ `EVENT | MATCH | COMPETITION | INSTITUTION | PUBLICATION`. El «me gusta» es polimórfico a propósito: no existe una entidad de publicaciones en ningún backend, y el discriminador evita una migración el día que aparezca.
* **`targetId` es texto, no número.** Para eventos es el `internalId` (`EVT-42`), que es su identidad pública; para partidos y competencias, el id (`"1"`).

ms-social **no verifica que el recurso exista**: leer la base de otro servicio está prohibido por la arquitectura, y consultarlo por REST convertiría un «me gusta» en una llamada que puede fallar con `503`. Solo valida que el enum sea conocido y que el `targetId` cumpla su formato. Consecuencia aceptada: un contador puede quedar apuntando a algo ya borrado, lo que es inofensivo.

### Control de Acceso y Roles

El acceso está protegido mediante autenticación federada con **AWS Cognito** (OAuth 2.0 / OIDC con Authorization Code + PKCE). `CognitoJwtConfig` valida el token y `SecurityConfig` exige el rol por ruta.

Se valida el **access token**, no el id token. Como el access token de Cognito **no trae `aud` sino `client_id`**, la validación de audiencia se implementa comparando ese claim a mano, y se suma un tercer validador:

| Comprobación | Por qué |
| :--- | :--- |
| `iss` == `COGNITO_ISSUER_URI` | Que el token venga de nuestro User Pool y no de otro |
| `client_id` == `COGNITO_CLIENT_ID` | Equivale a validar la audiencia; el claim `aud` no existe en el access token |
| `token_use` == `access` | Sin esto, un **id token** del mismo pool pasaría la validación de firma: los tres tipos de token van firmados con la misma clave |

El claim `cognito:groups` se mapea a `GrantedAuthority` de Spring Security (`ROLE_<GRUPO>`), lo que le permite a `/api/me/**` exigir `hasRole("CLIENTE")`.

* **`CLIENTE`:** único rol habilitado para operar sobre los datos propios. Se asigna al registrarse.
* **Sin token / sin el grupo:** acceso denegado (`401` sin token válido, `403` con token válido pero sin el grupo) a todo `/api/me/**`.
* **Rutas públicas:** `/api/public/**` y `/actuator/health` no requieren autenticación.
* **Todo lo no declarado:** `denyAll()`. Una ruta nueva nace cerrada, no abierta.

**El `sub` nunca llega desde el cliente.** Sale siempre del token validado, en un único punto del código (`CurrentUserProvider`); ningún DTO de petición tiene campo de identidad; y `fail-on-unknown-properties=true` hace que un cuerpo con `cognitoSub` responda `400` en vez de ignorarlo en silencio. Las tres capas juntas hacen que la regla sea **testeable**, no solo una convención.

Las respuestas de error siguen el formato consistente del proyecto (`ApiError`: `timestamp`, `status`, `error`, `message`), manejado centralmente por `GlobalExceptionHandler`.

| HTTP | Cuándo |
| :--- | :--- |
| `400` | JSON inválido, campo desconocido en el body, `targetType` o `targetId` con formato inválido |
| `401` | Sin token, token expirado, id token en vez de access token, o token de otro emisor |
| `403` | Token válido sin el grupo `CLIENTE` |
| `404` | El recurso propio referido no existe |
| `503` | `/oauth2/userInfo` de Cognito no responde (no se crea un perfil a medias) |

### Desarrollo Local

#### 1. Verificación de dependencias del entorno

* **Java:** JDK 21 (el `pom.xml` fija `java.version=21`).
* **Docker:** para levantar PostgreSQL con el `docker-compose.yml` incluido.
* **Maven:** no hace falta instalarlo, el repo incluye el wrapper (`./mvnw`).
* **Un User Pool de Cognito** accesible: sin él el servicio arranca, pero toda ruta autenticada responde `401`.

Verifica la versión instalada:

```bash
java -version
```

#### 2. Clonar e instalar dependencias

```bash
./mvnw dependency:go-offline
```

#### 3. Configuración del entorno (`.env`)

Copia la plantilla y completa los valores de Cognito:

```bash
cp .env.example .env
```

**Nunca subas `.env` al repositorio** (ya está en `.gitignore`). Lo carga Spring con `spring.config.import=optional:file:.env[.properties]`; en la EC2 ese archivo no existe y las variables llegan por `docker --env-file`.

| Variable | Por defecto | Descripción |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8085` | Puerto HTTP |
| `DB_URL` | `jdbc:postgresql://localhost:5435/social_db` | En producción, el RDS con `?sslmode=require` |
| `DB_USERNAME` | `social_user` | |
| `DB_PASSWORD` | **sin valor** | |
| `COGNITO_ISSUER_URI` | **sin valor** | `https://cognito-idp.<region>.amazonaws.com/<userPoolId>` |
| `COGNITO_CLIENT_ID` | **sin valor** | App Client; se compara contra el claim `client_id` |
| `COGNITO_USERINFO_URI` | **sin valor** | `https://<dominio>.auth.<region>.amazoncognito.com/oauth2/userInfo` |
| `CORS_PUBLIC_ORIGINS` | `http://localhost:5173` | Origen de las rutas anónimas |
| `CORS_USER_ORIGINS` | `http://localhost:5173` | Origen de `/api/me/**`; esta registración **sí** incluye `Authorization` en los headers permitidos |
| `JPA_SHOW_SQL` | `false` | |
| `LOG_LEVEL_SOCIAL` | `INFO` | |

Las cuatro variables sin valor por defecto son deliberadas: es mejor fallar al arrancar que a mitad de una petición, y así ninguna contraseña termina escrita en el repositorio.

#### 4. Base de datos

```bash
docker compose up -d
```

Levanta PostgreSQL en el **5435** (no en el 5432, para no chocar con otro servicio del proyecto corriendo en paralelo). Flyway aplica `db/migration/V1__init.sql` al arrancar la aplicación y crea las tres tablas: `user_profile`, `user_likes` y `user_favorite_teams`.

Para empezar de cero: `docker compose down -v`.

#### 5. Servidor de desarrollo

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8085/`.

#### 6. Compilación para producción

```bash
./mvnw clean package -DskipTests
```

El artefacto se genera en `target/social-0.0.1-SNAPSHOT.jar`.

#### 7. Ejecución de tests

```bash
docker compose up -d   # SocialApplicationTests necesita la base
./mvnw test
```

* **`CognitoJwtConfigTest`:** el mapeo de `cognito:groups` a `ROLE_<GRUPO>` — un grupo, varios grupos, en minúsculas, sin grupos — que los *scopes* sobrevivan junto a los roles, y que el principal sea el `sub`. **No** cubre todavía el rechazo del id token ni de un `client_id` ajeno (ver Pendiente).
* **`SecurityConfigTest`:** `401` sin token, **`403` con token válido sin el grupo**, `200` con `ROLE_CLIENTE`, rutas públicas anónimas y `denyAll` en lo no declarado.
* **`SocialApplicationTests`:** arranca el contexto completo, así que Flyway aplica y Hibernate valida que cada entidad calce con su tabla. Es el que caza un nombre de columna mal escrito. Usa un `JwtDecoder` simulado, por lo que **no necesita Cognito** para correr en CI.

### Despliegue

El microservicio se dockeriza con un `Dockerfile` multi-stage (build con JDK 21, runtime con JRE 21, usuario no-root) y se despliega mediante `.github/workflows/ci-cd.yml`:

1. **Test:** corre `./mvnw test` contra un servicio de PostgreSQL levantado en el propio job.
2. **Build & Push:** builda la imagen y la publica en Docker Hub.
3. **Deploy** (manual, `workflow_dispatch`): se conecta por SSH a la instancia de aplicación y levanta el contenedor, usando secrets del repositorio (`EC2_APP_HOST`, `EC2_SSH_USER`, `EC2_SSH_KEY`, `DOCKERHUB_TOKEN`). Termina con un **gate de salud** que consulta `/actuator/health` y falla el workflow si el contenedor no queda `UP`.

Las credenciales de producción **no viajan en el repositorio ni en los secrets**: viven en `/opt/athenet/social.env` dentro de la EC2, cargado con `docker --env-file`. La plantilla de ese archivo es `production.env.example`.

### Estructura del Proyecto

```text
src/main/java/com/athenet/social/
├── client/            # CognitoUserInfoClient (RestClient contra /oauth2/userInfo, con timeouts)
├── config/            # CognitoJwtConfig (validadores del access token) y SecurityConfig (rutas, roles, CORS)
├── controller/        # MeProfileController, MeLikeController, MeFavoriteTeamController, PublicLikeController
├── dto/               # request/ (sin ningún campo de identidad) y response/
├── exception/         # GlobalExceptionHandler, ApiError y excepciones de dominio
├── mapper/            # Mapeo entidad → DTO, a mano (sin MapStruct)
├── model/             # UserProfile, UserLike, UserFavoriteTeam y enums/LikeTargetType
├── repository/        # Interfaces Spring Data y LikeCountProjection (conteo agrupado)
└── service/           # CurrentUserProvider (resuelve el sub), UserProfileService, LikeService, FavoriteTeamService

src/main/resources/
├── application.properties
└── db/migration/      # V1__init.sql — el esquema lo manda el SQL, Hibernate solo valida
```

### Decisiones de Diseño

**La idempotencia la garantiza la base de datos, no el código.** No hay ningún `if` que compruebe si ya diste «me gusta»: el constraint `uk_user_likes_once (user_profile_id, target_type, target_id)` rechaza el duplicado y el servicio traduce ese rechazo en un `200`. Por eso `LikeService` **no** lleva `@Transactional`: una violación de constraint dentro de una transacción la marca como *rollback-only*, y el re-leído de rescate fallaría también.

**El perfil se crea solo, en la primera petición.** `CurrentUserProvider` resuelve `sub → perfil`; si el `sub` es nuevo, llama **una** vez a `/oauth2/userInfo` (el access token de Cognito no trae `email` ni `given_name`) y cachea el resultado. No hay endpoint de registro: registrarse es cosa de Cognito.

**El email es clave natural única.** Si el User Pool se recrea —algo que puede pasar en un entorno de laboratorio— todos los `sub` cambian. Al no encontrar el `sub`, el servicio busca por email y **re-vincula** el perfil existente en vez de dejar los «me gusta» y favoritos huérfanos.

**`teamName` e `institutionName` vuelven `null`.** Todavía no hay cliente de MS-Instituciones; el frontend ya tiene el catálogo de equipos cargado en sesión. Se guardan como *snapshot* para que un favorito se pueda dibujar aunque Instituciones esté caído.

### Pendiente

* Trigger Lambda *Post confirmation* en Cognito para que el grupo `CLIENTE` se asigne solo al registrarse (hoy se agrega a mano).
* Cliente de MS-Instituciones para completar los nombres de equipos e instituciones.
* **Test de los validadores del access token.** Los dos `JwtClaimValidator` de `CognitoJwtConfig` —`client_id` y `token_use=access`— no tienen ninguna prueba: hoy esos claims solo aparecen como datos de apoyo al construir el JWT de los tests del converter, no como algo que se verifique. Es la comprobación que respalda «se valida audiencia y tipo de token», así que conviene que exista.
* `LikeServiceTest` para el conteo agrupado y los caminos de idempotencia.
