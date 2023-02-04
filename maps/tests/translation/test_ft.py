# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_ft(translation_tester):
    """Test translations of features: seas, islands, etc.

    Test translations of features with "001" country code.
    """
    translation_tester.run_test()
