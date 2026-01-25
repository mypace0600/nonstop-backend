# Blue-Green 무중단 배포 전략

## 개요

Blue-Green 배포는 두 개의 동일한 환경(Blue, Green)을 운영하면서 트래픽을 전환하는 방식으로 무중단 배포를 구현하는 전략입니다.

```
                    ┌─────────────┐
                    │   Nginx     │
                    │  (Reverse   │
                    │   Proxy)    │
                    └──────┬──────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              ▼                         ▼
       ┌─────────────┐          ┌─────────────┐
       │    Blue     │          │    Green    │
       │  (Active)   │          │  (Standby)  │
       │  :28081     │          │  :28082     │
       └─────────────┘          └─────────────┘
```

## 현재 구성 vs Blue-Green 구성

### 현재 구성 (단일 인스턴스)
- 단일 컨테이너로 운영
- 배포 시 다운타임 발생 가능

### Blue-Green 구성
- 두 개의 앱 컨테이너 운영 (Blue: 28081, Green: 28082)
- Nginx가 앞단에서 트래픽 라우팅
- 새 버전 배포 시 대기 중인 환경에 배포 → 헬스체크 → 트래픽 전환

---

## 구현 방법

### 1. 디렉토리 구조

```
~/apps/nonstop/
├── docker-compose.prod.yml      # 기존 (삭제 예정)
├── docker-compose.blue-green.yml
├── nginx/
│   └── nginx.conf
├── .env
└── scripts/
    └── deploy.sh
```

### 2. docker-compose.blue-green.yml

```yaml
services:
  nginx:
    image: nginx:alpine
    container_name: nonstop-nginx
    ports:
      - "28080:80"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - app-blue
      - app-green
    restart: always
    networks:
      - app-net

  app-blue:
    image: ghcr.io/mypace0600/nonstop-backend:latest
    container_name: nonstop-app-blue
    ports:
      - "28081:28080"
    env_file:
      - .env
    environment:
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - KAFKA_SECURITY_PROTOCOL=PLAINTEXT
      - KAFKA_CONNECTION_STRING=dummy
      - DB_HOST=db
      - REDIS_HOST=redis
      - SERVER_PORT=28080
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_started
      kafka:
        condition: service_started
    restart: always
    networks:
      - app-net

  app-green:
    image: ghcr.io/mypace0600/nonstop-backend:latest
    container_name: nonstop-app-green
    ports:
      - "28082:28080"
    env_file:
      - .env
    environment:
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - KAFKA_SECURITY_PROTOCOL=PLAINTEXT
      - KAFKA_CONNECTION_STRING=dummy
      - DB_HOST=db
      - REDIS_HOST=redis
      - SERVER_PORT=28080
    depends_on:
      db:
        condition: service_healthy
      redis:
        condition: service_started
      kafka:
        condition: service_started
    restart: always
    networks:
      - app-net

  db:
    image: postgres:15-alpine
    container_name: nonstop-db
    environment:
      POSTGRES_DB: ${DB_NAME:-nonstop_local}
      POSTGRES_USER: ${DB_USERNAME:-postgres}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-password}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    restart: always
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-postgres}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - app-net

  redis:
    image: redis:7-alpine
    container_name: nonstop-redis
    restart: always
    networks:
      - app-net

  kafka:
    image: apache/kafka:3.7.0
    container_name: nonstop-kafka
    environment:
      - KAFKA_NODE_ID=1
      - KAFKA_PROCESS_ROLES=broker,controller
      - KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
      - KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092
      - KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER
      - KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      - KAFKA_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
      - KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
      - KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1
      - KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1
      - CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk
    networks:
      - app-net

  init-kafka:
    image: apache/kafka:3.7.0
    container_name: nonstop-init-kafka
    depends_on:
      - kafka
    entrypoint: [ '/bin/sh', '-c' ]
    command: |
      "
      /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --list
      /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic chat-messages --replication-factor 1 --partitions 10
      /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic chat-read-events --replication-factor 1 --partitions 5
      "
    networks:
      - app-net

volumes:
  postgres_data:

networks:
  app-net:
    driver: bridge
```

### 3. nginx/nginx.conf

```nginx
events {
    worker_connections 1024;
}

http {
    upstream backend {
        # 기본: Blue 활성화
        server app-blue:28080;
        # Green 전환 시 아래 주석 변경
        # server app-green:28080;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection 'upgrade';
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_cache_bypass $http_upgrade;
        }

        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }
}
```

### 4. scripts/deploy.sh

```bash
#!/bin/bash
set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

COMPOSE_FILE="docker-compose.blue-green.yml"
NGINX_CONF="nginx/nginx.conf"
HEALTH_CHECK_URL="http://localhost:28080/actuator/health"

# 현재 활성 환경 확인
get_active_env() {
    if grep -q "server app-blue:28080;" "$NGINX_CONF" && ! grep -q "# server app-blue:28080;" "$NGINX_CONF"; then
        echo "blue"
    else
        echo "green"
    fi
}

# 헬스 체크
health_check() {
    local port=$1
    local max_attempts=30
    local attempt=1

    echo -e "${BLUE}Health check on port $port...${NC}"

    while [ $attempt -le $max_attempts ]; do
        if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}Health check passed!${NC}"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts failed, retrying..."
        sleep 2
        attempt=$((attempt + 1))
    done

    echo -e "${RED}Health check failed after $max_attempts attempts${NC}"
    return 1
}

# Nginx 설정 전환
switch_nginx() {
    local target=$1

    if [ "$target" == "blue" ]; then
        sed -i 's/# server app-blue:28080;/server app-blue:28080;/' "$NGINX_CONF"
        sed -i 's/server app-green:28080;/# server app-green:28080;/' "$NGINX_CONF"
    else
        sed -i 's/server app-blue:28080;/# server app-blue:28080;/' "$NGINX_CONF"
        sed -i 's/# server app-green:28080;/server app-green:28080;/' "$NGINX_CONF"
    fi

    # Nginx reload
    docker exec nonstop-nginx nginx -s reload
    echo -e "${GREEN}Switched to $target environment${NC}"
}

# 메인 배포 로직
deploy() {
    local active=$(get_active_env)
    local target=""
    local target_port=""

    if [ "$active" == "blue" ]; then
        target="green"
        target_port="28082"
    else
        target="blue"
        target_port="28081"
    fi

    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Current active: $active${NC}"
    echo -e "${BLUE}Deploying to: $target${NC}"
    echo -e "${BLUE}========================================${NC}"

    # 1. 새 이미지 pull
    echo -e "${BLUE}Pulling latest image...${NC}"
    docker compose -f "$COMPOSE_FILE" pull "app-$target"

    # 2. 대기 환경 업데이트
    echo -e "${BLUE}Updating $target environment...${NC}"
    docker compose -f "$COMPOSE_FILE" up -d "app-$target"

    # 3. 헬스 체크
    if ! health_check "$target_port"; then
        echo -e "${RED}Deployment failed! Rolling back...${NC}"
        docker compose -f "$COMPOSE_FILE" up -d "app-$target"
        exit 1
    fi

    # 4. 트래픽 전환
    echo -e "${BLUE}Switching traffic to $target...${NC}"
    switch_nginx "$target"

    # 5. 이전 환경 정리 (선택적)
    echo -e "${BLUE}Cleaning up old images...${NC}"
    docker image prune -f

    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Deployment complete!${NC}"
    echo -e "${GREEN}Active environment: $target${NC}"
    echo -e "${GREEN}========================================${NC}"
}

# 롤백
rollback() {
    local active=$(get_active_env)
    local target=""

    if [ "$active" == "blue" ]; then
        target="green"
    else
        target="blue"
    fi

    echo -e "${RED}Rolling back to $target...${NC}"
    switch_nginx "$target"
    echo -e "${GREEN}Rollback complete! Active: $target${NC}"
}

# 상태 확인
status() {
    local active=$(get_active_env)
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Current active environment: ${GREEN}$active${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    docker compose -f "$COMPOSE_FILE" ps
}

# 명령어 처리
case "$1" in
    deploy)
        deploy
        ;;
    rollback)
        rollback
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: $0 {deploy|rollback|status}"
        exit 1
        ;;
esac
```

---

## 배포 흐름

### 초기 설정 (1회)

```bash
# 서버에서 실행
cd ~/apps/nonstop
mkdir -p nginx scripts

# 파일 복사 (scp 또는 git pull)
# docker-compose.blue-green.yml
# nginx/nginx.conf
# scripts/deploy.sh

chmod +x scripts/deploy.sh

# 최초 실행
docker compose -f docker-compose.blue-green.yml up -d
```

### 배포 시

```bash
# 배포
./scripts/deploy.sh deploy

# 상태 확인
./scripts/deploy.sh status

# 문제 발생 시 롤백
./scripts/deploy.sh rollback
```

---

## GitHub Actions 연동

`.github/workflows/deploy.yml` 수정:

```yaml
- name: Deploy to Azure VM
  uses: appleboy/ssh-action@v1.0.3
  with:
    host: ${{ secrets.SERVER_HOST }}
    username: ${{ secrets.SERVER_USERNAME }}
    key: ${{ secrets.SERVER_SSH_KEY }}
    script: |
      cd ~/apps/nonstop
      echo ${{ secrets.GHCR_TOKEN }} | docker login ghcr.io -u ${{ secrets.GHCR_USERNAME }} --password-stdin
      ./scripts/deploy.sh deploy
```

---

## 장점

1. **무중단 배포**: 트래픽 전환 시 다운타임 없음
2. **빠른 롤백**: 문제 발생 시 즉시 이전 버전으로 전환
3. **안전한 테스트**: 새 버전을 프로덕션 환경에서 사전 검증 가능

## 단점

1. **리소스 2배**: 두 개의 앱 인스턴스 운영
2. **복잡성 증가**: Nginx 설정 및 스크립트 관리 필요

---

## 다음 단계

1. **HTTPS 적용**: Let's Encrypt + Nginx SSL 설정
2. **모니터링**: Prometheus + Grafana 연동
3. **알림**: 배포 성공/실패 Slack 알림
