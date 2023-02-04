from yatest.common import build_path
from yt.wrapper import ypath_join
from maps.analyzer.pylibs.test_tools.compare_tables import assert_equal_tables_contents
import maps.analyzer.pylibs.envkit as envkit
import maps.analyzer.pylibs.schema as pyschema
import maps.analyzer.toolkit.lib as tk
import maps.analyzer.tools.mapbox_quality.py.lib.operations as ops


TEST_DATA_ROOT = 'maps/analyzer/tools/mapbox_quality/tests/data'
OSM_GRAPH_FILE = build_path(TEST_DATA_ROOT + '/park_kultury.osm')
LIVE_SPEED_DATA_ROOT = build_path(TEST_DATA_ROOT + '/live_data')
TYPICAL_SPEED_DATA = build_path(TEST_DATA_ROOT + '/typical_data.csv')


def test_integration(ytc):
    prepared = ops.prepare_data(ytc, LIVE_SPEED_DATA_ROOT)
    prep_expected = [
        {'hour': 12, 'value': '3137533961,3241777299,10', 'month': 4, 'year': 2020, 'day': 11, 'minute': 1},
        {'hour': 12, 'value': '3241777299,3241777297,15', 'month': 4, 'year': 2020, 'day': 11, 'minute': 1},
        {'hour': 12, 'value': '3241777297,3999424779,20', 'month': 4, 'year': 2020, 'day': 11, 'minute': 1},
    ]
    assert pyschema.is_strong_schema(ytc, prepared)
    assert envkit.yt.svn_revision_attribute(ytc, prepared) == envkit.config.SVN_REVISION, 'should set svn revision'
    assert_equal_tables_contents(
        prep_expected,
        list(ytc.read_table(prepared)),
        unordered=True,
    )

    jams = ops.build_jams(
        ytc,
        prepared,
        OSM_GRAPH_FILE,
        TYPICAL_SPEED_DATA,
        timezone=7200,
        min_coverage=0.75,
        op_spec={'reducer': {'cpu_limit': 1}},  # test cluster have no CPUs
    )

    assert ytc.exists(jams), 'should create jams {}'.format(jams)
    assert envkit.yt.svn_revision_attribute(ytc, jams) == envkit.config.SVN_REVISION, 'should set svn revision'
    assert ytc.get(ypath_join(jams, '@row_count')) > 0
    assert pyschema.is_strong_schema(ytc, jams)
    assert next(ytc.read_table(jams))[tk.schema.TIME.name] == '20200411T120100'
