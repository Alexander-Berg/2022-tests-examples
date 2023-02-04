# -*- coding: utf-8 -*-
import mock
import pytest

from balance.processors.oebs.person import process_person_by_firm


@pytest.mark.parametrize('is_exported', [None, 123])
@mock.patch('balance.processors.oebs.dao.customer.CustomerDao')
def test_export_itself(mock_customer_dao, session, person, firm, is_exported, use_cache_cfg):
    """При прямой выгрузке грузим плательщика только в те фирмы, в которые он был выгружен при экспорте счета/договора
    (get_cust_account_ex вернула идентификатор). При выгрузке плательщика с закешированными фирмами
     всегда выгружаем плательщика в переданную фирму.
     """
    dao_instance = mock.MagicMock()
    dao_instance.get_cust_account_ex.return_value = is_exported
    mock_customer_dao.return_value = dao_instance
    with mock.patch('balance.processors.oebs.person.create_{}_person'.format(person.type), create=True):
        if is_exported or use_cache_cfg:
            assert process_person_by_firm(person, firm, 1, 2) == u'%s (%s)' % (firm.title, firm.id)
            if use_cache_cfg:
                assert dao_instance.get_cust_account_ex.call_count == 0
        else:
            assert process_person_by_firm(person, firm, 1, 2) is None


@pytest.mark.parametrize('firm_from_cache', [0, 1])
@pytest.mark.parametrize('is_exported', [None, 123])
@mock.patch('balance.processors.oebs.dao.customer.CustomerDao')
def test_export_with_contract_or_invoice(mock_customer_dao, session, person, firm, is_exported, firm_from_cache):
    """Выгрузка плательщика, вызываемая при выгрузке счета/договора.
    Выгружаем плательщика вне зависимости от того, был ли он ранее выгружен в эту фирму."""
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = firm_from_cache
    dao_instance = mock.MagicMock()
    dao_instance.get_cust_account_ex.return_value = is_exported
    mock_customer_dao.return_value = dao_instance
    with mock.patch('balance.processors.oebs.person.create_{}_person'.format(person.type), create=True):
        assert process_person_by_firm(person, firm, 1, 2, force=True) == u'%s (%s)' % (firm.title, firm.id)
