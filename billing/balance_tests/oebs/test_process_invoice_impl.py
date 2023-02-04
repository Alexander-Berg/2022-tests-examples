# -*- coding: utf-8 -*-
import mock
import pytest

from balance import exc, constants as cst
from balance.processors.oebs import process_invoice_impl
from tests.balance_tests.oebs.common import patch_oebs


@pytest.mark.parametrize('w_cached_firm', [True, False])
def test_base_defer(session, invoice, w_cached_firm):
    """Если в процессе выгрузки счета выяснилось, что плательщик еще не был выгружен в ОЕБС (находится в процессе
     выгрузки), откладываем экспорт счета"""
    if w_cached_firm:
        invoice.person.firms = [invoice.firm]
    else:
        invoice.person.firms = []
    session.flush()
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = 1
    _, patch_customer_dao, patch_transaction_dao, _ = patch_oebs()

    assert invoice.person.exports['OEBS'].state == 0
    with patch_customer_dao, patch_transaction_dao, pytest.raises(exc.DEFERRED_ERROR)as exc_info:
        process_invoice_impl(invoice, invoice.firm, 1, 2)

    assert exc_info.value.msg == 'Export of {} has been deferred because its person (id = {}) is not in OEBS.' \
        .format(invoice, invoice.person.id)

    assert invoice.person.firms == [invoice.firm]


@pytest.mark.parametrize('export_state', [cst.ExportState.exported,
                                          cst.ExportState.failed,
                                          666])
def test_person_is_not_exported_is_not_in_process_of_exporting(session, invoice, export_state):
    """Если в процессе выгрузки счета выяснилось, что плательщик не был выгружен в ОЕБС
    (не проставлена oebs_export_dt) и плательщик не находится в процессе выгрузки (помечен как успешно выгруженный
    или сфейлившийся в t_export), проставляем плательщика в очередь, откладываем экспорт счета"""
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = 1
    _, patch_customer_dao, patch_transaction_dao, _ = patch_oebs()

    invoice.person.exports['OEBS'].state = export_state
    with patch_customer_dao, patch_transaction_dao, pytest.raises((exc.DEFERRED_ERROR, exc.CRITICAL_ERROR))as exc_info:
        process_invoice_impl(invoice, invoice.firm, 1, 2)

    if export_state == cst.ExportState.exported:
        assert exc_info.value.msg == 'Export of {} has been deferred because its person (id = {}) is not in OEBS.' \
            .format(invoice, invoice.person.id)
    else:
        assert exc_info.value.msg == 'Export of {} has been failed because export of' \
                                     ' its person (id = {}) has failed or has unknown state.' \
            .format(invoice, invoice.person.id)


def test_person_is_not_in_export_queue(session, invoice):
    """Если в процессе выгрузки счета выяснилось, что плательщик никогда не эспортировался (нет строки в t_export),
     откладываем экспорт счета, проставляем плательщика в очередь"""
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = 1
    _, patch_customer_dao, patch_transaction_dao, _ = patch_oebs()

    del invoice.person.exports['OEBS']
    session.flush()
    with patch_customer_dao, patch_transaction_dao, pytest.raises(exc.DEFERRED_ERROR)as exc_info:
        process_invoice_impl(invoice, invoice.firm, 1, 2)

    session.expire_all()
    assert invoice.person.exports['OEBS'].state == 0
    assert exc_info.value.msg == 'Export of {} has been deferred because its person (id = {}) is not in OEBS.' \
        .format(invoice, invoice.person.id)


@pytest.mark.parametrize('person_id_in_oebs', [122, None])
def test_base_already_exported(session, invoice, person_id_in_oebs):
    """Если плательщик уже был выгружен в оебс (проставлена oebs_export_dt), ходим в ОЕБС,
    чтобы забрать их идентификатор плательщика.
    Если плательщик нашелся, выгружаем счет. Иначе - вызываем исключение, но не выгружаем плательщика в ОЕБС.
    """
    session.config.__dict__['USE_FIRMS_FROM_PERSON_FIRM_CACHE'] = 1
    _, patch_customer_dao, patch_transaction_dao, _ = patch_oebs()
    patch_handle_person = mock.patch('balance.processors.oebs.person.handle_person')
    invoice.person.person_firms[0].oebs_export_dt = session.now()

    with patch_customer_dao as customer_dao, patch_transaction_dao, patch_handle_person as handle_person:
        dao_instance = mock.MagicMock()
        dao_instance.get_cust_account_ex.return_value = person_id_in_oebs
        customer_dao.return_value = dao_instance
        if person_id_in_oebs:
            process_invoice_impl(invoice, invoice.firm, 1, 2)
        else:
            with pytest.raises(ValueError) as exc_info:
                process_invoice_impl(invoice, invoice.firm, 1, 2)
            assert exc_info.value.message == "get_cust_account_ex('P{}') failed".format(invoice.person.id)
        assert handle_person.call_count == 0
