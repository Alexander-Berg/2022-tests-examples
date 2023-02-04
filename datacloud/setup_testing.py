import os
import itertools

from datacloud.score_api.storage.ydb import ydb_tables
from datacloud.score_api.storage.for_tests import sample_data_for_test as sample_data
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.dev_utils.ydb.lib.core import utils as ydb_utils
from datacloud.dev_utils.ydb.lib.core.utils import YdbPathConfig

from datacloud.score_api.storage.ydb.ydb_tables.config_tables.partner_scores_table import PartnerScoresTable
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.partner_tokens_table import PartnerTokensTable
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.score_path_table import ScorePathTable
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.geo_path_table import GeoPathTable
from datacloud.score_api.storage.ydb.ydb_tables.geo_tables.geo_logs_table import GeoLogsTable
from datacloud.score_api.storage.ydb.ydb_tables.score_tables.score_table import MetaScoreTable
from datacloud.score_api.storage.ydb.ydb_tables.stability_tables.stability_table import StabilityTable
from datacloud.score_api.storage.ydb.ydb_tables.rejected_tables.rejected_requests_table import RejectedRequestsTable

logger = get_basic_logger(__name__)


def create_tables(ydb_manager, tables_to_create, database_path):
    """
    Args:
        tables_to_create([YdbTable, table_path]): where YdbTable is child class of YdbTable and table_path is str path of table.
        session
    """
    for TableClass, table_path in tables_to_create:
        table = TableClass(ydb_manager, database_path, table_path)
        table.create()


def create_score_tables(ydb_manager, database_path, score_folder):
    ydb_utils.create_folder(ydb_manager.driver.driver, database_path, score_folder)
    tables_to_create = [(MetaScoreTable, os.path.join(score_folder, 'score'))]
    create_tables(ydb_manager, tables_to_create, database_path)


def create_config_tables(ydb_manager, database_path, config_folder):
    ydb_utils.create_folder(ydb_manager.driver.driver, database_path, config_folder)
    tables_to_create = [
        (PartnerScoresTable, os.path.join(config_folder, 'partner_scores')),
        (PartnerTokensTable, os.path.join(config_folder, 'partner_tokens')),
        (ScorePathTable, os.path.join(config_folder, 'score_path'))
    ]
    create_tables(ydb_manager, tables_to_create, database_path)


def create_stability_table(ydb_manager, database_path, stability_folder):
    ydb_utils.create_folder(ydb_manager.driver.driver, database_path, stability_folder)
    tables_to_create = [(StabilityTable, os.path.join(stability_folder, 'stability'))]
    create_tables(ydb_manager, tables_to_create, database_path)


def create_geo_tables(ydb_manager, database_path, ydb_path_config):
    ydb_utils.create_folder(ydb_manager.driver.driver, database_path, ydb_path_config.config_dir)
    ydb_utils.create_folder(ydb_manager.driver.driver, database_path, ydb_path_config.geo_root_path)
    tables_to_create = [
        (GeoPathTable, ydb_path_config.geo_path_table_path),
        (GeoLogsTable, os.path.join(ydb_path_config.geo_root_path, 'test-geo-logs-table')),
        (
            ydb_tables.score_tables.crypta_table.CryptaTable,
            os.path.join(ydb_path_config.geo_root_path, 'test-crypta-table')),
    ]
    create_tables(ydb_manager, tables_to_create, database_path)


def create_sandbox_score_tables(ydb_manager, database, root_path):
    score_tables_list = [
        ('partner_a', 'score_a'),
        ('partner_a', 'score_b'),
        ('partner_a', 'score_c'),
        ('partner_b', 'score_a'),
        ('partner_b', 'score_b'),
        ('partner_b', 'score_c'),
        ('partner_c', 'score_a'),
        ('partner_c', 'score_b'),
        ('partner_c', 'score_c'),
    ]
    date = 'date-23-10-2018'
    for partner, score in score_tables_list:
        create_score_tables(ydb_manager, database, os.path.join(root_path, 'scores', partner, score, date))


def create_rejected_requests_table(ydb_manager, database, rejected_folder):
    ydb_utils.create_folder(ydb_manager.driver.driver, database, rejected_folder)
    tables_to_create = [(RejectedRequestsTable, os.path.join(rejected_folder, 'rejected_requests'))]
    create_tables(ydb_manager, tables_to_create, database)


def fill_sandbox_tables_with_data(ydb_manager, database, root_path):
    # ## Config table
    partner_scores_table_path = os.path.join(root_path, 'config', 'partner_scores')
    partner_scores_table = PartnerScoresTable(ydb_manager, database, partner_scores_table_path)
    partner_scores_table.insert(sample_data.get_partner_scores_data())
    # ## End config table

    # ## Tokens table
    tokens_table_path = os.path.join(root_path, 'config', 'partner_tokens')
    tokens_table = PartnerTokensTable(ydb_manager, database, tokens_table_path)
    tokens_table.insert(sample_data.get_partner_tokens_data())
    # # ## End Tokens table

    # ## Score path
    score_path_table_path = os.path.join(root_path, 'config', 'score_path')
    score_path_table = ScorePathTable(ydb_manager, database, score_path_table_path)
    score_path_table.insert(sample_data.get_score_path_data())
    # ## End Score path

    geo_path_config = YdbPathConfig('testing')
    # ## Geo path
    geo_path_table = GeoPathTable(
        ydb_manager,
        database,
        geo_path_config.geo_path_table_path
    )
    geo_path_table.insert(sample_data.get_geo_path_data())
    # ## End Geo path

    # ## Geo logs
    geo_logs_table = GeoLogsTable(
        ydb_manager,
        database,
        os.path.join(geo_path_config.geo_root_path, 'test-geo-logs-table')
    )
    geo_logs_table.insert(sample_data.get_geo_logs_data())
    # ## End Geo logs

    # ## Geo crypta
    crypta_table = ydb_tables.score_tables.crypta_table.CryptaTable(
        ydb_manager,
        database,
        os.path.join(geo_path_config.geo_root_path, 'test-crypta-table')
    )
    crypta_table.insert(sample_data.get_geo_crypta_data())
    # ## End Geo crypta

    for partner_id, score_name in itertools.product(
        ('partner_a', 'partner_b', 'partner_c'),
        ('score_a', 'score_b', 'score_c')
    ):
        score_table_path = os.path.join(root_path, 'scores/{}/{}/date-23-10-2018/score'.format(partner_id, score_name))
        score_table = MetaScoreTable(ydb_manager, database, score_table_path)
        score_table.insert(sample_data.get_meta_score_data())

    stability_table_path = os.path.join(root_path, 'stability', 'stability')
    stability_table = StabilityTable(ydb_manager, database, stability_table_path)
    stability_table.insert(sample_data.get_stability_data())


def create_test_env(ydb_manager, database, path, root_path='sandbox_score_api'):
    ydb_utils.create_folder(ydb_manager.driver.driver, database, path)

    create_config_tables(ydb_manager, database, os.path.join(root_path, 'config'))
    create_sandbox_score_tables(ydb_manager, database, root_path)
    create_stability_table(ydb_manager, database, os.path.join(root_path, 'stability'))
    create_geo_tables(ydb_manager, database, YdbPathConfig('testing'))

    create_rejected_requests_table(ydb_manager, database, os.path.join(root_path, 'rejected'))
    fill_sandbox_tables_with_data(ydb_manager, database, root_path)


def main(ydb_manager, database, path=''):
    create_test_env(
        ydb_manager,
        database,
        path,
        root_path='testing'
    )
