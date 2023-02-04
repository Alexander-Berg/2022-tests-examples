from butils.application import getApplication
from hamcrest import assert_that, contains_exactly
import pytest
from mock import patch

from yb_darkspirit import interface
from yb_darkspirit.core import fs_replacement
from yb_darkspirit.scheme import CashRegister, FiscalStorage, Document
from yb_darkspirit.core.cash_register.state_checks import Checks

SERIAL_NUMBER = '12345'


@pytest.mark.parametrize('cr_state,fs_state,has_gaps,failed_checks', [
    ('POSTFISCAL', 'ARCHIVE_READING', False, []),
    ('NONCONFIGURED', 'READY_FISCAL', False, [Checks.POSTFISCAL, Checks.FISCAL_STORAGE_ARCHIVE_READING]),
    ('POSTFISCAL', 'ARCHIVE_READING', True, [Checks.HAS_NO_GAPS]),
])
def test_postfiscal_cash_has_no_blockers(session, cr_state, fs_state, has_gaps, failed_checks):
    _prepare_cash_register(session, cr_state, fs_state, has_gaps)
    with patch.object(interface.CashRegister, 'sync'):
        assert_that(
            [c.check for c in fs_replacement._device_state_blockers(getApplication(), SERIAL_NUMBER)],
            contains_exactly(*failed_checks),
        )


def _prepare_cash_register(session, cash_register_state, fs_state, has_gaps):
    # type: (Session, str, str, bool) -> None
    fiscal_storage = FiscalStorage(serial_number=SERIAL_NUMBER, state=fs_state, type_code='fn-1',
                                   last_document_number=(2 if has_gaps else 0))
    session.add(fiscal_storage)
    session.flush()
    cash_register = CashRegister(serial_number=SERIAL_NUMBER, state=cash_register_state, fiscal_storage_id=fiscal_storage.id)
    session.add(cash_register)
    if has_gaps:
        document = Document(fiscal_storage_id=fiscal_storage.id, fiscal_storage_number=1)
        session.add(document)
    session.flush()
