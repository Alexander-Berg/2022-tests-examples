from datetime import datetime

import pytest

from bcl.banks.registry import Alfa
from bcl.core.models import states, StatementRegister, StatementPayment

if False:
    from typing import *  # noqa


@pytest.fixture
def patch_onec_creator(monkeypatch):
    """Позволяет задать результат AlfaPaymentCreatorOneC.can_create_bundle()."""

    def patch_onec_creator_(*, can_create_bundle=False):
        creator = Alfa.payment_dispatcher.creators[0]
        assert creator.__name__ == 'AlfaPaymentCreatorOneC'
        monkeypatch.setattr(creator, 'can_create_bundle', lambda bundle: can_create_bundle)

    return patch_onec_creator_


def test_parse_status_file(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'alfa_statuses.xls', Alfa, '40702810826020000940', 'RUB'

    )[0]  # type: StatementRegister, List[StatementPayment]

    assert register.is_valid
    assert len(payments) == 6
    assert payments[0].number == '125917'


def test_parse_status_file_compat(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'alfa_statuses_xlsx_like.xls', Alfa, '40702810801850001181', 'RUB'

    )[0]  # type: StatementRegister, List[StatementPayment]

    assert register.is_valid
    assert len(payments) == 1
    assert payments[0].number == '3'


def test_payment_creator_once(patch_onec_creator, build_payment_bundle):
    # Проверка создания пакетов в формте 1С, которая должна происходить по
    # выходным и праздничным дням.
    patch_onec_creator(can_create_bundle=True)
    associate = Alfa

    filename, contents = build_payment_bundle(associate, account={'number': 'one'}).file_tuple

    assert b'1CClientBankExchange' in contents


def test_all_statuses(parse_statement_fixture, get_source_payment, get_payment_bundle):
    declined_pays = ['64', '63', '62', '33', '35', '36', '38']
    processing_pays = ['34', '39', '40', '41', '42', '43', '44', '45', '46', '47', '48', '49', '50', '51']

    payments_with_status = dict()
    acc_num = '40702810801850001181'
    associate = Alfa

    args = {
        'number': '10',
        'f_acc': acc_num,
        'summ': '11.21',
        'date': datetime.now().strftime('%Y-%m-%d'),
        'ground': 'Payment purpose'
    }
    for payment_num in declined_pays + processing_pays:
        args.update({'number': payment_num})
        payments_with_status[
            get_source_payment(args, associate=associate)] = states.DECLINED_BY_BANK \
            if payment_num in declined_pays else states.PROCESSING

    args.update({
        'number': '37',
    })
    payments_with_status[get_source_payment(args, associate=associate)] = states.COMPLETE

    args.update({
        'number': '52',
    })
    pay52 = get_source_payment(args, associate=associate)

    args.update({
        'number': '32',
    })
    pay32 = get_source_payment(args, associate=associate)
    get_payment_bundle(list(payments_with_status.keys()) + [pay52, pay32])

    # Выписка на веерные платежи.
    register, payments = parse_statement_fixture('all_statuses_alfa.xls', associate, acc_num, 'RUB')[0]

    assert register.intraday
    assert register.is_valid
    assert len(payments) == 24
    for payment in payments_with_status.keys():
        payment.refresh_from_db()
        assert payment.status == payments_with_status[payment]
    pay52.refresh_from_db()
    assert pay52.status == states.PROCESSING

    pay32.refresh_from_db()
    assert pay32.status == states.DECLINED_BY_BANK

    # теперь проверяем, что платежи проводятся по дневной выписке
    register, payments = parse_statement_fixture(
        'alfa_statement.txt', associate, acc_num, 'RUB')[0]

    assert not register.intraday
    assert register.is_valid
    assert len(payments) == 2

    pay52.refresh_from_db()
    assert pay52.is_complete
    pay32.refresh_from_db()
    assert pay32.is_complete


def test_split_tax_with_other(get_payment_bundle, get_source_payment):
    pay_n = get_source_payment({'n_kbk': '18210301000011000110'}, associate=Alfa)
    pay = get_source_payment(associate=Alfa)
    bundles = Alfa.payment_dispatcher.partition_payments([pay_n, pay])
    assert len(bundles) == 2
