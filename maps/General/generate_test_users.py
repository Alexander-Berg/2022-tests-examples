import logging

from maps.poi.personalized_poi.builder.lib.constants.tables import (
    YT_DEFAULT_COMMON_FOLDER,
    TEST_USERS_TABLE_PREFIX,
)
from maps.poi.personalized_poi.builder.lib.yt_utils import (
    yt_add_slashes,
)
from maps.poi.personalized_poi.builder.extractor.folders import MainFolder


logger = logging.getLogger(__name__)


def add_options(parser):
    parser.add_argument('--common-folder', default=YT_DEFAULT_COMMON_FOLDER, type=yt_add_slashes)
    parser.add_argument('--size', default=100, type=int)
    parser.add_argument('--top-pois-size', default=4000, type=int)


def prepare_khamovniki_oids(yql_client, top_pois_size, common_folder):
    dest = MainFolder(common_folder).test_users_folder.table('top_khamovniki')
    source = MainFolder(common_folder).group_folder.table('features_schematized')
    query = '''
        INSERT INTO `{dest}` WITH TRUNCATE
        SELECT oid, name, lat, lon, orgvisits FROM (
            SELECT
                oid,
                SOME(name) AS name,
                SOME(lat) AS lat,
                SOME(lon) AS lon,
                SUM(orgvisits__180_days) AS orgvisits
            FROM `{source}`
            -- Khamovniki
            WHERE Geo::IsRegionInRegion(CAST(region_id AS Int32), 120542)
            GROUP BY oid
        ) ORDER BY orgvisits DESC
        LIMIT {top_pois_size}
    '''.format(dest=dest, source=source, top_pois_size=top_pois_size)
    request = yql_client.query(query, syntax_version=1).run()
    return request.get_results()


def sample_oids(yql_client, seed, limit, dataset_size, common_folder):
    source = MainFolder(common_folder).test_users_folder.table('top_khamovniki')
    percent = min(100, (limit * 100.0) / dataset_size + 1)
    query = '''
        SELECT oid
        FROM `{source}`
        TABLESAMPLE BERNOULLI({percent}) REPEATABLE({seed})
        LIMIT {limit}
    '''.format(source=source, percent=percent, seed=seed, limit=limit)
    request = yql_client.query(query, syntax_version=1).run()
    return request.dataframe


def build_poi_record(oid):
    result = {
        'id': str(oid),
        'zooms': [13, 14, 15, 16, 17, 18, 19],
        'min_zoom': 13
    }
    return result


def build_user_visit_record(puid, oids):
    value = {'pois': [build_poi_record(oid) for oid in oids]}
    return {'puid': puid, 'value': value}


def upload_visits_table(yt_client, yql_client, user_visits, args):
    table_data = [build_user_visit_record(puid, oids) for puid, oids in user_visits]
    folder = MainFolder(args.common_folder).test_users_folder
    output_table = folder.table('{}ecstatic_{}'.format(TEST_USERS_TABLE_PREFIX, args.today))
    schema = [
        {'name': 'puid', 'type': 'int64'},
        {'name': 'value', 'type': 'any'},
    ]
    if not yt_client.exists(output_table):
        yt_client.create('table', output_table, attributes={'schema': schema})
    yt_client.write_table(output_table, table_data)


def generate_test_users(yt_client, yql_client, args):
    logger.info('generate_test_users started')
    logger.info('prepare_khamovniki_oids')
    prepare_khamovniki_oids(yql_client, args.top_pois_size, args.common_folder)

    # These users will come from core-masquerade.maps.yandex.net
    # geoinfra-masquerade-1, ..., geoinfra-masquerade-5
    infra_puids = [1239531580, 1239534037, 1239535716, 1239904670, 1239905998]
    # ppoitestaccount1, ..., ppoitestaccount10
    ppoi_puids = [749462302, 749547173, 749547774, 749548314, 749548919,
                  749549640, 749550324, 749550854, 749551362, 749551832]
    puids = infra_puids + ppoi_puids
    all_oids = set()
    user_visits = []
    for puid in puids:
        oids = sample_oids(yql_client, puid, args.size, args.top_pois_size, args.common_folder)['oid'].values
        user_visits.append((puid, oids))
        all_oids.update(oids)
    # note: 10-th user is fat. it will contain merged data from all users
    user_visits[-1] = (puids[-1], list(all_oids))

    logger.info('upload_visits_table')
    upload_visits_table(yt_client, yql_client, user_visits, args)

    logger.info('generate_test_users finished')
