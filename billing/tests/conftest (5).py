import copy
from unittest import mock
import multiprocessing.dummy

import pytest


@pytest.fixture
def multiprocessing_pool_mock():
    with mock.patch('multiprocessing.Pool', new=multiprocessing.dummy.Pool) as m:
        yield m


@pytest.fixture
def tracker_mock():
    with mock.patch('startrek_client.Startrek') as m:
        yield m


@pytest.fixture
def job_context_json():
    return copy.deepcopy(
        {
            'meta': {
                'workflowUid': 'beac47b3-6483-4a55-bae6-b451be0047d1',
                'workflowInstanceUid': 'd8efde68-6085-4d49-a36b-a64f3d7a9189',
                'workflowURL': 'https://nirvana.yandex-team.ru/process/d8efde68-6085-4d49-a36b-a64f3d7a9189',
                'operationUid': 'ab182089-664d-4cb6-8a55-a7afb62209f7',
                'blockUid': '747ac312-b8c0-497a-bf3a-fd9de952c0fe',
                'blockCode': 'operation-1510745292728-4',
                'blockURL': 'https://nirvana.yandex-team.ru/process/d8efde68-6085-4d49-a36b-a64f3d7a9189/graph/operation/747ac312-b8c0-497a-bf3a-fd9de952c0fe',
                'processUid': 'd8efde68-6085-4d49-a36b-a64f3d7a9189',
                'processURL': 'https://nirvana.yandex-team.ru/process/d8efde68-6085-4d49-a36b-a64f3d7a9189',
                'priority': {'min': 0, 'max': 1000, 'value': 500, 'normValue': 0.5},
                'description': 'test job_context',
                'owner': 'cherolex',
                'quotaProject': 'default',
            },
            'parameters': {
                'cpu-guarantee': 1,
                'debug-timeout': 0,
                'gpu-count': 0,
                'gpu-max-ram': 1024,
                'gpu-type': 'NONE',
                'job-command': "bash -c 'cp job_context.json ${output[\"job_context.json\"]}'",
                'job-environments': ['jq@1.5'],
                'job-is-vanilla': True,
                'job-use-dns64': False,
                'max-disk': 1024,
                'max-ram': 100,
                'retries-on-job-failure': 0,
                'retries-on-system-failure': 10,
                'ttl': 360,
            },
            'inputs': {},
            'outputs': {
                'job_context.json': ['/yt/disk2/hahn-data/slots/51/sandbox/d/out/job_context.json/job_context.json']
            },
            'ports': {'udp': {}, 'tcp': {'job_launcher': 1325}},
            'inputItems': {},
            'outputItems': {
                'job_context.json': [
                    {
                        'dataType': 'json',
                        'wasUnpacked': False,
                        'unpackedDir': '/yt/disk2/hahn-data/slots/51/sandbox/d/out/job_context.json',
                        'unpackedFile': '/yt/disk2/hahn-data/slots/51/sandbox/d/out/job_context.json/job_context.json',
                        'downloadURL': 'https://nirvana.yandex-team.ru/api/storage/fb6bb8eb-3b1b-4b92-9e3b-be1f72249716/data',
                    }
                ]
            },
            'secrets': {
                'yt-token': {
                    'name': 'robot-job-processor-yt-token',
                    'as_file': '/slot/sandbox/d/secret/robot-job-processor-yt-token',
                    'as_text': '***secret_robot-job-processor-yt-token***',
                }
            },
            'status': {
                'errorMsg': '/yt/disk2/hahn-data/slots/51/sandbox/nv_tmpfs/sys/job_launcher.error_msg.txt',
                'successMsg': '/yt/disk2/hahn-data/slots/51/sandbox/nv_tmpfs/sys/job_launcher.success_msg.txt',
                'log': '/yt/disk2/hahn-data/slots/51/sandbox/nv_tmpfs/sys/job_launcher.user_status.log',
            },
        }
    )
