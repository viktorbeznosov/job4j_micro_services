# Kubernetes Infrastructure Guide

Этот каталог содержит Kubernetes манифесты для развертывания микросервисов.

## Структура

```
infra/
├── namespace.yaml                    # Создание namespace
├── kustomization.yaml               # Kustomize конфигурация
├── configmap/                       # ConfigMaps для сервисов
│   ├── auth-configmap.yaml
│   ├── payment-configmap.yaml
│   ├── postgres-auth-configmap.yaml
│   └── postgres-payment-configmap.yaml
├── secrets/                         # Secrets для чувствительных данных
│   ├── auth-secret.yaml
│   ├── payment-secret.yaml
│   ├── postgres-auth-secret.yaml
│   └── postgres-payment-secret.yaml
├── postgres-auth/                    # PostgreSQL для auth
│   ├── postgres-auth-deployment.yaml
│   ├── postgres-auth-service.yaml
│   ├── postgres-auth-pvc.yaml
│   └── postgres-auth-init-configmap.yaml
├── postgres-payment/                 # PostgreSQL для payment
│   ├── postgres-payment-deployment.yaml
│   ├── postgres-payment-service.yaml
│   ├── postgres-payment-pvc.yaml
│   └── postgres-payment-init-configmap.yaml
├── auth/                            # Auth сервис
│   ├── auth-deployment.yaml
│   └── auth-service.yaml
├── payment/                         # Payment сервис
│   ├── payment-deployment.yaml
│   └── payment-service.yaml
├── ingress/                         # Ingress правила
│   └── app-ingress.yaml
└── README.md                        # Этот файл
```

## Назначение файлов

### Namespace и Kustomize
- **namespace.yaml** - создает изолированное пространство имен `microservices-demo`
- **kustomization.yaml** - позволяет применить все манифесты одной командой

### ConfigMaps
Содержат нечувствительные настройки:
- **auth-configmap.yaml** - порт, профиль, хост БД, URL auth сервиса
- **payment-configmap.yaml** - порт, профиль, хост БД, URL auth сервиса для межсервисного взаимодействия
- **postgres-*-configmap.yaml** - имя БД и пользователя

### Secrets
Содержат чувствительные данные (в production использовать external secrets):
- **auth-secret.yaml** - пароль БД, JWT secret
- **payment-secret.yaml** - пароль БД
- **postgres-*-secret.yaml** - пароли postgres

### PostgreSQL
Каждый сервис имеет свою БД:
- **postgres-*-deployment.yaml** - Deployment с health probes и ресурсами
- **postgres-*-service.yaml** - ClusterIP сервис для внутреннего доступа
- **postgres-*-pvc.yaml** - PersistentVolumeClaim для хранения данных
- **postgres-*-init-configmap.yaml** - SQL скрипт инициализации БД

### Микросервисы
- **auth-deployment.yaml** / **payment-deployment.yaml** - Deployment с:
  - Метрики готовности и жизнеспособности через Actuator
  - Лимиты и запросы ресурсов
  - Multi-stage Docker образы
- **auth-service.yaml** / **payment-service.yaml** - ClusterIP сервисы

### Ingress
- **app-ingress.yaml** - внешний доступ через:
  - `http://auth.localdev.me` -> auth-service
  - `http://payment.localdev.me` -> payment-service

## Как задеплоить

### 1. Проверка Ingress Controller

Убедитесь, что в кластере установлен Ingress Controller (например, nginx):
```bash
kubectl get pods -n ingress-nginx
```

Если нет, установите:
```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.8.2/deploy/static/provider/cloud/deploy.yaml
```

### 2. Сборка Docker образов

Для Minikube:
```bash
minikube start
eval $(minikube docker-env)
cd auth && docker build -t demo/auth-service:latest .
cd ../payment && docker build -t demo/payment-service:latest .
```

Для kind:
```bash
kind create cluster
kind load docker-image demo/auth-service:latest
kind load docker-image demo/payment-service:latest
```

### 3. Применение манифестов

```bash
kubectl apply -k infra/
```

Или по отдельности:
```bash
kubectl apply -f infra/namespace.yaml
kubectl apply -f infra/configmap/
kubectl apply -f infra/secrets/
kubectl apply -f infra/postgres-auth/
kubectl apply -f infra/postgres-payment/
kubectl apply -f infra/auth/
kubectl apply -f infra/payment/
kubectl apply -f infra/ingress/
```

## Проверка

### Проверка подов
```bash
kubectl get pods -n microservices-demo
kubectl get pods -n microservices-demo -w  # watch
```

### Проверка сервисов
```bash
kubectl get svc -n microservices-demo
```

### Проверка Ingress
```bash
kubectl get ingress -n microservices-demo
kubectl describe ingress app-ingress -n microservices-demo
```

### Проверка DNS внутри кластера
```bash
kubectl exec -it <payment-pod> -n microservices-demo -- nslookup auth-service
```

### Проверка связи между сервисами
```bash
kubectl exec -it <payment-pod> -n microservices-demo -- wget -qO- http://auth-service:8081/actuator/health
```

### Просмотр логов
```bash
kubectl logs -f <auth-pod> -n microservices-demo
kubectl logs -f <payment-pod> -n microservices-demo
```

## Внешний доступ

Добавьте в `/etc/hosts` (или `C:\Windows\System32\drivers\etc\hosts`):
```
127.0.0.1 auth.localdev.me
127.0.0.1 payment.localdev.me
```

Для Minikube:
```bash
minikube tunnel
```

Теперь доступны:
- http://auth.localdev.me/swagger-ui/index.html
- http://payment.localdev.me/swagger-ui/index.html

## Тестовые запросы

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

## Важные замечания

1. **Межсервисное взаимодействие**: Payment обращается к Auth через Kubernetes DNS:
   ```
   http://auth-service.microservices-demo.svc.cluster.local:8081
   ```

2. **Данные**: Каждый сервис использует свою БД в отдельном PostgreSQL поде

3. **Безопасность**: В production замените Secrets на внешнее хранилище (Vault, AWS Secrets Manager)

4. **PersistentVolume**: Для локального тестирования может потребоваться создать StorageClass
