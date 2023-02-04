# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_non_local_ru_latn(translation_tester):
    """Test that "ru => ru-Latn" translation is applied not only in Russian"""
    translation_tester.run_test()
