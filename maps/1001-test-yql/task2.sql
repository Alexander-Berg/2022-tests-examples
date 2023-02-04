USE hahn;

SELECT DISTINCT device_id
FROM (
    SELECT Yson::ConvertToStringList(device_ids) as device_list
    FROM `//home/maps/analytics/st/GDA/890-post-view-drive/anti_target_all_ids`
)
FLATTEN LIST BY device_list AS device_id;
