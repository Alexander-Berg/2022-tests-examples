import logging
from yt.wrapper import TablePath

from datacloud.dev_utils.testing.testing_utils import (
    data_by_name,
    Table2Spawn,
    TablesSpawnerCase
)

from datacloud.ml_utils.dolphin.prepare_cse.path_config import PathConfig
from datacloud.ml_utils.dolphin.prepare_cse.pipeline import (
    CleanConfig,
    step2_sample_eids
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
    '//aggs/clean_cse',
    schema=[
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'external_id', 'sort_order': 'ascending'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'retro_date'},
        {'type_v2': {'element': 'int64', 'metatype': 'optional'}, 'required': False,
            'type': 'int64', 'name': 'target'},
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


class TestStep2SampleEids(TablesSpawnerCase):
    @property
    def _tables2spawn(self):
        return [Table2Spawn(
            yt_table=yt_cse_table,
            data=data_by_name('step_2/clean_cse')
        )]

    def _logic(self, yt_client, yql_client):
        step2_sample_eids(
            yt_client=yt_client,
            yql_client=yql_client,
            path_config=fake_path_config,
            cconfig=fake_cconfig,
            logger=fake_logger
        )

        data = [
            r['external_id']
            for r in list(yt_client.read_table('//aggs/cse_interesting_eids'))
        ]
        assert data == [
            '11741156463866501525_2018-05-03_XPROD-19',
            '14188489920904505908_2017-10-11_XPROD-17',
            '14192738006286656154_2016-05-17_XPROD-7',
            '14900100718555024337_2019-05-09_XPROD-4',
            '15522744619327097161_2016-07-24_XPROD-7',
            '15647311326316736229_2017-06-27_XPROD-3',
            '1660220820304262619_2017-12-02_XPROD-18',
            '17616839134999388029_2017-10-29_XPROD-18',
            '17941672847493493712_2017-04-27_XPROD-18',
            '18273356085506082166_2018-12-02_XPROD-20',
            '3573392216939998717_2018-06-23_XPROD-7',
            '3787790330998346436_2017-06-23_XPROD-0',
            '439420473633460716_2018-09-24_XPROD-14',
            '515279645417147229_2018-10-15_XPROD-14',
            '515279645417147229_2018-10-28_XPROD-14',
            '515279645417147229_2018-11-28_XPROD-14',
            '6522960738998276076_2017-10-09_XPROD-19',
            '7224670462572788386_2016-12-26_XPROD-13',
            '8940619185341497720_2016-03-20_XPROD-0',
            '9670801311060164065_2016-03-18_XPROD-6'
        ]
