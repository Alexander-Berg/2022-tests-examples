# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_latn(translation_tester):
    """Test transliteration ("-Latn" versions).

    Test checks that module adds transliteration if it doesn't exist yet
    """
    translation_tester.run_test()
