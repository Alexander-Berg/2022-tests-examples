import os
import tempfile

from yatest.common import test_source_path
from maps.garden.libs.mapcompiler.designcompilation import process_stylesheet


def test_process_stylesheet():
    src_path = test_source_path('data/src_design.json')
    dst_path = os.path.join(tempfile.gettempdir(), 'compiled_design.json')
    expected_path = test_source_path('data/compiled_design.json')
    if os.getenv('YA_MAPS_CANONIZE_TESTS'):
        dst_path = expected_path
    process_stylesheet(src_path, dst_path, 'map', ['fill-extrusion'])
    try:
        assert open(expected_path).read() == open(dst_path).read()
    except AssertionError:
        assert False, 'Call `YA_MAPS_CANONIZE_TESTS=1 ya make -t` to update canonical test data'
