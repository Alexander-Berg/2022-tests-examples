# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_uppercase_exclusion(translation_tester):
    """Test that uppercase synonyms are not translated"""
    translation_tester.run_test()
