from os.path import dirname, join, realpath

from raiff_report import process


def test_raiff():
    fixture_path = join(dirname(realpath(__file__)), 'fixtures')
    with open(f'{fixture_path}/raiff_pulling.txt', 'r') as f:
        raiff_data = f.read()
    res = process(raiff_data)
    assert len(res) == 2
    assert len(res['01.11.2021']['pullacc']) == 2
    assert len(res['02.11.2021']['pullacc']) == 1
