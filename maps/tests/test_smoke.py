import pytest

from util import run_emulator, read_task


@pytest.mark.parametrize(
    'filename',
    [
        'example.json',
    ],
)
def test_smoke(filename):
    run_emulator(read_task(filename))
