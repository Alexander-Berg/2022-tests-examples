from maps.garden.sdk import test_utils
from maps.garden.modules.renderer_denormalization_source_config.lib.graph import (
    fill_graph,
    _get_resource_properties
)

SANDBOX_REPLY = {
    "items": [
        {
            "skynet_id": "rbtorrent:899f461ea9a4c69a6c9d7080dbcce1d244ea7ec9",
            "task": {
                "status": "SUCCESS",
                "url": "http://sandbox.yandex-team.ru/api/v1.0/task/443249075",
                "id": 443249075
            },
            "http": {
                "proxy": "https://proxy.sandbox.yandex-team.ru/979034940",
                "links": [
                    "http://sandbox1468.search.yandex.net:13578/5/7/443249075/source-config.sandbox.tar.gz"
                ]
            },
            "description": "maps/garden/modules/renderer_denormalization_source_config/src_pkg.json",
            "rights": "write",
            "url": "http://sandbox.yandex-team.ru/api/v1.0/resource/979034940",
            "file_name": "source-config.sandbox.tar.gz",
            "state": "READY",
            "arch": "any",
            "time": {
                "accessed": "2019-06-05T17:58:05Z",
                "updated": "2019-06-05T18:04:49.121000Z",
                "expires": "2019-07-05T17:58:05Z",
                "created": "2019-06-05T17:58:05Z"
            },
            "owner": "eak1mov",
            "attributes": {
                "resource_name": "source-config",
                "description": "source-config.linux.release.trunk.5105554",
                "backup_task": 443253666,
                "platform": "Linux-4.9.151-35-x86_64-with-Ubuntu-12.04-precise",
                "svn_path": "arcadia:/arc/trunk/arcadia",
                "svn_revision": "5105554",
                "branch": "trunk",
                "ttl": "30",
                "resource_version": "sandbox",
                "build_type": "release"
            },
            "size": 5120,
            "type": "MAPS_RENDERER_DENORMALIZATION_SOURCE_CONFIG",
            "id": 979034940,
            "md5": "709d9b4f344ea2b658caeb0afc79690e"
        }
    ]
}

EXPECTED_PROPERTIES = {
    "file_list": [
        {
            "md5": "709d9b4f344ea2b658caeb0afc79690e",
            "name": "source-config.sandbox.tar.gz",
            "url": "https://proxy.sandbox.yandex-team.ru/979034940"
        }
    ],
    "release_name": "2019.06.05-17:58-r5105554",
    "sandbox_resource_id": 979034940,
    "shipping_date": "2019-06-05T17:58:05Z",
    "svn_revision": "5105554"
}


def test_fill_graph():
    test_utils.graph.validate_fill_graph_routine(fill_graph)


def test_get_resource_properties():
    assert _get_resource_properties(SANDBOX_REPLY["items"][0]) == EXPECTED_PROPERTIES
