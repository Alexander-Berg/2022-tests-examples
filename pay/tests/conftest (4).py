
import os

import pytest


@pytest.fixture
def tpl_filename(tmpdir):
    tpl_file = tmpdir.join('tpl_file')
    return os.path.join(tpl_file.dirname, tpl_file.basename)


@pytest.fixture
def tpl_data_filename(tmpdir):
    tpl_data_file = tmpdir.join('tpl_data_file')
    return os.path.join(tpl_data_file.dirname, tpl_data_file.basename)


@pytest.fixture
def attachments_archive_filename(tmpdir):
    attachments_archive_file = tmpdir.join('attachments_archive')
    return os.path.join(attachments_archive_file.dirname, attachments_archive_file.basename)


@pytest.fixture
def attachments_markup_filename(tmpdir):
    attachments_markup_file = tmpdir.join('attachments_markup')
    return os.path.join(attachments_markup_file.dirname, attachments_markup_file.basename)


@pytest.fixture
def letter_data_filename(tmpdir):
    letter_data_file = tmpdir.join('letter_data_file')
    return os.path.join(letter_data_file.dirname, letter_data_file.basename)
