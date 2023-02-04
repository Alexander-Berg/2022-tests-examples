import contextlib
import glob
import io
import os
import subprocess
import tarfile
import tempfile

from library.python import resource

import yatest

SCRIPTS_RESOURCE_PATH = yatest.common.source_path('/translation/translation-data.tar.gz')
TEST_DATA_PATH = yatest.common.source_path('maps/garden/tools/translation/tests/data/ru')


def test_all():
    archive_data = resource.find(SCRIPTS_RESOURCE_PATH)
    script_base_path = tempfile.mkdtemp()

    with contextlib.closing(io.BytesIO(archive_data)) as stream:
        with contextlib.closing(tarfile.open(fileobj=stream)) as tar:
            tar.extractall(script_base_path)

    # Translation destination is Russian only
    script_base_path = os.path.join(script_base_path, 'ru')

    def get_script_path(src_lang):
        return os.path.join(script_base_path, src_lang, 'transcribe', 'run')

    for source_filename in glob.glob(TEST_DATA_PATH + '/*.txt'):
        source_basename = os.path.basename(source_filename)
        lang, _ = os.path.splitext(source_basename)
        with open(os.path.join(TEST_DATA_PATH, lang + '.expected')) as expected_file:
            script_path = get_script_path(lang)

            # If we try to translate `cs-CZ` and we cannot find the `cs-CZ`
            # directory, we must choose `cs` directory as the last chance
            if not os.path.exists(script_path):
                lang, _ = lang.rsplit('-', 1)
                script_path = get_script_path(lang)

            cmd = ['/usr/bin/perl', script_path]
            result = subprocess.check_output(cmd, stdin=open(source_filename)).decode('utf-8')
            assert expected_file.read() == result
