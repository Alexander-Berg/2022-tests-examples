import pytest
from deepdiff import DeepDiff
from google.protobuf import json_format
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import Coordinator, resolve_hosts


def test_empty(coordinator: Coordinator):
    resp = coordinator.dataset_versions(dataset='kek')
    assert len(resp.versions) == 0


@pytest.mark.parametrize('moved', (True, False))
@pytest.mark.parametrize('tag', (':tag', ''))
def test_not_active(coordinator: Coordinator, moved: bool, tag: str):
    dataset = 'pkg-a'
    version = '1'
    coordinator.upload(f'{dataset}{tag}', version, host='uploader', tvm_id=1)
    if moved:
        for branch in ('stable', 'prestable'):
            coordinator.move(dataset, version, f'+{branch}', 'host', tvm_id=1)

    resp_json = json_format.MessageToDict(
        coordinator.dataset_versions(dataset),
        preserving_proto_field_name=True,
        including_default_value_fields=True,
    )

    assert not DeepDiff(resp_json, {'versions': [
        {
            'version': version,
            'tag_statuses': [
                {
                    'tag': tag,
                    'branch_statuses': [
                        {
                            'branch': 'stable',
                            'moved': moved,
                            'on_hold': False,
                            'active_deploy_groups_count': 0,
                            'deploy_groups_count': 3,
                            'has_failed': False,
                        },
                        {
                            'branch': 'prestable',
                            'moved': moved,
                            'on_hold': False,
                            'active_deploy_groups_count': 0,
                            'deploy_groups_count': 1,
                            'has_failed': False,
                        },

                    ]
                }
            ]
        }
    ]}, ignore_order=True)


def test_active(coordinator: Coordinator):
    dataset = 'pkg-a'
    version = '1'
    hash = coordinator.upload(dataset, version, host='uploader', tvm_id=1)
    coordinator.move(dataset, version, '+stable', 'kek', tvm_id=1)
    coordinator.announce(hash, resolve_hosts('rtc:maps_a'))
    coordinator.postdl(dataset, version, resolve_hosts('rtc:maps_a'))
    coordinator.report_versions(resolve_hosts('rtc:maps_a'))
    resp_json = json_format.MessageToDict(
        coordinator.dataset_versions(dataset),
        preserving_proto_field_name=True,
        including_default_value_fields=True,
    )
    assert not DeepDiff(resp_json, {'versions': [
        {
            'version': version,
            'tag_statuses': [
                {
                    'tag': '',
                    'branch_statuses': [
                        {
                            'branch': 'stable',
                            'moved': True,
                            'on_hold': False,
                            'active_deploy_groups_count': 1,
                            'deploy_groups_count': 3,
                            'has_failed': False,
                        },
                        {
                            'branch': 'prestable',
                            'moved': False,
                            'on_hold': False,
                            'active_deploy_groups_count': 0,
                            'deploy_groups_count': 1,
                            'has_failed': False,
                        },

                    ]
                }
            ]
        }
    ]}, ignore_order=True)


def test_failed(coordinator: Coordinator):
    dataset = 'pkg-a'
    version = '1'
    hash = coordinator.upload(dataset, version, host='uploader', tvm_id=1)
    coordinator.move(dataset, version, '+stable', 'kek', tvm_id=1)
    coordinator.announce(hash, resolve_hosts('rtc:maps_a'))
    coordinator.postdl(dataset, version, resolve_hosts('rtc:maps_a'))
    coordinator.switch_failed(dataset, version, resolve_hosts('rtc:maps_a')[0])

    resp_json = json_format.MessageToDict(
        coordinator.dataset_versions(dataset),
        preserving_proto_field_name=True,
        including_default_value_fields=True,
    )

    assert not DeepDiff(resp_json, {'versions': [
        {
            'version': version,
            'tag_statuses': [
                {
                    'tag': '',
                    'branch_statuses': [
                        {
                            'branch': 'stable',
                            'moved': True,
                            'on_hold': False,
                            'active_deploy_groups_count': 0,
                            'deploy_groups_count': 3,
                            'has_failed': True,
                        },
                        {
                            'branch': 'prestable',
                            'moved': False,
                            'on_hold': False,
                            'active_deploy_groups_count': 0,
                            'deploy_groups_count': 1,
                            'has_failed': False,
                        },

                    ]
                }
            ]
        }
    ]}, ignore_order=True)


def test_hold(coordinator: Coordinator):
    dataset = 'pkg-a'
    version = '1'
    hash = coordinator.upload(dataset, version, host='uploader', tvm_id=1)
    coordinator.move(dataset, version, '+stable/hold', 'kek', tvm_id=1)
    coordinator.announce(hash, resolve_hosts('rtc:maps_a'))

    resp_json = json_format.MessageToDict(
        coordinator.dataset_versions(dataset),
        preserving_proto_field_name=True,
        including_default_value_fields=True,
    )

    assert not DeepDiff(resp_json, {'versions': [
        {
            'version': version,
            'tag_statuses': [
                {
                    'tag': '',
                    'branch_statuses': [
                        {
                            'branch': 'stable',
                            'moved': True,
                            'on_hold': True,
                            'active_deploy_groups_count': 0,
                            'deploy_groups_count': 3,
                            'has_failed': False,
                        },
                        {
                            'branch': 'prestable',
                            'moved': False,
                            'on_hold': False,
                            'active_deploy_groups_count': 0,
                            'deploy_groups_count': 1,
                            'has_failed': False,
                        },

                    ]
                }
            ]
        }
    ]}, ignore_order=True)
