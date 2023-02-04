import logging
import time
import typing

from unittest import mock

import pytest

import yatest

from infra.nanny.nanny_services_rest.nanny_services_rest.client import ServiceRepoClient

from infra.rtc_sla_tentacles.backend.lib.config.interface import ConfigInterface
from infra.rtc_sla_tentacles.backend.lib.harvesters.resource_maker import ResourceMaker
from infra.rtc_sla_tentacles.backend.lib.harvesters import yp_lite_switcher
from infra.rtc_sla_tentacles.backend.lib.yp_lite.pods_manager import YpLitePodsManager

resource_data = 'rbtorrent:ololo'


@pytest.fixture
def ts() -> int:
    return int(time.time())


@pytest.fixture
def with_resource_snapshot(config_interface, harvesters_snapshot_manager, ts):
    storage_dir = yatest.common.runtime.work_path('resources')

    resource_maker = ResourceMaker(
        "test_maker",
        arguments={
            'storage_dir': storage_dir,
            'keep_copies': 0,
        },
        common_parameters=None,
        common_settings={
            'chunk_size': 0,
            'data_list_path': None,
            'rotate_snapshots_older_than_sec': 10000,
            'update_interval_sec': 300,
        },
        snapshot_manager=harvesters_snapshot_manager,
        config_interface=config_interface,
        several_harvesters=False,
    )

    with mock.patch('subprocess.Popen') as mock_popen:
        with mock.patch.object(resource_maker.juggler_sender, "ok"):
            proc = mock_popen.return_value
            proc.returncode = 0
            proc.communicate.return_value = (b'rbtorrent:testvalue', b'')
            resource_maker.run(ts)


@pytest.fixture
def switcher(config_interface, monkeypatch,
             harvesters_snapshot_manager, with_resource_snapshot) -> yp_lite_switcher.YpLiteSwitcher:

    # noinspection PyUnusedLocal
    def mock_allow_redeploy(self, ts):
        return True

    monkeypatch.setattr(yp_lite_switcher.YpLiteSwitcher, "_allow_redeploy", mock_allow_redeploy)

    return yp_lite_switcher.YpLiteSwitcher(
        'rtc_sla_tentacles_testing_yp_lite',
        common_parameters={},
        arguments={
            'resource_maker': 'test_maker',
            'cooldown_after_redeployment_min': 1,
            'update_nanny_instances_with_allocated_pods': True
        },
        common_settings={
            'chunk_size': 0,
            'data_list_path': None,
            'rotate_snapshots_older_than_sec': 10000,
        },
        snapshot_manager=harvesters_snapshot_manager,
        config_interface=config_interface,
        several_harvesters=False,
    )


def test_get_resource(switcher: yp_lite_switcher.YpLiteSwitcher):
    # noinspection PyProtectedMember
    assert switcher._get_most_fresh_resource().rbtorrentid == 'rbtorrent:testvalue'


def test_new_spec(monkeypatch: typing.Any, switcher: yp_lite_switcher.YpLiteSwitcher, config_interface: ConfigInterface):

    resource_name = "myresource"

    def _mocked_nanny_service_repo_client_get_runtime_attrs(_name) -> dict:
        return {
            "_id": "some-snapshot-id",
            "content": {
                "resources": {
                    "url_files": [
                        {
                            "local_path": resource_name,
                            "url": "rbtorrent:old-testvalue"
                        }
                    ]
                },
                "instances": {
                    "yp_pod_ids": {
                        "pods": ["old-pod-foo", "old-pod-bar"]
                    }
                }
            }
        }

    nanny_service_repo_client = ServiceRepoClient(url="http://example.com/", token="some-token")
    monkeypatch.setattr(nanny_service_repo_client, "get_runtime_attrs",
                        _mocked_nanny_service_repo_client_get_runtime_attrs)

    def _mocked_pods_manager_get_allocated_pods() -> typing.List[str]:
        return ["new-pod-foo", "new-pod-bar"]
    with YpLitePodsManager(nanny_service_name="_foo",
                           yp_cluster="FAKE",
                           logger=logging.getLogger(),
                           config_interface=config_interface) as pods_manager:
        monkeypatch.setattr(pods_manager, "_get_actual_allocated_pods", _mocked_pods_manager_get_allocated_pods)

        agent_resource = yp_lite_switcher.AgentResource("rbtorrent:testvalue", tar=False, resource_name=resource_name)
        # noinspection PyProtectedMember
        request = switcher._get_runtime_attrs_change_request(agent_resource, nanny_service_repo_client, pods_manager)

    assert request == {
        "snapshot_id": "some-snapshot-id",
        "content": {
            "instances": {
                "yp_pod_ids": {
                    "pods": [
                        {"cluster": "FAKE", "pod_id": "new-pod-foo"},
                        {"cluster": "FAKE", "pod_id": "new-pod-bar"}
                    ]
                }
            },
            "resources": {
                "url_files": [
                    {
                        "local_path": resource_name,
                        "url": "rbtorrent:testvalue"
                    }
                ]
            }
        },
        "comment": f"Bump {resource_name}, rbtorrent:testvalue, updated pods list with 2 allocated pods",
        "meta_info": {
            "scheduling_config": {
                "scheduling_priority": "CRITICAL",
            }
        }
    }
