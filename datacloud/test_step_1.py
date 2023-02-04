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
    step1_clean_cse_table
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
    '//aggs/credit_scoring_events',
    schema=[
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'id_type', 'sort_order': 'ascending'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'id_value', 'sort_order': 'ascending'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'external_id'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'ticket'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'partner'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'retro_date'},
        {'type_v2': {'element': 'string', 'metatype': 'optional'}, 'required': False,
            'type': 'string', 'name': 'upper_bound_date'},
        {'type_v2': {'element': 'int64', 'metatype': 'optional'}, 'required': False,
            'type': 'int64', 'name': 'target'},
    ]
)


class Step1CleanCseCase(TablesSpawnerCase):
    @property
    def _spawn_data(self):
        raise NotImplementedError()

    @property
    def _expected_data(self):
        raise NotImplementedError()

    @property
    def _tables2spawn(self):
        return [Table2Spawn(
            yt_table=yt_cse_table,
            data=self._spawn_data
        )]

    def _logic(self, yt_client, yql_client):
        step1_clean_cse_table(
            yt_client=yt_client,
            yql_client=yql_client,
            path_config=fake_path_config,
            cconfig=fake_cconfig,
            logger=fake_logger
        )

        assert list(yt_client.read_table('//aggs/clean_cse')) == self._expected_data


class TestAllGood(Step1CleanCseCase):
    @property
    def _spawn_data(self):
        return data_by_name('step_1/all_good_cse')

    @property
    def _expected_data(self):
        return data_by_name('step_1/all_good_clean_cse')


class TestBadPartner(Step1CleanCseCase):
    @property
    def _spawn_data(self):
        return data_by_name('step_1/bad_partner')

    @property
    def _expected_data(self):
        return []


class TestBadTarget(Step1CleanCseCase):
    @property
    def _spawn_data(self):
        return data_by_name('step_1/bad_target')

    @property
    def _expected_data(self):
        return []


class TestBadRetroDate(Step1CleanCseCase):
    @property
    def _spawn_data(self):
        return data_by_name('step_1/bad_retro_date')

    @property
    def _expected_data(self):
        return []
