from __future__ import print_function
from collections import namedtuple, defaultdict
import logging
import os
import psycopg2
import subprocess as sp
import sys
import yaml

from StringIO import StringIO
from tempfile import NamedTemporaryFile
from traceback import format_exc

import yatest.common

from yandex.maps.pgpool3 import PgPool, PoolConstants
from maps.wikimap.mapspro.libs.python import revision

TRUNK_BRANCH_ID = 0

JSON_EXT = ".json"
YAML_EXT = ".yaml"

JSON_DIR = "json"

DEFAULT_RESTRICTIONS_CONFIG_PATH = "restrictions.xml"

VALIDATOR_MODULE_NAMES = [
    'adm-unit-checks',
    'hydro-checks',
    'vegetation-checks',
    'relief-checks',
    'urban-checks',
    'urban-roadnet-checks'
]

YMAPSDF_DB_SCHEMA_PREFIX = 'ymapsdf'

REVISION_SCHEMA_PATH = 'maps/wikimap/mapspro/libs/revision/sql/postgres_upgrade.sql'
REVISION_DB_SCHEMA = 'revision'

MAX_YMAPSDF_ID_BINARY = 'max-ymapsdf-id'
TOPOLOGY_FIXER_BINARY = 'topology-fixer'
TOPOLOGY_CHECKER_BINARY = 'topology-checker'

PASS = 'PASS'
FAIL = 'FAIL'
EXCEPTION = 'EXCEPTION'


_conns = {}


def get_conn(params):
    conn_string = params.connection_string
    return _conns.setdefault(conn_string, psycopg2.connect(conn_string))


def data_path(relpath):
    return yatest.common.source_path('maps/wikimap/mapspro/tools/topology_fixer/tests/' + relpath)


def binary_path(relpath):
    return yatest.common.binary_path('maps/wikimap/mapspro/tools/topology_fixer/bin/' + relpath + "/" + relpath)


def create_pgpool(conn_params):
    poolConstants = PoolConstants(1, 1, 2, 2)
    poolConstants.wait_for_availability_info = True
    conn_instance = ('localhost', conn_params.port)
    logger = logging.getLogger('pgpool3')
    logger.setLevel(logging.ERROR)

    return PgPool(
        conn_instance,
        [conn_instance],
        conn_params.connection_string,
        poolConstants,
        logger)


def pgpool_cfg(conn_params):
    return """<?xml version="1.0" encoding="utf-8"?>
<config>
    <common>
        <databases>
            <database id="long-read" name="{0.dbname}">
                <write host="localhost" port="{0.port}" user="{0.user}" pass="{0.password}"/>
                <read host="localhost" port="{0.port}" user="{0.user}" pass="{0.password}"/>
                <pools nearestDC="1" failPingPeriod="5" pingPeriod="30" timeout="5">
                    <revisionapi writePoolSize="2" writePoolOverflow="0" readPoolSize="2" readPoolOverflow="0"/>
                </pools>
            </database>
        </databases>
    </common>
</config>
""".format(conn_params)


def reset_revisions(conn_params):
    conn = get_conn(conn_params)
    cursor = conn.cursor()
    cursor.execute('DROP SCHEMA IF EXISTS %s CASCADE' % REVISION_DB_SCHEMA)
    sql = yatest.common.source_path(REVISION_SCHEMA_PATH)
    with open(sql) as sql_file:
        cursor.execute(sql_file.read())
    conn.commit()

    logging.info('YmapsDF schema %s reset', REVISION_DB_SCHEMA)


def drop_schema(conn_params, schema):
    conn = get_conn(conn_params)
    conn.cursor().execute("DROP SCHEMA IF EXISTS {} CASCADE".format(schema))
    conn.commit()


def patch_schema(conn_params, schema):
    """ymapsdf2json requires ymapsdf_tds table for id storage.
    But this table is not created by json2ymapsdf.
    So we have to create it manually."""

    conn = get_conn(conn_params)
    cursor = conn.cursor()
    cursor.execute('SET SEARCH_PATH=%s,public' % schema)
    cursor.execute(
        """CREATE TYPE ymapsdf_table AS ENUM (
            'ad','ad_nm',
            'addr','addr_nm',
            'bld',
            'model3d',
            'rd','rd_nm','rd_el','rd_jc',
            'cond', 'cond_dt',
            'ft','ft_nm',
            'face','edge','node',
            'meta');

        CREATE TABLE ymapsdf_tds (
            table_name ymapsdf_table NOT NULL,
            ymapsdf_id bigint NOT NULL,
            tds_id bigint NOT NULL
        );""")
    conn.commit()


def popen(cmd, *args, **kwargs):
    process = sp.Popen(cmd, *args, **kwargs)

    def waiter():
        process.wait()
        if process.returncode:
            raise sp.CalledProcessError(process.returncode, cmd)
    return process, waiter


def ymapsdf_to_revision(conn_params, ymapsdf_schema):
    reset_revisions(conn_params)
    with open(ymapsdf_schema + '.import.log', 'w') as err_stream:
        with NamedTemporaryFile() as rev_cfg:
            rev_cfg.write(pgpool_cfg(conn_params))
            rev_cfg.flush()

            ymapsdf2json_tool = yatest.common.binary_path('maps/wikimap/mapspro/tools/ymapsdf-conversion/ymapsdf2json/bin/ymapsdf2json')
            gen_json, wait_gen_json = popen(
                [ymapsdf2json_tool,
                 '--conn', conn_params.connection_string,
                 '--schema', ymapsdf_schema,
                 '--id-mode',
                 'original'],
                stdout=sp.PIPE,
                stderr=err_stream)

            revisionapi_tool = yatest.common.binary_path('maps/wikimap/mapspro/tools/revisionapi/revisionapi')
            _, wait_import_rev = popen(
                [revisionapi_tool,
                 '--cmd=import',
                 '--branch=trunk',
                 '--user-id=1',
                 '--start-from-json-id',
                 '--cfg=%s' % rev_cfg.name],
                stdin=gen_json.stdout,
                stdout=err_stream,
                stderr=err_stream)
            wait_gen_json()
            wait_import_rev()

    logging.info('Imported YmapsDF schema into revision schema %s', ymapsdf_schema)


def json_to_ymapsdf(fname, conn_params, schema):
    with open(fname) as json_file:
        json2ymapsdf_tool = yatest.common.binary_path('maps/wikimap/mapspro/tools/ymapsdf-conversion/json2ymapsdf/bin/json2ymapsdf')
        yatest.common.execute(
            [json2ymapsdf_tool,
             '--conn', conn_params.connection_string,
             '--threads', '1',
             '--transform-cfg', 'maps/wikimap/mapspro/tools/ymapsdf-conversion/json2ymapsdf/cfg/packages/test/export_ymapsdf/json2ymapsdf.xml',
             '--schema', schema],
            cwd=yatest.common.build_path(),
            stdin=json_file)

    logging.info('Imported json into YmapsDF schema %s', schema)


def max_ymapsdf_id(conn_params, ymapsdf_schema):
        max_id_sql_path = yatest.common.binary_path('maps/doc/schemas/ymapsdf/package/max_id.sql')
        result = yatest.common.execute(
            [binary_path(MAX_YMAPSDF_ID_BINARY),
             "--conn", conn_params.connection_string,
             "--schema", ymapsdf_schema,
             "--max-id-sql-path", max_id_sql_path])
        return int(result.std_out)


def call_fixer(conn_params, input_schema, output_schema, group, srid, restrictions_config, threads):
    with open(input_schema + '.fixer.log', 'w') as log_stream:
        _, fixer_wait = popen(
            [binary_path(TOPOLOGY_FIXER_BINARY),
             "--conn", conn_params.connection_string,
             "--restrictions-config", restrictions_config,
             "--input-schema", input_schema,
             "--output-schema", output_schema,
             "--group", group,
             "--start-ymapsdf-id", str(max_ymapsdf_id(conn_params, output_schema)),
             "--srid", srid,
             "--threads", str(threads)],
            stdout=log_stream, stderr=log_stream)
        fixer_wait()


def call_checker(conn_params, orig_schema, fixed_schema, group, srid):
    with open(orig_schema + '.checker.log', 'w') as log_stream:
        _, checker_wait = popen(
            [binary_path(TOPOLOGY_CHECKER_BINARY),
             "--conn", conn_params.connection_string,
             "--original-schema", orig_schema,
             "--fixed-schema", fixed_schema,
             "--group", group,
             "--srid", srid],
            stdout=log_stream, stderr=log_stream)
        try:
            checker_wait()
        except sp.CalledProcessError, e:
            return (FAIL, 'Diff checker exited with ' + str(e.returncode) + ' code')
    return (PASS, '')


ExpectedMessage = namedtuple('ExpectedMessage', 'description oids')
ExpectedAggMessage = namedtuple('ExpectedAggMessage', 'description count')

AggMessageDiff = namedtuple('AggMessageDiff', 'description got_count expected_count got_oids')


def compare(got, expected):
    class Diff:
        def __init__(self):
            self.got_count = 0
            self.expected_count = 0
            self.got_oids = []

    diffs = defaultdict(lambda: Diff())
    for message in got:
        d = diffs[message.description]
        d.got_count += 1
        d.got_oids.append(list(message.oids))
    for message in expected:
        d = diffs[message.description]
        d.expected_count += message.count

    return [AggMessageDiff(description, diff.got_count, diff.expected_count, diff.got_oids)
            for description, diff in diffs.iteritems() if diff.got_count != diff.expected_count]


TestData = namedtuple('TestData', 'name group srid restrictions_config expected')


def load_test_config(test_name):
    file = open(os.path.join(data_path('testcases'), test_name + YAML_EXT))
    data = yaml.load(file)
    if 'expected' in data:
        expected = [
            ExpectedAggMessage(m['description'], int(m['count']))
            for m in data['expected']]
    else:
        expected = []

    if 'restrictions_config' in data:
        restrictions_config_path = data['restrictions_config']
    else:
        restrictions_config_path = yatest.common.source_path(
            'maps/wikimap/mapspro/tools/topology_fixer/configs/' + DEFAULT_RESTRICTIONS_CONFIG_PATH)

    return TestData(
        test_name,
        data['group'],
        data['srid'],
        restrictions_config_path,
        expected)


def prepare_test_data(test_data, conn_params):
    input_schema = "%s_%s_orig" % (YMAPSDF_DB_SCHEMA_PREFIX, test_data.name)
    output_schema = "%s_%s_fixed" % (YMAPSDF_DB_SCHEMA_PREFIX, test_data.name)
    drop_schema(conn_params, input_schema)
    drop_schema(conn_params, output_schema)

    test_file = os.path.join(data_path(JSON_DIR), test_data.name + JSON_EXT)
    json_to_ymapsdf(test_file, conn_params, input_schema)
    json_to_ymapsdf(test_file, conn_params, output_schema)
    patch_schema(conn_params, output_schema)

    return input_schema, output_schema


def perform_test(test_data, conn_params, validator):
    """
    Returns (status, info).
    """
    def print_messages_diff(messages_diff, stream=sys.stdout, indent=0):
        def format_message(m):
            dict = {'description': m.description,
                    'got_count': m.got_count,
                    'expected_count': m.expected_count}
            if m.got_count > 0:
                dict['got_oids'] = m.got_oids
            return dict

        res = StringIO()
        yaml.dump([format_message(m) for m in messages_diff], res)
        res.seek(0)
        for line in res:
            print(' ' * indent + line, file=stream)

    try:
        input_schema, output_schema = prepare_test_data(test_data, conn_params)

        call_fixer(
            conn_params,
            input_schema,
            output_schema,
            test_data.group,
            test_data.srid,
            test_data.restrictions_config,
            threads=1)
        ymapsdf_to_revision(conn_params, output_schema)

        pool = create_pgpool(conn_params)
        head_commit_id = (revision.RevisionsGateway(pool, TRUNK_BRANCH_ID).head_commit_id())
        available_checks = sum([m.check_ids for m in validator.modules() if m.name in VALIDATOR_MODULE_NAMES], [])

        got = [ExpectedMessage(m.attributes.description, frozenset(m.oids))
               for m in validator.run(
                   available_checks,
                   pool,
                   TRUNK_BRANCH_ID,
                   head_commit_id).messages()]
        messages_diff = compare(got, test_data.expected)

    except Exception:
        return (EXCEPTION, format_exc().rstrip())

    status, info = (PASS, '')

    if len(messages_diff) > 0:
        info = StringIO()
        print('Expected and got validation messages differ: ', file=info)
        print_messages_diff(messages_diff, stream=info, indent=2)
        status, info = (FAIL, info.getvalue().rstrip())

    if input_schema != output_schema:
        checker_status, checker_info = call_checker(
            conn_params,
            input_schema,
            output_schema,
            test_data.group,
            test_data.srid)
        if checker_status == FAIL:
            status = FAIL
            info += '; ' + checker_info

    return (status, info)
