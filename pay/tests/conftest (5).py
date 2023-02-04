
import os
import smtplib
from unittest import mock

import pytest


@pytest.fixture(scope='module')
def smtp_options():
    return {'SMTP_HOST': '', 'SMTP_PORT': '', 'SMTP_USER': '', 'SMTP_PASSWORD': '', 'SMTP_MODE': ''}


@pytest.fixture(scope='module')
def session(smtp_options):
    if smtp_options.get('SMTP_MODE', None) == 'SSL':
        target = 'smtplib.SMTP_SSL'
    else:
        target = 'smtplib.SMTP'

    with mock.patch(target):
        if smtp_options.get('SMTP_MODE', None) == 'SSL':
            classname = smtplib.SMTP_SSL
        else:
            classname = smtplib.SMTP
        session = classname(host=smtp_options['SMTP_HOST'], port=smtp_options['SMTP_PORT'])
        yield session


@pytest.fixture
def tpl_filename(tmpdir):
    tpl_file = tmpdir.join('tpl_file')
    return os.path.join(tpl_file.dirname, tpl_file.basename)


@pytest.fixture
def tpl_data_filename(tmpdir):
    tpl_data_file = tmpdir.join('tpl_data_file')
    return os.path.join(tpl_data_file.dirname, tpl_data_file.basename)


@pytest.fixture
def report_filename(tmpdir):
    report_file = tmpdir.join('report_file')
    return os.path.join(report_file.dirname, report_file.basename)


@pytest.fixture
def unhandled_data_filename(tmpdir):
    unhandled_data_file = tmpdir.join('unhandled_data_file')
    return os.path.join(unhandled_data_file.dirname, unhandled_data_file.basename)
