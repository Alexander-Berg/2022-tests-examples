
import os

import pytest


@pytest.fixture
def input_filename(tmpdir):
    input_file = tmpdir.join('input_file')
    return os.path.join(input_file.dirname, input_file.basename)


@pytest.fixture
def output_filename(tmpdir):
    output_file = tmpdir.join('output_file')
    return os.path.join(output_file.dirname, output_file.basename)
