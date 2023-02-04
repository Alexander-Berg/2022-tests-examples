import logging
from yt.wrapper import TablePath

from datacloud.dev_utils.testing.testing_utils import (
    data_by_name,
    Table2Spawn,
    TablesSpawnerCase,
)

from datacloud.ml_utils.dolphin.prepare_cse.path_config import PathConfig
from datacloud.ml_utils.dolphin.prepare_cse.pipeline import (
    CleanConfig,
    step4_split_cse
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

yt_cse_table = TablePath(
    '//aggs/cse_interesting_eids',
    schema=[
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'external_id', 'sort_order': 'ascending'},
        {'type_v2': {'element': 'int64', 'metatype': 'optional'}, 'required': False,
            'type': 'int64', 'name': 'target'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'retro_date'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'id_value'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'ticket'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'id_type'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'upper_bound_date'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'partner'},
    ]
)

yt_eid2mark_table = TablePath(
    '//aggs/eid2mark',
    schema=[
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'external_id', 'sort_order': 'ascending'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'mark'},
    ]
)


class TestStep4SplitCse(TablesSpawnerCase):
    @property
    def _tables2spawn(self):
        return [
            Table2Spawn(
                yt_table=yt_cse_table,
                data=data_by_name('step_4/cse_interesting_eids')
            ),
            Table2Spawn(
                yt_table=yt_eid2mark_table,
                data=data_by_name('step_4/eid2mark')
            ),
        ]

    def _logic(self, yt_client, yql_client):
        step4_split_cse(
            yt_client=yt_client,
            yql_client=yql_client,
            path_config=fake_path_config,
            cconfig=fake_cconfig,
            logger=fake_logger
        )

        suffix2eids = {
            '1': [
                '15647311326316736229_2017-06-27_XPROD-3',
                '17616839134999388029_2017-10-29_XPROD-18',
                '515279645417147229_2018-09-07_XPROD-14',
                '515279645417147229_2018-09-21_XPROD-14',
                '515279645417147229_2018-10-28_XPROD-14',
                '515279645417147229_2018-11-28_XPROD-14',
            ],
            '2': [
                '11741156463866501525_2018-05-03_XPROD-19',
                '1660220820304262619_2017-12-02_XPROD-18',
                '6783494674308555868_2017-10-08_XPROD-19',
            ],
            'val': ['439420473633460716_2018-09-24_XPROD-14']
        }
        for suffix in ('1', '2', 'val'):
            eids = [
                r['external_id']
                for r in list(yt_client.read_table('//aggs/cse_{}'.format(suffix)))
            ]
            assert eids == suffix2eids[suffix]
