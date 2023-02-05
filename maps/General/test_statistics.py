import json
import maps.automotive.libs.large_tests.lib.docker as docker
import maps.automotive.libs.large_tests.lib.db as db

import lib.async_processor as async_processor

from lib.server import server


def test_check_stat_handle():
    with db.get_connection() as conn:
        with conn.cursor() as cur:
            def get_update_status():
                cur.execute("""
                    SELECT CURRENT_TIMESTAMP - last_update_time < '1 minutes'::interval AS has_been_updated
                    FROM statistics_update
                """)
                row = cur.fetchone()
                return row and row[0] is True

            async_processor.perform_all_work()
            assert get_update_status()

    response = docker.exec(
        "async-processor",
        ["curl", "-H", "Host: goods-async-processor.maps.yandex.net", "http://127.0.0.1/yasm_stats"])

    response = json.loads(response)
    assert isinstance(response, list)

    metrics_set = set(map(lambda x: x[0], response))
    expected = {
        "mod-processing-time_ahhh",
        "mod-images-in-progress_axxx",
        "mod-images-in-waiting_axxx",
        "mod-images-approved_axxx",
        "mod-images-declined_axxx",
        "mod-images-unused_axxx",
        "mod-texts-in-progress_axxx",
        "mod-texts-in-waiting_axxx",
        "mod-texts-approved_axxx",
        "mod-texts-declined_axxx",
        "mod-texts-unused_axxx",
        "photo-deletion-queue-size_axxx",
        "photo-deletion-queue-timings_ahhh",
        "photo-total-count_axxx",
        "photo-unused-count_axxx",
        "import-in-waiting-count_axxx",
        "import-in-progress-count_axxx",
        "import-processed-count_axxx",
        "import-failed-count_axxx",
        "import-processing-timings_ahhh",
        "publish-items-in-queue_axxx",
        "publish-from-edit-timings_ahhh",
        "publish-in-queue-timings_ahhh",

        "clean-web-send-rps_ammm",
        "clean-web-send-4xx_ammm",
        "clean-web-send-5xx_ammm",
        "clean-web-send-no-response_ammm",
        "clean-web-send-timings_ahhh",

        "DuplicatesManager-loop-failure-count_ammm",
        "ModerationMerger-loop-failure-count_ammm",
        "ModerationReceiver-loop-failure-count_ammm",
        "ModerationSender-loop-failure-count_ammm",
        "PhotoDeleter-loop-failure-count_ammm",
        "PhotoUploader-loop-failure-count_ammm",
        "PricesImporter-loop-failure-count_ammm",
        "PricesReplicator-loop-failure-count_ammm",
        "PricesSynchronizer-loop-failure-count_ammm",
        "Publisher-loop-failure-count_ammm",
        "ItemsDeleter-loop-failure-count_ammm",
        "StatisticsRefresher-loop-failure-count_ammm",

        "background-loop-failure-count_ammm",

        "DuplicatesManager-usefull-iterations-count_ammm",
        "ModerationMerger-usefull-iterations-count_ammm",
        "ModerationReceiver-usefull-iterations-count_ammm",
        "ModerationSender-usefull-iterations-count_ammm",
        "PhotoDeleter-usefull-iterations-count_ammm",
        "PhotoUploader-usefull-iterations-count_ammm",
        "PricesImporter-usefull-iterations-count_ammm",
        "PricesReplicator-usefull-iterations-count_ammm",
        "PricesSynchronizer-usefull-iterations-count_ammm",
        "Publisher-usefull-iterations-count_ammm",
        "ItemsDeleter-usefull-iterations-count_ammm",
        "StatisticsRefresher-usefull-iterations-count_ammm",

        "price-importer-hash-hit-rate_ahhh",
        "price-importer-external-id-hit-rate_ahhh",
        "price-importer-partial-hash-hit-rate_ahhh",

        "price-importer-any-hash-hit-rate_ahhh",
        "price-importer-any-hit-rate_ahhh",
    }
    assert metrics_set.intersection(expected) == expected


def test_check_api_server_stat_handle(user, company):
    server.get_prices(user, company) >> 200
    response = docker.exec(
        "server",
        ["curl", "-H", "Host: geoapp-goods-api-server.maps.yandex.net", "http://127.0.0.1/yasm_stats"])

    response = json.loads(response)
    assert isinstance(response, list)

    metrics_set = set(map(lambda x: x[0], response))
    expected = {
        "avatars-delete-rps_ammm",
        "avatars-delete-4xx_ammm",
        "avatars-delete-5xx_ammm",
        "avatars-delete-timings_ahhh",
        "avatars-delete-no-response_ammm",

        "avatars-upload-rps_ammm",
        "avatars-upload-4xx_ammm",
        "avatars-upload-5xx_ammm",
        "avatars-upload-no-response_ammm",
        "avatars-upload-timings_ahhh",

        "sprav-rps_ammm",
        "sprav-4xx_ammm",
        "sprav-5xx_ammm",
        "sprav-no-response_ammm",
        "sprav-timings_ahhh",
    }
    assert metrics_set.intersection(expected) == expected
