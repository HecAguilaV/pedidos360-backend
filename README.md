# Pedidos360 - Backend

Sistema backend de microservicios para la plataforma de comercio electrónico **Pedidos360** (Cloud Native). 

Diseñado con arquitectura orientada a eventos, comunicación asíncrona mediante mensajería y contenedores Docker para despliegue en la nube (AWS EC2 / RDS).

---

## 1. Diagrama de Arquitectura del Sistema

El siguiente diagrama muestra cómo interactúan los clientes, la puerta de enlace (Nginx), los 6 microservicios y la capa de infraestructura (Base de Datos y Broker de Eventos):

```mermaid
flowchart TD
    subgraph Acceso["Puerta de Entrada"]
        Navegador["Navegador Web / Frontend"]
        Nginx["Nginx Reverse Proxy & Gateway (:80 / :8080)"]
    end

    subgraph Microservicios["Microservicios Spring Boot (Java 21)"]
        MS_Prod["📦 productos-service (:8082)<br/>Catálogo y Stock"]
        MS_Ped["🛒 pedidos-service (:8081)<br/>Gestión de Órdenes"]
        MS_Cart["🛍️ carrito-service (:8083)<br/>Carritos de Compra"]
        MS_Notif["✉️ notifications-service (:8084)<br/>Boletas y Correos SMTP"]
        MS_Env["🚚 envios-service (:8085)<br/>Despachos y Guías"]
        MS_Pag["💳 pagos-service (:8086)<br/>Cobros y Liquidaciones"]
    end

    subgraph Infraestructura["Infraestructura y Datos"]
        DB[("🐘 PostgreSQL (:5432)<br/>Amazon RDS / Docker")]
        Rabbit["🐰 RabbitMQ Broker (:5672)<br/>Topic & Direct Exchanges"]
    end

    Navegador -->|HTTP REST| Nginx
    Nginx -->|/api/v1/productos| MS_Prod
    Nginx -->|/api/v1/pedidos| MS_Ped
    Nginx -->|/api/v1/carrito| MS_Cart
    Nginx -->|/api/v1/notificaciones| MS_Notif
    Nginx -->|/api/v1/envios| MS_Env
    Nginx -->|/api/v1/pagos| MS_Pag

    MS_Prod & MS_Ped & MS_Cart & MS_Env & MS_Pag -->|Lectura / Escritura| DB
    MS_Ped -->|Publica orden.creada| Rabbit
    Rabbit -->|Dispara tareas en paralelo| MS_Prod & MS_Notif & MS_Env & MS_Pag & MS_Cart
    MS_Pag -->|Publica pago.procesado| Rabbit
    Rabbit -->|Confirma pago a la orden| MS_Ped
```

---

## 2. Diagrama de Flujo de una Compra (Coreografía de Eventos)

Cuando un cliente completa una compra, los servicios no se llaman de forma síncrona ("bloqueante") entre sí, sino que colaboran mediante **eventos asíncronos** en RabbitMQ:

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Cliente (Navegador)
    participant Nginx as Nginx Gateway
    participant Pedidos as pedidos-service
    participant BD as PostgreSQL
    participant Rabbit as RabbitMQ (Broker)
    participant Servicios as Servicios Consumidores

    Cliente->>Nginx: POST /api/v1/pedidos (Pagar carrito)
    Nginx->>Pedidos: Enruta petición
    Pedidos->>BD: Guarda pedido en estado PENDIENTE
    Pedidos->>Rabbit: Publica evento "orden.creada" tras commit exitoso
    Pedidos-->>Cliente: Responde 201 Created (Pedido recibido)

    par Coreografía en segundo plano
        Rabbit->>Servicios: productos-service: Reserva y descuenta stock
        Rabbit->>Servicios: envios-service: Asigna tracking (TRACK-2026-XXXXX)
        Rabbit->>Servicios: notifications-service: Envía boleta por correo (SMTP)
        Rabbit->>Servicios: carrito-service: Vacía el carrito del usuario
        Rabbit->>Servicios: pagos-service: Registra y procesa el pago
    end

    Servicios->>Rabbit: pagos-service publica "pago.procesado"
    Rabbit->>Pedidos: pedidos-service recibe confirmación de pago
    Pedidos->>BD: Actualiza estado del pedido a PAGADO
```

---

## 3. ¿Qué hace cada microservicio?

| Microservicio | Puerto | Responsabilidad Principal |
|---|---|---|
| **`productos-service`** | `8082` | Catálogo de productos, precios, control de stock y reserva automática al comprar. |
| **`pedidos-service`** | `8081` | Registro de pedidos, máquina de estados (`PENDIENTE` $\rightarrow$ `PAGADO`) y publicación de eventos. |
| **`carrito-service`** | `8083` | Carrito persistente por usuario y vaciado automático tras la compra. |
| **`notifications-service`** | `8084` | Generación de boletas electrónicas en HTML y envío automático de correos vía SMTP (Gmail). |
| **`envios-service`** | `8085` | Logística, cálculo de despacho y generación de número de guía único (`TRACK-2026-XXXXX`). |
| **`pagos-service`** | `8086` | Registro de transacciones de pago e integración asíncrona con pedidos. |

---

## 4. Puesta en Marcha Rápida (Local o AWS)

### Paso 1: Configurar variables de entorno
Copia la plantilla de configuración:
```bash
cp .env.example .env
```
*(Si estás desplegando en AWS, puedes usar `./configure-lab.sh` para ingresar las credenciales de RDS y Cognito de forma guiada).*

### Paso 2: Levantar todos los servicios con Docker
```bash
docker compose up --build -d
```

### Paso 3: Verificar que todo esté funcionando
```bash
# Ver estado de los contenedores
docker compose ps

# Monitorear logs en vivo
docker compose logs -f
```

---

## 5. Resumen de Variables de Entorno (`.env`)

| Variable | Para qué sirve | Ejemplo |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Modo de seguridad (`dev` para pruebas locales, `prod` para AWS con validación JWT) | `prod` |
| `DB_HOST` | Servidor de base de datos (`postgres` en local o endpoint de RDS en AWS) | `pedidos360-db.xxxx.us-east-1.rds.amazonaws.com` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `pedidos360` |
| `DB_USERNAME` | Usuario de la base de datos | `postgres` |
| `DB_PASSWORD` | Contraseña de la base de datos | `<TU_PASSWORD_RDS>` |
| `RABBITMQ_USER` | Usuario para el broker RabbitMQ | `guest` |
| `RABBITMQ_PASS` | Contraseña para RabbitMQ | `<TU_PASSWORD_RABBITMQ>` |
| `MAIL_USERNAME` | Correo remitente para envío de boletas | `correo@gmail.com` |
| `MAIL_PASSWORD` | Clave de aplicación de Gmail (16 dígitos) | `<APP_PASSWORD_16_DIGITOS>` |
| `JWT_ISSUER` | Emisor de tokens Microsoft Entra ID | `https://login.microsoftonline.com/<TENANT_ID>/v2.0` |
| `JWT_AUDIENCE` | Audiencia de la API en Entra ID | `api://<CLIENT_ID>,<CLIENT_ID>` |
| `COGNITO_USER_POOL_ID` | Identificador del User Pool de AWS Cognito | `us-east-1_AbCdEf123` |
| `COGNITO_CLIENT_ID` | App Client ID de AWS Cognito | `1a2b3c4d5e6f7g8h9i0j` |
| `CORS_ALLOWED_ORIGINS` | URLs autorizadas para llamar a las APIs (frontend / CloudFront) | `https://<id>.cloudfront.net,http://<IP-EC2>` |
