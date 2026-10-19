# Pedidos360 - Backend

Repositorio de microservicios backend para la plataforma **Pedidos360** (Cloud Native).

## Arquitectura de Servicios

- **productos-service** (`:8082`): Gestión de catálogo de productos (CRUD) y actualización asíncrona de inventario.
- **pedidos-service** (`:8081`): Gestión del ciclo de vida de pedidos y publicación de `orden.creada`.
- **carrito-service** (`:8083`): Gestión de carrito de compras por usuario.
- **notifications-service** (`:8084`): Envío de boletas electrónicas HTML vía SMTP (consumidor RabbitMQ `order-notifications` y endpoints directos REST/form-data).
- **envios-service** (`:8085`): Gestión de despachos y generación de códigos de seguimiento (`TRACK-2026-XXXXX`).
- **pagos-service** (`:8086`): Procesamiento de pagos y emisión de `pago.procesado`.
- **RabbitMQ** (`:5672` / `:15672`): Broker de mensajería AMQP (`orders.exchange`).
- **PostgreSQL** (`:5432`): Base de datos relacional (Amazon RDS PostgreSQL en producción / Docker en local).

## Coreografía de Eventos (RabbitMQ)

- **Topología Pub/Sub (`TopicExchange` `orders.exchange`):** `pedidos-service` publica `orden.creada` hacia `orders.exchange` únicamente **después del commit** de la transacción en PostgreSQL (`TransactionSynchronization.afterCommit()`), evitando eventos huérfanos si la escritura falla.
- Cinco colas durables consumen `orden.creada`: `stock-decrement` (`productos-service`), `order-notifications` (`notifications-service`), `shipping-dispatch` (`envios-service`), `order-payments` (`pagos-service`) y `cart-clear` (`carrito-service`).
- **Topología Punto a Punto (`DirectExchange` `payments.direct.exchange`):** `pagos-service` registra la liquidación y publica `pago.procesado` hacia la cola dedicada `order-payment-confirmed` dentro de `TransactionSynchronization.beforeCommit(false)`. De esta forma, si el broker rechaza la publicación, la transacción local de `Pago` hace rollback y el mensaje `orden.creada` puede reintentarse sin ser descartado por el filtro de idempotencia (`findByOrderId`). `pedidos-service` consume `order-payment-confirmed` y actualiza la orden a `PAGADO`.

## Requisitos

- Docker & Docker Compose
- Java 21 (opcional para ejecución fuera de Docker)

## Ejecución Local

1. Copiar las variables de entorno (o ejecutar `./configure-lab.sh` en AWS):
   ```bash
   cp .env.example .env
   ```

2. Levantar la base de datos, los microservicios y el gateway Nginx:
   ```bash
   docker compose up --build -d
   ```

3. Verificar logs:
   ```bash
   docker compose logs -f
   ```

4. Endpoints disponibles en local (perfil `dev`, vía gateway `:80` / `:8080` o puertos directos):
   - **Productos:**
     - `GET http://localhost:8082/api/v1/productos`
     - `GET http://localhost:8082/api/v1/productos/1`
     - `POST http://localhost:8082/api/v1/productos`
   - **Pedidos:**
     - `GET http://localhost:8081/api/v1/pedidos`
     - `GET http://localhost:8081/api/v1/pedidos/1`
     - `POST http://localhost:8081/api/v1/pedidos`
   - **Notificaciones (`notifications-service`):**
     - `POST http://localhost:8084/api/email/enviar` (`multipart/form-data` o `application/x-www-form-urlencoded` con parámetros `to`, `subject`, `body`)
     - `POST http://localhost:8084/api/v1/notificaciones/enviar` (JSON payload)
     - `GET http://localhost:8084/api/v1/notificaciones/status`

## Variables de Entorno para Producción (AWS EC2 + RDS + Entra ID + Cognito)

Al desplegar en **AWS EC2** (o al rotar de laboratorio con `./configure-lab.sh`), configurar en `.env`:

| Variable | Descripción | Ejemplo |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Activa validación estricta JWT y RBAC (`prod` en AWS, `dev` en local) | `prod` |
| `DB_HOST` | Endpoint de Amazon RDS PostgreSQL (o `postgres` en Docker) | `pedidos360-db.xxxx.us-east-1.rds.amazonaws.com` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `pedidos360` |
| `DB_USERNAME` | Usuario maestro RDS o Postgres local | `postgres` |
| `DB_PASSWORD` | Contraseña de base de datos | `<TU_PASSWORD_RDS>` |
| `RABBITMQ_USER` | Usuario broker RabbitMQ | `guest` (o personalizado) |
| `RABBITMQ_PASS` | Contraseña broker RabbitMQ | `<TU_PASSWORD_RABBITMQ>` |
| `MAIL_HOST` | Servidor SMTP para boletas de notifications-service | `smtp.gmail.com` |
| `MAIL_PORT` | Puerto SMTP TLS | `587` |
| `MAIL_USERNAME` | Cuenta de correo emisora | `<TU_CORREO>@gmail.com` |
| `MAIL_PASSWORD` | Contraseña de aplicación (16 dígitos) | `<APP_PASSWORD_16_DIGITOS>` |
| `JWT_ISSUER` | Issuer de Microsoft Entra ID | `https://login.microsoftonline.com/<TENANT_ID>/v2.0` |
| `JWT_AUDIENCE` | Audience registrada en Entra (App ID URI) | `api://<BACKEND_APP_ID>,<BACKEND_APP_ID>` |
| `COGNITO_REGION` | Región del User Pool de AWS Cognito | `us-east-1` |
| `COGNITO_USER_POOL_ID` | ID del User Pool (inyectado en runtime al frontend) | `us-east-1_AbCdEf123` |
| `COGNITO_CLIENT_ID` | App Client ID público de Cognito | `1a2b3c4d5e6f7g8h9i0j` |
| `COGNITO_DOMAIN` | Dominio Hosted UI de Cognito | `https://<dominio>.auth.us-east-1.amazoncognito.com` |
| `COGNITO_REDIRECT_URI` | Redirect URI tras OAuth2 Hosted UI | `https://<id>.cloudfront.net/login` |
| `COGNITO_ISSUER` | Issuer validado por los 6 microservicios | `https://cognito-idp.us-east-1.amazonaws.com/us-east-1_AbCdEf123` |
| `CORS_ALLOWED_ORIGINS` | Orígenes habilitados en CORS (separados por coma) | `https://<id>.cloudfront.net,http://<IP-ELASTICA-EC2>` |

### Inyección Runtime de AWS Cognito en el Frontend (Rotación sin Rebuild)
El contenedor `pedidos360-frontend` ejecuta `/docker-entrypoint.d/40-runtime-config.sh` al iniciar Nginx y genera `/usr/share/nginx/html/assets/runtime-config.json` a partir de las variables `COGNITO_*` definidas en `.env` y pasadas en `docker-compose.yml`. Cuando cambia el laboratorio de AWS, basta con correr `./configure-lab.sh` y `docker compose up -d` sin recompilar la imagen de Angular. La configuración de Microsoft Entra ID permanece en `src/environments/environment.prod.ts`.

### HTTPS para Microsoft Entra ID (MSAL / WebCrypto) en AWS EC2
Microsoft Entra ID exige `https://` para Redirect URIs que no sean `localhost`, y `@azure/msal-browser` requiere `window.crypto.subtle` (disponible únicamente en contextos seguros `https://` o `http://localhost`). Dado que AWS Certificate Manager (ACM) no emite certificados para IPs públicas, se debe crear una distribución **AWS CloudFront** frente al origen HTTP (puerto `80`) de la instancia EC2 para obtener un dominio `https://<id>.cloudfront.net` con certificado TLS válido de AWS, y registrar ese `https://<id>.cloudfront.net` como Redirect URI SPA en Azure Entra ID.
