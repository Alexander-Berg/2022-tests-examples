import re
import subprocess

import pytest

from hamcrest import assert_that, equal_to, has_length, not_

from billing.yandex_pay.tools.fim.lib.common import (
    checksums_to_dict, dict_to_checksums, get_dir_digest, get_file_digest, get_location_digest
)
from billing.yandex_pay.tools.fim.lib.exceptions import (
    BadChecksumsFileError, ChecksumCalculationError, LocationNotFoundError, UnsupportedLocationError
)

EXPECTED_DIR_SHA = 'cdd9b33d0e1bcaec51ff71fb7c4524b410e86155923e0e2adec37e7ac00a09bf'
EXPECTED_EMPTY_SHA = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'


def test_get_file_digest(dummy_files_dict, dummy_dir):
    for path in dummy_dir.iterdir():
        expected = dummy_files_dict[path.name][1]
        assert_that(get_file_digest(path), equal_to(expected))


def test_get_digest_file_empty(tmp_path):
    empty = tmp_path / 'empty'
    empty.touch()
    assert_that(get_file_digest(empty), equal_to(EXPECTED_EMPTY_SHA))


def test_get_digest_file_missing(dummy_dir):
    with pytest.raises(subprocess.CalledProcessError):
        get_file_digest(dummy_dir / 'missing')


def test_get_dir_digest(dummy_dir):
    assert_that(get_dir_digest(dummy_dir), equal_to(EXPECTED_DIR_SHA))


def test_get_digest_dir_empty(tmp_path):
    empty = tmp_path / 'empty'
    empty.mkdir()
    assert_that(get_dir_digest(empty), equal_to(EXPECTED_EMPTY_SHA))


def test_get_digest_dir_missing(dummy_dir):
    with pytest.raises(FileNotFoundError):
        get_dir_digest(dummy_dir / 'missing')


def test_get_digest_command_fails(dummy_dir, mocker):
    mocker.patch('billing.yandex_pay.tools.fim.lib.common._COMMANDS', (['false'],))

    pattern = f"Getting digest for {str(dummy_dir)} failed with a code 1 and stderr: b''"
    with pytest.raises(ChecksumCalculationError, match=re.escape(pattern)):
        get_dir_digest(dummy_dir)


def test_get_location_digest(tmp_path, dummy_dir, dummy_files_dict):
    for path in dummy_dir.iterdir():
        expected = dummy_files_dict[path.name][1]
        assert_that(get_location_digest(path), equal_to(expected))

    assert_that(get_location_digest(dummy_dir), equal_to(EXPECTED_DIR_SHA))
    # although it contains only the same files as dummy_dir,
    # checksums shouldn't match because of the path differences
    # (as we use relative paths from the directory to such files
    # to calculate the aggregate hash)
    assert_that(get_location_digest(tmp_path), not_(equal_to(EXPECTED_DIR_SHA)))


def test_get_bad_location_digest(tmp_path, mocker):
    missing = tmp_path / 'missing'
    with pytest.raises(LocationNotFoundError):
        get_location_digest(missing)

    mocker.patch('pathlib.Path.exists', return_value=True)
    with pytest.raises(UnsupportedLocationError):
        get_location_digest(missing)


def test_checksum_conversion(dummy_files_dict, dummy_dir):
    checksum_dict = {
        path: dummy_files_dict[path.name][1]
        for path in dummy_dir.iterdir()
    }

    converted = dict_to_checksums(checksum_dict)
    assert_that(converted.split(';'), has_length(len(dummy_files_dict)))

    deconverted = checksums_to_dict(converted)
    assert_that(deconverted, equal_to(checksum_dict))


@pytest.mark.parametrize(
    'checksums,error',
    (
        ['', 'Checksums must not be empty'],
        ['foo=bar=baz', 'Bad checksum pair'],
        ['foo', 'Bad checksum pair'],
        ['foo=bar;foo=baz', 'Duplicate paths in checksums are not allowed'],
    ),
)
def test_bad_checksums_file(checksums, error):
    with pytest.raises(BadChecksumsFileError, match=error):
        checksums_to_dict(checksums)
