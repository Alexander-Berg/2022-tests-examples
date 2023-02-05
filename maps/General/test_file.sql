USE hahn;

$s_date = '2021-01-01';
$e_date = '2021-11-30';

-- Все GDU по в картах за 2021й год 
-- Как считать! - Некоторые пользователи совершают более одного gdu в день на одном пермалинке
-- https://yql.yandex-team.ru/Operations/Yaoc-yhnuY2pW7r_8Bxts112kmOVTCXmHVfFh0CQC_A=

$all_gdu_data = 
    SELECT 
        log_date,
        userid,
        permalink,
        COUNT(*) as gdu 
    FROM range(`//home/geoadv/statistics/clicks/processed_join/mobile_maps`, $s_date, $e_date)
    WHERE has_goal_deep_use == TRUE
        AND bc_type = 'discovery'
        AND userid is not null
        AND permalink is not null
    GROUP BY     
        log_date,
        userid,
        permalink
    ;

INSERT INTO `//home/maps/marketing/kvasnikov/GMA_1288/gdu_daily_data` WITH TRUNCATE
SELECT * 
FROM $all_gdu_data

