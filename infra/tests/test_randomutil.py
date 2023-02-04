import random

import pytest

from infra.swatlib import randomutil


def test_gen_random_str():
    assert len(randomutil.gen_random_str(96)) == 16
    assert len(randomutil.gen_random_str(64)) == 8
    assert len(randomutil.gen_random_str(128)) == 24
    assert len(randomutil.gen_random_str(src=random.SystemRandom()))
    with pytest.raises(ValueError):
        randomutil.gen_random_str(11)
