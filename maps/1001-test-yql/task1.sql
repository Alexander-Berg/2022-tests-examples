USE hahn;

-- Campaign id
$campaign_id = 34478;

-- Find campaign dates
SELECT start_date, end_date
FROM `//home/geodisplay/analytics/regular/pin_shows/geoadv_campaign-v2`
WHERE campaign_id = $campaign_id;

-- Find all campaign events (using dates from table above)
$campaign_events = (
    SELECT *
    FROM RANGE(
        `//home/geodisplay/analytics/regular/rich-events`,
        `2022-04-28`,
        `2022-05-09`
    )
    WHERE campaign_id = $campaign_id
);

-- Dayly statistics
$day_statistics = (
    SELECT
        fielddate AS day,
        COUNT_IF(event_name = 'BILLBOARD_SHOW') AS shows,
        COUNT(DISTINCT (CASE event_name WHEN 'BILLBOARD_SHOW' THEN device_id ELSE NULL END)) AS unique_shows,
        SUM(cost) / 1e6 AS cost,
        COUNT_IF(event_name = 'BILLBOARD_TAP') AS taps,
        COUNT_IF(event_name = 'ACTION_OPEN_SITE') AS site_openings,
        COUNT_IF(event_name = 'ACTION_SEARCH') AS searches
    FROM $campaign_events
    GROUP BY fielddate
);

-- Overall statictics (aggregate over whole table)
$overall_statistics = (
    SELECT
        'Total' AS day,
        COUNT_IF(event_name = 'BILLBOARD_SHOW') AS shows,
        COUNT(DISTINCT (CASE event_name WHEN 'BILLBOARD_SHOW' THEN device_id ELSE NULL END)) AS unique_shows,
        SUM(cost) / 1e6 AS cost,
        COUNT_IF(event_name = 'BILLBOARD_TAP') AS taps,
        COUNT_IF(event_name = 'ACTION_OPEN_SITE') AS site_openings,
        COUNT_IF(event_name = 'ACTION_SEARCH') AS searches
    FROM $campaign_events
);

-- Show results
SELECT
    ds.*,
    CAST(taps AS DOUBLE) / shows * 100 AS CTR
FROM $day_statistics AS ds
UNION ALL
SELECT
    os.*,
    CAST(taps AS DOUBLE) / shows * 100 AS CTR
FROM $overall_statistics AS os;
