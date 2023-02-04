# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_double_translation(translation_tester):
    """Test checks that name translated only once"""
    translation_tester.run_test()
