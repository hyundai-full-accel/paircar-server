# 페어카 핵심 시퀀스

기획서 시나리오(지훈이 같은 단지 동료 수아를 태우고 판교로 출근)를 기준으로, 차량 앱, 크루 서버, 동승자 웹 사이에서 실제로 오가는 호출을 정리합니다. API 경로는 이 저장소에 구현된 그대로입니다.

- 전체 구조: [architecture.md](architecture.md)
- 요청, 응답 필드: 서버 실행 후 `/swagger-ui.html`

| # | 시퀀스 | 언제 |
|---|---|---|
| 1 | [처음 설정](#1-처음-설정-운전자) | 운전자가 차량 앱을 처음 켤 때 |
| 2 | [초대와 참여](#2-초대와-참여) | 동승자를 크루에 넣을 때 |
| 3 | [실시간 구독](#3-실시간-구독) | 앱, 웹을 열 때마다 |
| 4 | [우회 거리 산출과 확인](#4-우회-거리-산출과-확인) | 동승자가 합류한 뒤 한 번 |
| 5 | [출근 한 번](#5-출근-한-번) | 매일 아침 |
| 6 | [예외 상황](#6-예외-상황) | 지각, 불참, 지점 변경, 미탑승 |
| 7 | [오프라인 재전송](#7-오프라인-재전송) | 운행 중 통신이 끊겼을 때 |
| 8 | [월말 정산](#8-월말-정산) | 매월 말 |

표기

- `->>` 요청, `-->>` 응답, `--)` 서버가 먼저 보내는 알림(SSE 또는 Web Push)
- "SSE"는 앱, 웹이 열려 있을 때만, "Push"는 웹을 닫아도 받습니다. 채널과 인증 설계는 [architecture.md](architecture.md#통신-방식)에 있습니다.

---

## 1. 처음 설정 (운전자)

```mermaid
sequenceDiagram
    actor D as 지훈 (운전자)
    participant C as 차량 앱
    participant S as 크루 서버

    D->>C: 닉네임 입력
    C->>S: POST /api/members {nickname}
    S-->>C: memberId, token
    Note over C: token은 이 응답에서만 받음. 기기에 저장
    C->>S: POST /api/cars {vehicleKey, fuelType}
    S-->>C: carId
    D->>C: 계좌 입력 (정차 중)
    C->>S: PUT /api/members/me/bank-account
    C->>S: POST /api/crews {name: "판교 출근 크루"}
    S-->>C: crewId (만든 사람은 자동 참여)
    C->>S: POST /api/crews/{crewId}/places (지훈 집, 판교 본관, ○○아파트 정문)
    S-->>C: placeId들
```

- 계좌가 없으면 운행을 시작할 수 없습니다(`BANK_ACCOUNT_REQUIRED`). 영수증에 넣을 계좌이기 때문입니다.
- 같은 `vehicleKey`로 다른 멤버(가족 등)가 등록하면 같은 차량에 연결됩니다.

## 2. 초대와 참여

```mermaid
sequenceDiagram
    participant C as 차량 앱 (지훈)
    participant S as 크루 서버
    participant W as 동승자 웹 (수아)

    C->>S: POST /api/crews/{crewId}/invites
    S-->>C: code "K7M2QX9P", inviteUrl, expiresAt (7일)
    Note over C: inviteUrl을 QR로 띄우거나 메신저로 공유
    C-->>W: 초대 링크
    W->>S: GET /api/invites/{code}
    S-->>W: crewName "판교 출근 크루"
    W->>S: POST /api/invites/{code}/members {nickname: "수아"}
    S-->>W: memberId, crewMemberId, token
    Note over W: token을 브라우저에 저장. 이후 로그인 없이 수아로 인식
```

- 이미 토큰이 있는 멤버(다른 크루의 운전자 등)는 `POST /api/invites/{code}/join`으로 참여합니다.

## 3. 실시간 구독

```mermaid
sequenceDiagram
    participant W as 동승자 웹
    participant S as 크루 서버
    participant P as 브라우저 푸시 서비스

    Note over W,S: SSE (웹이 열려 있을 때)
    W->>S: POST /api/live/tickets (Bearer token)
    S-->>W: ticket (60초, 일회용)
    W->>S: GET /api/live/events?ticket=...
    S--)W: event: connected
    loop 25초마다
        S--)W: keepalive 주석
    end

    Note over W,P: Web Push (웹을 닫아도)
    W->>S: GET /api/push/config
    S-->>W: publicKey (VAPID)
    W->>P: pushManager.subscribe(applicationServerKey)
    P-->>W: endpoint, keys
    W->>S: PUT /api/push/subscriptions {endpoint, keys}
```

- 차량 앱도 같은 방법으로 SSE를 구독해서 동승자 알림(지각, 불참 등)을 받습니다. Web Push 구독은 하지 않습니다.
- SSE가 끊기면 새 티켓을 받아 다시 구독합니다.
- iOS는 동승자 웹을 홈 화면에 추가해야 Web Push를 받을 수 있습니다.

## 4. 우회 거리 산출과 확인

기획서 별지 T4. 매일 계산하면 교통 상황에 따라 값이 흔들리므로, 합류 뒤 한 번 산출하고 양측이 확인한 값을 정산에 씁니다.

```mermaid
sequenceDiagram
    participant N as NaviHelper
    participant C as 차량 앱 (지훈)
    participant S as 크루 서버
    participant W as 동승자 웹 (수아)

    Note over C: 정차 중, 운전자 출발지 기준
    C->>N: requestTrip(회사)
    N-->>C: 직행 거리
    C->>N: requestRouteWithWaypoints(정문, 회사)
    N-->>C: 경유 거리
    Note over C: 우회 거리 = 경유 - 직행 = 3.0km
    C->>S: PUT /api/crews/{crewId}/detours {passenger: 수아, driver: 지훈, place: 정문, detourKm: 3.0}
    S-->>C: detourId, confirmed: false
    S--)W: detour.confirm-requested (SSE + Push) "○○아파트 정문 3.0km"
    C->>S: POST /api/crews/{crewId}/detours/{detourId}/confirm (지훈)
    W->>S: POST /api/crews/{crewId}/detours/{detourId}/confirm (수아)
    S-->>W: confirmed: true
```

- 교대 운전이면 운전자마다 출발지가 달라서 우회 거리를 운전자별로 따로 등록합니다.
- 값이 바뀌면(Odometer 실측 보정 등) 양측 확인이 초기화되고 다시 확인 요청이 갑니다.
- 둘 다 확인하지 않은 우회 거리는 정산에 쓰지 않습니다.

## 5. 출근 한 번

기획서 4장 "매일 아침 · 출근"의 다섯 순간(출발, 접근, 탑승, 주행, 하차)입니다.

```mermaid
sequenceDiagram
    participant V as 차량 신호 · 내비
    participant C as 차량 앱 (지훈)
    participant S as 크루 서버
    participant W as 동승자 웹 (수아)

    rect rgba(127,119,221,0.08)
    Note over V,W: ① 출발
    V-->>C: 시동, Odometer 10000.0
    C->>S: POST /api/trips {crewId, carId, origin: 집, destination: 본관, riders: [수아: 정문 → 본관]}
    S-->>C: tripId, waypoints: [정문]
    S--)W: trip.started (SSE + Push) "지훈님이 출발했어요"
    C->>V: requestRouteWithWaypoints(정문, 본관)
    end

    rect rgba(29,158,117,0.08)
    Note over V,W: ② 접근
    loop 5~10초마다
        V-->>C: 위치, WaypointInfo.remainTime
        C->>S: POST /trips/{tripId}/live/location {lat, lng, remainsByRider}
        S--)W: vehicle.location (SSE) "3분 남음"
    end
    V-->>C: addProximityAlert (정문 300m)
    C->>S: POST /trips/{tripId}/live/riders/{수아}/approaching
    S--)W: vehicle.approaching (SSE + Push) "차가 곧 도착해요"
    end

    rect rgba(216,90,48,0.08)
    Note over V,W: ③ 탑승
    V-->>C: 도어 열림 → 닫힘 → 60초 안에 벨트 체결
    Note over C: 탑승 확정 (서버 없이 차량 안에서 판단)
    C->>V: getWaypointInfo() → removeWaypoint(정문)
    C->>V: TTS "수아님 탑승했어요"
    C->>S: POST /trips/{tripId}/boardings/{수아}/board {occurredAt, odometerKm: 10005.0}
    S--)W: rider.boarded (SSE)
    end

    rect rgba(136,135,128,0.08)
    Note over V,W: ④ 주행, ⑤ 도착
    V-->>C: onDestinationArrived, Odometer 10017.0
    C->>S: POST /trips/{tripId}/segments [1: 10000→10005 (혼자), 2: 10005→10017 (수아)]
    C->>S: POST /trips/{tripId}/end {endOdometerKm: 10017.0, tollFareWon}
    S-->>C: 하차 기록 없는 동승자는 도착 값으로 하차 처리
    S--)W: trip.ended (SSE)
    end
```

- 구간은 탑승 인원이 바뀌는 지점마다 나눕니다. 운전자 혼자 달린 구간도 올리고, 정산에서만 빠집니다.
- 퇴근길도 같은 흐름이고, 출발지가 회사, 경유지가 하차 지점이 됩니다.

## 6. 예외 상황

기획서 4장 "예외 상황" 표의 처리입니다. 동승자 요청은 서버를 거쳐 차량 앱에 SSE로 가고, 차량 앱은 TTS로만 알립니다(주행 중 운전자 조작 없음).

```mermaid
sequenceDiagram
    participant W as 동승자 웹 (수아)
    participant S as 크루 서버
    participant C as 차량 앱 (지훈)
    participant V as 내비 · TTS

    Note over W,V: "5분 늦어요" (경로는 그대로)
    W->>S: POST /trips/{tripId}/live/me/delay {minutes: 5}
    S--)C: rider.delayed
    C->>V: TTS "수아님이 5분 늦어요"

    Note over W,V: "오늘은 후문에서 탈게요" (경유지 한 번만 갱신)
    W->>S: PUT /trips/{tripId}/boardings/{수아}/board-place {placeId: 후문}
    S--)C: rider.place-changed (후문 좌표)
    C->>V: 경유지 정문 → 후문, TTS "수아님이 후문에서 타요"

    Note over W,V: "오늘은 안 타요" (정산에서 빠짐)
    W->>S: DELETE /trips/{tripId}/boardings/{수아}
    S--)C: rider.absent
    C->>V: removeWaypoint, TTS "수아님이 오늘은 안 타요"

    Note over W,V: 경유지에 도착했는데 아직 안 탐
    V-->>C: onWayPointArrived, 탑승 미확정
    C->>S: POST /trips/{tripId}/live/riders/{수아}/arrived
    S--)W: vehicle.arrived (SSE + Push) "차가 도착했어요"
```

- 운전자가 Gleo로 "크루한테 10분 늦는다고 알려줘"라고 하면 `POST /trips/{tripId}/live/delay`로 아직 안 탄 동승자들에게 `driver.delayed`(SSE + Push)가 갑니다.
- 탈 지점 변경과 불참은 탑승 전에만 됩니다. 경유지는 최대 2곳입니다.
- 탑승 감지에 실패하면 내비게이션의 기본 경유지 통과 처리가 그대로 동작합니다. 그 동승자는 탑승 기록이 없으니 그날 정산에서 빠지고, 동승자 웹의 "탔어요" 버튼(PT 단계)으로 보완합니다.

## 7. 오프라인 재전송

터널, 지하주차장에서 통신이 끊겨도 운행 기록은 잃지 않습니다.

```mermaid
sequenceDiagram
    participant C as 차량 앱
    participant B as 차량 내 버퍼
    participant S as 크루 서버

    C-xS: POST .../board (통신 끊김)
    C->>B: 탑승 이벤트 저장
    C->>B: 하차, 구간, 종료 이벤트 저장
    Note over C,S: 통신 회복
    loop 버퍼에 쌓인 순서대로
        B->>S: board → alight → segments → end
        alt 2xx 또는 4xx (429 제외)
            S-->>B: 처리 완료. 버퍼에서 지움
        else 5xx, 429, 타임아웃
            S-->>B: 잠시 뒤 같은 요청을 다시 보냄
        end
    end
```

- 서버는 같은 요청을 여러 번 받아도 결과가 같습니다. 탑승, 하차는 처음 값을 유지하고, 이미 저장된 구간 번호는 건너뛰고, 끝난 운행의 종료 요청은 그대로 돌려줍니다.
- 4xx는 다시 보내도 같은 이유로 실패하므로 버립니다. 단 429(게이트웨이 요청 제한)는 잠시 뒤 다시 보냅니다.
- 순서가 바뀌면(종료가 하차보다 먼저 도착) 하차가 도착 값으로 대신 채워져 탄 거리가 조금 길게 잡힐 수 있습니다. 버퍼는 발생 순서대로 보내야 합니다.

## 8. 월말 정산

기획서 4장 "월말 · 정산"과 별지 T5입니다.

```mermaid
sequenceDiagram
    participant C as 차량 앱 (지훈)
    participant S as 크루 서버
    participant W as 동승자 웹 (수아)

    Note over S: 11월 말
    C->>S: POST /api/crews/{crewId}/receipts?month=2026-11
    Note over S: 1. 정산할 탑승 모으기 (끝난 운행, 구간 있음, 아직 정산 안 됨)<br/>2. 탑승마다 구간 분담금 + 확인된 우회 거리 비용 + 통행료 분담<br/>3. (수아, 지훈) 쌍으로 묶어 합계를 100원 단위 올림<br/>4. 영수증 + 하루치 줄 저장, 계좌 스냅샷
    S-->>C: 발행한 영수증 목록
    S--)W: receipt.issued (SSE + Push) "11월 영수증이 도착했어요 · 보낼 금액 20,700원"
    W->>S: GET /api/receipts/{receiptId}
    S-->>W: 탑승 18일, 함께 탄 거리 216km, 우회 54km, 내 부담 162km,<br/>연비 13.2, 유가 1,684원, 보낼 금액 20,700원, 계좌
    Note over W: 계좌, 금액 복사 → 은행 앱으로 송금 (결제 기능 없음)
```

- 영수증은 내는 사람과 받는 사람만 볼 수 있습니다.
- 발행된 영수증은 바뀌지 않습니다. 발행 뒤에 늦게 올라온 탑승은 다음 달 영수증에 들어갑니다.
- 지금은 발행 API를 직접 호출합니다. 월말 자동 발행은 이후에 추가합니다.
