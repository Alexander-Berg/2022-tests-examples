from maps.garden.sdk.resources.scanners import scan_sandbox, BuildExternalResource, SourceDataset

RESOURCE_NAME_TEMPLATE = "gtfs_src_url_{}"
MAX_GTFS_DATASETS_PER_CITY = 2
SANDBOX_GTFS_RESOURCE_TYPE = "MAPS_MASSTRANSIT_GTFS_SOURCE"
SANDBOX_MASSTRANSIT_GTFS_FEEDS_TASKS_OWNER = "MAPS-MASSTRANSIT"

FILL_GRAPH_TEST_CONFIG = {
    "feeds": ["milan", "phoenix"],
    "feeds_for_realtime": ["saint-petersburg"],
}
SANDBOX_REPLY = {
    "items": [
        {
            "skynet_id": "rbtorrent:c84310483435dab478074002934ccb17c9fa4df7",
            "task": {
                "url": "https://sandbox.yandex-team.ru/api/v1.0/task/110269082",
                "status": "RELEASED",
                "id": 110269082
            },
            "http": {
                "proxy": "https://proxy.sandbox.yandex-team.ru/260460562",
                "links": [
                    "http://sandbox-storage23.search.yandex.net:13578/resource/260460562/RESOURCE",
                    "http://sandbox-storage24.search.yandex.net:13578/resource/260460562/RESOURCE",
                    "http://sandbox744.search.yandex.net:13578/2/8/110269082/RESOURCE"
                ]
            },
            "description": "Upload gtfs source data for calgary",
            "rights": "write",
            "url": "https://sandbox.yandex-team.ru/api/v1.0/resource/260460562",
            "file_name": "RESOURCE",
            "state": "READY",
            "type": "MAPS_MASSTRANSIT_GTFS_SOURCE",
            "time": {
                "accessed": "2017-05-15T10:22:17.039000Z",
                "expires": None,
                "created": "2017-05-15T09:02:11.748264"
            },
            "owner": "MAPS-MASSTRANSIT",
            "attributes": {
                "city": "calgary",
                "released": "stable",
                "backup_task": 110282897,
                "ttl": "inf"
            },
            "size": 34555904,
            "arch": "any",
            "id": 260460562,
            "md5": "ac4b8e5fbe210051a8562842dabcb977"
        }
    ]
}

SCAN_RESOURCES_TEST_CONFIG = {
    "feeds": ["calgary"],
}


def _get_resource_properties(dataset):
    return {
        "file_list": [{"name": "gtfs_data", "url": dataset["http"]["proxy"], "md5": dataset["md5"]}],
        "shipping_date": dataset["time"]["created"],
        "region": dataset["attributes"]["city"]
    }


def _scan_resources(environment_settings):
    yield from scan_sandbox(
        environment_settings=environment_settings,
        sandbox_attrs_filter={"city": "calgary", "released": "stable"},
        sandbox_resource_type=SANDBOX_GTFS_RESOURCE_TYPE,
        sandbox_owner=SANDBOX_MASSTRANSIT_GTFS_FEEDS_TASKS_OWNER,
        get_garden_resource_properties_func=_get_resource_properties,
        get_garden_resource_name_func=lambda dataset: RESOURCE_NAME_TEMPLATE.format(dataset["attributes"]["city"]),
        limit=MAX_GTFS_DATASETS_PER_CITY
    )


def test_scan_sandbox_resources(requests_mock):
    environment_settings = {"sandbox-default": {"token": ""}}
    requests_mock.get(
        "https://sandbox.yandex-team.ru/api/v1.0/resource?type={}".format(SANDBOX_GTFS_RESOURCE_TYPE),
        [
            {"json": SANDBOX_REPLY, "status_code": 200},
        ]
    )

    datasets = list(_scan_resources(environment_settings))
    assert len(datasets) == 1
    assert isinstance(datasets[0], SourceDataset)
    foreign_key = datasets[0].foreign_key
    resources = datasets[0].resources
    assert foreign_key == {"sandbox_resource_id": str(SANDBOX_REPLY["items"][0]["id"])}
    assert len(resources) == 1
    assert isinstance(resources[0], BuildExternalResource)
    assert resources[0].resource_name == RESOURCE_NAME_TEMPLATE.format(SCAN_RESOURCES_TEST_CONFIG["feeds"][0])
    assert resources[0].properties == _get_resource_properties(SANDBOX_REPLY["items"][0])
