# import yatest.common

from maps.poi.personalized_poi.builder.lib.extract_info_mappers import (
    MetrikaClicksMapper,
    OrgvisitsMapper,
    OrgvisitsInfoValidateMapper,
    AltayRubricMapper,
    AltayInfoValidateMapper,
    BaseMapMapper,
    BaseMapInfoValidateMapper,
    CommonAddressesMapper,
    CommonAddressesInfoValidateMapper,
)

from maps.poi.personalized_poi.builder.lib.mappers import (
    FeaturesMapper,
    EcstaticOrgMapper,
    RubricZoomMapper,
    SpaceOidInfoMapper,
    SpacePuidInfoMapper,
    RatingFilterMapper,
    BebrUserPoiStatisticsMapper,
)

from maps.poi.personalized_poi.builder.lib.ut.mappers_rows import (
    altay_rubric_mapper_rows,
    altay_info_validate_mapper_rows,
    analytics_bebr_logs_mapper_rows,
    metrika_clicks_mapper_rows,
    orgvisits_mapper_rows,
    orgvisits_info_validate_mapper_rows,
    base_map_mapper_rows,
    base_map_info_validate_mapper_rows,
    common_addresses_mapper_rows,
    common_addresses_info_validate_mapper_rows,

    ecstatic_org_mapper_rows,
    feature_mapper_rows,
    rubric_zoom_mapper_rows,
    space_oid_info_mapper_rows,
    space_puid_info_mapper_rows,
    rating_filter_mapper_rows,
)


def mapper_test(mapper, rows):
    for row in rows:
        result = list(mapper(row['input']))
        assert result == row['output']


def test_metrika_clicks_mapper():
    mapper_test(MetrikaClicksMapper(), metrika_clicks_mapper_rows)


def test_orgvisits_mapper():
    mapper_test(OrgvisitsMapper(), orgvisits_mapper_rows)


def test_orgvisits_info_validate_mapper():
    mapper_test(OrgvisitsInfoValidateMapper(), orgvisits_info_validate_mapper_rows)


def test_altay_rubric_mapper():
    mapper_test(AltayRubricMapper(), altay_rubric_mapper_rows)


def test_altay_company_mapper():
    # TODO: update test to read binary proto
    # mapper_test(AltayCompanyMapper(), altay_company_mapper_rows)
    pass


def test_altay_info_validate_mapper():
    mapper_test(AltayInfoValidateMapper(), altay_info_validate_mapper_rows)


def test_base_map_mapper():
    mapper_test(BaseMapMapper(), base_map_mapper_rows)


def test_base_map_info_validate_mapper():
    mapper_test(BaseMapInfoValidateMapper(), base_map_info_validate_mapper_rows)


def test_common_addresses_mapper():
    mapper_test(CommonAddressesMapper(), common_addresses_mapper_rows)


def test_common_addresses_info_validate_mapper():
    mapper_test(CommonAddressesInfoValidateMapper(), common_addresses_info_validate_mapper_rows)


def test_maps_logs_mapper():
    # TODO: fix broken test
    # geodata_path = yatest.common.data_path('geo/geodata5.bin')
    # mapper = MapsLogsMapper(geodata_path)
    # mapper.start()
    # mapper_test(mapper, maps_logs_mapper_rows)
    pass


def test_feature_mapper():
    mapper_test(
        FeaturesMapper(['key'], [lambda info: {'my_feature': info}]),
        feature_mapper_rows
    )


def test_rubric_zoom_mapper():
    mapper = RubricZoomMapper(['key'], lambda x: x['condition'], '', min_zoom=15)
    mapper.rubric_to_zoom = {'0': 15, '1': 14, '2': 16, '3': 17}
    mapper_test(mapper, rubric_zoom_mapper_rows)


def test_space_oid_info_mapper():
    mapper_test(SpaceOidInfoMapper(), space_oid_info_mapper_rows)


def test_space_puid_info_mapper():
    mapper_test(SpacePuidInfoMapper(), space_puid_info_mapper_rows)


def test_rating_filter_mapper():
    mapper_test(
        RatingFilterMapper(
            min_rating=4,
            min_rating_count=30,
            model_orgs=set(['1', '2', '3', '4', '5']),
            rubric_info={'101': {'tags': ['personalized_poi']}, '102': {'tags': []}}
        ),
        rating_filter_mapper_rows
    )


def test_ecstatic_org_mapper():
    mapper_test(
        EcstaticOrgMapper(),
        ecstatic_org_mapper_rows
    )


def test_analytics_bebr_logs_mapper():
    mapper_test(
        BebrUserPoiStatisticsMapper(
            {'maps_www.map.poi_layer'}, {'click', 'poi_show'}, 'desktop'),
        analytics_bebr_logs_mapper_rows
    )
