from collections import defaultdict
from typing import Iterable, List

import maps.geoq.libs.spatial_reduce.grid_mapper as gm
import maps.geoq.libs.spatial_reduce.common as c

import data_points as dp


def check_grid_mapper_cells(
    output: Iterable[dict], key_row: dict, target: List[dict]
):
    """
    Checks whenever `key_row` meets all `target` points in GridMapper `outputs` cells
    """

    key_row_id = key_row['id']
    key_row_cells = set()
    cells_content = defaultdict(set)

    for row in output:
        if row['id'] == key_row_id:
            key_row_cells.add((row[c.GRID_I_COLUMN], row[c.GRID_J_COLUMN]))
        cells_content[(row[c.GRID_I_COLUMN], row[c.GRID_J_COLUMN])].add(row['id'])

    key_row_neighbours = set()
    for key_row_cell in key_row_cells:
        key_row_neighbours.update([
            point_id for point_id in cells_content[key_row_cell]
        ])

    for row in target:
        assert row['id'] in key_row_neighbours


def apply_mapper(mapper, test_case):
    for row in test_case:
        yield from mapper(row)


def threshold_multipliers_test(test_case, threshold, key_row, target):
    for threshold_multiplier in range(2, 32, 4):
        grid_mapper = gm.generate_grid_mapper(threshold, threshold_multiplier)

        mapper_result = apply_mapper(grid_mapper, test_case)
        check_grid_mapper_cells(mapper_result, key_row, target)


def test_grid_mapper_square():
    square_points = dp.generate_square(1.)

    threshold_multipliers_test(
        square_points, 112_000, square_points[0], square_points[:4])


def test_grid_mapper_equilateral():
    triangle_points = dp.generate_equilateral_triangle(179.0)

    for point in triangle_points:
        threshold_multipliers_test(
            triangle_points, 18_050_000, point, triangle_points)


def check_output_row_content(output_row, target_row):
    for target_key, target_value in target_row.items():
        assert output_row[target_key] == target_value

    for key in [c.GRID_I_COLUMN, c.GRID_J_COLUMN, c.ROW_TYPE_COLUMN]:
        assert key in output_row


def test_row_content():
    row = {
        'a': 1, 'b': 2, 'c': 3,
        'lon': 13.0, 'lat': 37.0,
        'row_type': 'krasivoye',
        'grid_j': 'не может быть такой колонки в таблице?'
    }

    grid_mapper = gm.generate_grid_mapper(100., 2.)
    for output_row in apply_mapper(grid_mapper, [row]):
        check_output_row_content(output_row, row)
