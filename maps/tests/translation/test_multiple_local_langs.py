# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_multiple_local_langs(translation_tester):
    """Test objects that have multiple local languages"""
    translation_tester.run_test()
