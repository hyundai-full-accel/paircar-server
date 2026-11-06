package com.fullaccel.paircarserver.global.time

import java.time.ZoneId

// 운행일, 정산 월 같은 날짜 경계의 기준 시간대. 시각은 UTC로 저장
val KST: ZoneId = ZoneId.of("Asia/Seoul")
