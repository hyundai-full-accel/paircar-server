package com.fullaccel.paircarserver.global.time

import java.time.ZoneId

// 어노테이션(@Scheduled zone)에 넣을 수 있는 상수
const val KST_ZONE_ID = "Asia/Seoul"

// 운행일, 정산 월 같은 날짜 경계의 기준 시간대. 시각은 UTC로 저장
val KST: ZoneId = ZoneId.of(KST_ZONE_ID)
