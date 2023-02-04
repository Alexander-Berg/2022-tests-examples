# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_transcriber(translation_tester):
    """Test "en => ru" transcriber

    It should work even on completely trash names
    """
    translation_tester.run_test()
