import numpy as np

from maps.poi.statistics.altay.lib.utils import get_all_reasons

from maps.poi.statistics.altay.lib.mappers import (
    BinarizationMapper,
    FlattenByBuildingsMapper,
    ReasonsMapper,
    OrgMovementMapper,
    OutsideBuildingMapper,
)

from mappers_rows import (
    binarization_mapper_rows,
    flatten_buildings_mapper_rows,
    reasons_mapper_rows,
    org_movement_mapper_rows,
    outside_building_mapper_rows,
)


def mapper_test(mapper, rows):
    for row in rows:
        result = list(mapper(row['input']))
        assert result == row['output']


def mapper_approx_test(mapper, rows, column):
    for row in rows:
        result = list(mapper(row['input']))
        reference = row['output']
        assert len(result) == len(reference)
        for result_row, reference_row in zip(result, reference):
            assert np.isclose(result_row[column], reference_row[column])


def test_reasons_mapper():
    all_reasons = get_all_reasons()
    mapper_test(ReasonsMapper(all_reasons), reasons_mapper_rows)


def test_outside_building_mapper():
    mapper_test(OutsideBuildingMapper(), outside_building_mapper_rows)


def test_flatten_by_buildings_mapper():
    mapper_test(FlattenByBuildingsMapper(), flatten_buildings_mapper_rows)


def test_binarization_mapper():
    mapper_test(BinarizationMapper(), binarization_mapper_rows)


def test_org_movement_mapper():
    mapper_approx_test(OrgMovementMapper(), org_movement_mapper_rows, 'movement_distance')
