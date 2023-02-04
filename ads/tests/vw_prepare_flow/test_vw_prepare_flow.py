import yatest.common
from yabs_nirvana import mr_table
from library.python.nirvana_test import job_context_fixture
from ads.libs.py_test_mapreduce import local_yt_ctx, dir_to_yt, canonical_table
import pytest
import shutil
import contextlib

src_table = {'type': 'content', 'data': {"table": "//home/bs/train_set/train", "cluster": "hahn"}}


job_context = job_context_fixture(
    outputs=['feature_map'],
    inputs={
        'src_tables': src_table
    },
    parameters={
        'learn_namespaces': ['u', 'u,b'],
        'num_bits': 29,
        'size_limit_to_flush': 5
    }
)


@pytest.yield_fixture()
def train_set():
    with local_yt_ctx():
        dir_to_yt('train_set', 'train_set')
        yield


def test_flow(job_context, train_set):
    yatest.common.execute(
        [
            yatest.common.binary_path('ads/nirvana/wormhole/feature_map/feature_map')
        ],
        check_exit_code=True
    )

    filter_feature_map = job_context_fixture(
        outputs=['dst_table_0'],
        inputs={
            'src_tables': job_context['outputs']['feature_map'][0]
        },
        parameters={
            'mappers': ['Grep("\',\' in r.namespace")']
        }
    )

    with contextlib.contextmanager(filter_feature_map)() as job_context_filter:
        yatest.common.execute(
            [
                yatest.common.binary_path('ads/nirvana/mr_operations/mr_do_map/mr_do_map')
            ],
            check_exit_code=True
        )

        shutil.copy(job_context_filter['outputs']['dst_table_0'][0], 'input_feature_map')

    create_mapper = job_context_fixture(
        outputs=['dumped_mappers'],
        inputs={
            'feature_map': 'input_feature_map'
        },
        parameters={
            'target_field': 'vw_row',
            'num_bits': 29,
            'learn_namespaces': ['u', 'u,b'],
        }
    )

    with contextlib.contextmanager(create_mapper)() as job_context_create_enumerator:
        yatest.common.execute(
            [
                yatest.common.binary_path('ads/nirvana/mappers/factor_enumerator/factor_enumerator')
            ],
            check_exit_code=True
        )
        shutil.copy(job_context_create_enumerator['outputs']['dumped_mappers'][0], 'input_dumped_mappers')

    create_mapper = job_context_fixture(
        outputs=['dumped_mappers'],
        inputs={
            'dumped_mappers': 'input_dumped_mappers'
        },
        parameters={
            'target_field': 'target',
            'target_field_type': 'binary',
            'auto_hashing': False,
            'categorical_factors': ['vw_row'],
            'vw_row_field': 'formatted_vw_row'
        }
    )

    with contextlib.contextmanager(create_mapper)() as job_context_formatter:
        yatest.common.execute(
            [
                yatest.common.binary_path('ads/nirvana/mappers/vw_formatter/vw_formatter')
            ],
            check_exit_code=True
        )
        shutil.copy(job_context_formatter['outputs']['dumped_mappers'][0], 'input_dumped_mappers')

    manual_hashing = job_context_fixture(
        outputs=['dst_table_0'],
        inputs={
            'dumped_mappers': 'input_dumped_mappers',
            'src_tables': src_table
        },
        parameters={
        }
    )

    with contextlib.contextmanager(manual_hashing)() as job_context_manual_hashing:
        yatest.common.execute(
            [
                yatest.common.binary_path('ads/nirvana/mr_operations/mr_do_map/mr_do_map'),
            ],
            check_exit_code=True
        )

        return canonical_table(mr_table(job_context_manual_hashing['outputs']['dst_table_0'][0])['table'])
