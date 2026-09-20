# Pedidos360 - Backend

Repositorio de microservicios backend para la plataforma **Pedidos360** (Cloud Native).

## Arquitectura de Servicios

- **productos-service** (`:8082`): Gestión de catálogo de productos (CRUD).
- **pedidos-service** (`:8081`): Gestión del ciclo de vida de pedidos.
- **PostgreSQL** (`:5432`): Base de datos relacional (Amazon RDS PostgreSQL en producción / Docker en local).

## Requisitos

- Docker & Docker Compose
- Java 21 (opcional para ejecución fuera de Docker)

## Ejecución Local

1. Copiar las variables de entorno (opcional si usás los defaults):
   ```bash
   cp .env.example .env
   ```

2. Levantar la base de datos y los microservicios:
   ```bash
   docker compose up --build -d
   ```

3. Verificar logs:
   ```bash
   docker compose logs -f
   ```

4. Endpoints disponibles en local (perfil `dev`):
   - **Productos:**
     - `GET http://localhost:8082/api/v1/productos`
     - `GET http://localhost:8082/api/v1/productos/1`
     - `POST http://localhost:8082/api/v1/productos`
   - **Pedidos:**
     - `GET http://localhost:8081/api/v1/pedidos`
     - `GET http://localhost:8081/api/v1/pedidos/1`
     - `POST http://localhost:8081/api/v1/pedidos`

## Variables de Entorno para Producción (AWS + Microsoft Entra ID)

Al desplegar en **AWS EC2** con **RDS** y **Entra ID**, configurar en el entorno:

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_HOST` | Endpoint del clúster Amazon RDS | `pedidos360-db.xyz.us-east-1.rds.amazonaws.com` |
| `DB_PORT` | Puerto de PostgreSQL | `5432` |
| `DB_NAME` | Nombre de la base de datos | `pedidos360` |
| `DB_USERNAME` | Usuario maestro RDS | `postgres` |
| `DB_PASSWORD` | Contraseña RDS | `miPasswordSeguro123` |
| `JWT_ISSUER` | Issuer de Microsoft Entra ID | `https://login.microsoftonline.com/<TENANT_ID>/v2.0` |
| `JWT_AUDIENCE` | Audience registrada en Entra | `api://<BACKEND_APP_ID>` |
| `SPRING_PROFILES_ACTIVE` | Activa validación estricta JWT | `prod` |
