import pytz
from datetime import datetime, timedelta
from maps.garden.sdk.yt import YtFileResource
from maps.garden.tools.unaccounted_resources.lib.storage_interfaces.yt import YTStorageInterface, IGNORE_PATHS

MAP_NODE = "map_node"
FAKE_NODE = "fake_node"
YT_PREFIX = "home"

YT_FILE_LIST = [
    f"{YT_PREFIX}/a",
    f"{YT_PREFIX}/a/1",
    f"{YT_PREFIX}/a/2",
    f"{YT_PREFIX}/b",
    f"{YT_PREFIX}/b/1",
    f"{YT_PREFIX}/b/2",
    f"{YT_PREFIX}/c",
    f"{YT_PREFIX}/c/1",
    f"{YT_PREFIX}/c/d",
    f"{YT_PREFIX}/c/d/1",
    f"{YT_PREFIX}/e",
]


def create_fake_yt_resource(mocker, server, path):
    fake_yt_resource = mocker.Mock(spec=YtFileResource)
    fake_yt_resource.server = server
    fake_yt_resource.path = path
    return fake_yt_resource


class FakeEntity:
    def __init__(self, path: str, type: str = FAKE_NODE, creation_time: datetime = None):
        if not creation_time:
            creation_time = datetime.now(tz=pytz.utc) - timedelta(days=10)

        self.path = path
        self.attributes = {
            "creation_time": creation_time.isoformat(),
            "type": type
        }

    def __str__(self):
        return self.path


def test_subtree_filter(environment_settings, default_missing_remover_config):
    interface = YTStorageInterface(default_missing_remover_config, environment_settings)

    ignored_paths = [
        f"{YT_PREFIX}{path}" for i, path in enumerate(IGNORE_PATHS)
    ]
    new_date = datetime.now(tz=pytz.utc)
    assert not interface._subtree_filter(ignored_paths, ignored_paths[0], FakeEntity(ignored_paths[0]))
    assert not interface._subtree_filter(ignored_paths, "/1/latest", FakeEntity("/1/latest"))
    assert not interface._subtree_filter(ignored_paths, "/1", FakeEntity(
        "/1", creation_time=new_date
    ))
    assert interface._subtree_filter(ignored_paths, "/1", FakeEntity("/1"))


def test_add_simple_add_and_check(environment_settings, mocker, default_missing_remover_config):
    interface = YTStorageInterface(default_missing_remover_config, environment_settings)
    yt_proxy = list(environment_settings["yt_servers"].keys())[0]
    yt_prefix = list(environment_settings["yt_servers"].values())[0]["prefix"]

    paths = {
        "exist_and_registered": f"{yt_prefix}/exist_and_registered",
        "exist_not_registered_recently": f"{yt_prefix}/exist_not_registered_recently",
        "exist_not_registered": f"{yt_prefix}/exist_not_registered",
        "not_exist_registered": f"{yt_prefix}/not_exist_registered",
        "latest_symlink": f"{yt_prefix}/latest",
    }

    interface.add_registered_resource(create_fake_yt_resource(mocker, yt_proxy, paths["exist_and_registered"]))
    interface.add_registered_resource(create_fake_yt_resource(mocker, yt_proxy, paths["not_exist_registered"]))
    assert len(interface.garden_resources[yt_proxy]) == 4
    # Todo uncomment it when remove hack with changing //home/garden to //home/maps/core/garden
    # assert len(interface.all_registered_paths[yt_proxy]) == len(yt_prefix.split("/")) + 2

    yt_client_patch = mocker.patch(
        "maps.garden.tools.unaccounted_resources.lib.storage_interfaces.yt.YtClient", autospec=True
    )
    yt_client_patch().search.return_value = [
        paths["exist_not_registered"]
    ]

    interface.fetch_external_resources()
    return interface.get_missing()


def test_dfs(environment_settings, mocker, default_missing_remover_config):
    interface = YTStorageInterface(default_missing_remover_config, environment_settings)
    yt_proxy = list(environment_settings["yt_servers"].keys())[0]
    environment_settings["yt_servers"][yt_proxy]["prefix"] = YT_PREFIX

    yt_client_patch = mocker.patch(
        "maps.garden.tools.unaccounted_resources.lib.storage_interfaces.yt.YtClient", autospec=True
    )

    yt_client_patch().search.return_value = YT_FILE_LIST

    interface.fetch_external_resources()
    return interface.get_missing()


def test_dfs_registered(environment_settings, mocker, default_missing_remover_config):
    interface = YTStorageInterface(default_missing_remover_config, environment_settings)
    yt_proxy = list(environment_settings["yt_servers"].keys())[0]
    environment_settings["yt_servers"][yt_proxy]["prefix"] = YT_PREFIX
    paths = [
        f"{YT_PREFIX}/a",
        f"{YT_PREFIX}/b",
        f"{YT_PREFIX}/c/d/1",
        f"{YT_PREFIX}/c/d",
    ]
    for path in paths:
        interface.add_registered_resource(create_fake_yt_resource(mocker, yt_proxy, path))

    yt_client_patch = mocker.patch(
        "maps.garden.tools.unaccounted_resources.lib.storage_interfaces.yt.YtClient", autospec=True
    )

    yt_client_patch().search.return_value = YT_FILE_LIST

    interface.fetch_external_resources()
    return interface.get_missing()
