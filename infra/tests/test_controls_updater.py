import copy
import logging

import mock
import pytest
from sepelib.core import config as appconfig

from awacs.model import controls_updater
from awtest import check_log


@pytest.fixture(autouse=True)
def config():
    run_section = copy.deepcopy(appconfig.get_value(u'run'))
    yield
    appconfig.set_value(u'run', run_section)


def test_basics(caplog):
    caplog.set_level(logging.DEBUG)
    appconfig.set_value(u'run', {
        u'auth': False,
        u'root_users': [u'xxx'],
    })

    c = controls_updater.ControlsUpdater(controls_dir=u'./controls')

    with check_log(caplog) as log:
        with mock.patch.object(c, u'_find_and_read_run_section_config',
                               return_value=(False, {})):
            c.process()
    assert u'has been changed' not in log.records_text()

    assert not appconfig.get_value(u'run.auth')
    with check_log(caplog) as log:
        with mock.patch.object(c, u'_find_and_read_run_section_config',
                               return_value=(True, {u'auth': True})):
            c.process()
    assert u'has been changed' in log.records_text()
    assert u'changed 1 run section values' in log.records_text()
    assert appconfig.get_value(u'run.auth')

    assert appconfig.get_value(u'run.root_users') == [u'xxx']
    with check_log(caplog) as log:
        with mock.patch.object(c, u'_find_and_read_run_section_config',
                               return_value=(True, {u'root_users': [u'yyy']})):
            c.process()
    assert u'has been changed' in log.records_text()
    assert u'changed 2 run section values' in log.records_text()
    assert appconfig.get_value(u'run.root_users') == [u'yyy']

    with check_log(caplog) as log:
        with mock.patch.object(c, u'_find_and_read_run_section_config',
                               return_value=(True, {u'root_users': [u'yyy']})):
            c.process()
    assert u'has been changed' not in log.records_text()

    with check_log(caplog) as log:
        with mock.patch.object(c, u'_find_and_read_run_section_config',
                               side_effect=ValueError(u'kek')):
            c.process()
    assert u'unexpected exception' in log.records_text()
    assert appconfig.get_value(u'run.root_users') == [u'yyy']

    with check_log(caplog) as log:
        with mock.patch.object(c, u'_find_and_read_run_section_config',
                               return_value=(True, {})):
            c.process()
    assert appconfig.get_value(u'run.root_users') == [u'xxx']

    with check_log(caplog) as log:
        with mock.patch.object(c, u'_find_and_read_run_section_config',
                               return_value=(True, {u'new_option': 31337})):
            c.process()
    assert appconfig.get_value(u'run.new_option') == 31337

    with check_log(caplog) as log:
        with mock.patch.object(c, u'_find_and_read_run_section_config',
                               return_value=(None, {})):
            c.process()
    assert u'has been changed' in log.records_text()
    assert u'changed 1 run section values' in log.records_text()
    assert appconfig.get_value(u'run.new_option', default=None) is None
