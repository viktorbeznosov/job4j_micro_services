# Microservices Project: Auth & Payment

Учебный production-like проект микросервисной архитектуры на Java 17 + Spring Boot 3.

## Этапы разработки

**Первый этап (выполнен):** Реализация микросервисов (auth и payment) с бизнес-логикой, Docker Compose для локального запуска.

**Второй этап (текущий):** Kubernetes deployment - добавлена папка `infra/` с манифестами для развертывания в Kubernetes.

## Архитектура

```
├── auth/           # Сервис аутентификации и пользователей
├── payment/        # Сервис заявок на оплату
├── docker-compose.yml
└── README.md
```

### Взаимодействие сервисов

```
Client -> Payment (Bearer Token) -> Auth (validate token) -> Payment (business logic)
```

Payment сервис при каждом защищенном запросе:
1. Получает Bearer token из заголовка Authorization
2. Отправляет запрос в Auth: `GET /api/auth/internal/validate`
3. Получает данные пользователя (id, username, roles)
4. Проверяет права доступа на основе ролей
5. Выполняет бизнес-логику

## Почему в Payment нет Foreign Key на Users

В микросервисной архитектуре каждый сервис владеет своими данными. Таблица `users` находится в базе `auth_db`, а `payments` — в `payment_db`. 

**Причины отсутствия FK:**
- Нарушение изоляции данных микросервисов
- Сложности с транзакционностью между разными базами данных
- При падении Auth сервиса Payment продолжает работать (частично)

Вместо FK используется `userId` (bigint) как логическая связь. Валидность пользователя проверяется через вызов Auth API.

## Технологии

- Java 17
- Spring Boot 3.2.5
- Spring Security 6
- Spring Data JPA
- PostgreSQL
- Maven
- JWT (jjwt 0.12.3)
- Lombok
- Swagger/OpenAPI
- Docker Compose

## Тестовые пользователи

Создаются автоматически при запуске Auth сервиса:

| Username | Password  | Email              | Full Name           | Role         |
|----------|-----------|--------------------|---------------------|--------------|
| admin    | admin123  | admin@test.com     | Администратор       | ROLE_ADMIN   |
| user     | user123   | user@test.com      | Обычный Пользователь| ROLE_USER    |
| manager  | manager123| manager@test.com   | Менеджер            | ROLE_MANAGER |

## Базы данных

Создайте две базы данных в PostgreSQL:
```sql
CREATE DATABASE auth_db;
CREATE DATABASE payment_db;
```

Или они будут созданы автоматически при использовании docker-compose.

## Как запустить

### Локально (без Docker)

1. Запустите PostgreSQL локально
2. Создайте базы данных `auth_db` и `payment_db`
3. Настройте `application.yml` в обоих сервисах (уже настроено для localhost)
4. Соберите проекты:
   ```bash
   cd auth && mvn clean package -DskipTests
   cd ../payment && mvn clean package -DskipTests
   ```
5. Запустите Auth сервис:
   ```bash
   cd auth && java -jar target/auth-service-1.0.0.jar
   ```
6. Запустите Payment сервис:
   ```bash
   cd payment && java -jar target/payment-service-1.0.0.jar
   ```

### Через Docker Compose

```bash
docker-compose up --build
```

Сервисы будут доступны по адресам:
- Auth: http://localhost:8081
- Payment: http://localhost:8082
- PostgreSQL: localhost:5432

## Роли и права доступа

### ROLE_USER
- Регистрация: `/api/auth/registration` (все)
- Логин: `/api/auth/login` (все)
- Просмотр своего профиля: `/api/auth/me`
- Создание заявки на оплату: `POST /api/payments`
- Просмотр своей заявки: `GET /api/payments/{id}`
- Просмотр своего профиля через Payment: `GET /api/payments/users/{id}` (только свой)

### ROLE_MANAGER
- Все что USER +
- Подтверждение заявки: `POST /api/payments/{id}/approve`
- Отклонение заявки: `POST /api/payments/{id}/reject`
- Просмотр всех заявок: `GET /api/payments`
- Просмотр любой заявки: `GET /api/payments/{id}`

### ROLE_ADMIN
- Просмотр всех пользователей: `GET /api/auth/users`
- Просмотр любого пользователя: `GET /api/auth/users/{id}`
- Просмотр всех заявок в Payment: `GET /api/payments`
- Просмотр любого пользователя через Payment: `GET /api/payments/users/{id}`

## Примеры запросов через curl

### 1. Регистрация нового пользователя
```bash
curl -X POST http://localhost:8081/api/auth/registration \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ivan",
    "email": "ivan@test.com",
    "password": "123456",
    "fullName": "Иван Иванов"
  }'
```

### 2. Логин пользователя (получение токена)
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user",
    "password": "user123"
  }'
```

Сохраните полученный токен:
```bash
TOKEN="eyJ..."
```

### 3. Получение текущего пользователя
```bash
curl -X GET http://localhost:8081/api/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Создание заявки на оплату (от имени user)
```bash
curl -X POST http://localhost:8082/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "description": "Оплата заказа #123"
  }'
```

### 5. Логин менеджера
```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "manager",
    "password": "manager123"
  }'
```

Сохраните токен менеджера:
```bash
MANAGER_TOKEN="eyJ..."
```

### 6. Просмотр всех заявок (менеджер)
```bash
curl -X GET http://localhost:8082/api/payments \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

### 7. Подтверждение заявки (менеджер)
```bash
curl -X POST http://localhost:8082/api/payments/1/approve \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

### 8. Отклонение заявки (менеджер)
```bash
curl -X POST http://localhost:8082/api/payments/1/reject \
  -H "Authorization: Bearer $MANAGER_TOKEN"
```

### 9. Просмотр пользователя через Payment
```bash
curl -X GET http://localhost:8082/api/payments/users/1 \
  -H "Authorization: Bearer $TOKEN"
```

### 10. Просмотр всех пользователей (admin)
```bash
curl -X GET http://localhost:8081/api/auth/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## Swagger UI

После запуска сервисов, Swagger UI доступен по адресам:
- Auth: http://localhost:8081/swagger-ui.html
- Payment: http://localhost:8082/swagger-ui.html

---

## Второй этап: Kubernetes Deployment

Папка `infra/` содержит Kubernetes манифесты для развертывания проекта в кластере.

### Быстрый старт

1. **Сборка Docker образов:**
```bash
# Для Minikube
minikube start
eval $(minikube docker-env)

cd auth && docker build -t demo/auth-service:latest .
cd ../payment && docker build -t demo/payment-service:latest .

# Для kind
kind create cluster
kind load docker-image demo/auth-service:latest
kind load docker-image demo/payment-service:latest
```

2. **Применение манифестов:**
```bash
kubectl apply -k infra/
```

3. **Проверка развертывания:**
```bash
kubectl get all -n microservices-demo
kubectl get ingress -n microservices-demo
```

### Архитектура в Kubernetes

```
├── auth/           # Сервис аутентификации и пользователей
├── payment/        # Сервис заявок на оплату
├── infra/          # Kubernetes манифесты
│   ├── namespace.yaml
│   ├── kustomization.yaml
│   ├── configmap/
│   ├── secrets/
│   ├── postgres-auth/
│   ├── postgres-payment/
│   ├── auth/
│   ├── payment/
│   ├── ingress/
│   └── README.md
├── docker-compose.yml
└── README.md
```

### Как работает сервисы в Kubernetes

1. **Docker образы**: Сервисы собираются как multi-stage Docker образы (Maven build + JRE runtime)
2. **Service + DNS**: 
   - Auth доступен по: `http://auth-service.microservices-demo.svc.cluster.local:8081`
   - Payment доступен по: `http://payment-service.microservices-demo.svc.cluster.local:8082`
3. **Межсервисное взаимодействие**: Payment обращается к Auth через Kubernetes DNS
   - Переменная `AUTH_SERVICE_URL=http://auth-service.microservices-demo.svc.cluster.local:8081`
4. **Внешний трафик**: Идет через Ingress
   - `http://auth.localdev.me` -> auth-service:8081
   - `http://payment.localdev.me` -> payment-service:8082

### Команды для проверки

```bash
# Проверка подов
kubectl get pods -n microservices-demo
kubectl logs -f <pod-name> -n microservices-demo

# Проверка сервисов
kubectl get svc -n microservices-demo

# Проверка ingress
kubectl get ingress -n microservices-demo
kubectl describe ingress app-ingress -n microservices-demo

# Проверка DNS внутри кластера
kubectl exec -it <payment-pod> -n microservices-demo -- nslookup auth-service

# Проверка связи между сервисами
kubectl exec -it <payment-pod> -n microservices-demo -- wget -qO- http://auth-service:8081/actuator/health
```

### Внешние адреса

Добавьте в `/etc/hosts` (или `C:\Windows\System32\drivers\etc\hosts`):
```
127.0.0.1 auth.localdev.me
127.0.0.1 payment.localdev.me
```

Для Minikube выполните:
```bash
minikube tunnel
```

Теперь доступны:
- http://auth.localdev.me/swagger-ui/index.html
- http://payment.localdev.me/swagger-ui/index.html

### Примеры запросов через Ingress

```bash
# Логин
curl -X POST http://auth.localdev.me/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Создание платежа (с токеном)
curl -X POST http://payment.localdev.me/api/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"description":"Test payment"}'
```

### Подробная документация

См. `infra/README.md` для детального описания всех манифестов и порядка деплоя.

## Структура базы данных

### Auth DB
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL
);

CREATE TABLE user_roles (
    user_id BIGINT REFERENCES users(id),
    role_id INT REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);
```

### Payment DB
```sql
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    manager_id BIGINT,
    status VARCHAR(20) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);
```

## Как устроена авторизация

1. **Auth Service** генерирует JWT токен при успешном логине
2. Токен содержит: `sub` (username), `userId`, `username`, `roles`
3. Токен подписывается HS256 с использованием секрета из конфигурации
4. При каждом запросе в Payment, токен передается в заголовке `Authorization: Bearer <token>`
5. Payment вызывает Auth для валидации токена и получения данных пользователя

## Как Payment валидирует пользователя через Auth

```java
// PaymentController
@PostMapping
public ResponseEntity<PaymentResponse> createPayment(
        @RequestHeader("Authorization") String authorizationHeader) {
    CurrentUserDto currentUser = currentUserProvider.getCurrentUser(authorizationHeader);
    // currentUser содержит id, username, email, roles
    // Далее используем эти данные для проверки прав доступа
}
```

Процесс валидации:
1. `CurrentUserProvider.getCurrentUser()` вызывает `AuthClient.validateTokenAndGetUser()`
2. `AuthClient` отправляет запрос в Auth: `GET /api/auth/internal/validate`
3. Auth валидирует JWT токен и возвращает данные пользователя
4. Payment получает `CurrentUserDto` и проверяет права на основе ролей

## Что можно улучшить для Production

1. **Безопасность:**
   - Хранить JWT secret в Vault или环境 переменных
   - Использовать HTTPS
   - Добавить rate limiting
   - Усилить валидацию входных данных

2. **Надежность:**
   - Добавить Circuit Breaker (Resilience4j) для вызовов между сервисами
   - Настроить retry механизм для AuthClient
   - Добавить кеширование пользователей в Payment (Redis)

3. **Мониторинг:**
   - Добавить Distributed Tracing (Zipkin/Sleuth)
   - Настроить метрики (Micrometer/Prometheus)
   - Centralized logging (ELK stack)

4. **База данных:**
   - Настроить connection pooling (HikariCP)
   - Добавить индексы на часто используемые поля
   - Настроить миграции (Flyway/Liquibase) вместо hibernate ddl-auto

5. **API:**
   - Добавить версионирование API
   - Пагинация для списков
   - Добавить фильтрацию и сортировку

6. **Service Discovery:**
   - Добавить Eureka или использовать Kubernetes service discovery
   - Centralized configuration (Spring Cloud Config)

## Лицензия

Учебный проект. Свободное использование.
