from maps.search.libs.integration_testlib.stage_description import TestStageDescription

from maps.search.libs.geosrc.py_lib.split_geoids_geosrc import split_geoids_geosrc
from maps.search.offline.indexer.geocoder.py_lib.build_geocoder_cache import build_geocoder_cache
from maps.search.offline.indexer.metainfo.py_lib.build_metainfo import build_metainfo

from itertools import chain

geocoder_data_pack = []

GEOIDS = [143, 192, 24618]
LOCALES = ['RU', 'EN']

OFFLINE_WEIGHTS_INPUT_FILE = 'maps/search/offline/indexer/geocoder/tests/data/offline_weights.txt'
MISSPELLS_INPUT_FILE = 'maps/search/offline/indexer/geocoder/tests/data/misspells.txt'
FLATBUFFER_SCHEMATA = 'maps/search/geocoder/flatbuffers'
GEOSRC_YT_PATH = '//sandbox/maps/geocoder_indexer/geosrc/20.01.20-1'


class RegionInfo(object):
    def __init__(
        self,
        input_yaml_files,
        imported_geostorage_yt_table,
        imported_geostorage_nameless_yt_table,
        geostorage_dir,
        geostorage_yt_table,
        geostorage_nameless_yt_table,
        index_dir,
        geosrc_yt_path,
        povs,
    ):
        self.input_yaml_files = input_yaml_files
        self.imported_geostorage_yt_table=imported_geostorage_yt_table
        self.imported_geostorage_nameless_yt_table=imported_geostorage_nameless_yt_table
        self.geostorage_dir = geostorage_dir
        self.geostorage_yt_table = geostorage_yt_table
        self.geostorage_nameless_yt_table = geostorage_nameless_yt_table
        self.index_dir = index_dir
        self.geosrc_yt_path = geosrc_yt_path
        self.povs = povs


REGION1 = RegionInfo(
    input_yaml_files=[
        'maps/search/integration_tests/geocoder/yaml/world/world.yml',
        'maps/search/integration_tests/geocoder/yaml/world/france.yml',
        'maps/search/integration_tests/geocoder/yaml/world/israel.yml',
        'maps/search/integration_tests/geocoder/yaml/world/turkey.yml',
        'maps/search/integration_tests/geocoder/yaml/world/uae.yml',
        'maps/search/integration_tests/geocoder/yaml/world/imaginary.yml',
        'maps/search/integration_tests/geocoder/yaml/world/imaginary_geomland.yml',
        'maps/search/integration_tests/geocoder/yaml/world/imaginary_taxiland.yml',
        'maps/search/integration_tests/geocoder/yaml/world/imaginary_urland.yml',
    ],
    imported_geostorage_yt_table='//sandbox/maps/geocoder_indexer/geostorage/imported_world',
    imported_geostorage_nameless_yt_table='//sandbox/maps/geocoder_indexer/geostorage/imported_world_nameless',
    geostorage_dir='merge_geostorage_world',
    geostorage_yt_table='//sandbox/maps/geocoder_indexer/geostorage/world',
    geostorage_nameless_yt_table='//sandbox/maps/geocoder_indexer/geostorage/world_nameless',
    index_dir='index/world/index',
    geosrc_yt_path='//sandbox/maps/geocoder_indexer/geosrc/buf/20.01.20-1/world',
    povs=['001'],
)

REGION2 = RegionInfo(
    input_yaml_files=[
        'maps/search/integration_tests/geocoder/yaml/rubk/russia.yml',
        'maps/search/integration_tests/geocoder/yaml/rubk/crimea.yml',
        'maps/search/integration_tests/geocoder/yaml/rubk/ukraine.yml',
        'maps/search/integration_tests/geocoder/yaml/rubk/belarus.yml',
        'maps/search/integration_tests/geocoder/yaml/rubk/russia_kazan_locality.yml',
        'maps/search/integration_tests/geocoder/yaml/rubk/russia_moscow_locality.yml',
        'maps/search/integration_tests/geocoder/yaml/rubk/russia_moscow_region.yml',
        'maps/search/integration_tests/geocoder/yaml/rubk/russia_spb_locality.yml',
        'maps/search/integration_tests/geocoder/yaml/rubk/russia_spb_province.yml',
    ],
    imported_geostorage_yt_table='//sandbox/maps/geocoder_indexer/geostorage/imported_rubk',
    imported_geostorage_nameless_yt_table='//sandbox/maps/geocoder_indexer/geostorage/imported_rubk_nameless',
    geostorage_dir='merge_geostorage_rubk',
    geostorage_yt_table='//sandbox/maps/geocoder_indexer/geostorage/rubk',
    geostorage_nameless_yt_table='//sandbox/maps/geocoder_indexer/geostorage/rubk_nameless',
    index_dir='index/rubk/index',
    geosrc_yt_path='//sandbox/maps/geocoder_indexer/geosrc/buf/20.01.20-1/rubk',
    povs=['001', 'RU'],
)

HYDRO = RegionInfo(
    input_yaml_files=None,
    imported_geostorage_yt_table=None,
    imported_geostorage_nameless_yt_table=None,
    geostorage_dir='merge_global_hydro',
    geostorage_yt_table='//sandbox/maps/geocoder_indexer/geostorage/hydro',
    geostorage_nameless_yt_table='//sandbox/maps/geocoder_indexer/geostorage/hydro_nameless',
    index_dir='index/hydro/index',
    geosrc_yt_path='//sandbox/maps/geocoder_indexer/geosrc/buf/20.01.20-1/hydro',
    povs=[],
)

GEOSRC_POVS = sorted(set(REGION1.povs + REGION2.povs + HYDRO.povs))


class ImportYaml(TestStageDescription):
    name_program = 'import-yaml'

    def __init__(self, region_info):
        self.cmds = [
            f'''{{program}}
                    --server {{mr_server}}
                    --yt-geostorage-path {region_info.imported_geostorage_yt_table}
                    --nameless-yt-geostorage-path {region_info.geostorage_nameless_yt_table}''' +
            ''.join(f'''
                    --yaml-input-file {input_file}'''
                    for input_file in region_info.input_yaml_files)
        ]

        self.input_files = region_info.input_yaml_files

        self.output_tables = [
            f'{region_info.imported_geostorage_yt_table}',
            f'{region_info.geostorage_nameless_yt_table}',
        ]

geocoder_data_pack.extend(ImportYaml(region_info) for region_info in (REGION1, REGION2))


class MergeGeostorage(TestStageDescription):
    name_program = 'merge-geostorage'

    def __init__(self, region_info):
        self.cmds = [
            f'''{{program}}
                    --server {{mr_server}}
                    --input-yt-geostorage-path {region_info.imported_geostorage_yt_table}
                    --output-yt-geostorage-path {region_info.geostorage_yt_table}'''
        ]

        self.input_tables = [
            f'{region_info.imported_geostorage_yt_table}',
        ]

        self.output_tables = [
            f'{region_info.geostorage_yt_table}',
        ]

geocoder_data_pack.extend(MergeGeostorage(region_info) for region_info in (REGION1, REGION2))


class MergeGlobalHydro(TestStageDescription):
    name_program = 'merge-global-hydro'

    def __init__(self, hydro_region_info, *regions_info):
        self.cmds = [
            f'''{{program}}
                    --server {{mr_server}}
                    --output-yt-geostorage-path {hydro_region_info.geostorage_yt_table}
                    --output-nameless-yt-geostorage-path {hydro_region_info.geostorage_nameless_yt_table}''' +
            ''.join(f'''
                    --input-yt-geostorage-paths {region_info.imported_geostorage_yt_table}'''
                    for region_info in regions_info)
        ]

        self.input_tables = [
            f'{region_info.imported_geostorage_yt_table}'
            for region_info in regions_info
        ]

        self.output_tables = [
            f'{hydro_region_info.geostorage_yt_table}',
            f'{hydro_region_info.geostorage_nameless_yt_table}',
        ]

geocoder_data_pack.append(MergeGlobalHydro(HYDRO, REGION1, REGION2))


class BuildIndex(TestStageDescription):
    name_program = 'build-index'

    def __init__(self, region_info):
        self.cmds = [
            f'''{{program}}
                    --index-dir {region_info.index_dir}
                    --server {{mr_server}}
                    --yt-geostorage-path {region_info.geostorage_yt_table}
                    --nameless-yt-geostorage-path {region_info.geostorage_nameless_yt_table}'''
        ]

        self.input_tables = [
            region_info.geostorage_yt_table,
            region_info.geostorage_nameless_yt_table,
        ]

        self.testless_output_files = [
            # will not be checked later
            f'{region_info.index_dir}/house-spatial-index.mms',
            f'{region_info.index_dir}/island-spatial-index.mms',
            f'{region_info.index_dir}/point-spatial-index.mms',
            f'{region_info.index_dir}/polygon-spatial-index.mms',
            f'{region_info.index_dir}/street-spatial-index.mms',

            # will be checked later
            f'{region_info.index_dir}/text-index.bin',

            f'{region_info.index_dir}/names-pool.mms',

            f'{region_info.index_dir}/addr-unity-storage.mms',
            f'{region_info.index_dir}/entrance-map.mms',
            f'{region_info.index_dir}/flat-map.mms',
            f'{region_info.index_dir}/exclusion-pool.mms',
            f'{region_info.index_dir}/house-shape-map.mms',
            f'{region_info.index_dir}/keynames-pool.mms',
            f'{region_info.index_dir}/level-map.mms',
            f'{region_info.index_dir}/nameless-islands.bin',
            f'{region_info.index_dir}/point-names-pool.mms',
            f'{region_info.index_dir}/postal-map.mms',
            f'{region_info.index_dir}/recognition-map.bin',
            f'{region_info.index_dir}/source-id-map.mms',
            f'{region_info.index_dir}/toponyms.bin',
        ]

geocoder_data_pack.extend(BuildIndex(region_info) for region_info in (REGION1, REGION2, HYDRO))


class PrintTextIndex(TestStageDescription):
    name_program = 'print_index'

    def __init__(self, region_info):
        self.cmds = [
            f'''{{program}}
                    --index {region_info.index_dir}/text-index.bin
                    --task documents
                    --all > {region_info.index_dir}/text-index.bin.dump'''
        ]
        self.input_files = [
            f'{region_info.index_dir}/text-index.bin'
        ]
        self.output_files = [
            f'{region_info.index_dir}/text-index.bin.dump'
        ]

geocoder_data_pack.extend(PrintTextIndex(region_info) for region_info in (REGION1, REGION2, HYDRO))


class PrintFbs(TestStageDescription):
    name_program = 'flatc64'

    def __init__(self, region_info):
        self.cmds = [
            f'''{{program}} --json --raw-binary --natural-utf8
                    -o {region_info.index_dir}/
                    {FLATBUFFER_SCHEMATA}/recognition.fbs64 --
                    {region_info.index_dir}/recognition-map.bin''',
            f'''{{program}} --json --raw-binary --natural-utf8
                    -o {region_info.index_dir}/
                    {FLATBUFFER_SCHEMATA}/toponyms.fbs64 --
                    {region_info.index_dir}/toponyms.bin''',
            f'''{{program}} --json --raw-binary --natural-utf8
                    -o {region_info.index_dir}/
                    {FLATBUFFER_SCHEMATA}/toponyms.fbs64 --
                    {region_info.index_dir}/nameless-islands.bin''',
        ]

        self.input_files = [
            f'{FLATBUFFER_SCHEMATA}/geometry.fbs64',
            f'{FLATBUFFER_SCHEMATA}/named_point.fbs64',
            f'{FLATBUFFER_SCHEMATA}/recognition.fbs64',
            f'{FLATBUFFER_SCHEMATA}/toponyms.fbs64',
            f'{region_info.index_dir}/recognition-map.bin',
            f'{region_info.index_dir}/toponyms.bin',
            f'{region_info.index_dir}/nameless-islands.bin',
        ]

        self.output_files = [
            f'{region_info.index_dir}/recognition-map.json',
            f'{region_info.index_dir}/toponyms.json',
            f'{region_info.index_dir}/nameless-islands.json',
        ]

geocoder_data_pack.extend(PrintFbs(region_info) for region_info in (REGION1, REGION2, HYDRO))


class PrintMms(TestStageDescription):
    name_program = 'print-mms-dump'

    def __init__(self, region_info):
        file_names = [
            'addr-unity-storage',
            'entrance-map',
            'flat-map',
            'exclusion-pool',
            'house-shape-map',
            'keynames-pool',
            'level-map',
            'names-pool',
            'point-names-pool',
            'postal-map',
            'source-id-map',
        ]

        self.cmds = [
            f'''{{program}}
                    {region_info.index_dir}/{file_name}.mms
                    > {region_info.index_dir}/{file_name}.mms.dump'''
            for file_name in file_names
        ]

        self.input_files = [
            f'{region_info.index_dir}/{file_name}.mms'
            for file_name in file_names
        ]

        self.output_files = [
            f'{region_info.index_dir}/{file_name}.mms.dump'
            for file_name in file_names
        ]

geocoder_data_pack.extend(PrintMms(region_info) for region_info in (REGION1, REGION2, HYDRO))


class PrintStats(TestStageDescription):
    name_program = 'print-stats'

    def __init__(self, region_info):
        self.cmds = [
            f'''{{program}}
                    {region_info.index_dir}/toponyms.bin
                    {region_info.index_dir}/recognition-map.bin
                    RU > {region_info.index_dir}/stats.tsv'''
        ]
        self.input_files = [
            f'{region_info.index_dir}/toponyms.bin',
            f'{region_info.index_dir}/recognition-map.bin',
        ]
        self.output_files = [
            f'{region_info.index_dir}/stats.tsv'
        ]

geocoder_data_pack.extend(PrintStats(region_info) for region_info in (REGION1, REGION2, HYDRO))


class BuildGeosrcYt(TestStageDescription):
    name_program = 'build-geosrc-yt'

    def __init__(self, region_info):
        self.cmds = [
            f'''{{program}}
                    --server {{mr_server}}
                    --geosrc-path {region_info.geosrc_yt_path}
                    --yt-geostorage-path {region_info.geostorage_yt_table}
                    --index-dir {region_info.index_dir}'''
        ]

        self.input_tables = [
            region_info.geostorage_yt_table,
        ]

        self.output_tables = [
            f'{region_info.geosrc_yt_path}/arrival_points',
            f'{region_info.geosrc_yt_path}/geometries',
            f'{region_info.geosrc_yt_path}/houses',
            f'{region_info.geosrc_yt_path}/names',
            f'{region_info.geosrc_yt_path}/toponyms',
            f'{region_info.geosrc_yt_path}/pov/recognition_map',
        ] + list(chain(*[(
            f'{region_info.geosrc_yt_path}/pov/{pov}/addresses',
            f'{region_info.geosrc_yt_path}/pov/{pov}/hierarchy',
            f'{region_info.geosrc_yt_path}/pov/{pov}/uri')
            for pov in region_info.povs
        ]))

geocoder_data_pack.extend(BuildGeosrcYt(region_info) for region_info in (REGION1, REGION2, HYDRO))


class MergeGeosrcYt(TestStageDescription):
    name_program = 'merge-geosrc-yt'

    def __init__(self, *regions_info):
        self.cmds = [
            f'''{{program}}
                    --server {{mr_server}}
                    --result-geosrc-path {GEOSRC_YT_PATH}''' +
            ''.join(f'''
                    --region-geosrc-paths {region_info.geosrc_yt_path}'''
                    for region_info in regions_info)
        ]

        self.input_tables = list(chain(*[
            [
                f'{region_info.geosrc_yt_path}/arrival_points',
                f'{region_info.geosrc_yt_path}/geometries',
                f'{region_info.geosrc_yt_path}/houses',
                f'{region_info.geosrc_yt_path}/names',
                f'{region_info.geosrc_yt_path}/toponyms',
                f'{region_info.geosrc_yt_path}/pov/recognition_map',
            ] + list(chain(*[(
                f'{region_info.geosrc_yt_path}/pov/{pov}/addresses',
                f'{region_info.geosrc_yt_path}/pov/{pov}/hierarchy',
                f'{region_info.geosrc_yt_path}/pov/{pov}/uri')
                for pov in region_info.povs
            ]))
            for region_info in regions_info
        ]))

        self.output_tables = [
            f'{GEOSRC_YT_PATH}/arrival_points',
            f'{GEOSRC_YT_PATH}/geometries',
            f'{GEOSRC_YT_PATH}/houses',
            f'{GEOSRC_YT_PATH}/names',
            f'{GEOSRC_YT_PATH}/toponyms',
            f'{GEOSRC_YT_PATH}/pov/recognition_map',
        ] + list(chain(*[(
            f'{GEOSRC_YT_PATH}/pov/{pov}/addresses',
            f'{GEOSRC_YT_PATH}/pov/{pov}/hierarchy',
            f'{GEOSRC_YT_PATH}/pov/{pov}/uri')
            for pov in GEOSRC_POVS
        ]))

geocoder_data_pack.append(MergeGeosrcYt(REGION1, REGION2, HYDRO))


class AggregateToponyms(TestStageDescription):
    func = split_geoids_geosrc
    kwargs = {
        'yt_server': '{mr_server}',
        'yt_token': '',
        'geosrc_db_path': GEOSRC_YT_PATH,
        'geoids_map': {geoid: [geoid] for geoid in GEOIDS},
    }
    kwargs_list = [
        {
            'pov': locale,
            'result_path': f'{GEOSRC_YT_PATH}/aggregated_table_{locale}',
        }
        for locale in LOCALES
    ]
    input_tables = [
        f'{GEOSRC_YT_PATH}/arrival_points',
        f'{GEOSRC_YT_PATH}/geometries',
        f'{GEOSRC_YT_PATH}/houses',
        f'{GEOSRC_YT_PATH}/names',
        f'{GEOSRC_YT_PATH}/toponyms',
        f'{GEOSRC_YT_PATH}/pov/recognition_map',
    ] + list(chain(*[(
        f'{GEOSRC_YT_PATH}/pov/{pov}/addresses',
        f'{GEOSRC_YT_PATH}/pov/{pov}/hierarchy',
        f'{GEOSRC_YT_PATH}/pov/{pov}/uri')
        for pov in GEOSRC_POVS
    ]))
    output_tables = [
        f'{GEOSRC_YT_PATH}/aggregated_table_{locale}'
        for locale in LOCALES
    ]

geocoder_data_pack.append(AggregateToponyms)


class BuildGeocoderCache(TestStageDescription):
    func = build_geocoder_cache
    kwargs = {
        'yt_server': '{mr_server}',
        'yt_token': '',
        'misspell_count': 20000,
        'input_weights': OFFLINE_WEIGHTS_INPUT_FILE,
        'input_misspells': MISSPELLS_INPUT_FILE,
    }
    kwargs_list = [
        {
            'representing_geoid': geoid,
            'path_locale_map': {f'build_geocoder_cache_dir/{geoid:d}_{locale}': locale},
            'input_geosrc_table_path': f'{GEOSRC_YT_PATH}/aggregated_table_{locale}',
        }
        for geoid in GEOIDS
        for locale in LOCALES
    ]

    input_tables = [
        f'{GEOSRC_YT_PATH}/aggregated_table_{locale}'
        for locale in LOCALES
    ]
    input_files = [
        OFFLINE_WEIGHTS_INPUT_FILE,
        MISSPELLS_INPUT_FILE,
    ]
    testless_output_files = [  # will be checked later in PrintGeocoderCacheDescription
        f'build_geocoder_cache_dir/{geoid:d}_{locale}'
        for geoid in GEOIDS
        for locale in LOCALES
    ]

geocoder_data_pack.append(BuildGeocoderCache)


class PrintGeocoderCache(TestStageDescription):
    name_program = 'fb-print'
    cmds = [
        f'''{{program}}
                build_geocoder_cache_dir/{geoid:d}_{locale}
                > build_geocoder_cache_dir/{geoid:d}_{locale}.dump'''
        for geoid in GEOIDS
        for locale in LOCALES
    ]
    input_files = [
        f'build_geocoder_cache_dir/{geoid:d}_{locale}'
        for geoid in GEOIDS
        for locale in LOCALES
    ]
    output_files = [
        f'build_geocoder_cache_dir/{geoid:d}_{locale}.dump'
        for geoid in GEOIDS
        for locale in LOCALES
    ]

geocoder_data_pack.append(PrintGeocoderCache)


class BuildMetainfo(TestStageDescription):
    func = build_metainfo
    kwargs = {
        'yt_server': '{mr_server}',
        'yt_token': '',
        'geometry_file': 'geoid/geoid.mms.1',
        'cache_version': '20.01.20-1',
    }
    kwargs_list = [
        {
            'input_geosrc_path': f'{GEOSRC_YT_PATH}/aggregated_table_{locale}',
            'representing_geoid': geoid,
            'geoids': [geoid],
            'path_locale_map': {f'build_metainfo_dir/{geoid:d}_{locale}': locale},
        }
        for geoid in GEOIDS
        for locale in LOCALES
    ]
    input_tables = [
        f'{GEOSRC_YT_PATH}/aggregated_table_{locale}'
        for locale in LOCALES
    ]
    input_files = [
        'geoid/geoid.mms.1',
    ]
    testless_output_files = [  # will be checked later in PrintMetainfoDescription
        f'build_metainfo_dir/{geoid:d}_{locale}'
        for geoid in GEOIDS
        for locale in LOCALES
    ]

geocoder_data_pack.append(BuildMetainfo)


class PrintMetainfo(TestStageDescription):
    name_program = 'pb-print'
    cmds = [
        f'''{{program}}
                build_metainfo_dir/{geoid:d}_{locale}
                > build_metainfo_dir/{geoid:d}_{locale}.dump'''
        for geoid in GEOIDS
        for locale in LOCALES
    ]
    input_files = [
        f'build_metainfo_dir/{geoid:d}_{locale}'
        for geoid in GEOIDS
        for locale in LOCALES
    ]
    output_files = [
        f'build_metainfo_dir/{geoid:d}_{locale}.dump'
        for geoid in GEOIDS
        for locale in LOCALES
    ]

geocoder_data_pack.append(PrintMetainfo)
