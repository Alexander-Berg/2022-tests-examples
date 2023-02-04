import re
from datetime import datetime, timedelta
from decimal import Decimal

import pytest

from bcl.banks.common.swift_mx import Statuses
from bcl.banks.party_unicredit import UnicreditPaymentCreator
from bcl.banks.registry import Unicredit
from bcl.core.models import PaymentsBundle, Currency, COMPLETE, PROCESSING, BCL_INVALIDATED, ERROR, EXPORTED_H2H, \
    FOR_DELIVERY, ACCEPTED_H2H, DECLINED_BY_BANK, NEW
from bcl.core.tasks import process_bundles
from bcl.exceptions import ConfigurationError, ValidationError, UserHandledException

SWIFT_DATA = {
    'DEUTGB2LXXX': {
        'countryCode': 'GB', 'instName': 'DEUTSCHE BANK AG',
        'addr': 'LONDON EC2N 2DB WINCHESTER HOUSE 1 GREAT WINCHESTER STREET', 'city': 'LONDON',
        'street': 'WINCHESTER HOUSE'
    },
}


def test_autosigning(
        compose_bundles, get_source_payment_mass, dss_signing_right, time_shift, mock_signer, read_fixture,
        monkeypatch, mocker):

    payments = get_source_payment_mass(3, Unicredit)
    compose_bundles(Unicredit, payment_ids=[pay.id for pay in payments])

    bundle = PaymentsBundle.objects.all().first()

    process_bundles(None)
    bundle.refresh_from_db()

    assert 'Отсутствует право на автоматическую подпись уровня 1' in bundle.processing_notes

    class Signature:

        serial = '124d455d1500d780e811c9255da2cff0'

        def as_bytes(self):
            return b''

    monkeypatch.setattr(
        'bcl.toolbox.signatures.XmlSignature.from_signed', lambda *args, **kwargs: Signature())

    monkeypatch.setattr(
        'bcl.banks.party_unicredit.payment_sender.UnicreditSftpConnector.get_client', mocker.Mock())

    bundle.account.org.connection_id = 'ya'

    def init_rights(username, level, org=None):
        right = dss_signing_right(associate=Unicredit, username=username, org=org)
        right.level = level
        right.autosigning = True
        right.save()

    init_rights('user1', 1)
    init_rights('user2', 2)

    mock_signer(None, '124d455d1500d780e811c9255da2cff0')

    with time_shift(100):
        process_bundles(None)

    bundle.refresh_from_db()

    assert bundle.status == EXPORTED_H2H

    init_rights('user3', 1, org=bundle.account.org)
    bundle.status = FOR_DELIVERY
    bundle.bundlesign_set.all().delete()
    bundle.save()
    mock_signer(None, '124d455d1500d780e811c9255da2cff0')

    with time_shift(100):
        process_bundles(None)

    bundle.refresh_from_db()

    assert bundle.status == EXPORTED_H2H
    assert list(bundle.bundlesign_set.filter(level=1))[0].user.username == 'user3'

    # Проверка неподдерживамого алгоритма.
    bundle.status = FOR_DELIVERY
    bundle.bundlesign_set.all().delete()
    bundle.save()
    mock_signer(None, '124d455d1500d780e811c9255da2cff0', algo='10.20.30.xxx')

    with time_shift(200):
        process_bundles(None)
    bundle.refresh_from_db()

    assert bundle.status == ERROR
    assert "Неизвестный алгоритм '10.20.30.xxx'." in bundle.processing_notes


def test_payment_creator(get_source_payment, get_payment_bundle, read_fixture, mocker):

    creator = Unicredit.payment_dispatcher.get_creator(get_payment_bundle([
        get_source_payment({
            'ground': 'some&some' + ('0' * 290),
        })
    ]))
    compiled = creator.create_bundle()

    expected = read_fixture('unicredit_correct_payment.xml').decode('cp1251').replace(
        '2019-07-17', datetime.now().strftime('%Y-%m-%d')
    )
    compiled = re.sub(r'<MsgId>?.*</MsgId>', '', compiled).strip()
    assert re.sub(r'<CreDtTm>?.*</CreDtTm>', '', compiled) == expected

    payment_wo_ground = get_source_payment({
            'ground': '',
        })

    with pytest.raises(ValidationError):
        Unicredit.payment_dispatcher.get_creator(get_payment_bundle([
            payment_wo_ground
        ])).create_bundle()

    payment_wo_ground.refresh_from_db()
    assert payment_wo_ground.status == BCL_INVALIDATED

    payment_wo_ground.set_status(NEW)

    payment_inv_i_swift = get_source_payment({'urgent': True, 'i_swiftcode': 'XXXZZZYY'})

    creator = Unicredit.payment_dispatcher.get_creator(get_payment_bundle([
        get_source_payment({
            'urgent': True,
            'i_swiftcode': 'DEUTGB2LXXX',
        }),
        payment_inv_i_swift,
        payment_wo_ground
    ]))

    mocker.patch('bcl.toolbox.utils.Swift.get_bic_info', lambda swiftcode: SWIFT_DATA.get(swiftcode, {}))
    compiled = creator.create_bundle()
    assert '<Cd>URGP</Cd>' in compiled
    assert '<IntrmyAgt1><FinInstnId><BIC>DEUTGB2L</BIC><Nm>DEUTSCHE BANK AG</Nm><PstlAdr><Ctry>GB</Ctry>' in compiled

    payment_inv_i_swift.refresh_from_db()
    assert payment_inv_i_swift.status == BCL_INVALIDATED
    assert 'Используется неактивный или несуществующий SWIFT-код' in payment_inv_i_swift.processing_notes

    payment_wo_ground.refresh_from_db()
    assert payment_wo_ground.status == BCL_INVALIDATED
    assert 'Назначение платежа' in payment_wo_ground.processing_notes

    creator = Unicredit.payment_dispatcher.get_creator(
        get_payment_bundle([get_source_payment({'n_okato': '', 'n_kbk': '1'})]))

    with pytest.raises(ValidationError):  # Не заполнено ОКАТО при заполненном КБК.
        creator.create_bundle()

    creator = Unicredit.payment_dispatcher.get_creator(get_payment_bundle([
        get_source_payment({
            'income_type': '3',
        })
    ]))
    compiled = creator.create_bundle()
    assert '<Tp>PTCD</Tp>' in compiled
    assert '<Cd>3</Cd>' in compiled


def test_payments_grouping(get_source_payment):

    def group(payments):
        return UnicreditPaymentCreator.group(payments)

    docdate = datetime(2018, 8, 8)

    payments = [
        get_source_payment({
            'currency_id': Currency.USD,
            'date': docdate,
        }),
        get_source_payment({
            'currency_id': Currency.USD,
            'date': docdate,
        }),
        get_source_payment({
            'currency_id': Currency.USD,
            'date': docdate,
        }),
    ]
    assert len(group(payments)) == 1

    payments[1].currency_id = Currency.RUB
    assert len(group(payments)) == 2

    payments[2].paid_by = 'BEN'
    assert len(group(payments)) == 3

    payments[1].currency_id = Currency.USD
    assert len(group(payments)) == 2


@pytest.mark.parametrize('empty_field', ['t_name', 't_bankname', 't_acc', 't_bic'])
def test_field_validation(empty_field, get_source_payment, get_payment_bundle):
    payment = get_source_payment({empty_field: ''})
    creator = Unicredit.payment_dispatcher.get_creator(get_payment_bundle([payment]))

    with pytest.raises(ValidationError):
        creator.create_bundle()
    payment.refresh_from_db()
    assert payment.status == BCL_INVALIDATED

    payment = get_source_payment({empty_field: ''})
    bundle = get_payment_bundle([payment, get_source_payment()])
    Unicredit.payment_dispatcher.get_creator(bundle).create_bundle()

    bundle = PaymentsBundle.objects.get(id=bundle.id)
    payment.refresh_from_db()

    assert len(bundle.payments) == 1
    assert payment.status == BCL_INVALIDATED
    assert empty_field in payment.processing_notes


def get_tax_xml(payment_parameters):

    def get_t_kpp_xml(t_kpp):
        return f'<Cdtr><TaxTp>{t_kpp}</TaxTp></Cdtr>'if t_kpp else ''

    def get_f_kpp_xml(f_kpp):
        return f'<Dbtr><TaxTp>{f_kpp}</TaxTp></Dbtr>' if f_kpp else ''

    def get_n_kbk_xml(params, n_kbk):
        data = (
            '<AdmstnZn>{n_okato}</AdmstnZn><RefNb>{n_doc_num}</RefNb>{n_doc_date_xml}<Rcrd>'
            '<Tp>{n_type}</Tp><Ctgy>{n_ground}</Ctgy><CtgyDtls>{n_kbk}</CtgyDtls>'
            '<DbtrSts>{n_status}</DbtrSts>{n_period_xml}</Rcrd>'
        )
        return data.format(**params) if n_kbk else ''

    def get_n_doc_date_xml(n_docdate):
        return '<Dt>{}</Dt>'.format(datetime.strptime(n_docdate, '%d-%m-%Y').strftime('%Y-%m-%d')) if n_docdate else ''

    def get_n_period_xml(nalperiod):
        return '<Prd><Yr>2013-01-01</Yr><Tp>MM06</Tp></Prd>' if nalperiod else ''

    TAX_XML = '</Purp><Tax>{t_kpp_xml}{f_kpp_xml}{n_kbk_xml}</Tax><RmtInf>'

    t_kpp_xml = get_t_kpp_xml(payment_parameters.get('t_kpp'))
    f_kpp_xml = get_f_kpp_xml(payment_parameters.get('f_kpp'))
    n_doc_date = payment_parameters.get('n_doc_date')
    n_doc_date_xml = get_n_doc_date_xml(n_doc_date if n_doc_date != '0' else None)
    nalperiod = payment_parameters.get('n_period')
    n_period_xml = get_n_period_xml(nalperiod if nalperiod != '0' else None)

    payment_parameters.update(
        {'n_doc_date_xml': n_doc_date_xml, 'n_period_xml': n_period_xml})
    n_kbk_xml = get_n_kbk_xml(payment_parameters, payment_parameters.get('n_kbk'))
    return TAX_XML.format(t_kpp_xml=t_kpp_xml, f_kpp_xml=f_kpp_xml, n_kbk_xml=n_kbk_xml)


@pytest.mark.parametrize(
    'additional_tax_params',
    [
        {'n_kbk': '1', 'f_kpp': '1', 't_kpp': '1', 'n_doc_date': '11-08-2017', 'n_period': 'МС.06.2013'},
        {'n_kbk': '', 'f_kpp': '1', 't_kpp': '1', 'n_doc_date': '11-08-2017', 'n_period': 'МС.06.2013'},
        {'n_kbk': '1', 'f_kpp': '', 't_kpp': '1', 'n_doc_date': '11-08-2017', 'n_period': 'МС.06.2013'},
        {'n_kbk': '1', 'f_kpp': '1', 't_kpp': '', 'n_doc_date': '11-08-2017', 'n_period': 'МС.06.2013'},

        {'n_kbk': '1', 'f_kpp': '1', 't_kpp': '1', 'n_doc_date': '0', 'n_period': 'МС.06.2013', 'fail': True},
        {'n_kbk': '1', 'f_kpp': '1', 't_kpp': '1', 'n_doc_date': '11-08-2017', 'n_period': '0', 'fail': True},
    ])
def test_tax_payment_validation(additional_tax_params, get_payment_bundle, get_source_payment):
    tax_params = {
        'n_doc_num': '8',
        'n_type': '1',
        'n_ground': '1',
        'n_okato': '11',
        'n_status': '2',
    }
    tax_params.update(additional_tax_params)

    expect_failure = tax_params.pop('fail', False)

    bundle = get_payment_bundle([get_source_payment(tax_params)])
    creator = Unicredit.payment_dispatcher.get_creator(bundle)

    if expect_failure:
        with pytest.raises(ValidationError):
            creator.create_bundle()

    else:
        payment_file = creator.create_bundle()
        assert get_tax_xml(tax_params) in payment_file


statement_params = dict(
    associate=Unicredit,
    acc='40702810600014307627',
    curr='RUB',
    encoding=Unicredit.statement_dispatcher.parsers[0].encoding
)


def test_sender(get_source_payment, get_payment_bundle, monkeypatch, mocker):

    bundle = get_payment_bundle([get_source_payment()])
    Unicredit.payment_dispatcher.get_creator(bundle).create_bundle()

    bundle.refresh_from_db()

    bundle.account.org.connection_id = 'faked'

    with pytest.raises(ConfigurationError):  # Неизвестный псевдоним подключения.
        Unicredit.payment_sender(bundle).prepare_send_sync()

    monkeypatch.setattr(
        'bcl.banks.party_unicredit.payment_sender.UnicreditSftpConnector.get_client', mocker.Mock())

    bundle.account.org.connection_id = 'ya'

    Unicredit.payment_sender(bundle).prepare_send_sync()

    # Симулируем невалидный документ.
    bundle.file.zip_raw = bundle.file.zip_raw.decode('utf-8').replace('GrpHdr', 'Bogus').encode('utf-8')
    bundle.file.save()

    with pytest.raises(ValidationError) as einfo:
        Unicredit.payment_sender(bundle).prepare_send_sync()

    assert "Bogus': This element is not expected." in einfo.value.msg


def test_statement_parser(parse_statement_fixture, read_fixture):
    Unicredit.statement_dispatcher.parsers[0].validate_file_format(read_fixture('unicredit_rub_I_statement.xml'))

    register, payments = parse_statement_fixture('unicredit_rub_I_statement.xml', **statement_params)[0]

    assert register.is_valid
    assert len(payments) == 1
    assert payments[0].summ == Decimal('19045.22')
    assert payments[0].date_valuated == datetime(2018, 1, 12)
    assert payments[0].get_info_purpose()

    mutator_func = lambda st_text: st_text.replace('<ValDt><Dt>2018-01-12</Dt></ValDt>', '')

    register, payments = parse_statement_fixture(
        'unicredit_rub_I_statement.xml', **statement_params, mutator_func=mutator_func
    )[0]

    assert register.is_valid
    assert not payments[0].date_valuated


def test_statement_intraday(parse_statement_fixture):

    def process(fixture_name):
        return parse_statement_fixture(
            fixture_name,
            mutator_func=lambda text: text.replace('2017-04-03', str(datetime.today().date())),
            **statement_params
        )

    _, payments1 = process('unicredit_rub_I_statement_1.xml')[0]
    _, payments2 = process('unicredit_rub_I_statement_2.xml')[0]

    assert len(payments1) == 1
    assert len(payments2) == 1  # Содержит также платёж из payments1, но он не учитывается, так как загружен ранее.
    assert payments1[0].summ == Decimal('1.5')
    assert payments2[0].summ == Decimal('6.6')

    assert payments1[0].get_info_bik() == '666666666'


@pytest.mark.parametrize(
    'external_status, internal_status',
    [
        (Statuses.STATUS_VERIFIED_FORMAT, ACCEPTED_H2H),
        (Statuses.STATUS_VERIFIED_REQ, PROCESSING),
        (Statuses.STATUS_REJECTED, DECLINED_BY_BANK),
    ]
)
def test_status_parser(external_status, internal_status, read_fixture, get_payment_bundle, get_source_payment):

    old_payment = get_source_payment({
        'number': '24742', 'status': EXPORTED_H2H, 'date': datetime(2018, 11, 7)},
        associate=Unicredit)

    bundle = get_payment_bundle(
        [{'number': '24742', 'status': EXPORTED_H2H}, {'number': '24744', 'status': EXPORTED_H2H}],
        associate=Unicredit, id=765642)

    xml = read_fixture('unicredit_payment_statuses.xml', decode='utf-8').replace('ACTC', external_status)
    syncer = Unicredit.payment_synchronizer()
    status = syncer.parse_statuses_xml(xml)
    syncer.update_payments(status)

    assert len(status['payments']) == 2

    bundle.refresh_from_db()
    assert bundle.status == internal_status

    payment1, payment2 = bundle.payments
    assert payment1.status == COMPLETE
    assert 'test_state_description' in payment1.processing_notes

    assert payment2.status == PROCESSING
    assert 'ProcessedDate' in payment2.processing_notes

    old_payment.refresh_from_db()
    assert old_payment.status == EXPORTED_H2H


def test_statement_balance_check(parse_statement_fixture, mocker, mailoutbox, django_assert_num_queries):
    """Проверяет сверку начального сальдо текущей выписики с конечным предыдущей."""

    def replace_and_parse(date, opening_balance, closing_balance):

        def replace(text):
            for param, value in (('OPBD', opening_balance), ('CLBD', closing_balance)):
                text = re.sub(
                    r'({}.+?<Amt.+?>).+?(</Amt>)'.format(param),
                    r'\g<1>{}\g<2>'.format(value), text, flags=re.DOTALL
                )
            return text.replace('2017-03-16', date)

        return parse_statement_fixture('unicredit_rub_A_statement.xml', mutator_func=replace, **statement_params)[0]

    reg, payments = replace_and_parse(date='2017-03-14', opening_balance='0.00', closing_balance='1.50')
    assert str(reg.statement_date) == '2017-03-18'

    replace_and_parse(date='2017-03-15', opening_balance='1.50', closing_balance='3.00')

    with django_assert_num_queries(16) as _:
        with pytest.raises(ValidationError):
            replace_and_parse(date='2017-03-16', opening_balance='60.00', closing_balance='61.50')

    assert '[Юникредит]' in mailoutbox[0].subject
    assert 'Ошибка сверки баланса' in mailoutbox[0].subject
    assert 'не была обработана из-за ошибки сверки сальдо предыдущего и текущего дней.' in mailoutbox[0].body
    assert 'Входящее сальдо в загружаемой выписке: 60.00' in mailoutbox[0].body
    assert 'Исходящее сальдо в предыдущей выписке: 3.000000' in mailoutbox[0].body


def test_unicredit_payment_partitioner(get_source_payment_mass):
    payments = Unicredit.payment_dispatcher.partition_payments(get_source_payment_mass(300, Unicredit))

    assert len(payments) == 2
    assert len(payments[0]) == 200
    assert len(payments[1]) == 100


def test_empty_statement(parse_statement_fixture):
    register, payments = parse_statement_fixture(
        'unicredit_empty_statement.xml', Unicredit, '40702810600014307627', 'RUB',
        encoding=Unicredit.statement_dispatcher.parsers[0].encoding
    )[0]

    assert len(payments) == 0

    assert register.opening_balance == Decimal('10002.00')
    assert register.closing_balance == Decimal('10002.00')

    assert register.is_valid


def test_intraday_zero_payment(parse_statement_fixture):
    statement_params = dict(
        associate=Unicredit,
        acc='40702840200014307628',
        curr='USD',
        encoding=Unicredit.statement_dispatcher.parsers[0].encoding
    )

    def process(fixture_name):
        return parse_statement_fixture(
            fixture_name,
            mutator_func=lambda text: text.replace('2018-06-26', str(datetime.today().date())),
            **statement_params
        )

    register, payments = process('unicredit_usd_intraday_zero_payment.xml')[0]

    assert len(payments) == 1
    statement = register.statement

    assert statement.type == statement.TYPE_INTRADAY
    assert register.is_valid
    assert len(payments) == 1
    assert payments[0].summ == Decimal(0)


def test_unrecognized_account(parse_statement_fixture):

    def process(fixture_name):
        return parse_statement_fixture(
            fixture_name,
            mutator_func=lambda text: text.replace('2018-06-26', str(datetime.today().date())),
            **statement_params
        )

    with pytest.raises(UserHandledException) as e:
        process('unicredit_usd_intraday_zero_payment.xml')

    assert 'Счёт 40702840200014307628 не зарегистрирован' in e.value.msg


def test_check_data_statement(parse_statement_fixture):
    def replace_date(text):
        return text.replace('2018-06-28', (datetime.now() + timedelta(days=1)).strftime('%Y-%m-%d'))

    register, payments = parse_statement_fixture(
        'unicredit_tomorrow_statement.xml', Unicredit, '40702810600014307627', 'RUB', mutator_func=replace_date,
        encoding=Unicredit.statement_dispatcher.parsers[0].encoding
    )[0]
    statement = register.statement
    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert register.statement_date == (datetime.now() + timedelta(days=1)).date()


def test_statement_check_end_amount( parse_statement_fixture):
    """Проверяет сверку начального сальдо текущей выписики с конечным"""

    def replace_sum(text):
        return text.replace('<Amt Ccy="RUB">0.00</Amt>', '<Amt Ccy="RUB">1.00</Amt>')

    register, payments = parse_statement_fixture(
        'unicredit_rub_A_statement.xml', Unicredit, '40702810600014307627', 'RUB', mutator_func=replace_sum,
        encoding=Unicredit.statement_dispatcher.parsers[0].encoding
    )[0]
    statement = register.statement
    assert statement.status == ERROR
    assert not register.is_valid
