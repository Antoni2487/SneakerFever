# Auditoría técnica integral del backend — SneakerFever

**Fecha:** 10 de septiembre de 2026
**Rama auditada:** `develop` (HEAD `f1d42e5`, incluye la migración de autenticación HttpSession → JWT)
**Stack:** Spring Boot 3.5.5 · Java 21 · Spring Data JPA (Hibernate) · Spring Security + JWT (jjwt) · MySQL 8
**Alcance:** `src/main/java/com/example/acceso/**` (paquetes: `brand`, `carrito`, `category`, `cliente`, `common`, `config`, `dashboard`, `inventario`, `perfil`, `personalizacion`, `product`, `usuario`, `venta`), `src/test/java/**`, `pom.xml`, `application(-example).properties`, `docker-compose.yml`.
**Modo:** solo lectura. Ningún archivo de código fue modificado.

**Nota sobre auditorías previas:** este repo tenía tres informes anteriores en la raíz (`AUDITORIA_BACKEND.md` del 25-ago, `AUDITORIA_INTEGRAL.md` del 28-ago, `AUDITORIA.md` del 9-sep) que quedaron eliminados (movidos a la Papelera de reciclaje de Windows, recuperables) justo antes de esta sesión — probablemente al pedir explícitamente un informe nuevo. Este documento es un análisis fresco contra el código actual, no un diff contra esos informes, aunque su contenido conocido se usó para no repetir verificaciones ya hechas (p. ej. que no hay secretos commiteados — se re-verificó igual, ver §10).

---

## 1. Resumen ejecutivo

El backend tiene una arquitectura **package-by-feature limpia y consistente** (13 paquetes de dominio, cada uno con su Controller→Service→Repository→Entity), inyección de dependencias 100% por constructor, N+1 ya resuelto con `@EntityGraph`, y manejo de errores centralizado en un `GlobalExceptionHandler`. La migración de sesión a JWT (commit `a03e20b`) está bien implementada y documentada en el propio código (Javadoc explica explícitamente qué queda pendiente). **El problema más grave hoy es de autorización, no de autenticación**: cualquier usuario autenticado con un JWT válido puede llamar cualquier endpoint protegido, sin distinción de rol/perfil — el propio código lo admite (`JwtAuthenticationFilter.java:29-31`, `SecurityConfig.java:22-25`). El segundo problema real es que el patrón "DTO puro" se rompe en el dominio de créditos: `CuotaPagoResponse` y `CreditoVentaResponse` calculan reglas de negocio (días vencidos, % pagado) dentro de la propia clase de respuesta. Testing con Mockito existe pero cubre solo 2 de 12 servicios. No hay Dockerfile de la app, ni CI/CD.

---

## 2. Tabla de hallazgos por punto

| # | Área | Estado | Hallazgo | Riesgo | Archivo(s) afectado(s) |
|---|---|---|---|---|---|
| 1 | Arquitectura general | ✅ | Package-by-feature aplicado de forma consistente en los 13 paquetes; dentro de cada uno se respeta Controller→Service→Repository→Entity. Decisión deliberada (no es una capa "layered" incompleta). | Bajo | `src/main/java/com/example/acceso/*` |
| 1 | Arquitectura general | ⚠️ | 11 controllers de dominio siguen siendo híbridos: mezclan `@GetMapping` que devuelve una vista Thymeleaf con métodos `@ResponseBody` de API en la misma clase, aunque el panel admin ya está 100% migrado a React. | Medio (deuda, no riesgo de seguridad per se) | `PerfilController.java`, `BrandController.java`, `ProductController.java`, `UsuarioController.java`, `CategoryController.java`, `ClienteController.java`, `VentaController.java`, `CreditoVentaController.java`, `MovimientoInventarioController.java`, `DashboardController.java`, `PersonalizacionController.java` |
| 1 | Arquitectura general | ⚠️ | `CheckoutController` (100% Thymeleaf) y `CartController` (100% JSON) implementan el mismo flujo de checkout dos veces, con la misma regla hardcodeada de serie de comprobante (`"FACTURA".equals(tipo) ? "F001" : "B001"`) que contradice la fuente de verdad real (`ComprobanteSecuenciaService`). | Medio — dos respuestas posibles a "¿cuál es la serie activa?" | `CheckoutController.java`, `CartController.java` |
| 2 | DTOs / Response | ⚠️ | `CuotaPagoResponse` y `CreditoVentaResponse` tienen métodos de cálculo de negocio (`calcularDiasParaVencer`, `calcularDiasVencida`, `calcularPorcentajePagado`, `calcularDiasRestantes`) dentro del propio DTO — el Service los construye y luego llama a esos métodos sobre el objeto ya armado para rellenar sus propios campos. Ver evidencia abajo. | Medio — el DTO deja de ser transporte puro; la regla de negocio queda fuera del Service, más difícil de testear aisladamente | `venta/CuotaPagoResponse.java:55-76`, `venta/CreditoVentaResponse.java:97-107`, uso en `venta/CreditoVentaService.java:449,471-473` |
| 2 | DTOs / Response | ✅ | El resto de los 12 `*Response` (Usuario, Product, Cliente, Brand, Perfil, Opcion, MovimientoInventario, Category, Venta, RegistroPago) son transporte puro: sin acceso a repositorios, mapeo vía `Response.from(entity)` (Factory Method), sin `@Entity` expuesta directamente en ningún controller. | Bajo | `usuario/UsuarioResponse.java`, `product/ProductResponse.java`, etc. |
| 3 | POO | ✅ | Encapsulamiento correcto (atributos privados + Lombok), Builder consistente, un dominio por Service (SRP respetado a nivel de paquete). | Bajo | — |
| 3 | POO | ⚠️ | No hay interfaces de servicio (`XxxService` + `XxxServiceImpl`) en ningún dominio — son 12 clases concretas. Decisión deliberada del proyecto (se consolidó `PerfilService` de interfaz+Impl a una sola clase durante el refactor), consistente en todo el código, no una omisión accidental. | Bajo (justificado: sin múltiples implementaciones reales que requieran el contrato) | `*/*.Service.java` (12 archivos) |
| 3 | POO | ⚠️ | Cero uso de `record` de Java 21 para DTOs — todos los `*Request`/`*Response` son clases mutables con Lombok `@Data`/`@Builder`. Única excepción: `AuthController.LoginRequest`, un record inline de 2 campos. | Bajo — inconsistencia de estilo, no de correctitud | Todos los `*Request.java`/`*Response.java` salvo `AuthController.java:39` |
| 4 | Inyección de dependencias | ✅ | Inyección 100% por constructor en Controllers/Services verificados; cero `@Autowired` en campo en el código propio del dominio (`common`, `config`, y los 13 paquetes de negocio). | Bajo | — |
| 5 | Patrones de diseño | ✅ | Factory Method (`Response.from(entity)`, 12 clases), Builder (Lombok, mayoría de entidades/DTOs), Repository (Spring Data JPA), `@ControllerAdvice` como cross-cutting (`GlobalExceptionHandler`). Bien aplicados. | Bajo | — |
| 5 | Patrones de diseño | ⚠️ | Oportunidad real de Strategy: la regla "serie de comprobante activa" está duplicada (ver hallazgo de §1) en vez de resolverse en un único punto (`ComprobanteSecuenciaService`). | Medio | `CheckoutController.java`, `CartController.java`, `ComprobanteSecuenciaService.java` |
| 6 | Manejo de excepciones | ✅ | `GlobalExceptionHandler` (`@ControllerAdvice`) centraliza el manejo; excepciones de negocio propias por dominio (`MarcaException`, `CategoriaException`, `ClienteException`, `ProductoException`, `PersonalizacionException`, `AccesoDenegadoException`, como clases anidadas `static` en cada Service). Sin catch vacíos detectados en el código de negocio. | Bajo | `common/GlobalExceptionHandler.java` |
| 6 | Manejo de excepciones | ⚠️ | El formato de error (`ApiResponses.error`) es consistente entre sí (`success`, `message`, `errors` opcional) pero **no incluye `timestamp` ni `path`**, como sí pide la buena práctica REST estándar. Además, `handleTypeMismatchException` devuelve un `String` de vista (`"redirect:/"`) mientras el resto del handler devuelve `ResponseEntity<Map>` — mezcla vista/API otra vez, ahora en el propio manejador global. | Bajo-Medio | `common/ApiResponses.java`, `common/GlobalExceptionHandler.java:34-45` |
| 7 | Testing con Mockito | ⚠️ | Existen tests reales con Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`) para **2 de 12 Services**: `CategoryServiceTest` y `CreditoVentaServiceTest` (este último, buena elección — es lógica de negocio crítica de cuotas/pagos). El resto (`VentaService`, `UsuarioService`, `ProductService`, `BrandService`, `ClienteService`, `PerfilService`, `PersonalizacionService`, `MovimientoInventarioService`, `ComprobanteSecuenciaService`, `JwtService`) no tiene tests. | Alto — `VentaService`/`UsuarioService` concentran reglas de negocio críticas (última cuenta admin, cálculo de venta) sin cobertura | `src/test/java/com/example/acceso/{category,venta}/*Test.java` |
| 8 | Consultas JPA/Hibernate | ✅ | N+1 ya resuelto en los listados principales vía `@EntityGraph` (Product, Brand, Venta, Credito, Usuario) según refactor previo — verificado que sigue vigente en el código actual. | Bajo | `*/*.Repository.java` |
| 8 | Consultas JPA/Hibernate | ⚠️ | `Pageable`/`Page<>` solo se usa en **un** método de todo el backend (`DetalleVentaRepository.findProductosMasVendidos(Pageable)`). El resto de los listados (`findAll`, `listarXxx`) trae todo a memoria sin paginación — funciona hoy por volumen de datos bajo, pero no escala. | Medio | Todos los `*Repository.java` salvo `DetalleVentaRepository` |
| 9 | APIs REST | ⚠️ | 167 endpoints en 18 controllers, sin prefijo `/api` uniforme (algunos son `/xxx/api/**`, otros `/api/xxx/**`), sin versionado. Verbos/códigos de estado usados correctamente donde se revisó (200/201/400/401/403/404). | Medio (consistencia, no correctitud) | Todos los `*Controller.java` |
| 9 | APIs REST | ❌ | Sin paginación real expuesta en la API para listados potencialmente grandes (Productos, Ventas, Clientes) — consistente con el hallazgo de §8. | Medio | `ProductController.java`, `VentaController.java`, `ClienteController.java` |
| 10 | Seguridad — Autenticación | ✅ | JWT (HS256, jjwt) bien implementado: access token corto (30 min) + refresh token largo (7 días) en cookie `httpOnly`, `sameSite=Strict`, `secure` configurable por entorno (`jwt.cookie.secure`). El filtro valida tipo de token (access vs refresh) y **revalida contra BD** que el usuario siga activo en cada request (no confía solo en el claim del token) — mejor que la sesión anterior. | Bajo | `config/JwtService.java`, `config/JwtAuthenticationFilter.java`, `usuario/AuthController.java` |
| 10 | Seguridad — Autorización | ❌ | **Sin RBAC**: `SecurityConfig` solo distingue `permitAll()` vs `authenticated()` por ruta; cualquier usuario autenticado (sin importar su `Perfil`/`Opcion`) puede llamar cualquier endpoint de `/usuarios/api/**`, `/perfiles/api/**`, etc. El propio código lo documenta como brecha abierta pendiente de una fase siguiente. | **Alto** | `config/SecurityConfig.java:22-25`, `config/JwtAuthenticationFilter.java:29-31` |
| 10 | Seguridad — Contraseñas | ✅ | `BCryptPasswordEncoder` usado para verificar clave en login (`UsuarioService.verificarContrasena`); `Usuario.clave` no se serializa en `UsuarioResponse`. | Bajo | `usuario/UsuarioService.java`, `usuario/UsuarioResponse.java` |
| 10 | Seguridad — Secretos | ✅ | `application.properties` real (con `jwt.secret`, credenciales DB, token de API externa) está gitignoreado y **nunca estuvo en el historial de git** (`git log --all` sobre el archivo: sin resultados). Solo `application-example.properties` con placeholders está versionado. | Bajo | `.gitignore:26`, `src/main/resources/application.properties` (no versionado) |
| 10 | Seguridad — CSRF/CORS | ⚠️ | CSRF deshabilitado globalmente; superficie real reducida a `/api/auth/refresh` y `/api/auth/logout` (únicos que leen la cookie httpOnly de refresh) — el propio `SecurityConfig` lo documenta como pendiente de revisión a fondo. CORS con `Customizer.withDefaults()` (configuración por defecto de Spring, no hay `CorsConfigurationSource` propio revisado explícitamente para producción). | Medio | `config/SecurityConfig.java:26-31,58` |
| 10 | Seguridad — Logout | ⚠️ | Logout solo borra la cookie del navegador que hace la petición; no hay blacklist de servidor para el access token ya emitido (documentado explícitamente como limitación conocida en `AuthController`). Con TTL de 30 min para el access token, la ventana de exposición es acotada pero real si un token se filtra. | Medio | `usuario/AuthController.java:101-107` |
| 10 | Seguridad — Validación de entrada | ✅ | `@Valid`/Bean Validation presente en los DTOs `*Request` revisados; sin concatenación de SQL en ningún `@Query` (JPQL parametrizado). | Bajo | `*/*.Request.java`, `*/*.Repository.java` |
| 11 | Comunicación entre capas | ✅ | `RestTemplate` (usado hacia el microservicio Node de contacto/RENIEC-SUNAT) tiene timeout de conexión y lectura configurados centralmente (10 s cada uno). | Bajo | `config/WebConfig.java:25-30` |
| 11 | Comunicación entre capas | ⚠️ | Sin reintentos ni circuit breaker ante fallos del microservicio Node (`127.0.0.1:3000`, no disponible en la mayoría de entornos) — el código maneja el fallo devolviendo un mensaje de error controlado, pero no hay backoff/retry. | Bajo-Medio (el fallo ya se maneja sin caerse) | `common/WebController.java`, `common/PublicSiteController.java`, `cliente/ClienteService.java` |
| 12 | Docker | ⚠️ | `docker-compose.yml` en la raíz **solo levanta MySQL** para desarrollo local (`MYSQL_ALLOW_EMPTY_PASSWORD=yes`, aceptable solo en dev local, puerto mapeado `3308:3306`). Sin healthcheck definido. | Bajo (es explícitamente solo para dev local, no para producción) | `docker-compose.yml` |
| 12 | Docker | ❌ | **No implementado**: no existe `Dockerfile` para la aplicación Spring Boot ni para el frontend — no hay build multi-stage, imagen liviana, ni usuario no-root porque no hay imagen de la app todavía. | — (gap, no riesgo activo) | — |
| 13 | CI/CD | ❌ | **No implementado**: no existe `.github/workflows/`. El único contenido de `.github/` son artefactos de una herramienta de modernización de Java (`.github/java-upgrade/`, `.github/modernize/`), no un pipeline. Sin build/test/análisis estático/build de imagen automatizado. | Medio (sin quality gate que bloquee un merge con tests rotos) | — |

---

## 3. Evidencia — Response con lógica de negocio (hallazgo §2)

`CreditoVentaService.java` construye el DTO y luego **llama a métodos del propio DTO** para completar sus campos derivados:

```java
// venta/CreditoVentaService.java:471-473
response.setDiasParaVencer(response.calcularDiasParaVencer());
response.setDiasVencida(response.calcularDiasVencida());
response.setPorcentajePagado(response.calcularPorcentajePagado());
```

Y esos métodos viven dentro de la clase de transporte, no en el Service ni en la entidad:

```java
// venta/CuotaPagoResponse.java:55-76
public Long calcularDiasParaVencer() {
    if (estado == EstadoCuota.PAGADA) return 0L;
    long dias = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), fechaVencimiento);
    return dias > 0 ? dias : 0L;
}

public BigDecimal calcularPorcentajePagado() {
    if (montoCuota.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
    return montoPagado.multiply(BigDecimal.valueOf(100))
            .divide(montoCuota, 2, java.math.RoundingMode.HALF_UP);
}
```

El mismo patrón se repite en `CreditoVentaResponse.calcularPorcentajePagado()` / `calcularDiasRestantes()`. Efecto práctico: esta regla de negocio (qué es "vencido", cómo se calcula el % pagado) no se puede testear sin instanciar el DTO completo, y queda fuera de la cobertura de `CreditoVentaServiceTest` (que sí existe, pero no ejercita estos métodos directamente).

---

## 4. Top 5 problemas críticos (por impacto)

1. **Sin autorización por rol (RBAC)** — cualquier usuario autenticado puede operar cualquier endpoint administrativo, sin importar su `Perfil`. Es el hallazgo de mayor impacto real: un vendedor con cuenta activa podría, vía API directa, gestionar usuarios o perfiles aunque la UI no se lo muestre. (`SecurityConfig.java`, `JwtAuthenticationFilter.java`)
2. **Testing crítico insuficiente** — 10 de 12 Services sin ningún test, incluyendo `VentaService` (creación de ventas, integración con crédito/inventario) y `UsuarioService` (reglas de "último administrador", ya una vez causaron un bug real de reactivación silenciosa según el historial del proyecto).
3. **Lógica de negocio en DTOs de crédito** — `CuotaPagoResponse`/`CreditoVentaResponse` calculan reglas de vencimiento/porcentaje pagado, dificultando testearlas de forma aislada y violando la separación DTO/lógica que el propio proyecto respeta en los otros 10 DTOs.
4. **Serie de comprobante con dos fuentes de verdad** — `CheckoutController`/`CartController` hardcodean `F001`/`B001` en paralelo a `ComprobanteSecuenciaService`, con riesgo real de números de comprobante inconsistentes entre el checkout público y el POS admin.
5. **Sin paginación real en listados grandes** — funciona hoy por volumen bajo de datos, pero es el tipo de deuda que se vuelve incidente de producción sin aviso previo a medida que crece el catálogo/ventas.

---

## 5. Plan de refactor sugerido por fases

Dado que hay entregas de curso en curso y el proyecto está en producción activa de desarrollo (no romper lo que funciona):

- **Fase A — Autorización (RBAC).** Extender `JwtAuthenticationFilter`/`SecurityConfig` para cargar las `Opcion` del `Perfil` del usuario como `GrantedAuthority` y usar `@PreAuthorize` o `requestMatchers(...).hasAuthority(...)` por endpoint. Es el cambio de mayor impacto de seguridad y el propio código ya está preparado para recibirlo (el Javadoc de `SecurityConfig` lo señala como "fase siguiente").
- **Fase B — Tests de los Services críticos.** Priorizar `VentaService` y `UsuarioService` (por su historial de bugs reales de negocio), luego `ProductService`/`BrandService`. Mismo patrón que los dos tests existentes (`@ExtendWith(MockitoExtension.class)`).
- **Fase C — Extraer lógica de `CuotaPagoResponse`/`CreditoVentaResponse` a `CreditoVentaService`** (o a un `CreditoCalculator` dedicado), dejando los DTOs como transporte puro — sin cambiar el JSON de salida, solo moviendo dónde se calcula.
- **Fase D — Unificar la resolución de serie de comprobante** en `ComprobanteSecuenciaService`, eliminando el hardcode de `CheckoutController`/`CartController`.
- **Fase E — Paginación real** en los endpoints de listado de Productos/Ventas/Clientes (`Pageable` de extremo a extremo, backend + frontend).
- **Fase F (más adelante, no urgente para el curso) — Docker de la app y CI/CD básico** (build + test en cada push a `develop`), una vez el RBAC y los tests críticos estén cerrados.

---

## 6. Quick wins (bajo esfuerzo, alto impacto)

- Agregar `timestamp` y `path` al formato de error de `ApiResponses.error` — cambio de una función, mejora la trazabilidad de cualquier error reportado por el frontend.
- Corregir `GlobalExceptionHandler.handleTypeMismatchException` para que devuelva JSON (`ResponseEntity<Map>`) en vez de `"redirect:/"`, consistente con el resto del handler — hoy cualquier error de tipo en una ruta `/api/**` devuelve una respuesta de vista en vez de JSON.
- Añadir `@EntityGraph`/`Pageable` a `ProductRepository`/`VentaRepository`/`ClienteRepository` allí donde ya existe el método pero sin paginar — es extender un patrón ya usado en `DetalleVentaRepository`, no introducir uno nuevo.
- Documentar (o remover) el `docker-compose.yml` como "solo desarrollo local" en el README, para que no se confunda con un setup de producción (ya usa `MYSQL_ALLOW_EMPTY_PASSWORD`).

---

¿Procedemos con el plan de refactor fase por fase (empezando por la Fase A, RBAC) antes de tocar código, o prefieres priorizar otro orden?

---

## 7. Seguimiento — estado al 14 de octubre de 2026

Revisión de cada hallazgo contra `develop` (HEAD `91cafa1`). Tests: 128 en
verde (`./mvnw test`, sin MySQL ni secretos), más CI en GitHub Actions.

| # | Hallazgo | Estado | Dónde se resolvió |
|---|---|---|---|
| 1 | Controllers híbridos (vista Thymeleaf + API) | ✅ | Thymeleaf retirado por completo (`d872cab`): los controllers son `@RestController` y la interfaz es 100 % React. |
| 1 / 5 | Serie de comprobante duplicada (`F001`/`B001`) | ✅ | Fase D (`eb51f00`): todo pasa por `ComprobanteSecuenciaService`. |
| 2 | Lógica de negocio en DTOs de crédito | ✅ | Fase C (`01281f9`). |
| 3 | Sin `record` en DTOs | ➖ Se mantiene | Estilo, no correctitud; se conserva Lombok por consistencia. |
| 6 | Error sin `timestamp`/`path`; TypeMismatch devolvía vista | ✅ | `a1dd993`. Además: mensajes técnicos ocultos al cliente (`ApiResponses.mensajeSeguro`, `79b3a26`) y 403 en vez de 500 ante `@PreAuthorize` (`21646f7`). |
| 7 | Tests en 2 de 12 services | ✅ | Los 12 services (11 de dominio + `JwtService`) tienen tests (`ad15ef2`, `3ab5137`); test de contexto completo e integración de seguridad con MockMvc (`4bb327e`); `ControllersPreAuthorizeTest` impide endpoints sin permiso. |
| 8 / 9 | Sin paginación | ✅ | `8e0832f`: `/api/pagina` en productos, ventas y clientes (filtro en la BD, sin fetch join de colecciones) y paginador en React. |
| 9 | Prefijos `/api` no uniformes, sin versionado | ➖ Se mantiene | Renombrar rompería el frontend sin beneficio funcional hoy. |
| 10 | Sin RBAC | ✅ | Fase A (`0b25638`) y luego permisos por código estable (`opciones.permiso`) + `@PreAuthorize` por controller (`21646f7`); denegar por defecto (`b6f8c67`). |
| 10 | CORS por defecto / mappings sueltos | ✅ | `76a03db`: un único `CorsConfigurationSource`, configurable con `app.cors.allowed-origins`. |
| 10 | CSRF deshabilitado | ✅ Aceptado | Superficie limitada a `/api/auth/refresh` y `/logout`, cookie `httpOnly` + `SameSite=Strict`. |
| 10 | Logout sin revocar el access token | ✅ | Logout revoca access y refresh de la sesión por `jti` (tabla `tokens_revocados`, limpieza horaria). |
| 11 | Sin reintentos hacia el microservicio Node | ➖ Se mantiene | El fallo ya se maneja sin caer; reintentar un servicio local opcional no aporta. |
| 12 | Sin Dockerfile; compose sin healthcheck | ✅ | `e6ef1a8`: imágenes multi-stage no-root (backend JRE alpine, frontend nginx), healthcheck de MySQL, `acceso.sql` cargado al crear el volumen. |
| 13 | Sin CI/CD | ✅ | `91cafa1`: GitHub Actions (tests backend, lint + build frontend, build de imágenes). |

### Defectos encontrados durante el seguimiento (no estaban en este informe)

- Cada venta descontaba el stock dos veces; si no alcanzaba, la venta entera
  fallaba (`0b32eee`).
- Tras la migración a JWT, varios controllers seguían leyendo `HttpSession`:
  checkout web, crear venta, registrar pago, registrar movimiento de
  inventario y las protecciones de "no eliminarte a ti mismo" no funcionaban
  (`c5aa95c`, `79b3a26`).
- El carrito aceptaba cantidades negativas (`c5aa95c`).
- `acceso.sql` solo traía 3 opciones de menú: con el RBAC, el administrador
  quedaba sin acceso en una instalación nueva (`1ef7d38`).
- Anular una venta no registraba la devolución en el kardex (`79b3a26`).

### Decisiones tomadas después del seguimiento (14-oct)

Las tres decisiones pendientes se resolvieron: se retiró el Thymeleaf
(incluido el sitio público; se pierden las secciones de preguntas frecuentes
e información de la tienda, que React no tenía), se paginaron los listados y
el logout revoca los tokens en el servidor. Al hacerlo apareció otro defecto:
varias pantallas leían datos de otro dominio y el RBAC las bloqueaba (p. ej.
un vendedor no podía registrar ventas); se corrigió en `8feff3a`.
