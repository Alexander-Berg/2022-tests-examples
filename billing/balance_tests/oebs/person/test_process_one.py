# -*- coding: utf-8 -*-
import mock
import pytest

from balance.queue_processor import QueueProcessor
from tests.balance_tests.oebs.common import check_export_obj
from tests.balance_tests.oebs.conftest import create_person


@pytest.mark.parametrize('new_oebs_export', [
    {},
    {'Person': 1}])
def test_use_oebs_api(session, firm, new_oebs_export):
    person = create_person(session, country=firm.country)
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {}
    session.clear_cache()
    session.flush()
    export_obj = person.exports['OEBS']
    assert not person.exports.get('OEBS_API')
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = new_oebs_export
    mock_handle_person = mock.patch('balance.processors.oebs.person.handle_person',
                                    return_value='success_message')
    with mock_handle_person as handle_person:
        QueueProcessor('OEBS').process_one(export_obj)
    session.flush()
    session.expire_all()
    if new_oebs_export:
        assert handle_person.call_count == 0
        check_export_obj(export_obj,
                         state=1,
                         output='Person {} will be exported with OEBS_API instead'.format(person.id),
                         error=None,
                         input=None,
                         rate=0,
                         next_export=None)
        check_export_obj(person.exports['OEBS_API'],
                         state=0,
                         output=None,
                         error=None,
                         input=None,
                         rate=0,
                         next_export=None)
    else:
        assert handle_person.call_count == 1
        assert not person.exports.get('OEBS_API')
        check_export_obj(export_obj,
                         state=1,
                         output='success_message',
                         error=None,
                         input=None,
                         rate=0,
                         next_export=None)


@pytest.mark.parametrize('is_partner_config', [0, 1])
@pytest.mark.parametrize('is_partner_person', [0, 1])
def test_use_oebs_api_partner_filter(session, firm, is_partner_config, is_partner_person):
    person = create_person(session, country=firm.country, is_partner=is_partner_person)
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Person': 1}
    session.clear_cache()
    session.flush()
    export_obj = person.exports['OEBS']
    assert not person.exports.get('OEBS_API')
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Person': {'is_partner': is_partner_config,
                                                                               'pct': 100}}
    mock_handle_person = mock.patch('balance.processors.oebs.person.handle_person',
                                    return_value='success_message')
    with mock_handle_person as handle_person:
        QueueProcessor('OEBS').process_one(export_obj)
    session.flush()
    session.expire_all()
    if is_partner_person == is_partner_config:
        assert handle_person.call_count == 0
        check_export_obj(export_obj,
                         state=1,
                         output='Person {} will be exported with OEBS_API instead'.format(person.id),
                         error=None,
                         input=None,
                         rate=0,
                         next_export=None)
        check_export_obj(person.exports['OEBS_API'],
                         state=0,
                         output=None,
                         error=None,
                         input=None,
                         rate=0,
                         next_export=None)
    else:
        assert handle_person.call_count == 1
        assert not person.exports.get('OEBS_API')
        check_export_obj(export_obj,
                         state=1,
                         output='success_message',
                         error=None,
                         input=None,
                         rate=0,
                         next_export=None)
