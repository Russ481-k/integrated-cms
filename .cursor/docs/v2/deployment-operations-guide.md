# 통합 CMS 배포 및 운영 가이드

## 1. 배포 전략

### 1.1 환경별 배포 구성

#### 1.1.1 개발 환경 (Development)

```yaml
# docker-compose.dev.yml
version: "3.8"

services:
  unified-api:
    build:
      context: ./server
      dockerfile: Dockerfile.dev
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_HOST=unified-db
      - REDIS_HOST=redis
    volumes:
      - ./server:/app
      - ~/.m2:/root/.m2
    depends_on:
      - unified-db
      - redis
    restart: unless-stopped

  unified-frontend:
    build:
      context: ./client
      dockerfile: Dockerfile.dev
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=development
      - NEXT_PUBLIC_API_URL=http://localhost:8080
    volumes:
      - ./client:/app
      - /app/node_modules
    restart: unless-stopped

  unified-db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: dev_password
      MYSQL_DATABASE: unified_cms_dev
      MYSQL_USER: dev_user
      MYSQL_PASSWORD: dev_password
    ports:
      - "3306:3306"
    volumes:
      - unified_db_dev_data:/var/lib/mysql
      - ./server/src/main/resources/db:/docker-entrypoint-initdb.d
    restart: unless-stopped

  service1-db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: dev_password
      MYSQL_DATABASE: service1_cms_dev
      MYSQL_USER: service1_user
      MYSQL_PASSWORD: service1_password
    ports:
      - "3307:3306"
    volumes:
      - service1_db_dev_data:/var/lib/mysql
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis_dev_data:/data
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx/dev.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - unified-api
      - unified-frontend
    restart: unless-stopped

volumes:
  unified_db_dev_data:
  service1_db_dev_data:
  redis_dev_data:
```

#### 1.1.2 스테이징 환경 (Staging)

```yaml
# docker-compose.staging.yml
version: "3.8"

services:
  unified-api:
    image: unified-cms/api:staging
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=staging
      - DB_HOST=unified-db
      - REDIS_HOST=redis
      - ENCRYPTION_KEY=${ENCRYPTION_KEY}
    env_file:
      - .env.staging
    depends_on:
      - unified-db
      - redis
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  unified-frontend:
    image: unified-cms/frontend:staging
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=production
      - NEXT_PUBLIC_API_URL=https://staging-api.company.com
    restart: unless-stopped

  unified-db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MYSQL_DATABASE: unified_cms_staging
      MYSQL_USER: ${DB_USER}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    volumes:
      - unified_db_staging_data:/var/lib/mysql
      - ./backups:/backups
    restart: unless-stopped

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_staging_data:/data
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/staging.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - unified-api
      - unified-frontend
    restart: unless-stopped

volumes:
  unified_db_staging_data:
  redis_staging_data:
```

#### 1.1.3 프로덕션 환경 (Production)

```yaml
# docker-compose.prod.yml
version: "3.8"

services:
  unified-api:
    image: unified-cms/api:${VERSION}
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 2G
          cpus: "1.0"
        reservations:
          memory: 1G
          cpus: "0.5"
      restart_policy:
        condition: on-failure
        delay: 10s
        max_attempts: 3
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=unified-db
      - REDIS_HOST=redis
      - ENCRYPTION_KEY=${ENCRYPTION_KEY}
      - JVM_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC
    env_file:
      - .env.prod
    secrets:
      - db_password
      - redis_password
      - encryption_key
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  unified-frontend:
    image: unified-cms/frontend:${VERSION}
    deploy:
      replicas: 2
      resources:
        limits:
          memory: 512M
          cpus: "0.5"
    environment:
      - NODE_ENV=production
      - NEXT_PUBLIC_API_URL=https://api.company.com

  unified-db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD_FILE: /run/secrets/db_root_password
      MYSQL_DATABASE: unified_cms
      MYSQL_USER: ${DB_USER}
      MYSQL_PASSWORD_FILE: /run/secrets/db_password
    volumes:
      - unified_db_prod_data:/var/lib/mysql
      - ./backups:/backups
    secrets:
      - db_root_password
      - db_password
    deploy:
      resources:
        limits:
          memory: 4G
          cpus: "2.0"

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass_file /run/secrets/redis_password
    volumes:
      - redis_prod_data:/data
    secrets:
      - redis_password

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/prod.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    deploy:
      resources:
        limits:
          memory: 256M
          cpus: "0.25"

secrets:
  db_root_password:
    external: true
  db_password:
    external: true
  redis_password:
    external: true
  encryption_key:
    external: true

volumes:
  unified_db_prod_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/mysql
  redis_prod_data:
    driver: local
    driver_opts:
      type: none
      o: bind
      device: /data/redis
```

### 1.2 CI/CD 파이프라인

#### 1.2.1 GitHub Actions 워크플로우

```yaml
# .github/workflows/ci-cd.yml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_PREFIX: unified-cms

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: "11"
          distribution: "temurin"

      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: "18"
          cache: "npm"
          cache-dependency-path: client/package-lock.json

      - name: Cache Maven dependencies
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Run backend tests
        working-directory: ./server
        run: |
          mvn clean test
          mvn jacoco:report

      - name: Run frontend tests
        working-directory: ./client
        run: |
          npm ci
          npm run test:coverage

      - name: Upload coverage reports
        uses: codecov/codecov-action@v3
        with:
          files: ./server/target/site/jacoco/jacoco.xml,./client/coverage/lcov.info

  security-scan:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Run security scan (backend)
        working-directory: ./server
        run: |
          mvn dependency-check:check

      - name: Run security scan (frontend)
        working-directory: ./client
        run: |
          npm audit --audit-level high

  build:
    needs: [test, security-scan]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' || github.ref == 'refs/heads/develop'

    strategy:
      matrix:
        component: [api, frontend]

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v2

      - name: Log in to Container Registry
        uses: docker/login-action@v2
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_PREFIX }}/${{ matrix.component }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=sha,prefix={{branch}}-
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push Docker image
        uses: docker/build-push-action@v4
        with:
          context: ./${{ matrix.component == 'api' && 'server' || 'client' }}
          file: ./${{ matrix.component == 'api' && 'server' || 'client' }}/Dockerfile
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'
    environment: staging

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Deploy to staging
        run: |
          echo "Deploying to staging environment..."
          # SSH into staging server and deploy
          echo "${{ secrets.STAGING_DEPLOY_KEY }}" > staging_key
          chmod 600 staging_key
          ssh -i staging_key -o StrictHostKeyChecking=no ${{ secrets.STAGING_USER }}@${{ secrets.STAGING_HOST }} << 'EOF'
            cd /opt/unified-cms
            docker-compose -f docker-compose.staging.yml pull
            docker-compose -f docker-compose.staging.yml up -d
            docker system prune -f
          EOF

  deploy-production:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    environment: production

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Deploy to production
        run: |
          echo "Deploying to production environment..."
          # Blue-Green deployment script
          ./scripts/deploy-production.sh

  e2e-tests:
    needs: [deploy-staging]
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/develop'

    steps:
      - name: Checkout code
        uses: actions/checkout@v3

      - name: Set up Node.js
        uses: actions/setup-node@v3
        with:
          node-version: "18"

      - name: Install Playwright
        working-directory: ./e2e
        run: |
          npm ci
          npx playwright install

      - name: Run E2E tests
        working-directory: ./e2e
        run: |
          npx playwright test
        env:
          PLAYWRIGHT_BASE_URL: https://staging.company.com

      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: ./e2e/playwright-report/
```

#### 1.2.2 배포 스크립트

```bash
#!/bin/bash
# scripts/deploy-production.sh

set -e

# 환경 변수 설정
DOCKER_REGISTRY="ghcr.io/unified-cms"
VERSION=${GITHUB_SHA::7}
DEPLOYMENT_DIR="/opt/unified-cms"
BACKUP_DIR="/opt/backups"

echo "Starting production deployment..."
echo "Version: $VERSION"

# 현재 서비스 상태 확인
echo "Checking current service status..."
docker-compose -f docker-compose.prod.yml ps

# 데이터베이스 백업
echo "Creating database backup..."
BACKUP_FILE="$BACKUP_DIR/unified_cms_$(date +%Y%m%d_%H%M%S).sql"
docker-compose -f docker-compose.prod.yml exec -T unified-db \
  mysqldump -u root -p$DB_ROOT_PASSWORD unified_cms > $BACKUP_FILE

if [ $? -eq 0 ]; then
    echo "Database backup created: $BACKUP_FILE"
else
    echo "Database backup failed!"
    exit 1
fi

# Blue-Green 배포 시작
echo "Starting Blue-Green deployment..."

# Green 환경 설정
cp docker-compose.prod.yml docker-compose.green.yml
sed -i 's/unified-api:/unified-api-green:/g' docker-compose.green.yml
sed -i 's/unified-frontend:/unified-frontend-green:/g' docker-compose.green.yml
sed -i 's/8080:8080/8081:8080/g' docker-compose.green.yml
sed -i 's/3000:3000/3001:3000/g' docker-compose.green.yml

# Green 환경 이미지 태그 업데이트
export VERSION=$VERSION
envsubst < docker-compose.green.yml > docker-compose.green.tmp.yml
mv docker-compose.green.tmp.yml docker-compose.green.yml

# Green 환경 시작
echo "Starting Green environment..."
docker-compose -f docker-compose.green.yml up -d unified-api-green unified-frontend-green

# Green 환경 헬스체크
echo "Performing health checks on Green environment..."
for i in {1..30}; do
    if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
        echo "Green environment is healthy"
        break
    fi

    if [ $i -eq 30 ]; then
        echo "Green environment health check failed!"
        docker-compose -f docker-compose.green.yml down
        exit 1
    fi

    echo "Waiting for Green environment to be ready... ($i/30)"
    sleep 10
done

# E2E 테스트 실행 (Green 환경 대상)
echo "Running smoke tests on Green environment..."
if ! ./scripts/smoke-test.sh http://localhost:8081; then
    echo "Smoke tests failed on Green environment!"
    docker-compose -f docker-compose.green.yml down
    exit 1
fi

# 트래픽 전환 (Nginx 설정 업데이트)
echo "Switching traffic to Green environment..."
cp nginx/prod-green.conf nginx/prod.conf
docker-compose -f docker-compose.prod.yml exec nginx nginx -s reload

# 트래픽 전환 확인
sleep 10
if ! curl -f http://localhost/actuator/health > /dev/null 2>&1; then
    echo "Traffic switch verification failed!"
    # 롤백
    git checkout nginx/prod.conf
    docker-compose -f docker-compose.prod.yml exec nginx nginx -s reload
    docker-compose -f docker-compose.green.yml down
    exit 1
fi

# Blue 환경 종료
echo "Stopping Blue environment..."
docker-compose -f docker-compose.prod.yml stop unified-api unified-frontend

# Green을 새로운 Blue로 전환
echo "Promoting Green to Blue..."
docker-compose -f docker-compose.green.yml down
docker-compose -f docker-compose.prod.yml up -d

# 정리
rm -f docker-compose.green.yml

echo "Production deployment completed successfully!"
echo "New version $VERSION is now live"

# 슬랙 알림 (선택사항)
if [ -n "$SLACK_WEBHOOK_URL" ]; then
    curl -X POST -H 'Content-type: application/json' \
        --data "{\"text\":\"🚀 Production deployment completed successfully! Version: $VERSION\"}" \
        $SLACK_WEBHOOK_URL
fi
```

### 1.3 환경별 설정 관리

#### 1.3.1 환경 변수 관리

```bash
# .env.dev
DB_HOST=localhost
DB_PORT=3306
DB_NAME=unified_cms_dev
DB_USER=dev_user
DB_PASSWORD=dev_password

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

ENCRYPTION_KEY=dev_encryption_key_32_chars_long

LOG_LEVEL=DEBUG
```

```bash
# .env.staging
DB_HOST=staging-db
DB_PORT=3306
DB_NAME=unified_cms_staging
DB_USER=staging_user
DB_PASSWORD=${DB_PASSWORD}

REDIS_HOST=staging-redis
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}

ENCRYPTION_KEY=${ENCRYPTION_KEY}

LOG_LEVEL=INFO
```

```bash
# .env.prod
DB_HOST=prod-db
DB_PORT=3306
DB_NAME=unified_cms
DB_USER=prod_user
DB_PASSWORD=${DB_PASSWORD}

REDIS_HOST=prod-redis
REDIS_PORT=6379
REDIS_PASSWORD=${REDIS_PASSWORD}

ENCRYPTION_KEY=${ENCRYPTION_KEY}

LOG_LEVEL=WARN
```

## 2. 백업 및 복구 전략

### 2.1 데이터베이스 백업

#### 2.1.1 자동 백업 스크립트

```bash
#!/bin/bash
# scripts/backup-database.sh

set -e

# 설정
BACKUP_DIR="/opt/backups/mysql"
RETENTION_DAYS=30
DATE=$(date +%Y%m%d_%H%M%S)

# 백업 디렉토리 생성
mkdir -p $BACKUP_DIR

# 통합 DB 백업
echo "Backing up unified CMS database..."
docker-compose exec -T unified-db mysqldump \
    -u root -p$DB_ROOT_PASSWORD \
    --single-transaction \
    --routines \
    --triggers \
    unified_cms > $BACKUP_DIR/unified_cms_$DATE.sql

# 서비스별 DB 백업
for SERVICE in service1 service2; do
    echo "Backing up $SERVICE database..."
    docker-compose exec -T ${SERVICE}-db mysqldump \
        -u root -p$DB_ROOT_PASSWORD \
        --single-transaction \
        --routines \
        --triggers \
        ${SERVICE}_cms > $BACKUP_DIR/${SERVICE}_cms_$DATE.sql
done

# 백업 파일 압축
echo "Compressing backup files..."
gzip $BACKUP_DIR/*_$DATE.sql

# 구형 백업 파일 삭제
echo "Cleaning up old backup files..."
find $BACKUP_DIR -name "*.sql.gz" -mtime +$RETENTION_DAYS -delete

# S3에 백업 업로드 (선택사항)
if [ -n "$AWS_S3_BUCKET" ]; then
    echo "Uploading backups to S3..."
    aws s3 sync $BACKUP_DIR s3://$AWS_S3_BUCKET/database-backups/
fi

echo "Database backup completed: $DATE"
```

#### 2.1.2 백업 복구 스크립트

```bash
#!/bin/bash
# scripts/restore-database.sh

set -e

if [ $# -ne 2 ]; then
    echo "Usage: $0 <database_name> <backup_file>"
    echo "Example: $0 unified_cms /opt/backups/mysql/unified_cms_20240115_120000.sql.gz"
    exit 1
fi

DATABASE_NAME=$1
BACKUP_FILE=$2

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Backup file not found: $BACKUP_FILE"
    exit 1
fi

echo "Restoring database: $DATABASE_NAME"
echo "From backup file: $BACKUP_FILE"
echo "WARNING: This will overwrite the current database!"
read -p "Are you sure? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Restore cancelled"
    exit 0
fi

# 백업 파일 압축 해제 (필요한 경우)
if [[ $BACKUP_FILE == *.gz ]]; then
    echo "Decompressing backup file..."
    UNCOMPRESSED_FILE=${BACKUP_FILE%.gz}
    gunzip -c $BACKUP_FILE > $UNCOMPRESSED_FILE
    BACKUP_FILE=$UNCOMPRESSED_FILE
fi

# 데이터베이스 복구
echo "Restoring database..."
docker-compose exec -T unified-db mysql \
    -u root -p$DB_ROOT_PASSWORD \
    $DATABASE_NAME < $BACKUP_FILE

# 임시 파일 정리
if [[ $BACKUP_FILE == *".sql" ]] && [[ $1 == *.gz ]]; then
    rm -f $BACKUP_FILE
fi

echo "Database restore completed successfully"
```

### 2.2 설정 및 코드 백업

#### 2.2.1 설정 백업 스크립트

```bash
#!/bin/bash
# scripts/backup-config.sh

set -e

BACKUP_DIR="/opt/backups/config"
DATE=$(date +%Y%m%d_%H%M%S)
CONFIG_BACKUP="$BACKUP_DIR/config_$DATE.tar.gz"

mkdir -p $BACKUP_DIR

echo "Creating configuration backup..."

# 설정 파일들 백업
tar -czf $CONFIG_BACKUP \
    docker-compose.*.yml \
    .env.* \
    nginx/ \
    ssl/ \
    scripts/ \
    --exclude='*.log' \
    --exclude='*.tmp'

echo "Configuration backup created: $CONFIG_BACKUP"

# S3에 업로드 (선택사항)
if [ -n "$AWS_S3_BUCKET" ]; then
    aws s3 cp $CONFIG_BACKUP s3://$AWS_S3_BUCKET/config-backups/
fi

# 구형 백업 삭제
find $BACKUP_DIR -name "config_*.tar.gz" -mtime +30 -delete
```

## 3. 모니터링 및 알림

### 3.1 시스템 모니터링

#### 3.1.1 서버 리소스 모니터링

```bash
#!/bin/bash
# scripts/system-monitor.sh

set -e

# 시스템 리소스 체크
check_disk_usage() {
    USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
    if [ $USAGE -gt 85 ]; then
        echo "CRITICAL: Disk usage is ${USAGE}%"
        return 1
    elif [ $USAGE -gt 75 ]; then
        echo "WARNING: Disk usage is ${USAGE}%"
        return 1
    fi
    echo "OK: Disk usage is ${USAGE}%"
    return 0
}

check_memory_usage() {
    USAGE=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100.0}')
    if [ $USAGE -gt 90 ]; then
        echo "CRITICAL: Memory usage is ${USAGE}%"
        return 1
    elif [ $USAGE -gt 80 ]; then
        echo "WARNING: Memory usage is ${USAGE}%"
        return 1
    fi
    echo "OK: Memory usage is ${USAGE}%"
    return 0
}

check_cpu_usage() {
    USAGE=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | awk -F'%' '{print $1}')
    if (( $(echo "$USAGE > 90" | bc -l) )); then
        echo "CRITICAL: CPU usage is ${USAGE}%"
        return 1
    elif (( $(echo "$USAGE > 80" | bc -l) )); then
        echo "WARNING: CPU usage is ${USAGE}%"
        return 1
    fi
    echo "OK: CPU usage is ${USAGE}%"
    return 0
}

# 서비스 상태 체크
check_service_health() {
    local service_name=$1
    local health_url=$2

    if curl -f -s $health_url > /dev/null; then
        echo "OK: $service_name is healthy"
        return 0
    else
        echo "CRITICAL: $service_name is not responding"
        return 1
    fi
}

# 메인 모니터링 실행
main() {
    local exit_code=0

    echo "=== System Resource Check ==="
    check_disk_usage || exit_code=1
    check_memory_usage || exit_code=1
    check_cpu_usage || exit_code=1

    echo ""
    echo "=== Service Health Check ==="
    check_service_health "Unified API" "http://localhost:8080/actuator/health" || exit_code=1
    check_service_health "Frontend" "http://localhost:3000" || exit_code=1

    echo ""
    echo "=== Docker Container Status ==="
    docker-compose ps

    if [ $exit_code -ne 0 ]; then
        echo ""
        echo "ALERT: System monitoring detected issues!"
        # 알림 발송
        if [ -n "$SLACK_WEBHOOK_URL" ]; then
            curl -X POST -H 'Content-type: application/json' \
                --data '{"text":"🚨 System monitoring alert on '$(hostname)'"}' \
                $SLACK_WEBHOOK_URL
        fi
    fi

    return $exit_code
}

main "$@"
```

### 3.2 자동화된 알림

#### 3.2.1 알림 시스템

```bash
#!/bin/bash
# scripts/alert-system.sh

# 설정
WEBHOOK_URL=${SLACK_WEBHOOK_URL}
EMAIL_RECIPIENT=${ALERT_EMAIL}
LOG_FILE="/var/log/unified-cms/alerts.log"

send_slack_alert() {
    local level=$1
    local title=$2
    local message=$3
    local color=""

    case $level in
        "CRITICAL") color="#FF0000" ;;
        "WARNING") color="#FFA500" ;;
        "INFO") color="#00FF00" ;;
    esac

    if [ -n "$WEBHOOK_URL" ]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{
                \"attachments\": [{
                    \"color\": \"$color\",
                    \"title\": \"$title\",
                    \"text\": \"$message\",
                    \"fields\": [{
                        \"title\": \"Server\",
                        \"value\": \"$(hostname)\",
                        \"short\": true
                    }, {
                        \"title\": \"Time\",
                        \"value\": \"$(date)\",
                        \"short\": true
                    }]
                }]
            }" \
            $WEBHOOK_URL
    fi
}

send_email_alert() {
    local subject=$1
    local body=$2

    if [ -n "$EMAIL_RECIPIENT" ]; then
        echo "$body" | mail -s "$subject" $EMAIL_RECIPIENT
    fi
}

log_alert() {
    local level=$1
    local message=$2

    echo "$(date '+%Y-%m-%d %H:%M:%S') [$level] $message" >> $LOG_FILE
}

# 메인 알림 함수
alert() {
    local level=$1
    local title=$2
    local message=$3

    log_alert "$level" "$title: $message"

    case $level in
        "CRITICAL")
            send_slack_alert "$level" "$title" "$message"
            send_email_alert "[CRITICAL] $title" "$message"
            ;;
        "WARNING")
            send_slack_alert "$level" "$title" "$message"
            ;;
        "INFO")
            send_slack_alert "$level" "$title" "$message"
            ;;
    esac
}

# 사용 예시
# alert "CRITICAL" "Database Connection Failed" "Unable to connect to primary database"
# alert "WARNING" "High Memory Usage" "Memory usage is at 85%"
# alert "INFO" "Deployment Completed" "Version 1.2.3 deployed successfully"
```

## 4. 보안 운영

### 4.1 보안 모니터링

#### 4.1.1 보안 스캔 스크립트

```bash
#!/bin/bash
# scripts/security-scan.sh

set -e

SCAN_RESULTS_DIR="/opt/security-scans"
DATE=$(date +%Y%m%d_%H%M%S)

mkdir -p $SCAN_RESULTS_DIR

echo "Starting security scan..."

# 도커 이미지 취약점 스캔
echo "Scanning Docker images for vulnerabilities..."
for image in $(docker images --format "{{.Repository}}:{{.Tag}}" | grep unified-cms); do
    echo "Scanning $image..."
    docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
        aquasec/trivy image $image > $SCAN_RESULTS_DIR/trivy_${image//\//_}_$DATE.txt
done

# 네트워크 포트 스캔
echo "Scanning network ports..."
nmap -sS -O localhost > $SCAN_RESULTS_DIR/nmap_localhost_$DATE.txt

# 파일 시스템 권한 검사
echo "Checking file system permissions..."
find /opt/unified-cms -type f -perm /o+w > $SCAN_RESULTS_DIR/world_writable_files_$DATE.txt

# SSL 인증서 검사
echo "Checking SSL certificates..."
echo | openssl s_client -connect localhost:443 2>/dev/null | \
    openssl x509 -noout -dates > $SCAN_RESULTS_DIR/ssl_cert_check_$DATE.txt

# 로그 분석
echo "Analyzing security logs..."
grep -i "failed\|error\|unauthorized\|forbidden" /var/log/unified-cms/*.log | \
    tail -1000 > $SCAN_RESULTS_DIR/security_log_analysis_$DATE.txt

echo "Security scan completed. Results saved in $SCAN_RESULTS_DIR"

# 고위험 발견사항 알림
HIGH_RISK_COUNT=$(grep -c "HIGH\|CRITICAL" $SCAN_RESULTS_DIR/trivy_*_$DATE.txt || true)
if [ $HIGH_RISK_COUNT -gt 0 ]; then
    alert "WARNING" "Security Scan Alert" "$HIGH_RISK_COUNT high/critical vulnerabilities found"
fi
```

### 4.2 접근 제어 및 감사

#### 4.2.1 사용자 접근 감사

```bash
#!/bin/bash
# scripts/access-audit.sh

set -e

AUDIT_LOG="/var/log/unified-cms/access-audit.log"
REPORT_FILE="/tmp/access-audit-report-$(date +%Y%m%d).txt"

# 함수: 로그 엔트리 추가
log_entry() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') $1" >> $AUDIT_LOG
}

# 활성 세션 감사
audit_active_sessions() {
    echo "=== Active Sessions Audit ===" > $REPORT_FILE
    echo "Date: $(date)" >> $REPORT_FILE
    echo "" >> $REPORT_FILE

    # SSH 세션
    echo "SSH Sessions:" >> $REPORT_FILE
    who >> $REPORT_FILE
    echo "" >> $REPORT_FILE

    # 도커 컨테이너 접근
    echo "Container Access:" >> $REPORT_FILE
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" >> $REPORT_FILE
    echo "" >> $REPORT_FILE
}

# 실패한 로그인 시도 분석
audit_failed_logins() {
    echo "=== Failed Login Attempts ===" >> $REPORT_FILE
    grep "Failed password" /var/log/auth.log | tail -20 >> $REPORT_FILE
    echo "" >> $REPORT_FILE
}

# 권한 변경 감사
audit_permission_changes() {
    echo "=== Permission Changes ===" >> $REPORT_FILE
    find /opt/unified-cms -type f -newer /tmp/last-audit 2>/dev/null | \
        xargs ls -la >> $REPORT_FILE
    touch /tmp/last-audit
    echo "" >> $REPORT_FILE
}

# 네트워크 연결 감사
audit_network_connections() {
    echo "=== Network Connections ===" >> $REPORT_FILE
    netstat -tulpn | grep LISTEN >> $REPORT_FILE
    echo "" >> $REPORT_FILE
}

# 메인 감사 실행
main() {
    log_entry "Starting access audit"

    audit_active_sessions
    audit_failed_logins
    audit_permission_changes
    audit_network_connections

    # 의심스러운 활동 탐지
    SUSPICIOUS=$(grep -c "FAILED\|UNAUTHORIZED\|SUSPICIOUS" $REPORT_FILE || true)
    if [ $SUSPICIOUS -gt 0 ]; then
        alert "WARNING" "Suspicious Activity Detected" "Found $SUSPICIOUS potential security events"

        # 보고서를 보안팀에 전송
        if [ -n "$SECURITY_EMAIL" ]; then
            mail -s "Security Audit Report - $(date +%Y-%m-%d)" -a $REPORT_FILE $SECURITY_EMAIL < $REPORT_FILE
        fi
    fi

    log_entry "Access audit completed"
}

main "$@"
```

## 5. 성능 최적화 및 튜닝

### 5.1 시스템 튜닝

#### 5.1.1 데이터베이스 튜닝

```sql
-- MySQL 성능 최적화 설정
-- /etc/mysql/mysql.conf.d/unified-cms.cnf

[mysqld]
# 기본 설정
innodb_buffer_pool_size = 2G
innodb_log_file_size = 256M
innodb_log_buffer_size = 64M
innodb_flush_log_at_trx_commit = 2

# 연결 설정
max_connections = 500
thread_cache_size = 50
table_open_cache = 2000

# 쿼리 캐시
query_cache_type = 1
query_cache_size = 256M
query_cache_limit = 2M

# 느린 쿼리 로깅
slow_query_log = 1
slow_query_log_file = /var/log/mysql/slow-query.log
long_query_time = 2
log_queries_not_using_indexes = 1

# 바이너리 로그
binlog_format = ROW
expire_logs_days = 7
max_binlog_size = 100M

# 임시 테이블
tmp_table_size = 64M
max_heap_table_size = 64M
```

#### 5.1.2 JVM 튜닝

```bash
# JVM 옵션 설정
# docker-compose 환경 변수

JVM_OPTS="
-Xms2g
-Xmx4g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+UseStringDeduplication
-XX:+OptimizeStringConcat
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers
-Djava.awt.headless=true
-Dfile.encoding=UTF-8
-Duser.timezone=Asia/Seoul
-Djava.security.egd=file:/dev/./urandom
"
```

### 5.2 성능 모니터링

#### 5.2.1 성능 측정 스크립트

```bash
#!/bin/bash
# scripts/performance-monitor.sh

set -e

METRICS_FILE="/var/log/unified-cms/performance-metrics.log"
THRESHOLD_RESPONSE_TIME=2000  # 2초

# API 응답 시간 측정
measure_api_response_time() {
    local endpoint=$1
    local start_time=$(date +%s%3N)

    if curl -f -s $endpoint > /dev/null; then
        local end_time=$(date +%s%3N)
        local response_time=$((end_time - start_time))
        echo $response_time
        return 0
    else
        echo -1
        return 1
    fi
}

# 데이터베이스 성능 측정
measure_db_performance() {
    local query_time=$(docker-compose exec -T unified-db mysql -u root -p$DB_ROOT_PASSWORD \
        -e "SELECT BENCHMARK(1000000, MD5('test'));" 2>/dev/null | tail -1 | awk '{print $2}')
    echo $query_time
}

# 메인 모니터링
main() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    # API 엔드포인트들 측정
    local api_endpoints=(
        "http://localhost:8080/actuator/health"
        "http://localhost:8080/api/unified/services"
        "http://localhost:3000"
    )

    echo "[$timestamp] Performance Monitoring Results:" >> $METRICS_FILE

    for endpoint in "${api_endpoints[@]}"; do
        local response_time=$(measure_api_response_time $endpoint)
        echo "[$timestamp] $endpoint: ${response_time}ms" >> $METRICS_FILE

        if [ $response_time -gt $THRESHOLD_RESPONSE_TIME ] && [ $response_time -ne -1 ]; then
            alert "WARNING" "Slow API Response" "$endpoint responded in ${response_time}ms (threshold: ${THRESHOLD_RESPONSE_TIME}ms)"
        elif [ $response_time -eq -1 ]; then
            alert "CRITICAL" "API Endpoint Down" "$endpoint is not responding"
        fi
    done

    # 시스템 리소스 측정
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | awk -F'%' '{print $1}')
    local memory_usage=$(free | grep Mem | awk '{printf "%.1f", $3/$2 * 100.0}')
    local disk_usage=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')

    echo "[$timestamp] CPU: ${cpu_usage}%, Memory: ${memory_usage}%, Disk: ${disk_usage}%" >> $METRICS_FILE

    # 성능 지표를 Prometheus로 전송 (선택사항)
    if command -v prometheus_push_gateway &> /dev/null; then
        echo "api_response_time{endpoint=\"health\"} $response_time" | \
            curl --data-binary @- http://localhost:9091/metrics/job/unified-cms-monitoring
    fi
}

main "$@"
```

## 6. 장애 복구 절차

### 6.1 장애 대응 플레이북

#### 6.1.1 서비스 장애 복구

```bash
#!/bin/bash
# scripts/disaster-recovery.sh

set -e

BACKUP_DIR="/opt/backups"
LOG_FILE="/var/log/unified-cms/disaster-recovery.log"

log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') $1" | tee -a $LOG_FILE
}

# 서비스 상태 확인
check_service_status() {
    log_message "Checking service status..."

    if docker-compose ps | grep -q "Up"; then
        log_message "Some services are running"
        docker-compose ps
        return 0
    else
        log_message "No services are running"
        return 1
    fi
}

# 데이터베이스 복구
recover_database() {
    local backup_file=$1

    log_message "Starting database recovery from $backup_file"

    if [ ! -f "$backup_file" ]; then
        log_message "ERROR: Backup file not found: $backup_file"
        return 1
    fi

    # 데이터베이스 서비스 중지
    docker-compose stop unified-db

    # 데이터 볼륨 백업
    docker run --rm -v unified_cms_unified_db_data:/data -v $BACKUP_DIR:/backup \
        alpine tar czf /backup/db_data_backup_$(date +%Y%m%d_%H%M%S).tar.gz -C /data .

    # 데이터 볼륨 정리
    docker volume rm unified_cms_unified_db_data || true

    # 데이터베이스 재시작
    docker-compose up -d unified-db

    # 백업 데이터 복원
    sleep 30  # DB 시작 대기
    docker-compose exec -T unified-db mysql -u root -p$DB_ROOT_PASSWORD < $backup_file

    log_message "Database recovery completed"
}

# 전체 시스템 복구
full_system_recovery() {
    log_message "Starting full system recovery..."

    # 모든 서비스 중지
    docker-compose down

    # 최신 백업 파일 찾기
    local latest_backup=$(ls -t $BACKUP_DIR/unified_cms_*.sql.gz | head -1)

    if [ -n "$latest_backup" ]; then
        log_message "Found latest backup: $latest_backup"

        # 압축 해제
        local uncompressed_backup=${latest_backup%.gz}
        gunzip -c $latest_backup > $uncompressed_backup

        # 데이터베이스 복구
        recover_database $uncompressed_backup

        # 임시 파일 정리
        rm -f $uncompressed_backup
    else
        log_message "No database backup found, starting with empty database"
    fi

    # 모든 서비스 시작
    docker-compose up -d

    # 서비스 헬스체크
    log_message "Waiting for services to be ready..."
    sleep 60

    local max_attempts=30
    local attempt=1

    while [ $attempt -le $max_attempts ]; do
        if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_message "System recovery completed successfully"
            alert "INFO" "System Recovery Completed" "All services are back online"
            return 0
        fi

        log_message "Attempt $attempt/$max_attempts: Services not ready yet..."
        sleep 10
        ((attempt++))
    done

    log_message "ERROR: System recovery failed - services are not responding"
    alert "CRITICAL" "System Recovery Failed" "Services are not responding after recovery attempt"
    return 1
}

# 롤백 기능
rollback_deployment() {
    local target_version=$1

    log_message "Rolling back to version: $target_version"

    # 이전 버전 이미지로 롤백
    export VERSION=$target_version
    docker-compose pull
    docker-compose up -d

    # 헬스체크
    sleep 30
    if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        log_message "Rollback completed successfully"
        alert "INFO" "Rollback Completed" "Successfully rolled back to version $target_version"
    else
        log_message "ERROR: Rollback failed"
        alert "CRITICAL" "Rollback Failed" "Failed to rollback to version $target_version"
        return 1
    fi
}

# 메인 함수
main() {
    local action=$1

    case $action in
        "check")
            check_service_status
            ;;
        "recover-db")
            recover_database $2
            ;;
        "full-recovery")
            full_system_recovery
            ;;
        "rollback")
            rollback_deployment $2
            ;;
        *)
            echo "Usage: $0 {check|recover-db <backup_file>|full-recovery|rollback <version>}"
            exit 1
            ;;
    esac
}

main "$@"
```

### 6.2 자동 장애 복구

#### 6.2.1 자동 복구 스크립트

```bash
#!/bin/bash
# scripts/auto-recovery.sh

set -e

MAX_RESTART_ATTEMPTS=3
HEALTH_CHECK_INTERVAL=30
LOG_FILE="/var/log/unified-cms/auto-recovery.log"

log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') $1" | tee -a $LOG_FILE
}

# 서비스 헬스체크
health_check() {
    local service_name=$1
    local health_url=$2

    if curl -f -s --max-time 10 $health_url > /dev/null; then
        return 0
    else
        return 1
    fi
}

# 서비스 재시작
restart_service() {
    local service_name=$1
    local attempt=1

    while [ $attempt -le $MAX_RESTART_ATTEMPTS ]; do
        log_message "Attempting to restart $service_name (attempt $attempt/$MAX_RESTART_ATTEMPTS)"

        docker-compose restart $service_name
        sleep 30

        case $service_name in
            "unified-api")
                if health_check "unified-api" "http://localhost:8080/actuator/health"; then
                    log_message "Successfully restarted $service_name"
                    alert "INFO" "Service Recovered" "$service_name has been automatically restarted"
                    return 0
                fi
                ;;
            "unified-frontend")
                if health_check "unified-frontend" "http://localhost:3000"; then
                    log_message "Successfully restarted $service_name"
                    alert "INFO" "Service Recovered" "$service_name has been automatically restarted"
                    return 0
                fi
                ;;
        esac

        ((attempt++))
    done

    log_message "Failed to restart $service_name after $MAX_RESTART_ATTEMPTS attempts"
    alert "CRITICAL" "Service Recovery Failed" "Failed to automatically restart $service_name"
    return 1
}

# 메인 모니터링 루프
main() {
    log_message "Starting auto-recovery monitoring"

    while true; do
        # API 서비스 헬스체크
        if ! health_check "unified-api" "http://localhost:8080/actuator/health"; then
            log_message "unified-api health check failed"
            restart_service "unified-api"
        fi

        # 프론트엔드 서비스 헬스체크
        if ! health_check "unified-frontend" "http://localhost:3000"; then
            log_message "unified-frontend health check failed"
            restart_service "unified-frontend"
        fi

        # 데이터베이스 연결 확인
        if ! docker-compose exec -T unified-db mysqladmin ping -h localhost -u root -p$DB_ROOT_PASSWORD > /dev/null 2>&1; then
            log_message "Database connection failed"
            restart_service "unified-db"
        fi

        sleep $HEALTH_CHECK_INTERVAL
    done
}

# 시그널 핸들러
cleanup() {
    log_message "Auto-recovery monitoring stopped"
    exit 0
}

trap cleanup SIGTERM SIGINT

main "$@"
```

## 7. 운영 매뉴얼

### 7.1 일상 운영 체크리스트

#### 7.1.1 일일 점검 사항

```markdown
# 일일 운영 체크리스트

## 시스템 상태 확인

- [ ] 모든 서비스가 정상 실행 중인지 확인
- [ ] CPU, 메모리, 디스크 사용률 확인
- [ ] 네트워크 연결 상태 확인
- [ ] 로그 파일에서 에러 메시지 확인

## 백업 상태 확인

- [ ] 전날 자동 백업이 정상적으로 완료되었는지 확인
- [ ] 백업 파일 크기가 정상 범위 내에 있는지 확인
- [ ] S3 백업 업로드 상태 확인 (해당하는 경우)

## 보안 확인

- [ ] 실패한 로그인 시도 검토
- [ ] 비정상적인 접근 패턴 확인
- [ ] SSL 인증서 만료일 확인

## 성능 확인

- [ ] API 응답 시간 확인
- [ ] 데이터베이스 성능 지표 확인
- [ ] 캐시 히트율 확인
```

### 7.2 주간/월간 점검

#### 7.2.1 주간 점검 스크립트

```bash
#!/bin/bash
# scripts/weekly-maintenance.sh

set -e

REPORT_FILE="/tmp/weekly-maintenance-report-$(date +%Y%m%d).txt"
LOG_FILE="/var/log/unified-cms/weekly-maintenance.log"

log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') $1" | tee -a $LOG_FILE
}

# 주간 보고서 생성
generate_weekly_report() {
    echo "=== Weekly Maintenance Report ===" > $REPORT_FILE
    echo "Report Date: $(date)" >> $REPORT_FILE
    echo "" >> $REPORT_FILE

    # 시스템 통계
    echo "=== System Statistics ===" >> $REPORT_FILE
    echo "Uptime: $(uptime)" >> $REPORT_FILE
    echo "Load Average: $(uptime | awk '{print $NF}')" >> $REPORT_FILE
    echo "Memory Usage: $(free -h)" >> $REPORT_FILE
    echo "Disk Usage: $(df -h)" >> $REPORT_FILE
    echo "" >> $REPORT_FILE

    # 서비스 통계
    echo "=== Service Statistics ===" >> $REPORT_FILE
    docker stats --no-stream >> $REPORT_FILE
    echo "" >> $REPORT_FILE

    # 로그 분석
    echo "=== Log Analysis ===" >> $REPORT_FILE
    echo "Error Count (Last 7 days):" >> $REPORT_FILE
    grep -c "ERROR" /var/log/unified-cms/*.log | tail -10 >> $REPORT_FILE
    echo "" >> $REPORT_FILE

    # 백업 상태
    echo "=== Backup Status ===" >> $REPORT_FILE
    ls -la /opt/backups/mysql/ | tail -10 >> $REPORT_FILE
    echo "" >> $REPORT_FILE
}

# 로그 로테이션
rotate_logs() {
    log_message "Rotating log files"

    find /var/log/unified-cms -name "*.log" -size +100M -exec gzip {} \;
    find /var/log/unified-cms -name "*.log.gz" -mtime +30 -delete

    # 도커 로그 정리
    docker system prune -f --volumes
}

# 데이터베이스 최적화
optimize_database() {
    log_message "Optimizing database"

    # 테이블 최적화
    docker-compose exec -T unified-db mysql -u root -p$DB_ROOT_PASSWORD \
        -e "OPTIMIZE TABLE information_schema.tables WHERE table_schema='unified_cms';"

    # 통계 업데이트
    docker-compose exec -T unified-db mysql -u root -p$DB_ROOT_PASSWORD \
        -e "ANALYZE TABLE information_schema.tables WHERE table_schema='unified_cms';"
}

# 보안 업데이트 확인
check_security_updates() {
    log_message "Checking for security updates"

    # 시스템 패키지 업데이트 확인
    apt list --upgradable | grep -i security > /tmp/security-updates.txt || true

    if [ -s /tmp/security-updates.txt ]; then
        alert "WARNING" "Security Updates Available" "$(cat /tmp/security-updates.txt)"
    fi

    # 도커 이미지 업데이트 확인
    docker images --format "{{.Repository}}:{{.Tag}}" | \
        grep unified-cms | \
        xargs -I {} docker pull {} --quiet
}

# 메인 실행
main() {
    log_message "Starting weekly maintenance"

    generate_weekly_report
    rotate_logs
    optimize_database
    check_security_updates

    # 보고서 전송
    if [ -n "$MAINTENANCE_EMAIL" ]; then
        mail -s "Weekly Maintenance Report - $(date +%Y-%m-%d)" \
             -a $REPORT_FILE $MAINTENANCE_EMAIL < $REPORT_FILE
    fi

    log_message "Weekly maintenance completed"
}

main "$@"
```

---

이렇게 통합 CMS의 배포, 운영, 모니터링, 백업/복구에 대한 종합적인 가이드를 완성했습니다.

## 📋 완성된 가이드 문서 세트

1. **[통합 CMS 아키텍처 설계서](unified-cms-architecture.md)** - 전체 아키텍처 및 설계 개요
2. **[상세 구현 가이드](implementation-guide.md)** - 단계별 구현 방법 및 코드 예시
3. **[테스팅 및 성능 최적화 가이드](testing-performance-guide.md)** - 테스트 전략 및 성능 튜닝
4. **[모니터링 및 로깅 가이드](monitoring-logging-guide.md)** - 모니터링 시스템 구축
5. **[배포 및 운영 가이드](deployment-operations-guide.md)** - 배포 전략 및 운영 절차

이제 기존 단일 CMS를 통합 CMS로 고도화하기 위한 완전한 로드맵과 구현 가이드가 준비되었습니다. 각 문서는 실제 운영 환경에서 바로 적용할 수 있도록 구체적인 코드와 설정 예시를 포함하고 있습니다.

어떤 부분부터 시작하고 싶으시거나 추가로 필요한 가이드가 있다면 말씀해 주세요!
