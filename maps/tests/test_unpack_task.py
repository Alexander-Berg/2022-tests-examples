import os
import tarfile
import zipfile

from maps.garden.sdk.core import Version
from maps.garden.sdk.resources import DirResource

from maps.garden.sdk.extensions.unpack_task import UnpackFileTask

_TEST_FILE_CONTENT = "test content"
_TEST_FILE_NAME = "testfile"


def test_unpack():
    _test_unpack(subfolder=None, tgz_needed=False, zip_content_needed=False)
    _test_unpack(subfolder=None, tgz_needed=True, zip_content_needed=False)
    _test_unpack(subfolder="module", tgz_needed=False, zip_content_needed=False)
    _test_unpack(subfolder="module", tgz_needed=True, zip_content_needed=False)
    _test_unpack(subfolder=None, tgz_needed=False, zip_content_needed=True)
    _test_unpack(subfolder=None, tgz_needed=True, zip_content_needed=True)
    _test_unpack(subfolder="module", tgz_needed=False, zip_content_needed=True)
    _test_unpack(subfolder="module", tgz_needed=True, zip_content_needed=True)


def _test_unpack(subfolder=None, tgz_needed=False, zip_content_needed=False):
    item_filename = _write_test_file(zip_content_needed)
    tar_file = _write_tar_file(item_filename, tgz_needed)
    dest_dir = _do_unpack_task(tar_file, subfolder, tgz_needed, zip_content_needed)

    with open(os.path.join(dest_dir, subfolder if subfolder else "", _TEST_FILE_NAME), "r+") as f:
        lines = f.readlines()
        assert 1 == len(lines)
        assert _TEST_FILE_CONTENT == lines[0]


# create a file with test content
def _write_test_file(zip_content_needed):
    filename = _TEST_FILE_NAME
    with open(filename, "w+") as f:
        f.write(_TEST_FILE_CONTENT)
    if zip_content_needed:
        new_filename = filename + ".gz"
        with zipfile.ZipFile(new_filename, 'w') as zip:
            zip.write(filename)
        filename = new_filename
    return filename


def _write_tar_file(item_filename, tgz_needed):
    tar_file = _TEST_FILE_NAME + ".tar"
    with tarfile.open(tar_file, "w:gz" if tgz_needed else "w") as tar:
        tar.add(item_filename, arcname=item_filename)
    return tar_file


def _do_unpack_task(tar_file, subfolder, tgz_needed, zip_content_needed):
    task = UnpackFileTask(subfolder=subfolder, tar_zipped=tgz_needed, content_zipped=zip_content_needed)

    dest_dir = "dest_testdir"
    archive_resource = DirResource('archive', tar_file)
    archive_resource._working_dir = os.getcwd()
    archive_resource.version = Version(
        properties={'file_list': [{"name": _TEST_FILE_NAME + ".tar", "url": "test_url"}]})
    unpacked_resource = DirResource('unpacked', dest_dir)
    unpacked_resource._working_dir = dest_dir
    task(archive=archive_resource, unpacked=unpacked_resource)
    return dest_dir
