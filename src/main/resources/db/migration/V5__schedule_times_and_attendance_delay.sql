-- 동승자 웹 시안에 맞춘 시각 정보와 출발 전 지각
-- crews.arrival_time: 평소 도착 시각 (시안 "7:10 ~ 8:32")
-- crew_members.pickup_time: 평소 픽업 시각 (시안 경로 상세 "07:32 판교역에서 탑승"). 차량 앱이 요청했던 pickupTime
-- crew_attendances.delay_minutes: 출발 전에 알린 그날 지각 ("내일 10분 늦어요")

alter table crews
    add column arrival_time time;

alter table crew_members
    add column pickup_time time;

alter table crew_attendances
    add column delay_minutes integer,
    add constraint ck_crew_attendances_delay_minutes check (delay_minutes between 1 and 120);
