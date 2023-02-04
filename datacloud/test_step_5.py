import logging
import csv
import os
from yt.wrapper import TablePath

from datacloud.dev_utils.testing.testing_utils import (
    data_by_name,
    Table2Spawn,
    TablesSpawnerCase,
)

from datacloud.ml_utils.dolphin.prepare_cse.path_config import PathConfig
from datacloud.ml_utils.dolphin.prepare_cse.pipeline import (
    CleanConfig,
    step5_compact_cse
)

fake_cconfig = CleanConfig(
    experiment_name='experimnet',
    path_to_original_cse='//credit_scoring_events',
    aggs_folder='//aggs',
    crypta_folder='//crypta',
    zeros_vs_ones=1.,
    min_retro_date='2000-01-01',
    no_go_partners=['insurance'],
    n_folds=2,
    val_size=1,
    steps=[]
)

fake_path_config = PathConfig(fake_cconfig)

fake_logger = logging.getLogger(__name__)

schema = [
    {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
        'type': 'string', 'name': 'external_id', 'sort_order': 'ascending'},
    {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
        'type': 'string', 'name': 'retro_date', 'sort_order': 'ascending'},
    {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
        'type': 'string', 'name': 'partner', 'sort_order': 'ascending'},
    {'type_v2': {'element': 'int64', 'metatype': 'optional'}, 'required': False,
        'type': 'int64', 'name': 'target', 'sort_order': 'ascending'},
    {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
        'type': 'string', 'name': 'id_value'},
    {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
        'type': 'string', 'name': 'ticket'},
    {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
        'type': 'string', 'name': 'id_type'},
    {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
        'type': 'string', 'name': 'upper_bound_date'},
]


class TestStep5CompactCse(TablesSpawnerCase):
    @property
    def _tables2spawn(self):
        return [
            Table2Spawn(
                yt_table=TablePath('//aggs/cse_1', schema=schema),
                data=data_by_name('step_5/cse_1')
            ),
            Table2Spawn(
                yt_table=TablePath('//aggs/cse_2', schema=schema),
                data=data_by_name('step_5/cse_2')
            ),
            Table2Spawn(
                yt_table=TablePath('//aggs/cse_val', schema=schema),
                data=data_by_name('step_5/cse_val')
            ),
        ]

    def _logic(self, yt_client, yql_client):
        os.mkdir('experimnet')
        step5_compact_cse(
            yt_client=yt_client,
            yql_client=yql_client,
            path_config=fake_path_config,
            cconfig=fake_cconfig,
            logger=fake_logger
        )

        suffix2data = {
            '1': [{
                'external_id': '11741156463866501525_2018-05-03_XPROD-19',
                'phone_id_value': '',
                'email_id_value': 'ac85627e72606a6107e9f4b9f6a345d3,3bb99207272ccd5334af38711bd88ce5',
                'retro_date': '2018-05-03',
                'partner': '0ea9d6364136c04a46a3dd22937efe24',
                'target': '0'
            }],
            '2': [{
                'external_id': '17616839134999388029_2017-10-29_XPROD-18',
                'phone_id_value': '4d2d1f28088e51d2734950aa978d3f7e',
                'email_id_value': '9a7092d7c5777c2b8ed4e4bb72051767',
                'retro_date': '2017-10-29',
                'partner': '3e91a7719b5f567c79ca1f66150aa0a9',
                'target': '0'
            }],
            'val': [{
                'external_id': '515279645417147229_2018-09-07_XPROD-14',
                'phone_id_value': '4d2d1f28088e51d2734950aa978d3f7e',
                'email_id_value': '',
                'retro_date': '2018-09-07',
                'partner': '81ed00999aab987b9270b674f0683192',
                'target': '0'
            }]
        }
        for suffix in ('1', '2', 'val'):
            data = list(csv.DictReader(
                yt_client.read_file('//aggs/normalized_{}.tsv'.format(suffix)),
                delimiter='\t'
            ))
            assert data == suffix2data[suffix]
