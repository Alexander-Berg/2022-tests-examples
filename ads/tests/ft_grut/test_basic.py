import pytest

from ads.bsyeti.caesar.tests.ft.common import import_all_cases
from ads.bsyeti.caesar.tests.ft.common import run_test_case


@pytest.mark.parametrize("case_class", import_all_cases("ads/bsyeti/caesar/tests/ft_grut/cases"))
def test_basic(request, case_class, port_manager, config_test_default_enabled):
    run_test_case(request, case_class, port_manager)
