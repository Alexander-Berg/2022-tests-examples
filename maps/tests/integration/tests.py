# coding=utf-8
import binascii
import io
import json

import yatest.common
from yandex.maps import geolib3


DAP_SNIPPETS = yatest.common.binary_path(
    'maps/carparks/tools/dap_snippets/bin/dap_snippets')


def convert_string_with_new_lines_to_array(obj):
    for key, value in obj.iteritems():
        if type(value) in (str, unicode) and '\n' in value:
            obj[key] = value.split('\n')

    return obj


def dump_table(ytc, yt_table, filename):
    rows = ytc.read_table(yt_table)

    with io.open(filename, 'w', encoding='utf8') as f:
        for row in rows:
            row = convert_string_with_new_lines_to_array(row)
            s = json.dumps(row, indent=2, sort_keys=True,
                           encoding='utf-8', ensure_ascii=False)

            f.write(unicode(s, encoding='utf-8'))
            f.write(u'\n')


def create_config(yt_stuff):
    config_file = 'config.json'

    config = {
        'yt_proxy': yt_stuff.get_server(),
    }

    with open(config_file, 'w') as f:
        json.dump(config, f)

    return config_file


def load_rows(ytc, table):
    rows = list(ytc.read_table(table))
    for row in rows:
        row['value'] = row['value'].decode('utf-8')
    return rows


def ewkb_hex_point(wkt_string):
    return binascii.hexlify(
        geolib3.Point2.from_WKT(wkt_string).to_EWKB(
            geolib3.SpatialReference.Epsg4326))


def transform_nyak_points(ytc, nyak_table_path):
    nyak_rows = list(ytc.read_table(nyak_table_path))
    for row in nyak_rows:
        row['anchor'] = ewkb_hex_point(row['anchor'])
        row['target'] = ewkb_hex_point(row['target'])
    ytc.write_table(nyak_table_path, nyak_rows)


def test_main_scenario(ytc, yt_stuff):
    config_file = create_config(yt_stuff)
    yatest.common.execute([DAP_SNIPPETS,
                           '-c', config_file,
                           '-g', '//mined_carparks/few_targets_and_clusters',
                           '-o', '//output',
                           '--sprav', '//sprav/company',
                           ],
                          check_exit_code=True)

    assert ytc.exists('//output/all')
    dump_table(ytc, '//output/all', 'all_dump')
    rows = list(ytc.read_table('//output/all'))
    assert ([row['key'] for row in rows] ==
            ['100', '200', 'geocoder_id_11', 'geocoder_id_12'])
    assert all(row['source_type'] == 'mined' for row in rows)

    yatest.common.execute([DAP_SNIPPETS,
                           '-c', config_file,
                           '--filter-errors', '//output/all',
                           '-o', '//output/all_filtered'
                           ],
                          check_exit_code=True)

    assert ytc.exists('//output/all_filtered')
    dump_table(ytc, '//output/all_filtered', 'all_filtered_dump')
    rows = list(ytc.read_table('//output/all_filtered'))
    assert ([row['key'] for row in rows] ==
            ['100', '200', 'geocoder_id_11', 'geocoder_id_12'])
    assert all(row['source_type'] == 'mined' for row in rows)

    transform_nyak_points(ytc, '//nyak/export')
    yatest.common.execute([DAP_SNIPPETS,
                           '-c', config_file,
                           '-m', '//output/all_filtered',
                           '--nyak', '//nyak/export',
                           '--sprav', '//sprav/company',
                           '-o', '//output/all_merged'
                           ],
                          check_exit_code=True)

    assert ytc.exists('//output/all_merged')
    dump_table(ytc, '//output/all_merged', 'all_merged_dump')

    yatest.common.execute([DAP_SNIPPETS,
                           '-c', config_file,
                           '-s', '//output/all_merged',
                           '-o', '//output'
                           ],
                          check_exit_code=True)

    assert ytc.exists('//output/organizations')
    assert ytc.exists('//output/toponyms')

    dump_table(ytc, '//output/organizations', 'organizations_dump')
    dump_table(ytc, '//output/toponyms', 'toponyms_dump')

    rows = load_rows(ytc, '//output/organizations')
    assert [r['key'] for r in rows] == ['100', '200', '300']
    assert all(row['is_organization'] for row in rows)
    assert all(row['kind'] is None for row in rows)

    row = rows[0]  # 100
    assert row['source_type'] == 'sprav'
    assert row['value'].count('<DrivingArrivalPoint>') == 1
    assert row['value'].count(u'Ручное описание точки 1 у организации 100') == 1

    row = rows[1]  # 200
    assert row['source_type'] == 'mined'
    assert row['value'].count('<DrivingArrivalPoint>') == 2
    assert row['value'].count(u'улица Организации 200') == 1
    assert row['value'].count(u'площадь у Организации 200') == 1

    row = rows[2]  # 300
    assert row['source_type'] == 'sprav'
    assert row['value'].count('<DrivingArrivalPoint>') == 2
    assert row['value'].count(u'Ручное описание точки 1 у организации 300') == 1
    assert row['value'].count(u'Ручное описание точки 2 у организации 300') == 1

    rows = load_rows(ytc, '//output/toponyms')
    assert ([r['key'] for r in rows] ==
            ['geocoder_id_11', 'geocoder_id_12', 'geocoder_id_13'])
    assert all(not row['is_organization'] for row in rows)

    # А
    row = rows[0]
    assert row['source_type'] == 'nyak'
    assert row['value'].count(u'А. Точка прибытия 2') == 1
    assert row['kind'] is None

    # Б
    row = rows[1]
    assert row['source_type'] == 'mined'
    assert row['kind'] == 'house'
    assert row['value'].count(u'парковка в здании Б') == 1

    # В
    row = rows[2]
    assert row['source_type'] == 'nyak'
    assert row['value'].count(u'В. Точка прибытия 1') == 1
    assert row['kind'] is None

    return [yatest.common.canonical_file('all_dump', local=True),
            yatest.common.canonical_file('organizations_dump', local=True),
            yatest.common.canonical_file('toponyms_dump', local=True)]
