import pytest
from ads.quality.table.table import Table


@pytest.fixture()
def data():
    left = Table(keys=['k', 'q'], fields=['x', 'y', 'z'])
    left.add_record((0, 0), (1, 1, 1))
    left.add_record((0, 1), (1, 1, 2))
    left.add_record((0, 2), (1, 2, 1))
    left.add_record((1, 0), (3, 1, 1))
    left.add_record((1, 1), (1, 3, 1))

    right = Table(keys=['k', 'c'], fields=['a', 'b', 'z'])
    right.add_record((0, 0), (1, 1, 1))
    right.add_record((0, 1), (1, 1, 2))
    right.add_record((0, 2), (1, 2, 1))
    right.add_record((1, 0), (3, 1, 1))
    right.add_record((1, 1), (1, 3, 1))

    return left, right


def test_join(data):
    print
    joined_table = data[0].join(data[1])
    for r in joined_table:
        print r, joined_table[r]
