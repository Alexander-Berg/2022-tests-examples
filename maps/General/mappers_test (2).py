# -*- coding: utf-8 -*-
from datetime import datetime

from maps.poi.static_poi.buildings import OrgBuildingsMapper
from maps.poi.static_poi.bundle import ExtraPoiBundleMapper
from maps.poi.static_poi.constants import experiments
from maps.poi.static_poi.detailed_zooms import DetailedZoomsDispClassMapper
from maps.poi.static_poi.disp_class import NaviDispClassMapper
from maps.poi.static_poi.geocube import GeoCubeMapper
from maps.poi.static_poi.nyak_export import ExtraPoiNyakExportMapper
from maps.poi.static_poi.organizations import OrgInfoMapper
from maps.poi.static_poi.poi import (
    NoisyDispClassMapper,
    PreparePoiMapper,
    PriorityPoiMapper,
    ProcessPoiTypesMapper
)
from maps.poi.static_poi.ymapsdf import (
    BuildingCellMapper,
    NmapsPositionVerifiedMapper,
    OrgCellMapper,
    OrgNmapsObjectsMapper
)

from mappers_rows import (
    building_cell_mapper_rows,
    detailed_zooms_disp_class_mapper_rows,
    extra_poi_bundle_mapper_rows,
    extra_poi_bundle_no_extra_orgs_mapper_rows,
    extra_poi_nk_export_mapper_rows,
    geocube_mapper_rows,
    navi_disp_class_mapper_rows,
    nmaps_position_verified_mapper_rows,
    noisy_disp_class_mapper_rows,
    org_buildings_mapper_rows,
    org_cell_mapper_rows,
    org_info_mapper_rows,
    org_nmaps_objects_mapper_rows,
    prepare_poi_mapper_rows,
    priority_poi_mapper_rows,
    priority_poi_mapper_visibility_rows,
    priority_poi_mapper_lower_all_rows,
    priority_poi_mapper_raise_all_rows,
    process_poi_types_mapper_rows,
    remove_orgs_dc10_percent_25_mapper_rows,
    remove_orgs_dc9_percent_25_mapper_rows
)


def mapper_test(mapper, rows):
    for row in rows:
        result = list(mapper(row['input']))
        assert result == row['output']


def test_building_cell_mapper():
    mapper_test(BuildingCellMapper(0.036, 0.014), building_cell_mapper_rows)


def test_detailed_zooms_disp_class_mapper():
    mapper_test(
        DetailedZoomsDispClassMapper(10),
        detailed_zooms_disp_class_mapper_rows
    )


def test_extra_poi_bundle_mapper():
    mapper_test(ExtraPoiBundleMapper(), extra_poi_bundle_mapper_rows)
    mapper_test(
        ExtraPoiBundleMapper(experiment='no_extra_orgs'),
        extra_poi_bundle_no_extra_orgs_mapper_rows
    )


def test_extra_poi_nk_export_mapper():
    mapper_test(
        ExtraPoiNyakExportMapper(datetime(2021, 5, 5)),
        extra_poi_nk_export_mapper_rows
    )


def test_geocube_mapper():
    mapper_test(GeoCubeMapper(), geocube_mapper_rows)


def test_navi_disp_class_mapper():
    mapper_test(NaviDispClassMapper(), navi_disp_class_mapper_rows)


def test_nmaps_position_verified_mapper():
    mapper_test(
        NmapsPositionVerifiedMapper('cis1'),
        nmaps_position_verified_mapper_rows
    )


def test_org_buildings_mapper():
    mapper_test(OrgBuildingsMapper(), org_buildings_mapper_rows)


def test_org_cell_mapper():
    mapper_test(OrgCellMapper(0.025, 0.011), org_cell_mapper_rows)


def test_org_info_mapper():
    manual_publications = {
        1000002435: {
            'EN': 'Torovo'
        }
    }
    altay_providers = {
        'foursquare': 703,
        'nyak': 1537416776,
        'gas_stations_internal': 3501572629
    }
    mapper_test(
        OrgInfoMapper(manual_publications, altay_providers),
        org_info_mapper_rows
    )


def test_org_nmaps_objects_mapper():
    mapper_test(
        OrgNmapsObjectsMapper({'RU': 'cis1', 'KZ': 'cis2'}),
        org_nmaps_objects_mapper_rows
    )


def test_prepare_poi_mapper():
    rubric_info = {
        '184105274': {
            'name': 'АЗС',
            'tags': [
                'personalized_poi',
                'static_poi'
            ],
            'disp_class': 5,
            'nyak_ft_type_id': 253,
            'nyak_ft_type_name': 'gasstation'
        },
        '184108075': {
            'name': 'Гипермаркет',
            'tags': [
                'personalized_poi',
                'static_poi'
            ],
            'disp_class': 4,
            'nyak_ft_type_id': 1313,
            'nyak_ft_type_name': 'hypermarket'
        },
        '88844575693': {
            'name': 'Детская площадка',
            'disp_class': 7,
            'nyak_ft_type_id': 202,
            'nyak_ft_type_name': 'urban-sportground'
        },
        '1488': {
            'name': 'Какая-то плохая рубрика',
            'disp_class': 7
        }
    }
    mapper_test(
        PreparePoiMapper(
            iso_codes=['RU', 'UA', 'BY', 'TR', 'GH', 'IL'],
            rubric_info=rubric_info
        ),
        prepare_poi_mapper_rows
    )


def test_priority_poi_mapper():
    mapper_test(PriorityPoiMapper(), priority_poi_mapper_rows)
    mapper_test(PriorityPoiMapper(experiment=experiments.VISIBILITY),
                priority_poi_mapper_visibility_rows)
    mapper_test(PriorityPoiMapper(experiment=experiments.LOWER_ALL),
                priority_poi_mapper_lower_all_rows)
    mapper_test(PriorityPoiMapper(experiment=experiments.RAISE_ALL),
                priority_poi_mapper_raise_all_rows)


def test_process_poi_types_mapper():
    mapper_test(
        ProcessPoiTypesMapper(iso_codes_with_simplified_filtering=('TR',)),
        process_poi_types_mapper_rows
    )


def test_remove_random_orgs_dc10():
    mapper_test(
        ExtraPoiBundleMapper(experiment='random_orgs_remove_dc10.0_percent_25'),
        remove_orgs_dc10_percent_25_mapper_rows
    )


def test_remove_random_orgs_dc9():
    mapper_test(
        ExtraPoiBundleMapper(experiment='random_orgs_remove_dc9.499_percent_25'),
        remove_orgs_dc9_percent_25_mapper_rows
    )


def test_noisy_disp_class_mapper():
    mapper_test(
        NoisyDispClassMapper(experiment=experiments.NOISY_DISPCLASS),
        noisy_disp_class_mapper_rows,
    )
