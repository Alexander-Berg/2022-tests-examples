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
    step3_mark_eids,
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


class TestStep3MarkEids(TablesSpawnerCase):
    @property
    def _tables2spawn(self):
        return [Table2Spawn(
            yt_table=yt_cse_table,
            data=data_by_name('step_3/cse_interesting_eids')
        )]

    def _logic(self, yt_client, yql_client):
        fake_path_config.id_value_to_cid = '//crypta/id_value_to_cid'

        step3_mark_eids(
            yt_client=yt_client,
            yql_client=yql_client,
            path_config=fake_path_config,
            cconfig=fake_cconfig,
            logger=fake_logger
        )

        data = list(yt_client.read_table('//aggs/eid2mark'))
        assert data == [
            {'external_id': '11741156463866501525_2018-05-03_XPROD-19', 'mark': '2'},
            {'external_id': '15647311326316736229_2017-06-27_XPROD-3', 'mark': '1'},
            {'external_id': '1660220820304262619_2017-12-02_XPROD-18', 'mark': '2'},
            {'external_id': '17616839134999388029_2017-10-29_XPROD-18', 'mark': '1'},
            {'external_id': '439420473633460716_2018-09-24_XPROD-14', 'mark': 'val'},
            {'external_id': '515279645417147229_2018-09-07_XPROD-14', 'mark': '1'},
            {'external_id': '515279645417147229_2018-09-21_XPROD-14', 'mark': '1'},
            {'external_id': '515279645417147229_2018-10-28_XPROD-14', 'mark': '1'},
            {'external_id': '515279645417147229_2018-11-28_XPROD-14', 'mark': '1'},
            {'external_id': '6783494674308555868_2017-10-08_XPROD-19', 'mark': '2'},
        ]
