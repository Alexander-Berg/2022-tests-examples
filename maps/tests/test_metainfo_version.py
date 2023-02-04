import nose

from maps.garden.sdk.core.modules.search_cache_builder import metainfo

def input_resources(options, graph_builder, module_info):

    version = Version(properties={
        "release": "local_execution",
        "release_name": "local_execution",
        "shipping_date": "local_execution"
    })

    resources_versions = {}

    resources_versions['backa_export'] = version
    resources_versions['rating'] = version
    resources_versions['offline_weights'] = version
    resources_versions[resources.GEOCODER_GEOSRC] = version

    return resources_versions, []

def test_combined_versions():
    versions = [
        ('biz', '1.2.3-4'),
        ('geo', '2.1.1alpha+20120614-2'),
        ('meta', '3.26+nmu4ubuntu1')
    ]
    expected_version = 'biz=1.2.3-4;geo=2.1.1alpha+20120614-2;meta=3.26+nmu4ubuntu1'

    assert metainfo.combine_versions(versions) == expected_version

def test_strange_combined_indexer_version():
    versions = [
        ('biz', 'on'),
        ('geo', 'graph'),
        ('meta', 'morph')
    ]
    expected_version = 'biz=on;geo=graph;meta=morph'

    assert metainfo.combine_versions(versions) == expected_version

def test_splitter_escape():
    versions = [
        ('biz', ';;;'),
        ('geo', '==='),
        ('meta', ';=;=;=')
    ]
    expected_version = 'biz=:::;geo=---;meta=:-:-:-'

    assert metainfo.combine_versions(versions) == expected_version
