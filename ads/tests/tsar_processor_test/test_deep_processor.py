import tempfile
import shutil
import yatest.common
from ads.pytorch.lib.online_learning.production.processors.tsar_processor.lib.deep_part_processor import DeepPartProcessor


def test_deep_part_processing():
    yt_model_path = "UserNamespaces_test/deep_part"
    processor = DeepPartProcessor()
    tempdir = tempfile.mkdtemp()
    try:
        processor.process(yt_model_path, tempdir)
        return yatest.common.canonical_dir(tempdir)
    finally:
        shutil.rmtree(tempdir)
