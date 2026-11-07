# infra

| 파일 | 용도 |
|---|---|
| `Dockerfile` | 서버 이미지 (멀티 스테이지, 레이어 분리 JRE) |
| `compose.local.yaml` | 개발용 PostgreSQL. `./gradlew bootRun` 하면 자동으로 뜸 |
| `compose.yaml` | nginx + 서버 + PostgreSQL. 로컬과 서버 공통 (HTTP) |
| `compose.prod.yaml` | 서버(HTTPS) 전용 덧붙임: 443 포트, 인증서, certbot |
| `nginx/shared/` | HTTP, HTTPS 공통 설정 (라우팅, SSE, 요청 제한, 오류 응답) |
| `nginx/http.conf.template` | 로컬용 nginx 서버 블록 (80) |
| `nginx/https.conf.template` | 서버용 nginx 서버 블록 (80 → 443 이동, 443) |
| `.env.example` | 환경변수 예시. `infra/.env`로 복사해서 씀 (커밋 금지) |

## 로컬

```bash
cp infra/.env.example infra/.env   # POSTGRES_PASSWORD만 바꾸면 됨
docker compose -f infra/compose.yaml up -d --build --wait
```

- `http://localhost` (Swagger: `http://localhost/swagger-ui.html`)
- 끄기: `docker compose -f infra/compose.yaml down` (데이터 유지, 지우려면 `down -v`)

코드를 고치면서 개발할 때는 `./gradlew bootRun`이 더 빠릅니다. DB만 Docker로 뜹니다.

## 서버 (HTTPS)

### 1. 준비 (처음 한 번)

1. DNS: `A 레코드  api.paircar.app → 서버 공인 IP`
2. 방화벽: 80, 443 포트 열기 (80은 인증서 발급, 갱신 확인과 HTTPS 이동에 씀)
3. 저장소를 받고 `infra/.env` 작성

   ```bash
   cp infra/.env.example infra/.env
   ```

   `POSTGRES_PASSWORD`, `DOMAIN`, `CERTBOT_EMAIL`, `PAIRCAR_INVITE_BASE_URL`(https 주소), VAPID 키를 채웁니다.

### 2. 인증서 발급 (처음 한 번)

nginx가 443을 열려면 인증서가 먼저 있어야 해서, nginx를 띄우기 전에 certbot이 80 포트를 직접 받아 발급합니다.

```bash
set -a; . infra/.env; set +a
docker compose -f infra/compose.yaml -f infra/compose.prod.yaml run --rm -p 80:80 certbot \
  certonly --standalone -d "$DOMAIN" -m "$CERTBOT_EMAIL" --agree-tos -n
```

### 3. 실행

```bash
docker compose -f infra/compose.yaml -f infra/compose.prod.yaml up -d --build --wait
```

- `https://api.paircar.app` (HTTP로 들어오면 HTTPS로 이동)
- 로컬과 같은 `compose.yaml`에 `compose.prod.yaml`만 덧붙입니다. nginx 설정이 HTTPS 템플릿으로 바뀌고 443, 인증서가 추가됩니다.

### 4. 인증서 갱신 (90일마다 만료)

nginx가 떠 있는 동안에는 80 포트를 nginx가 쓰므로, 갱신은 webroot 방식으로 합니다. 서버 cron에 등록하세요.

```bash
0 3 * * * cd /opt/paircar && docker compose -f infra/compose.yaml -f infra/compose.prod.yaml run --rm certbot renew --webroot -w /var/www/certbot -q && docker compose -f infra/compose.yaml -f infra/compose.prod.yaml exec nginx nginx -s reload
```

## 환경변수

| 이름 | 로컬 | 서버 | 설명 |
|---|---|---|---|
| `POSTGRES_PASSWORD` | 필수 | 필수 | DB 비밀번호 |
| `POSTGRES_DB`, `POSTGRES_USER` | | | 기본값 `paircar` |
| `HTTP_PORT`, `HTTPS_PORT` | | | 기본값 80, 443 |
| `DOMAIN` | | 필수 | 인증서를 받을 도메인 (예: `api.paircar.app`) |
| `CERTBOT_EMAIL` | | 필수 | 인증서 만료 알림 메일 |
| `PAIRCAR_INVITE_BASE_URL` | | 권장 | 동승자 웹의 초대 참여 주소. 초대 링크, QR에 들어감 |
| `PAIRCAR_VAPID_PUBLIC_KEY`, `PAIRCAR_VAPID_PRIVATE_KEY` | | 권장 | Web Push 키 (`npx web-push generate-vapid-keys`). 비우면 Push만 꺼짐 |
| `PAIRCAR_VAPID_SUBJECT` | | 권장 | Web Push 연락처 (`mailto:...`) |

## 참고

- 외부에 열리는 포트는 nginx뿐입니다. 서버(8080)와 DB(5432)는 내부 네트워크에만 있습니다.
- HTTPS에서는 HSTS를 켭니다. 한 번 HTTPS로 접속한 브라우저는 1년 동안 HTTP로 접속하지 않습니다.
- Web Push와 실제 차량(Android) 연동은 HTTPS가 필요합니다. 로컬 HTTP에서는 `localhost`만 예외입니다.
