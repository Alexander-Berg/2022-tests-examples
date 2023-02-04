import os
import itertools
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.dev_utils.ydb.lib.core.utils import get_ydb_driver
from datacloud.dev_utils.ydb.lib.core.ydb_manager import YdbManager, YdbConnectionParams
from datacloud.score_api.storage.ydb import ydb_tables
from datacloud.score_api.storage.ydb.dev_utils import datacloud.score_api_sample_data as sample_data
from datacloud.score_api.storage.ydb.dev_utils import create_ydb_env
from datacloud.score_api.storage.ydb import utils as ydb_utils
from datacloud.score_api.storage.ydb.test_connection_params import TEST_YDB_PATH_CONFIG_PRESET


logger = get_basic_logger(__name__)


def fill_sandbox_tables_with_data(ydb_manager, database, root_path):
    # ## Config table
    partner_scores_table_path = os.path.join(root_path, 'config', 'partner_scores')
    partner_scores_table = ydb_tables.config_tables.PartnerScoresTable(ydb_manager, database, partner_scores_table_path)
    partner_scores_table.insert(sample_data.get_partner_scores_data())
    partner_scores_table.describe()
    partner_scores_table.get_one(ydb_tables.config_tables.PartnerScoresTable.Record('partner_a', 'score_a'))
    # ## End config table

    # ## Tokens table
    tokens_table_path = os.path.join(root_path, 'config', 'partner_tokens')
    tokens_table = ydb_tables.config_tables.PartnerTokensTable(ydb_manager, database, tokens_table_path)
    tokens_table.insert(sample_data.get_partner_tokens_data())
    tokens_table.describe()
    print(tokens_table.get_one('xieciethiefexodieheikipiumaingaizahyabuquiokeezooreikieghangekoh'))
    # ## End Tokens table

    # ## Score path
    score_path_table_path = os.path.join(root_path, 'config', 'score_path')
    score_path_table = ydb_tables.config_tables.ScorePathTable(ydb_manager, database, score_path_table_path)
    score_path_table.insert(sample_data.get_score_path_data())
    score_path_table.describe()
    print(score_path_table.get_one(ydb_tables.config_tables.ScorePathTable.Record('partner_a-score-a')))
    # ## End Score path

    # ## Geo path
    geo_path_table = ydb_tables.config_tables.GeoPathTable(
        ydb_manager,
        database,
        TEST_YDB_PATH_CONFIG_PRESET.geo_path_table_path
    )
    geo_path_table.insert(sample_data.get_geo_path_data())
    geo_path_table.describe()
    print(geo_path_table.get_one(ydb_tables.config_tables.GeoPathTable.Record('test-internal-score-name')))
    # ## Geo path

    # ## Geo logs
    geo_logs_table = ydb_tables.geo_tables.geo_logs_table.GeoLogsTable(
        ydb_manager,
        database,
        os.path.join(TEST_YDB_PATH_CONFIG_PRESET.geo_root_path, 'test-geo-logs-table')
    )
    geo_logs_table.insert(sample_data.get_geo_logs_data())
    geo_logs_table.describe()
    print(geo_logs_table.get_one(ydb_tables.geo_tables.geo_logs_table.GeoLogsTable.Record(1)))
    # ## Geo logs

    # ## Geo crypta
    crypta_table = ydb_tables.score_tables.crypta_table.CryptaTable(
        ydb_manager,
        database,
        os.path.join(TEST_YDB_PATH_CONFIG_PRESET.geo_root_path, 'test-crypta-table')
    )
    crypta_table.insert(sample_data.get_geo_crypta_data())
    crypta_table.describe()
    print(crypta_table.get_one(ydb_tables.score_tables.crypta_table.CryptaTable.Record(1001)))
    # ## Geo crypta

    use_data_from_yt = False

    if use_data_from_yt:
        from datacloud.dev_utils.yt import yt_utils
        yt_client = yt_utils.get_yt_client()
        for partner_id, score_name in itertools.product(
            ('partner_a', 'partner_b', 'partner_c'),
            ('score_a', 'score_b', 'score_c')
        ):
            # TODO: Creates base score with crypta table. Remove later.
            print('Start upload sandbox for {} {}'.format(partner_id, score_name))

            score_table_path = os.path.join(root_path, 'scores/{}/{}/23-10-2018/score'.format(partner_id, score_name))
            score_table = ydb_tables.score_tables.ScoreTable(ydb_manager, database, score_table_path)
            score_table.insert(sample_data.sandbox_yt_score_generator(yt_client,
                               '//projects/scoring/tmp/re9ulusv/tables-to-transfer-ydb/{}-{}'.format(partner_id, score_name)))
            score_table.describe()

            crypta_table_path = os.path.join(root_path, 'scores/{}/{}/23-10-2018/crypta'.format(partner_id, score_name))
            crypta_table = ydb_tables.score_tables.CryptaTable(ydb_manager, database, crypta_table_path)
            crypta_table.insert(sample_data.sandbox_yt_crypta_generator(yt_client,
                                '//projects/scoring/tmp/re9ulusv/tables-to-transfer-ydb/new-crypta'))
            crypta_table.describe()
    else:
        for partner_id, score_name in itertools.product(
            ('partner_a', 'partner_b', 'partner_c'),
            ('score_a', 'score_b', 'score_c')
        ):
            print('Start upload sandbox for {} {}'.format(partner_id, score_name))
            score_table_path = os.path.join(root_path, 'scores/{}/{}/date-23-10-2018/score'.format(partner_id, score_name))
            score_table = ydb_tables.score_tables.MetaScoreTable(ydb_manager, database, score_table_path)
            score_table.insert(sample_data.get_meta_score_data())
            score_table.describe()

    stability_table_path = os.path.join(root_path, 'stability', 'stability')
    stability_table = ydb_tables.stability_tables.StabilityTable(ydb_manager, database, stability_table_path)
    stability_table.insert(sample_data.get_stability_data())
    stability_table.describe()
    # print(score_path_table.get_one(ydb_tables.stability_tables.StabilityTable.Record()))


def create_test_env(endpoint, database, path, auth_token, root_path='sandbox_score_api'):
    driver = get_ydb_driver(endpoint, database, auth_token)

    connection_params = YdbConnectionParams(
        endpoint=endpoint,
        database=database,
        auth_token=auth_token
    )
    ydb_manager = YdbManager(connection_params)

    ydb_utils.ensure_path_exists(driver, database, path)

    create_ydb_env.create_datacloud.score_api_config_tables(ydb_manager, database, os.path.join(root_path, 'config'))
    create_ydb_env.create_sandbox_score_tables(ydb_manager, database, root_path)
    create_ydb_env.create_stability_table(ydb_manager, database, os.path.join(root_path, 'stability'))
    create_ydb_env.create_geo_tables(ydb_manager, database, TEST_YDB_PATH_CONFIG_PRESET)

    create_ydb_env.create_rejected_requests_table(ydb_manager, database, os.path.join(root_path, 'rejected'))
    fill_sandbox_tables_with_data(ydb_manager, database, root_path)


if __name__ == '__main__':
    endpoint = 'ydb-ru-prestable.yandex.net:2135'
    database = '/ru-prestable/impulse/test/xprod-scoring-api-db'
    path = ''

    # TODO: Remove auth_token
    auth_token = os.environ['YDB_TOKEN']

    create_test_env(
        endpoint,
        database,
        path,
        auth_token,
        root_path='testing_score_api'
    )
