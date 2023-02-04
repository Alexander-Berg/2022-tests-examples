import pytest
from bmtest.env import make_fs_root, make_merged_root, list_bm_unittests, run_bm_unittests


@pytest.fixture()
def unpack():
    return [make_fs_root(), make_merged_root()]


@pytest.mark.parametrize('testname', list_bm_unittests('dynsmart'))
def test_dynsmart(unpack, testname):
    run_bm_unittests(unpack[0], unpack[1], 'dynsmart', testname)
