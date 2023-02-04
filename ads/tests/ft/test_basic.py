import pytest

from ads.bsyeti.caesar.tests.ft import common


@pytest.mark.parametrize("case_class", common.import_all_cases("ads/bsyeti/caesar/tests/ft/cases"))
def test_basic(request, case_class, port_manager):
    common.run_test_case(request, case_class, port_manager)
