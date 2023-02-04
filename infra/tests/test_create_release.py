from unittest import mock

import pytest
from google.protobuf import json_format

from infra.deploy_ci.create_release import impl as create_release


ACL = [{
    'action': 'allow',
    'subjects': ['test'],
    'permissions': ['write'],
}]


def make_project(client) -> str:
    return client.create_object(object_type="project", attributes={
        'spec': {
            'account_id': 'tmp'
        }
    })


def make_stage(client, project_id: str) -> str:
    return client.create_object(object_type="stage", attributes={
        "meta": {
            'acl': ACL,
            'project_id': project_id,
        },
        'labels': {
            'separate_du_revision': True,
        },
        'spec': {
            'account_id': 'tmp',
            'revision': 1,
            'deploy_units': {
                'du1': {
                    'deploy_settings': {
                        'cluster_sequence': [
                            {
                                'need_approval': True,
                                'yp_cluster': 'man',
                            },
                            {
                                'need_approval': True,
                                'yp_cluster': 'sas',
                            },
                        ],
                    },
                    'replica_set': {
                        'per_cluster_settings': {
                            'man': {},
                            'sas': {},
                        },
                        'replica_set_template': {
                            'pod_template_spec': {
                                'spec': {
                                    'pod_agent_payload': {
                                        'spec': {
                                            'resources': {
                                                'static_resources': [
                                                    {
                                                        'id': 'resource1',
                                                        'url': 'sbr:2347449518',
                                                    },
                                                ],
                                                'layers': [
                                                    {
                                                        'url': 'sbr:2083514813',
                                                        'checksum': 'EMPTY:',
                                                        'id': 'layer',
                                                    },
                                                    {
                                                        'url': 'sbr:755375039',
                                                        'checksum': 'EMPTY:',
                                                        'id': 'simple_http_server',
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
                'du2': {
                    'replica_set': {
                        'replica_set_template': {
                            'pod_template_spec': {
                                'spec': {
                                    'pod_agent_payload': {
                                        'spec': {
                                            'resources': {
                                                'static_resources': [
                                                    {
                                                        'id': 'resource1',
                                                        'url': 'sbr:2347449518',
                                                    },
                                                ],
                                                'layers': [
                                                    {
                                                        'url': 'sbr:2083514813',
                                                        'checksum': 'EMPTY:',
                                                        'id': 'layer',
                                                    },
                                                    {
                                                        'url': 'sbr:755375039',
                                                        'checksum': 'EMPTY:',
                                                        'id': 'simple_http_server',
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
                'du3': {
                    'images_for_boxes': {
                        'box': {
                            'registry_host': 'registry.yandex.net',
                            'tag': 'tag1',
                            'name': 'docker1',
                            'digest': 'sha256:' + 'a' * 64,
                        }
                    },
                    'replica_set': {
                        'replica_set_template': {
                            'pod_template_spec': {
                                'spec': {
                                    'pod_agent_payload': {
                                        'spec': {
                                            'boxes': [{
                                                'id': 'box',
                                                'rootfs': {},
                                            }],
                                            'resources': {},
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
                'du4': {
                    'images_for_boxes': {
                        'box': {
                            'registry_host': 'registry.yandex.net',
                            'tag': 'tag1',
                            'name': 'docker1',
                            'digest': 'sha256:' + 'a' * 64,
                        }
                    },
                    'replica_set': {
                        'replica_set_template': {
                            'pod_template_spec': {
                                'spec': {
                                    'pod_agent_payload': {
                                        'spec': {
                                            'boxes': [{
                                                'id': 'box',
                                                'rootfs': {},
                                            }],
                                            'resources': {},
                                        },
                                    },
                                },
                            },
                        },
                    },
                },
            },
            "dynamic_resources": {
                "dr1": {
                    "deploy_unit_ref": "du3",
                    "dynamic_resource": {
                        "revision": 1,
                        "update_window": 1,
                        "deploy_groups": [{
                            "mark": "all",
                            "urls": ["raw:nonexistent"],
                            "storage_options": {
                                "storage_dir": "/tmp",
                                "destination": "/tmp/dr1",
                                "box_ref": "box",
                            },
                        }],
                    }
                }
            },
        },
        'status': {
            'revision': 1,
            'deploy_units': {
                'du1': {
                    'replica_set': {
                        'revision_id': 1,
                    },
                    'target_revision': 1,
                    'ready': {
                        'status': 'true',
                    },
                    'in_progress': {
                        'status': 'false',
                    },
                    'failed': {
                        'status': 'false',
                    }
                },
                'du2': {
                    'replica_set': {
                        'revision_id': 1,
                    },
                    'target_revision': 1,
                    'ready': {
                        'status': 'true',
                    },
                    'in_progress': {
                        'status': 'false',
                    },
                    'failed': {
                        'status': 'false',
                    }
                },
                'du3': {
                    'replica_set': {
                        'revision_id': 1,
                    },
                    'target_revision': 1,
                    'ready': {
                        'status': 'true',
                    },
                    'in_progress': {
                        'status': 'false',
                    },
                    'failed': {
                        'status': 'false',
                    }
                },
            },
            "dynamic_resources": {
                "dr1": {
                    "status": {
                        "ready": {
                            "condition": {
                                "status": "true",
                            },
                        },
                        "error": {
                            "condition": {
                                "status": "false",
                            },
                        },
                        "in_progress": {
                            "condition": {
                                "status": "false",
                            },
                        },
                        "revision": 1,
                    },
                },
            },
        }
    })


def make_simple_static_resource_flow(stage_id: str):
    return '''
    {
        "config": {
            "release_type": "testing",
            "stage_id": "%(stage_id)s",
            "release_title": "Release: Autodeploy deploy_ci simple pipeline commit #30",
            "yp_cluster": "man-pre",
            "patches": [
                {
                    "sandbox": {
                        "static": {
                            "deploy_unit_id": "du1",
                            "static_resource_ref": "resource1"
                        },
                        "sandbox_resource_type": "TEST_RESOURCE"
                    }
                }
            ]
        },
        "sandbox_resources": [
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_OUTPUT",
                "id": "2347450248",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_OUTPUT_HTML",
                "id": "2347453282",
                "task_id": "1044454185"
            },
            {
                "type": "TASK_LOGS",
                "id": "2347449960",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_LOGS",
                "id": "2347453466",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "backup_task": "true"
                },
                "type": "YA_MAKE_HTML_RESULTS",
                "id": "2347450192",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_STATISTICS",
                "id": "2347453757",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "build_platform": "Linux-4.19.183-42.2mofed-x86_64-with-Ubuntu-12.04-precise",
                    "arcadia_tag": "",
                    "ttl": "14",
                    "pack_tar": "0",
                    "arcadia_branch": "",
                    "build_tool": "ya",
                    "arcadia_revision": "108146400334e61309d3756a6a33c8123490adf2",
                    "arcadia_trunk": "False"
                },
                "type": "TEST_RESOURCE",
                "id": "2347449518",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "backup_task": "true"
                },
                "type": "TEST_ENVIRONMENT_JSON_V2",
                "id": "2347450184",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_LOGS",
                "id": "2347453614",
                "task_id": "1044454185"
            }
        ],
        "context": {
            "job_instance_id": {
                "flow_launch_id": "2c1088f4dd585763d67511237d6983bfaa8e4ea75a593d5f9df9473b7c207a89",
                "job_id": "release-to-deploy",
                "number": 1
            },
            "target_revision": {
                "pull_request_id": "1944828",
                "hash": "108146400334e61309d3756a6a33c8123490adf2",
                "number": "8514667"
            },
            "title": "Autodeploy deploy_ci simple pipeline commit #30",
            "launch_number": 30,
            "version_info": {
                "major": "30",
                "full": "30"
            },
            "branch": "trunk",
            "version": "30",
            "ci_url": "https://a.yandex-team.ru/ci/workflow/details/2c1088f4dd585763d67511237d6983bfaa8e4ea75a593d5f9df9473b7c207a89",
            "config_info": {
                "path": "infra/deploy_ci/examples/simple_pipeline/a.yaml",
                "dir": "infra/deploy_ci/examples/simple_pipeline"
            },
            "flow_triggered_by": "torkve",
            "target_commit": {
                "pull_request_id": 1944828,
                "author": "aseppar",
                "date": "2021-08-12T09:42:48Z",
                "message": "fix routestats: fix ResultWrapper for optional",
                "issues": [
                    "EFFICIENCYDEV-14161"
                ],
                "revision": {
                    "pull_request_id": "1944828",
                    "hash": "108146400334e61309d3756a6a33c8123490adf2",
                    "number": "8514667"
                }
            },
            "secret_uid": "sec-01fctgsnj7cqzp1zj21sy5pwrr"
        }
    }
    ''' % {"stage_id": stage_id}


def make_simple_dynamic_resource_flow(stage_id: str):
    return '''
    {
        "config": {
            "release_type": "testing",
            "stage_id": "%(stage_id)s",
            "release_title": "Release: Autodeploy deploy_ci simple pipeline commit #30",
            "yp_cluster": "man-pre",
            "patches": [
                {
                    "sandbox": {
                        "dynamic": {
                            "dynamic_resource_id": "dr1",
                            "deploy_group_mark": "all"
                        },
                        "sandbox_resource_type": "TEST_RESOURCE"
                    }
                }
            ]
        },
        "sandbox_resources": [
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_OUTPUT",
                "id": "2347450248",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_OUTPUT_HTML",
                "id": "2347453282",
                "task_id": "1044454185"
            },
            {
                "type": "TASK_LOGS",
                "id": "2347449960",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_LOGS",
                "id": "2347453466",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "backup_task": "true"
                },
                "type": "YA_MAKE_HTML_RESULTS",
                "id": "2347450192",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_STATISTICS",
                "id": "2347453757",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "build_platform": "Linux-4.19.183-42.2mofed-x86_64-with-Ubuntu-12.04-precise",
                    "arcadia_tag": "",
                    "ttl": "14",
                    "pack_tar": "0",
                    "arcadia_branch": "",
                    "build_tool": "ya",
                    "arcadia_revision": "108146400334e61309d3756a6a33c8123490adf2",
                    "arcadia_trunk": "False"
                },
                "type": "TEST_RESOURCE",
                "id": "2347449518",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "backup_task": "true"
                },
                "type": "TEST_ENVIRONMENT_JSON_V2",
                "id": "2347450184",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_LOGS",
                "id": "2347453614",
                "task_id": "1044454185"
            }
        ],
        "context": {
            "job_instance_id": {
                "flow_launch_id": "2c1088f4dd585763d67511237d6983bfaa8e4ea75a593d5f9df9473b7c207a89",
                "job_id": "release-to-deploy",
                "number": 1
            },
            "target_revision": {
                "pull_request_id": "1944828",
                "hash": "108146400334e61309d3756a6a33c8123490adf2",
                "number": "8514667"
            },
            "title": "Autodeploy deploy_ci simple pipeline commit #30",
            "launch_number": 30,
            "version_info": {
                "major": "30",
                "full": "30"
            },
            "branch": "trunk",
            "version": "30",
            "ci_url": "https://a.yandex-team.ru/ci/workflow/details/2c1088f4dd585763d67511237d6983bfaa8e4ea75a593d5f9df9473b7c207a89",
            "config_info": {
                "path": "infra/deploy_ci/examples/simple_pipeline/a.yaml",
                "dir": "infra/deploy_ci/examples/simple_pipeline"
            },
            "flow_triggered_by": "torkve",
            "target_commit": {
                "pull_request_id": 1944828,
                "author": "aseppar",
                "date": "2021-08-12T09:42:48Z",
                "message": "fix routestats: fix ResultWrapper for optional",
                "issues": [
                    "EFFICIENCYDEV-14161"
                ],
                "revision": {
                    "pull_request_id": "1944828",
                    "hash": "108146400334e61309d3756a6a33c8123490adf2",
                    "number": "8514667"
                }
            },
            "secret_uid": "sec-01fctgsnj7cqzp1zj21sy5pwrr"
        }
    }
    ''' % {"stage_id": stage_id}


def make_docker_resource_flow(stage_id: str):
    return '''
    {
        "config": {
            "release_type": "testing",
            "stage_id": "%(stage_id)s",
            "release_title": "Release: Autodeploy deploy_ci simple pipeline commit #30",
            "yp_cluster": "man-pre",
            "patches": [
                {
                    "docker": {
                        "docker_image_ref": {
                            "box_id": "box",
                            "deploy_unit_id": "du3"
                        },
                        "image_name": "deploy-ci/image:tag1"
                    }
                },
                {
                    "docker": {
                        "docker_image_ref": {
                            "box_id": "box",
                            "deploy_unit_id": "du3"
                        },
                        "image_name": "deploy-ci/image:tag1"
                    }
                }
            ]
        },
        "sandbox_resources": [
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_OUTPUT",
                "id": "2347450248",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_OUTPUT_HTML",
                "id": "2347453282",
                "task_id": "1044454185"
            },
            {
                "type": "TASK_LOGS",
                "id": "2347449960",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_LOGS",
                "id": "2347453466",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "backup_task": "true"
                },
                "type": "YA_MAKE_HTML_RESULTS",
                "id": "2347450192",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_STATISTICS",
                "id": "2347453757",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "build_platform": "Linux-4.19.183-42.2mofed-x86_64-with-Ubuntu-12.04-precise",
                    "arcadia_tag": "",
                    "ttl": "14",
                    "pack_tar": "0",
                    "arcadia_branch": "",
                    "build_tool": "ya",
                    "arcadia_revision": "108146400334e61309d3756a6a33c8123490adf2",
                    "arcadia_trunk": "False"
                },
                "type": "TEST_RESOURCE",
                "id": "2347449518",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "backup_task": "true"
                },
                "type": "TEST_ENVIRONMENT_JSON_V2",
                "id": "2347450184",
                "task_id": "1044454185"
            },
            {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_LOGS",
                "id": "2347453614",
                "task_id": "1044454185"
            }
        ],
        "context": {
            "job_instance_id": {
                "flow_launch_id": "2c1088f4dd585763d67511237d6983bfaa8e4ea75a593d5f9df9473b7c207a89",
                "job_id": "release-to-deploy",
                "number": 1
            },
            "target_revision": {
                "pull_request_id": "1944828",
                "hash": "108146400334e61309d3756a6a33c8123490adf2",
                "number": "8514667"
            },
            "title": "Autodeploy deploy_ci simple pipeline commit #30",
            "launch_number": 30,
            "version_info": {
                "major": "30",
                "full": "30"
            },
            "branch": "trunk",
            "version": "30",
            "ci_url": "https://a.yandex-team.ru/ci/workflow/details/2c1088f4dd585763d67511237d6983bfaa8e4ea75a593d5f9df9473b7c207a89",
            "config_info": {
                "path": "infra/deploy_ci/examples/simple_pipeline/a.yaml",
                "dir": "infra/deploy_ci/examples/simple_pipeline"
            },
            "flow_triggered_by": "torkve",
            "target_commit": {
                "pull_request_id": 1944828,
                "author": "aseppar",
                "date": "2021-08-12T09:42:48Z",
                "message": "fix routestats: fix ResultWrapper for optional",
                "issues": [
                    "EFFICIENCYDEV-14161"
                ],
                "revision": {
                    "pull_request_id": "1944828",
                    "hash": "108146400334e61309d3756a6a33c8123490adf2",
                    "number": "8514667"
                }
            },
            "secret_uid": "sec-01fctgsnj7cqzp1zj21sy5pwrr"
        }
    }
    ''' % {"stage_id": stage_id}


@pytest.fixture(scope='function')
def stage_id(yp_env):
    project_id = make_project(yp_env)
    return make_stage(yp_env, project_id)


@pytest.fixture(scope='function')
def sandbox_client():
    class DictProxy(dict):
        def __getitem__(self, item):
            if isinstance(item, slice):
                return self
            return super().__getitem__(item)

    def get_resource(res_id: str) -> dict:
        return DictProxy({
            2347450248: {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_OUTPUT",
                "id": "2347450248",
                "task": {"id": "1044454185"},
                'skynet_id': '',
                'description': '',
                'arch': '',
                'md5': '',
            },
            2347453282: {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_OUTPUT_HTML",
                "id": "2347453282",
                "task": {"id": "1044454185"},
                'skynet_id': '',
                'description': '',
                'arch': '',
                'md5': '',
            },
            2347449960: {
                "type": "TASK_LOGS",
                "id": "2347449960",
                "task": {"id": "1044454185"},
                'skynet_id': '',
                'description': '',
                'arch': '',
                'md5': '',
            },
            2347453466: {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_LOGS",
                "id": "2347453466",
                "task": {"id": "1044454185"},
                'skynet_id': '',
                'description': '',
                'arch': '',
                'md5': '',
            },
            2347450192: {
                "attributes": {
                    "backup_task": "true"
                },
                "type": "YA_MAKE_HTML_RESULTS",
                "id": "2347450192",
                "task": {"id": "1044454185"},
                'skynet_id': '',
                'description': '',
                'arch': '',
                'md5': '',
            },
            2347453757: {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_STATISTICS",
                "id": "2347453757",
                "task": {"id": "1044454185"},
                'skynet_id': '',
                'description': '',
                'arch': '',
                'md5': '',
            },
            2347449518: {
                "attributes": {
                    "build_platform": "Linux-4.19.183-42.2mofed-x86_64-with-Ubuntu-12.04-precise",
                    "arcadia_tag": "",
                    "ttl": "14",
                    "pack_tar": "0",
                    "arcadia_branch": "",
                    "build_tool": "ya",
                    "arcadia_revision": "108146400334e61309d3756a6a33c8123490adf2",
                    "arcadia_trunk": "False"
                },
                "type": "TEST_RESOURCE",
                "id": "2347449518",
                'skynet_id': 'rbtorrent:' + 'a' * 64,
                "task": {"id": "1044454185"},
                'file_name': 'file.tgz',
                'description': '',
                'arch': '',
                'md5': '',
            },
            2347450184: {
                "attributes": {
                    "backup_task": "true"
                },
                "type": "TEST_ENVIRONMENT_JSON_V2",
                "id": "2347450184",
                "task": {"id": "1044454185"},
                'skynet_id': '',
                'description': '',
                'arch': '',
                'md5': '',
            },
            2347453614: {
                "attributes": {
                    "ttl": "4"
                },
                "type": "BUILD_LOGS",
                "id": "2347453614",
                "task": {"id": "1044454185"},
                'skynet_id': '',
                'description': '',
                'arch': '',
                'md5': '',
            }
        }[res_id])

    client = mock.MagicMock()
    client.user.current.read.return_value = DictProxy({"login": "user"})
    client.task.__getitem__.return_value = DictProxy({
        'type': 'TASK',
        'description': 'description',
        'id': '1234567',
        'author': 'user',
        'time': {'created': '2021-08-07T15:20:04.725000Z'},
    })
    client.resource.__getitem__.side_effect = get_resource
    return client


@pytest.mark.parametrize('attempts', [1, 2])
@pytest.mark.parametrize('patch', ['static_resource', 'dynamic_resource'])
def test_simple_flow(yp_env, sandbox_client, stage_id, attempts, patch):
    flow_fn = {
        'static_resource': make_simple_static_resource_flow,
        'dynamic_resource': make_simple_dynamic_resource_flow,
    }[patch]
    input_json = flow_fn(stage_id)

    input_msg = create_release.CreateReleaseImpl.__holder_cls__.Input()
    json_format.Parse(input_json, input_msg)

    yp_address = 'xdc'

    for attempt in range(attempts):
        input_msg.context.job_instance_id.number += 1
        yp_stub = yp_env.create_grpc_object_stub()

        # step 0
        sandbox_resources, sandbox_task_info = create_release.collect_sandbox_infos(sandbox_client, input_msg.sandbox_resources)

        # step 1
        yp_timestamp, yp_tx_id = create_release.create_transaction(yp_stub)

        # step 2
        release_kind, release_id = create_release.create_release(
            yp_stub,
            sandbox_client,
            sandbox_resources,
            sandbox_task_info,
            input_msg,
            yp_tx_id,
            yp_timestamp,
            yp_address,
        )

        assert release_kind == 'sandbox'
        assert release_id

        # step 3
        (
            affected_deploy_units,
            affected_dynamic_resources,
            deploy_ticket_id,
            commit_needed
        ) = create_release.create_ticket(
            yp_stub,
            input_msg,
            release_id,
            yp_tx_id,
            yp_timestamp,
            yp_address,
        )

        if patch == 'static_resource':
            assert not affected_dynamic_resources
            assert affected_deploy_units == {'du1'}
        elif patch == 'dynamic_resource':
            assert not affected_deploy_units
            assert affected_dynamic_resources == {'dr1'}

        assert deploy_ticket_id
        assert commit_needed is True

        # step 4

        if commit_needed:
            create_release.commit_ticket(yp_stub, input_msg, deploy_ticket_id, yp_tx_id, yp_address)

        # step 5
        commit_timestamp = create_release.commit_transaction(yp_stub, yp_tx_id)

        # step 6
        deploy_units, dynamic_resources, need_approvals = create_release.get_locations(
            yp_stub,
            input_msg,
            affected_deploy_units,
            affected_dynamic_resources,
            commit_timestamp,
        )

        if patch == 'static_resource':
            assert not dynamic_resources
            assert deploy_units == {'du1': 2}
            assert len(need_approvals) == 2
            assert ('du1', 'man', 2) in need_approvals
            assert ('du1', 'sas', 2) in need_approvals
        elif patch == 'dynamic_resource':
            assert dynamic_resources == {'dr1': 2}
            assert not deploy_units
            assert not need_approvals

    # step 7
    deployed = create_release.wait_for_deploy(yp_stub, deploy_ticket_id, timeout=1)

    assert not deployed

    yp_env.update_object(object_identity=deploy_ticket_id, object_type='deploy_ticket', set_updates=[
        {
            'path': '/status',
            'value': {
                'progress': {
                    'closed': {
                        'status': 'true',
                        'reason': 'CLOSED',
                        'message': "Success",
                    }
                },
                'patches': {
                    'patch0': {
                        'progress': {
                            'success': {
                                'status': 'true',
                                'reason': 'SUCCESS',
                                'message': 'Success',
                            },
                        },
                    },
                },
            },
        },
    ])

    deployed = create_release.wait_for_deploy(yp_stub, deploy_ticket_id, timeout=1)

    assert deployed


def test_docker_deduplication(yp_env, sandbox_client, stage_id):
    input_json = make_docker_resource_flow(stage_id)

    input_msg = create_release.CreateReleaseImpl.__holder_cls__.Input()
    json_format.Parse(input_json, input_msg)

    yp_address = 'xdc'

    yp_stub = yp_env.create_grpc_object_stub()

    # step 0
    sandbox_resources, sandbox_task_info = create_release.collect_sandbox_infos(sandbox_client, input_msg.sandbox_resources)

    # step 1
    yp_timestamp, yp_tx_id = create_release.create_transaction(yp_stub)

    with mock.patch.object(create_release, '_resolve_docker_info') as _resolve_docker_info:
        def resolve(registry: str, image: str, tag: str) -> dict:
            assert registry == 'registry.yandex.net'
            assert image == 'deploy-ci/image'
            assert tag == 'tag1'
            return {
                'hash': 'b' * 32,
            }
        _resolve_docker_info.side_effect = resolve

        # step 2
        release_kind, release_id = create_release.create_release(
            yp_stub,
            sandbox_client,
            sandbox_resources,
            sandbox_task_info,
            input_msg,
            yp_tx_id,
            yp_timestamp,
            yp_address,
        )

    assert release_kind == 'docker'
    assert release_id

    # step 5
    create_release.commit_transaction(yp_stub, yp_tx_id)

    docker_spec = yp_env.get_object('release', release_id, selectors=['/spec'])[0]['docker']
    assert len(docker_spec['images']) == 1
    image = docker_spec['images'][0]
    assert image['name'] == 'deploy-ci/image'
    assert image['tag'] == 'tag1'
    assert image['registry_host'] == 'registry.yandex.net'
