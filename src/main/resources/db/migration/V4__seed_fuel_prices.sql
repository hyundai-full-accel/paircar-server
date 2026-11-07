-- 연료별 월평균 유가 초기값. 차량에 직접 입력한 유가가 없을 때 정산이 씀
-- 정산은 정산 월의 행만 찾으므로, 이후 달은 이 테이블에 따로 넣어야 함 (없으면 FUEL_PRICE_NOT_FOUND)
-- 값은 근사값: 휘발유는 기획서 예시(1,684원/L)에 맞추고, 전기는 환경부 공공 급속충전 단가(347.2원/kWh)를 반올림
-- 실제 월평균으로 바꾸려면 새 마이그레이션에서 update 할 것 (이 파일은 고치지 않음)

insert into fuel_prices (fuel_type, month, price_won, collected_at)
select fuel_type, month::date, price_won, now()
from (
    values ('GASOLINE', 1684),
           ('DIESEL',   1550),
           ('LPG',      1050),
           ('EV',        347)
) as price (fuel_type, price_won)
cross join (
    values ('2026-09-01'), ('2026-10-01'), ('2026-11-01')
) as m (month);
