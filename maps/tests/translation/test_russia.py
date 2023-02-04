# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_russia(translation_tester):
    """Test translation of "Russia" into other languages"""
    translation_tester.run_test()
