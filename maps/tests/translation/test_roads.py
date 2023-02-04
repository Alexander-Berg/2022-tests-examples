# -*- coding: utf-8 -*-
import pytest

from conftest import YT_SERVER


@pytest.mark.use_local_yt(YT_SERVER)
def test_roads(translation_tester):
    """Test translations of road names.

    Currently, only ru-Latn transliterations should be performed.
    """
    translation_tester.run_test()
