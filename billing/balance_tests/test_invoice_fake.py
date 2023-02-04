from balance.actions.invoice_fake import date_checker
from tests import object_builder as ob
import datetime


def test_errors_is_signed(session):
    contract = ob.ContractBuilder(**{'is_signed': None}).build(session).obj
    try:
        date_checker(contract, datetime.datetime.now())
    except Exception as exc_info:
        assert exc_info.msg == u'Invalid parameter for function: No current state for contract {}'.format(contract.id)
        assert exc_info.error_booster_msg == u'Invalid parameter for function: No current state for contract %s'
