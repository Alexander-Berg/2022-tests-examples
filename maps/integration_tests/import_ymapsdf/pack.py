from maps.search.libs.integration_testlib.stage_description import TestStageDescription

from maps.search.geocoder.indexer.py_lib.import_ymapsdf_yt import import_ymapsdf_yt
from maps.search.geocoder.indexer.py_lib.constants import YMAPSDF_IMPORT_TABLE_NAMES as TABLES

from itertools import chain

import_ymapsdf_data_pack = []

REGIONS = ['region1', 'region2']


class ImportYmapsdfDescription(TestStageDescription):
    func = import_ymapsdf_yt
    kwargs = {
        'yt_server': '{mr_server}',
    }
    kwargs_list = [
        dict([
            ('geoobject_storage_dir', 'geoobject_storage_dir/{region}'.format(region=region)),
            ('ymapsdf_region_table_paths', dict([
                (table, '//import_ymapsdf/{region}/{table}'.format(region=region, table=table))
                for table in TABLES
            ]))
        ])
        for region in REGIONS
    ]
    input_tables = [
        '//import_ymapsdf/{region}/{table}'.format(region=region, table=table)
        for table in TABLES
        for region in REGIONS
    ]

    testless_output_files = list(chain(*[(
        'geoobject_storage_dir/{region}/objects.mms'.format(region=region),
        'geoobject_storage_dir/{region}/objects.mms.0'.format(region=region),
        'geoobject_storage_dir/{region}/objects.mms.1'.format(region=region),
        'geoobject_storage_dir/{region}/nameless/objects.mms'.format(region=region),
        'geoobject_storage_dir/{region}/nameless/objects.mms.0'.format(region=region))
        for region in REGIONS
    ]))

import_ymapsdf_data_pack.append(ImportYmapsdfDescription)


class PrintGeoobjectsDescription(TestStageDescription):
    name_program = 'print-geoobjects'
    cmds = list(chain(*[(
        '''{{program}}
                geoobject_storage_dir/{region}
              > geoobject_storage_dir/{region}.dump'''.format(region=region),
        '''{{program}}
                geoobject_storage_dir/{region}/nameless
              > geoobject_storage_dir/{region}/nameless.dump'''.format(region=region))
        for region in REGIONS
    ]))
    input_files = list(chain(*[(
        'geoobject_storage_dir/{region}/objects.mms'.format(region=region),
        'geoobject_storage_dir/{region}/objects.mms.0'.format(region=region),
        'geoobject_storage_dir/{region}/objects.mms.1'.format(region=region),
        'geoobject_storage_dir/{region}/nameless/objects.mms'.format(region=region),
        'geoobject_storage_dir/{region}/nameless/objects.mms.0'.format(region=region))
        for region in REGIONS
    ]))
    output_files = list(chain(*[(
        'geoobject_storage_dir/{region}/nameless.dump'.format(region=region),
        'geoobject_storage_dir/{region}.dump'.format(region=region))
        for region in REGIONS
    ]))

import_ymapsdf_data_pack.append(PrintGeoobjectsDescription)
