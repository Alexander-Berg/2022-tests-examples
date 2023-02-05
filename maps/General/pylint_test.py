from maps.pylibs.recipes.pylint import pylint

import pytest


@pytest.mark.parametrize('mod_name', pylint.get_params())
def test(mod_name):
    pylint.pylint_report(mod_name)
