# SneakerFever

Tienda online de zapatillas, ropa y accesorios, con panel de administración
(ventas, créditos, inventario, catálogo, usuarios y perfiles).
Proyecto académico de la Universidad Tecnológica del Perú (UTP).

## Arquitectura

| Parte | Tecnología | Ubicación |
|---|---|---|
| Backend / API REST | Java 21, Spring Boot 3.5, Spring Data JPA, Spring Security + JWT | `src/main/java/com/example/acceso` |
| Frontend (panel admin + tienda) | React 19, TypeScript, Vite, Tailwind | `frontend/` |
| Base de datos | MySQL 8 | `docker-compose.yml`, `acceso.sql` |
| Microservicio de contacto / FAQ (opcional) | Node.js + Express, puerto 3000 | `src/main/java/com/example/acceso/servicios-api` |

El backend se organiza por dominio (`producto`, `venta`, `inventario`, ...),
cada uno con su Controller → Service → Repository → Entity.

El backend es solo API REST (JSON): toda la interfaz, tanto la tienda como el
panel de administración, es el SPA de React en `frontend/`. Las imágenes del
sitio están en `src/main/resources/static/web/images` y se sirven en `/web/images/**`.

## Seguridad

- **Login con JWT**: `POST /api/auth/login` devuelve un access token (30 min,
  en memoria del frontend) y deja el refresh token (7 días) en una cookie
  `httpOnly`.
- **Logout en el servidor**: `POST /api/auth/logout` revoca el access y el
  refresh token de esa sesión (tabla `tokens_revocados`, por `jti`): dejan de
  servir aunque alguien los haya copiado. Las sesiones de otros dispositivos
  siguen abiertas. Las filas vencidas se limpian solas cada hora.
- **Permisos por perfil**: cada `Perfil` tiene `Opciones` (tabla
  `perfil_opcion`), y cada opción otorga un código de permiso estable
  (`opciones.permiso`: `PRODUCTOS`, `VENTAS`, `DASHBOARD`...; ver
  `seguridad/Permisos.java`). Cada controller lo exige con
  `@PreAuthorize`. La `ruta` de la opción es solo el link del sidebar: se
  puede cambiar sin afectar los permisos.
  En una base anterior a esta columna, el permiso se completa solo al arrancar
  (`PermisosOpcionInitializer`, deducido de la ruta).
- **Denegar por defecto**: lo público es una lista explícita en
  `SecurityConfig`; cualquier endpoint sin regla responde 403.

## Cómo correrlo localmente

### 1. Base de datos

```bash
docker compose up -d        # MySQL 8 en localhost:3308, base "acceso", root sin clave
```

> El `docker-compose.yml` es **solo para desarrollo local**: usa
> `MYSQL_ALLOW_EMPTY_PASSWORD`. No usarlo así en un servidor.

La primera vez (volumen vacío), MySQL carga solo `acceso.sql`: perfiles,
opciones de menú con su permiso y usuarios. Hibernate crea el resto de las
tablas al arrancar el backend (`ddl-auto=update`). Si ya tenías el volumen de
antes, tus datos se conservan y el script no se vuelve a ejecutar.

### 2. Backend

```bash
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

Edita `application.properties` (está en `.gitignore`, nunca se sube):

- `spring.datasource.*`: con el docker-compose,
  `jdbc:mysql://localhost:3308/acceso`, usuario `root`, sin clave.
- `jwt.secret`: al menos 32 caracteres; genera uno con `openssl rand -base64 48`.
- `miapi.token`: token de miapi.cloud para la consulta de DNI/RUC.
- `upload.path`: carpeta local donde se guardan las imágenes subidas.

```bash
./mvnw spring-boot:run      # API en http://localhost:8082
./mvnw test                 # tests unitarios
```

### 3. Frontend

```bash
cd frontend
npm install
npm run dev                 # http://localhost:5173 (proxy hacia :8082)
npm run build               # verificación de tipos + build de producción
npm run lint
```

### 4. Microservicio Node (opcional)

Sirve el formulario de contacto y las preguntas frecuentes del sitio público.
Si no está levantado, el sitio funciona igual y esas secciones muestran un
aviso.

```bash
cd src/main/java/com/example/acceso/servicios-api
npm install && node index.js   # http://localhost:3000
```

### Todo con Docker (demo, sin instalar Java ni Node)

```bash
cp .env.example .env        # y completa JWT_SECRET (openssl rand -base64 48) y MIAPI_TOKEN
docker compose --profile app up -d --build
```

Levanta MySQL, el backend y el frontend (nginx) en **http://localhost:8080**.
nginx sirve el frontend y hace de proxy hacia el backend por el mismo origen,
así que no hace falta CORS. Las imágenes subidas quedan en el volumen
`sneakerfever-uploads`. Si falta `JWT_SECRET`, el backend no arranca y lo
indica en `docker logs sneakerfever-backend`.

Si el frontend se sirve desde otro dominio, habilítalo en el backend con
`app.cors.allowed-origins` (variable `APP_CORS_ALLOWED_ORIGINS`).

## Proyecto académico

Universidad Tecnológica del Perú · 2025–2026
