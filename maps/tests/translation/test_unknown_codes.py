# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_unknown_codes(translation_tester):
    """Test that we not die on unknown language or country codes"""
    translation_tester.run_test()
