use arnold;
pragma yt.Pool = 'robot-webmaster';
pragma yt.InferSchema = '1';
pragma yt.MaxRowWeight = '128M';

INSERT INTO `home/webmaster/test/searchqueries/reports_v4/last_queries` WITH TRUNCATE
SELECT key, subkey, value, path, clicks, shows
FROM `home/webmaster/prod/searchqueries/reports_v4/last_queries` as q
LEFT SEMI JOIN `//home/webmaster/test/export/archive/webmaster-verified-hosts-latest` as wh
ON q.key == wh.host_url;

INSERT INTO `home/webmaster/test/searchqueries/reports_v4/favorite_queries` WITH TRUNCATE
SELECT key, subkey, value, path, clicks, shows
FROM `home/webmaster/prod/searchqueries/reports_v4/favorite_queries` as q
LEFT SEMI JOIN `//home/webmaster/test/export/archive/webmaster-verified-hosts-latest` as wh
ON q.key == wh.host_url;

INSERT INTO `home/webmaster/test/searchqueries/reports_v4/top_urls` WITH TRUNCATE
SELECT key, subkey, value, path, clicks, shows
FROM `home/webmaster/prod/searchqueries/reports_v4/top_urls` as q
LEFT SEMI JOIN `//home/webmaster/test/export/archive/webmaster-verified-hosts-latest` as wh
ON q.key == wh.host_url;

INSERT INTO `home/webmaster/test/searchqueries/reports_v4/top_3month.top` WITH TRUNCATE
SELECT key, subkey, value, path, clicks, shows
FROM `home/webmaster/prod/searchqueries/reports_v4/top_3month.top` as q
LEFT SEMI JOIN `//home/webmaster/test/export/archive/webmaster-verified-hosts-latest` as wh
ON q.key == wh.host_url;

$groupsToProcess = (select aggregate_list(TableName(prod.Path)) from
    folder(`home/webmaster/prod/searchqueries/reports_v4/groups`) as prod
    left only join folder(`home/webmaster/test/searchqueries/reports_v4/groups`) as test
    on TableName(prod.Path) == TableName(test.Path)
    where TableName(prod.Path) >= 'clicks_shows_20200901_20200901_for_wmc_web'
);

EVALUATE FOR $group IN $groupsToProcess DO
BEGIN
    $testTable = 'home/webmaster/test/searchqueries/reports_v4/groups/' || $group;
    $prodTable = 'home/webmaster/prod/searchqueries/reports_v4/groups/' || $group;

    INSERT INTO $testTable WITH TRUNCATE
    SELECT Yson::ConvertToString(q.key) as key, Yson::ConvertToString(q.subkey) as subkey, WeakField(q.value, String) as value
    FROM $prodTable AS q
    LEFT SEMI JOIN `//home/webmaster/test/export/archive/webmaster-verified-hosts-latest` as wh
    ON Yson::ConvertToString(q.key) == wh.host_url;
END DO;