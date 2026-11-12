# C4 다이어그램 SVG 생성. 실행: python3 docs/images/generate_c4.py docs/images
import sys
from html import escape

OUT = sys.argv[1]
FONT = "Pretendard, 'Apple SD Gothic Neo', 'Noto Sans KR', 'Malgun Gothic', sans-serif"

PERSON = ("#08427b", "#052e56", "#fff")
SYSTEM = ("#1168bd", "#0b4884", "#fff")
CONTAINER = ("#438dd5", "#2e6295", "#fff")
COMPONENT = ("#85bbf0", "#5d82a8", "#000")
EXTERNAL = ("#999999", "#6b6b6b", "#fff")


class Svg:
    def __init__(self, w, h, title, subtitle):
        self.w, self.h = w, h
        self.parts = []
        self.t(40, 46, title, 22, "#222", weight="700", anchor="start")
        self.t(40, 72, subtitle, 14, "#666", anchor="start")

    def t(self, x, y, s, size, color, weight="400", anchor="middle", halo=False):
        extra = ' paint-order="stroke" stroke="#fff" stroke-width="5" stroke-linejoin="round"' if halo else ""
        self.parts.append(
            f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" fill="{color}" '
            f'text-anchor="{anchor}"{extra}>{escape(s)}</text>'
        )

    def body(self, x, y, w, h, colors, name, tag, lines, top=None):
        _, _, fg = colors
        cx = x + w / 2
        n = len(lines)
        # name, tag, then description lines, vertically centered in the box
        block = 22 + 18 + (10 + 18 * n if n else 0)
        cy = (top if top is not None else y) + ((y + h) - (top if top is not None else y) - block) / 2
        self.t(cx, cy + 18, name, 17, fg, weight="700")
        self.t(cx, cy + 38, tag, 12, fg)
        for i, line in enumerate(lines):
            self.t(cx, cy + 66 + 18 * i, line, 13, fg)

    def box(self, x, y, w, h, colors, name, tag, lines=(), stroke_width=2, badge=None):
        fill, stroke, fg = colors
        self.parts.append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="10" fill="{fill}" stroke="{stroke}" stroke-width="{stroke_width}"/>'
        )
        if badge:
            bw = 14 * len(badge) + 16
            self.parts.append(
                f'<rect x="{x + w - bw - 8}" y="{y + 8}" width="{bw}" height="22" rx="11" fill="#fff" fill-opacity="0.9"/>'
            )
            self.t(x + w - bw / 2 - 8, y + 24, badge, 12, fill, weight="700")
        self.body(x, y, w, h, colors, name, tag, list(lines))

    def person(self, x, y, w, h, name, tag, lines=()):
        fill, stroke, _ = PERSON
        r = 26
        self.parts.append(
            f'<circle cx="{x + w / 2}" cy="{y - r + 8}" r="{r}" fill="{fill}" stroke="{stroke}" stroke-width="2"/>'
        )
        self.parts.append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="24" fill="{fill}" stroke="{stroke}" stroke-width="2"/>'
        )
        self.body(x, y, w, h, PERSON, name, tag, list(lines))

    def cylinder(self, x, y, w, h, colors, name, tag, lines=(), stroke_width=2, badge=None):
        fill, stroke, _ = colors
        e = 14
        self.parts.append(
            f'<path d="M{x},{y + e} A{w / 2},{e} 0 0 1 {x + w},{y + e} V{y + h - e} '
            f'A{w / 2},{e} 0 0 1 {x},{y + h - e} Z" fill="{fill}" stroke="{stroke}" stroke-width="{stroke_width}"/>'
        )
        self.parts.append(
            f'<path d="M{x},{y + e} A{w / 2},{e} 0 0 0 {x + w},{y + e}" fill="none" stroke="{stroke}" stroke-width="{stroke_width}"/>'
        )
        if badge:
            bw = 14 * len(badge) + 16
            self.parts.append(
                f'<rect x="{x + w / 2 - bw / 2}" y="{y + h - e - 30}" width="{bw}" height="22" rx="11" fill="#fff" fill-opacity="0.9"/>'
            )
            self.t(x + w / 2, y + h - e - 14, badge, 12, fill, weight="700")
            h -= 26
        self.body(x, y, w, h, colors, name, tag, list(lines), top=y + 2 * e)

    def boundary(self, x, y, w, h, label):
        self.parts.append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="none" stroke="#666" '
            f'stroke-width="1.5" stroke-dasharray="8 6"/>'
        )
        self.t(x + 16, y + h - 14, label, 14, "#444", weight="700", anchor="start")

    def arrow(self, pts, label=(), at=None, anchor="middle", dashed=False):
        d = "M" + " L".join(f"{px},{py}" for px, py in pts)
        dash = ' stroke-dasharray="7 5"' if dashed else ""
        self.parts.append(
            f'<path d="{d}" fill="none" stroke="#666" stroke-width="1.6"{dash} marker-end="url(#arrow)"/>'
        )
        if label:
            lx, ly = at
            for i, line in enumerate(label):
                self.t(lx, ly + 17 * i, line, 13, "#444", anchor=anchor, halo=True)

    def legend(self, y, items):
        x = 40
        for kind, label in items:
            if kind == "line" or kind == "dash":
                dash = ' stroke-dasharray="7 5"' if kind == "dash" else ""
                self.parts.append(
                    f'<path d="M{x},{y} L{x + 36},{y}" stroke="#666" stroke-width="1.6"{dash} marker-end="url(#arrow)"/>'
                )
                self.t(x + 46, y + 5, label, 13, "#444", anchor="start")
                x += 56 + 14 * len(label)
            else:
                fill, stroke, _ = kind
                self.parts.append(
                    f'<rect x="{x}" y="{y - 10}" width="22" height="20" rx="4" fill="{fill}" stroke="{stroke}"/>'
                )
                self.t(x + 30, y + 5, label, 13, "#444", anchor="start")
                x += 44 + 14 * len(label)

    def save(self, path):
        head = (
            f'<svg xmlns="http://www.w3.org/2000/svg" width="{self.w}" height="{self.h}" '
            f'viewBox="0 0 {self.w} {self.h}" font-family="{escape(FONT, quote=True)}">'
            '<defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" '
            'orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" fill="#666"/></marker></defs>'
            f'<rect width="{self.w}" height="{self.h}" fill="#fff"/>'
        )
        with open(path, "w", encoding="utf-8") as f:
            f.write(head + "\n" + "\n".join(self.parts) + "\n</svg>\n")


# Level 1: system context
s = Svg(1000, 720, "[시스템 컨텍스트] 페어카", "누가 페어카를 쓰고, 페어카가 바깥의 무엇과 맞닿는지")
s.person(140, 140, 220, 110, "운전자", "[Person]", ["차를 몰고 동료를 태움"])
s.person(640, 140, 220, 110, "동승자", "[Person]", ["얻어 타고 한 달 기름값을 보냄"])
s.box(330, 330, 340, 130, SYSTEM, "페어카", "[Software System]", ["운행을 기록하고", "함께 탄 만큼 월 정산"])
s.box(110, 540, 280, 110, EXTERNAL, "Pleos Connect", "[External System]", ["차량 데이터, 내비게이션"])
s.box(610, 540, 280, 110, EXTERNAL, "브라우저 푸시 서비스", "[External System]", ["FCM, APNs, Mozilla Push"])
s.arrow([(250, 250), (420, 328)], ["운행 시작, 탑승 확인"], at=(300, 300), anchor="end")
s.arrow([(750, 250), (580, 328)], ["출석, 영수증 확인"], at=(700, 300), anchor="start")
s.arrow([(420, 460), (260, 538)], ["주행거리, 연비, 위치 읽기", "경유지 설정"], at=(320, 490), anchor="end")
s.arrow([(580, 460), (740, 538)], ["알림 보내기", "[Web Push]"], at=(680, 490), anchor="start")
s.arrow([(890, 595), (950, 595), (950, 195), (862, 195)], ["폰 알림"], at=(958, 400), anchor="start", dashed=True)
s.legend(695, [(PERSON, "사람"), (SYSTEM, "페어카"), (EXTERNAL, "외부 시스템"), ("dash", "푸시 알림")])
s.save(f"{OUT}/c4-1-context.svg")

# Level 2: containers
s = Svg(1120, 960, "[컨테이너] 페어카", "페어카 안의 실행 단위. 굵은 테두리가 이 저장소(paircar-server)의 범위")
s.person(150, 140, 200, 90, "운전자", "[Person]")
s.person(750, 140, 200, 90, "동승자", "[Person]")
s.boundary(60, 270, 980, 470, "페어카 [Software System]")
s.box(110, 300, 280, 140, CONTAINER, "차량 앱", "[Container: Pleos Connect 앱]",
      ["탑승 감지, 거리·연비 측정", "오프라인 때 모았다 재전송"])
s.box(710, 300, 280, 140, CONTAINER, "동승자 웹", "[Container: React PWA]",
      ["초대 참여, 차 도착 시간", "출석, 영수증"])
s.box(410, 460, 280, 100, SYSTEM, "게이트웨이", "[Container: nginx]", ["HTTPS 종료, 요청 ID 부여"],
      stroke_width=4, badge="이 저장소")
s.box(410, 600, 280, 120, SYSTEM, "크루 서버", "[Container: Spring Boot, Kotlin]",
      ["크루, 운행, 정산 API", "SSE, Web Push 발송"], stroke_width=4, badge="이 저장소")
s.cylinder(800, 590, 200, 140, SYSTEM, "DB", "[Container: PostgreSQL 17]",
           ["크루, 운행, 구간, 영수증"], stroke_width=4)
s.box(110, 790, 280, 90, EXTERNAL, "Pleos Connect", "[External System]")
s.box(760, 790, 260, 90, EXTERNAL, "브라우저 푸시 서비스", "[External System]")
s.arrow([(250, 230), (250, 298)], ["사용"], at=(260, 270), anchor="start")
s.arrow([(850, 230), (850, 298)], ["사용"], at=(860, 270), anchor="start")
s.arrow([(180, 440), (180, 788)], ["차량 데이터 읽기"], at=(190, 620), anchor="start")
s.arrow([(390, 400), (480, 458)], ["REST, SSE", "[HTTPS]"], at=(450, 400), anchor="start")
s.arrow([(710, 400), (620, 458)], ["REST, SSE", "[HTTPS]"], at=(650, 400), anchor="end")
s.arrow([(550, 560), (550, 598)], ["프록시 [HTTP]"], at=(560, 584), anchor="start")
s.arrow([(690, 660), (798, 660)], ["읽기, 쓰기", "[JPA]"], at=(744, 640))
s.arrow([(600, 720), (800, 788)], ["Web Push", "[VAPID]"], at=(680, 772), anchor="end")
s.arrow([(1020, 835), (1075, 835), (1075, 185), (952, 185)], ["폰 알림"], at=(1068, 500), anchor="end", dashed=True)
s.legend(930, [(PERSON, "사람"), (SYSTEM, "이 저장소"), (CONTAINER, "다른 저장소"), (EXTERNAL, "외부 시스템")])
s.save(f"{OUT}/c4-2-container.svg")

# Level 3: components of the crew server
s = Svg(1140, 1050, "[컴포넌트] 크루 서버", "도메인별 모듈로 나눈 모듈러 모놀리스. 호출은 위에서 아래로만, 위로 알릴 때는 이벤트")
s.box(400, 100, 300, 80, EXTERNAL, "차량 앱, 동승자 웹", "[Container]")
s.box(880, 100, 220, 80, EXTERNAL, "브라우저 푸시 서비스", "[External System]")
s.boundary(40, 220, 1040, 620, "크루 서버 [Container: Spring Boot]")
s.box(70, 260, 260, 150, COMPONENT, "global", "[Component]",
      ["공통 응답, 예외", "기기 토큰 인증, CORS", "요청 로깅 (MDC)"])
s.t(200, 432, "모든 모듈이 사용", 13, "#444")
s.box(400, 260, 280, 100, COMPONENT, "settlement", "[Component]", ["정산 엔진, 영수증, 월 자동 발행"])
s.box(400, 420, 280, 100, COMPONENT, "trip", "[Component]", ["운행, 탑승, 구간"])
s.box(860, 390, 200, 140, COMPONENT, "live", "[Component]", ["SSE 연결, 일회용 티켓", "Web Push 발송"])
s.box(300, 580, 240, 100, COMPONENT, "car", "[Component]", ["차량, 연비, 차량별 유가"])
s.box(600, 580, 240, 100, COMPONENT, "crew", "[Component]", ["크루, 초대, 장소", "우회 거리, 출석"])
s.box(440, 720, 260, 90, COMPONENT, "member", "[Component]", ["멤버, 계좌, 기기 토큰"])
s.cylinder(80, 880, 220, 100, EXTERNAL, "DB", "[PostgreSQL]")
s.arrow([(550, 180), (550, 258)], ["REST, SSE [HTTPS]", "각 모듈의 api/ 컨트롤러"], at=(562, 196), anchor="start")
s.arrow([(970, 390), (990, 182)], ["Web Push [VAPID]"], at=(990, 300), anchor="start")
s.arrow([(540, 360), (540, 418)])
s.arrow([(860, 485), (682, 485)], ["조회"], at=(770, 504))
s.arrow([(470, 520), (430, 578)])
s.arrow([(610, 520), (690, 578)])
s.arrow([(600, 630), (542, 630)])
s.arrow([(420, 680), (520, 718)])
s.arrow([(700, 680), (630, 718)])
s.arrow([(680, 300), (910, 300), (910, 388)], ["도메인 이벤트"], at=(790, 292), dashed=True)
s.arrow([(680, 445), (858, 445)], ["도메인 이벤트", "(커밋 뒤)"], at=(770, 430), dashed=True)
s.arrow([(840, 600), (960, 600), (960, 532)], ["도메인 이벤트"], at=(968, 580), anchor="start", dashed=True)
s.arrow([(190, 840), (190, 878)], ["모든 모듈 [JPA], 각자 자기 테이블만"], at=(205, 864), anchor="start")
s.legend(1015, [(COMPONENT, "모듈"), (EXTERNAL, "바깥"), ("line", "호출"), ("dash", "도메인 이벤트")])
s.save(f"{OUT}/c4-3-component.svg")
