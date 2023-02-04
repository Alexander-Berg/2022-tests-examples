# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_morocco(translation_tester):
    """Test translations in Morocco, which are possible only from "ar-Latn" language"""
    translation_tester.run_test()
