# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_abkhazia(translation_tester):
    """Test support of "AB" country code and test several important translations"""
    translation_tester.run_test()
