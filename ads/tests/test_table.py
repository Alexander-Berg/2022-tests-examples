import pytest
from ads.quality.table.table import Table


@pytest.fixture
def data():
    table = Table(keys=['k', 'q'], fields=['x', 'y', 'z'])
    table.add_record((0, 0), (1, 1, 1))
    table.add_record((0, 1), (1, 1, 2))
    table.add_record((0, 2), (1, 2, 1))
    table.add_record((1, 0), (3, 1, 1))
    table.add_record((1, 1), (1, 3, 1))
    return table


def xtest_filter(data):
    table_filtered = data.filter(lambda r, **kw: r['k'] > 0)
    for k in table_filtered:
        assert table_filtered[k]['k'] > 0
    assert len(table_filtered.data) == 2


def test_reduce(data):
    table_reduced = data.reduce(keys=['k'])
    assert len(table_reduced) == 2
    for k in table_reduced:
        if table_reduced[k]['k'] == 0:
            assert table_reduced[k]['x'] == 3
        elif table_reduced[k]['k'] == 1:
            assert table_reduced[k]['x'] == 4
        else:
            raise


def test_map(data):
    def mapfcn(r,  **kw):
        r['u'] = r['x'] + r['y'] + r['z']
        return r
    table_mapped = data.map(mapfcn, keys=['q'], fields=['u'])
    for r in table_mapped:
        if r == (0,):
            assert table_mapped[r]['u'] == 8


def test_sort_keys(data):
    table_sorted = data.map(keys=['q', 'k']).sort()
    previous = (float('-Inf'), float('-Inf'))
    for r in table_sorted:
        value = (table_sorted[r]['q'], table_sorted[r]['k'])
        assert value >= previous
        previous = value


def xtest_sort(data):
    table_sorted = data.sort(keyfcn=lambda r: r['y'], reverse=True)
    previous = float('Inf')
    for r in table_sorted:
        value = table_sorted[r]['y']
        assert value >= previous
        previous = value


def test_cut(data):
    table_cutted = data.map(fields=['x', 'y'])
    for r in table_cutted:
        assert 'z' not in table_cutted[r]


def test_normalize(data):
    etalon = data.reduce(keys=['k'], fields=['x', 'z'])
    data_normalized = data.normalize(etalon)
    data_normalized_reduced = data_normalized.reduce(keys=['k'])
    for r in data_normalized_reduced:
        assert data_normalized_reduced[r]['x'] == 1.0
        assert data_normalized_reduced[r]['z'] == 1.0


def test_write_json_stream(data, tmpdir):
    filename = str(tmpdir.join('file.txt'))
    with open(filename, 'wb') as f:
        data.write_json_stream(f)
    with open(filename) as f:
        assert len(f.readlines()) == 5


def test_slice(data):
    table_slice = data.map(keys=['q', 'k']).sort().slice(-1)
    assert table_slice[(2, 0)] is not None
