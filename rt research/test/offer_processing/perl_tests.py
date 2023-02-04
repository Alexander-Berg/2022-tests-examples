import pytest
from bmtest.env import make_fs_root, make_merged_root, list_bm_unittests, run_bm_unittests


TESTS_PATH = 'offer_processing'


@pytest.fixture()
def unpack():
    return [make_fs_root(), make_merged_root()]


@pytest.mark.parametrize('testname', list_bm_unittests(TESTS_PATH))
def test_filters(unpack, testname):
    run_bm_unittests(unpack[0], unpack[1], TESTS_PATH, testname)
