from maps.search.libs.integration_testlib.stage_description import TestStageDescription
from maps.search.offline.indexer.business.py_lib.business_splitter import business_splitter
from maps.search.offline.indexer.business.py_lib.build_offline_cache import build_offline_cache

offline_business_data_pack = []


class SplitCompanyDescription(TestStageDescription):
    func = business_splitter
    kwargs = {
        'yt_altay_snapshot_dir': '//altay',
        'yt_sliced_company_table': '//altay/splitted_company',
        'geoid_coverage_geometry_file': 'geoid/geoid.mms.1',
        'geoid_slices': set([47, 213])
    }
    input_files = [
        'geoid/geoid.mms.1'
    ]
    input_tables = [
        '//altay/company'
    ]
    testless_output_tables = [
        '//altay/splitted_company'
    ]

offline_business_data_pack.append(SplitCompanyDescription)


class BuildOfflineCacheDescription(TestStageDescription):
    func = build_offline_cache
    kwargs = {
        'yt_altay_snapshot_dir': '//altay',
        'yt_sliced_company_table': '//altay/splitted_company',
        'misspell_file_path': 'maps/search/offline/indexer/business/tests/data/misspells.txt',
        'weights_file_path': 'maps/search/offline/indexer/business/tests/data/weights',
        'locale': 'ru',
        'fetching_geoids': set([47]),
        'misspell_count_limit': 200000,
        'output_file_path': 'offline_caches/biz_47_ru.bin'
    }
    input_files = [
        'maps/search/offline/indexer/business/tests/data/misspells.txt',
        'maps/search/offline/indexer/business/tests/data/weights'
    ]
    input_tables = [
        '//altay/splitted_company',
        '//altay/chain',
        '//altay/rubric',
        '//altay/feature',
        '//altay/feature_enum_value',
    ]
    testless_output_files = [  # will be checked later in PrintOfflineCacheDescription
        'offline_caches/biz_47_ru.bin'
    ]

offline_business_data_pack.append(BuildOfflineCacheDescription)


class PrintOfflineCacheDescription(TestStageDescription):
    name_program = 'fb-print'
    cmds = [
        '''{program} offline_caches/biz_47_ru.bin > offline_caches/biz_47_ru.dump'''
    ]
    input_files = [
        'offline_caches/biz_47_ru.bin'
    ]
    output_files = [
        'offline_caches/biz_47_ru.dump'
    ]

offline_business_data_pack.append(PrintOfflineCacheDescription)
