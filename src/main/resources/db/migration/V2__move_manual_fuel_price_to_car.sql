-- 직접 입력 유가를 크루에서 차량으로 옮김
-- 운전자 교체로 한 크루에서 연료가 다른 차들이 섞이면 크루 값 하나로는 맞지 않음
-- 전기차 충전 단가처럼 차 주인만 아는 값이라, 차를 등록한 멤버만 바꿀 수 있게 함

alter table cars
    add column manual_fuel_price_won integer,
    add constraint ck_cars_manual_fuel_price_won check (manual_fuel_price_won > 0);

-- 크루에 넣어 둔 값은 그 크루에서 운행한 차량으로 옮김. 여러 크루에서 쓴 차는 가장 최근 운행의 크루 값
update cars c
set manual_fuel_price_won = latest.price_won
from (
    select distinct on (t.car_id) t.car_id, cr.manual_fuel_price_won as price_won
    from trips t
    join crew_members cm on cm.id = t.driver_id
    join crews cr on cr.id = cm.crew_id
    where cr.manual_fuel_price_won is not null
    order by t.car_id, t.started_at desc
) latest
where c.id = latest.car_id;

alter table crews drop column manual_fuel_price_won;
