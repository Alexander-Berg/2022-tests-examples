# -*- coding: utf-8 -*-

import hamcrest
import mock
import pytest

from balance.processors.oebs.person import handle_person
from tests.balance_tests.oebs.conftest import create_firm


@pytest.mark.parametrize('process_person_result', ['success', None])
def test_handle_person_w_firm_oebs_export_dt(session, person, process_person_result, use_cache_cfg):
    """
    При форсированной выгрузке плательщика с указанием фирмы, заполняем oebs_export_dt
    в person_firms, если выгрузка была успешной.
    """
    cached_firm = create_firm(session)
    person.firms = [cached_firm]
    session.flush()

    patch_process_person_by_firm = mock.patch('balance.processors.oebs.person.process_person_by_firm',
                                              return_value=process_person_result)
    patch_get_firm_cursor = mock.patch('balance.processors.oebs.person.get_firm_cursor')
    with patch_get_firm_cursor as patch_cursor, patch_process_person_by_firm:
        patch_cursor.return_value.__enter__.return_value = [1, 2]
        handle_person(person, cached_firm)
        session.flush()
    if process_person_result:
        assert person.person_firms[0].oebs_export_dt
    else:
        assert person.person_firms[0].oebs_export_dt is None


@pytest.mark.parametrize('is_exported', [None, 123])
def test_handle_person_w_firm(session, person, is_exported, firm, use_cache_cfg):
    """
    Выгрузка плательщика, вызываемая при выгрузке счета/договора.
    Выгружаем плательщика вне зависимости от того, был ли он ранее выгружен в эту фирму.
    """
    patch_customer_dao = mock.patch('balance.processors.oebs.dao.customer.CustomerDao')
    dao_instance = mock.MagicMock()
    dao_instance.get_cust_account_ex.return_value = is_exported
    patch_customer_dao.return_value = dao_instance

    patch_get_firm_cursor = mock.patch('balance.processors.oebs.person.get_firm_cursor')

    patch_create_person = mock.patch('balance.processors.oebs.person.create_{}_person'.format(person.type), create=True)
    with patch_get_firm_cursor as patch_cursor, patch_customer_dao, patch_create_person:
        patch_cursor.return_value.__enter__.return_value = [1, 2]
        assert handle_person(person, firm) == u'%s (%s)' % (firm.title, firm.id)
        session.flush()


@pytest.mark.parametrize('is_exported', [None, 123])
def test_handle_person_w_firm_wo_cached(session, person, is_exported, firm):
    """
    Выгрузка плательщика, вызываемая при выгрузке счета/договора.
    Выгружаем плательщика вне зависимости от того, был ли он ранее выгружен в эту фирму.
    """
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = 1

    patch_customer_dao = mock.patch('balance.processors.oebs.dao.customer.CustomerDao')
    dao_instance = mock.MagicMock()
    dao_instance.get_cust_account_ex.return_value = is_exported
    patch_customer_dao.return_value = dao_instance

    patch_get_firm_cursor = mock.patch('balance.processors.oebs.person.get_firm_cursor')

    patch_create_person = mock.patch('balance.processors.oebs.person.create_{}_person'.format(person.type), create=True)
    with patch_get_firm_cursor as patch_cursor, patch_customer_dao, patch_create_person:
        patch_cursor.return_value.__enter__.return_value = [1, 2]
        handle_person(person, firm)
        session.flush()
    assert len(person.person_firms) == 1
    assert person.person_firms[0].oebs_export_dt
    assert person.person_firms[0].firm == firm


def test_success(session, person, use_cache_cfg):
    firm = create_firm(session, country=person.person_category.country)

    cached_firm = create_firm(session)
    person.firms = [cached_firm]

    session.flush()

    patch_get_firm_cursor = mock.patch('balance.processors.oebs.person.get_firm_cursor')
    patch_process_person_by_firm = mock.patch('balance.processors.oebs.person.process_person_by_firm',
                                              return_value='success_message')
    with patch_get_firm_cursor as patch_cursor, patch_process_person_by_firm as patch_process:
        patch_cursor.return_value.__enter__.return_value = [1, 2]
        assert handle_person(person) == 'success_message'
        assert patch_cursor.call_count == 1
        assert patch_process.call_count == 1
        if use_cache_cfg:
            assert patch_cursor.call_args == mock.call(cached_firm)
            assert patch_process.call_args[0][1] == cached_firm
        else:
            assert patch_cursor.call_args == mock.call(firm)
            assert patch_process.call_args[0][1] == firm


def test_fail(session, person, use_cache_cfg):
    firm = create_firm(session, country=person.person_category.country)

    cached_firm = create_firm(session)
    person.firms = [cached_firm]

    session.flush()

    patch_get_firm_cursor = mock.patch('balance.processors.oebs.person.get_firm_cursor')
    patch_process_person_by_firm = mock.patch('balance.processors.oebs.person.process_person_by_firm',
                                              return_value=None)
    with patch_get_firm_cursor as patch_cursor, patch_process_person_by_firm as patch_process:
        patch_cursor.return_value.__enter__.return_value = [1, 2]
        assert handle_person(person) == u'No firms'
        assert patch_cursor.call_count == 1
        assert patch_process.call_count == 1


def test_person_wo_firm(session, person):
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = 1
    firm = create_firm(session, person.person_category.country)

    patch_get_firm_cursor = mock.patch('balance.processors.oebs.person.get_firm_cursor')
    patch_process_person_by_firm = mock.patch('balance.processors.oebs.person.process_person_by_firm',
                                              return_value=None)
    with patch_get_firm_cursor as patch_cursor, patch_process_person_by_firm as patch_process:
        patch_cursor.return_value.__enter__.return_value = [1, 2]
        assert handle_person(person) == u'No firms'
        assert patch_cursor.call_count == 0
        assert patch_process.call_count == 0


def test_partial_export(session, person):
    """При выгрузке плательщика выгрузка запускается для каждой из закешированных фирм.
    В случае успешной выгрузки,у фирмы проставляется oebs_export_dt"""
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = 1

    firms = [create_firm(session) for _ in range(2)]
    person.firms = firms

    session.flush()

    def return_message(*args, **kwargs):
        if args[1] == firms[0]:
            return None
        else:
            return 'another_message'

    patch_get_firm_cursor = mock.patch('balance.processors.oebs.person.get_firm_cursor')
    patch_process_person_by_firm = mock.patch('balance.processors.oebs.person.process_person_by_firm',
                                              side_effect=return_message)
    with patch_get_firm_cursor as patch_cursor, patch_process_person_by_firm as patch_process:
        patch_cursor.return_value.__enter__.return_value = [1, 2]

        assert handle_person(person) == 'another_message'

    hamcrest.assert_that(
        person.person_firms,
        hamcrest.contains_inanyorder(
            hamcrest.has_properties(
                person=person,
                firm=firms[0],
                oebs_export_dt=None
            ),
            hamcrest.has_properties(
                person=person,
                firm=firms[1],
                oebs_export_dt=hamcrest.is_not(None)
            )
        )
    )
    assert patch_cursor.call_count == 2
    assert patch_process.call_count == 2
