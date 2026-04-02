# Order Management System (OMS)

Un prototipo de sistema de gestión de pedidos construido con **Java 21**, **Spring Boot 3.x** y estructurado bajo los principios arquitectónicos **Hexagonales (Ports and Adapters)** orientados al desarrollo Cloud-Native.

---

## 🏗️ Arquitectura

El sistema aplica estrictamente una separación de responsabilidades:
- **Dominio Central (`com.oms.domain`)**: Contiene las reglas de negocio base (`Order`, `OrderItem`), puras y sin dependencias de frameworks externos.
- **Puertos y Casos de Uso (`com.oms.application`)**: Las interfaces que dictan qué se puede hacer de entrada (REST) y qué recursos precisamos de salida (Kafka, Redis, Postgres).
- **Adaptadores de Infraestructura (`com.oms.infrastructure`)**: Módulos técnicos reemplazables. Almacenaje relacional con JPA, caché local con Redis y distribución de eventos mediante Apache Kafka.

---

## 🚀 Requisitos y Configuración Local

Para levantar el proyecto en tu entorno local, asegúrate de tener instalados:
- **Java 21**
- **Docker** y **Docker Compose**
- **Maven** (opcional si usas el wrapper)

### 1. Variables de Entorno (.env)
El sistema cumple con el III Factor de *12-Factor Apps* (Configuración en el ambiente). Copia el archivo `Template` para tus credenciales locales:
```bash
cp .env.example .env
```

### 2. Levantar los 'Attached Resources' (Docker Compose)
Ejecuta la orquestación local para levantar PostgreSQL, Redis y Kafka:
```bash
docker-compose up -d
```
> Puedes inspeccionar si los contenedores están saludables con `docker-compose ps`.

### 3. Ejecutar la Aplicación Spring Boot
```bash
mvn spring-boot:run
```

---

## 📡 API Endpoints

### 1. Crear un Pedido
**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/orders \
-H "Content-Type: application/json" \
-d '{
  "customerName": "John Doe",
  "items": [
    {
      "productId": "PROD-001",
      "productName": "Laptop Mac",
      "quantity": 1,
      "unitPrice": 1200.50
    }
  ]
}'
```
**Efecto:** Guardado en PostgreSQL y evento `OrderCreatedEvent` emitido hacia el Topic `order-events` de Kafka.

### 2. Buscar un Pedido (por UUID)
Obtiene un pedido existente. El patrón _Cache-Aside_ lo buscará primero en **Redis**.
**Request:**
```bash
curl http://localhost:8080/api/v1/orders/{UUID}
```

### 3. Actualizar Estado de un Pedido
Mutará el comportamiento validado del root aggregate.
**Request:**
```bash
curl -X PATCH http://localhost:8080/api/v1/orders/{UUID}/status \
-H "Content-Type: application/json" \
-d '{"status": "CONFIRMED"}'
```

---

## 🔍 Observabilidad y Trazabilidad (Fase 4)
El sistema implementa **Distributed Tracing** con **Micrometer Tracing** y **OpenTelemetry** para rastrear peticiones entre el API y Kafka.

- **Zipkin UI:** Visualiza trazas de peticiones en `http://localhost:9411`
- **Logs:** Los logs incluyen `traceId` y `spanId` entre corchetes (ej: `[oms, 5f1..., a2b...]`).
- **Healthcheck & Status:** `http://localhost:8080/actuator/health`
- **Métricas Internas:** `http://localhost:8080/actuator/metrics`
- **Kafka UI:** `http://localhost:8090`
