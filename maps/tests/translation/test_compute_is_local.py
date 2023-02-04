# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_compute_is_local(translation_tester):
    """
    Test computation of is_local property for child objects that don't have
    any local name.
    `is_local` is computed only for names that don't have any local name
    """
    translation_tester.run_test()
