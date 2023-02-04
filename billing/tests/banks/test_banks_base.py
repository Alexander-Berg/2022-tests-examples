from datetime import datetime

import pytest

from bcl.banks.base import Associate, PaymentSender, PaymentSynchronizer
from bcl.banks.common.payment_creator import PaymentCreator, QuantityPartitioner
from bcl.banks.common.payment_dispatcher import PaymentDispatcher
from bcl.banks.common.statement_dispatcher import StatementDispatcher
from bcl.banks.common.statement_downloader import StatementDownloader
from bcl.banks.common.statement_parser import StatementParser
from bcl.banks.registry import _ASSOCIATES, Sber, YooMoney, Alfa, Raiffeisen, Unicredit, JpMorgan, Ing, RaiffeisenSpb
from bcl.core.models import Currency, PaymentsBundle, Service, states, Payment, \
    Organization
from bcl.exceptions import (
    BclException, FileFormatError, ValidationError, UserHandledException, LogicError
)


class TstStatementDownloader(StatementDownloader):

    bank_id = 'fakebik'

    def fetch_accounts_data(self):
        data = {'1234': 'qwe'}
        return data


class TstPaymentSender(PaymentSender):

    def send(self, contents):
        super().send(contents)


class TstStatementParser(StatementParser):

    @classmethod
    def can_parse_statement(cls, contents):
        if contents == b'not a statement':
            return False

        return True

    @classmethod
    def validate_file_format(cls, contents):
        if not contents.startswith(b'<'):
            raise FileFormatError()

    def process(self):
        pass


class TstPaymentCreator(PaymentCreator):

    encoding = 'cp1251'

    def create(self):
        return 'кот'


class TstPaymentSynchronizer(PaymentSynchronizer):

    def run(self):
        super().run()
        return states.EXPORTED_H2H


class TstAssociate(Associate):

    id = 0
    bid = 0
    title = 'TstAssociate'

    alias = 'testas'
    statement_dispatcher = StatementDispatcher(TstStatementParser)
    statement_downloader = TstStatementDownloader
    payment_sender = TstPaymentSender
    payment_dispatcher = PaymentDispatcher(
        (TstPaymentCreator,),
        partitioners=(
            QuantityPartitioner(per_bundle=2),
        )
    )
    payment_synchronizer = TstPaymentSynchronizer
    auto_payments_enabled = True
    uses_h2h = True
    auto_payments_sync_enabled = True
    auto_statements_enabled = True


_ASSOCIATES['id'][0] = TstAssociate


def test_tech_account():

    associate = TstAssociate()

    assert associate.tech_account_decompose('TECH1234567') == '1234567'

    with pytest.raises(ValidationError):
        # не технический
        associate.tech_account_decompose('1234567')

    assert associate.tech_account_compose('987654') == 'TECH987654'

    with pytest.raises(ValidationError):
        # уже технический
        associate.tech_account_compose('TECH987654')


def test_statement_dispatcher(get_statement):
    dispatcher = TstAssociate().statement_dispatcher

    parser = dispatcher.get_parser(b'<statement/>')
    assert parser is TstStatementParser

    with pytest.raises(UserHandledException):
        dispatcher.get_parser(b'not a statement')

    parser = dispatcher.get_parser(get_statement(b'statement', TstAssociate.id))
    assert isinstance(parser, TstStatementParser)

    with pytest.raises(UserHandledException) as e:
        statement = get_statement(b'not a statement', TstAssociate.id)

        statement.id = 1218500
        statement.save()

        dispatcher.get_parser(statement)

    assert str(e.value) == (
        'Неподдерживаемый формат выписки с id 1218500 для банка testas.')


def test_statement_parser():
    def validate(contents):
        TstAssociate().statement_dispatcher.get_parser(contents).validate_file_format(contents)

    with pytest.raises(FileFormatError):
        validate(b'abcde')

    validate(b'<?xml')


def test_payment_partitioners(get_source_payment_mass):
    batch_one = get_source_payment_mass(3, TstAssociate, attrs={'f_acc': '1234'})
    batch_two = get_source_payment_mass(3, TstAssociate, attrs={'f_acc': '12345'})

    payments = TstAssociate.payment_dispatcher.partition_payments(batch_one + batch_two)

    assert len(payments) == 4
    assert len(payments[1]) == 1


def test_payment_creator(get_assoc_acc_curr, get_payment_bundle, get_source_payment):
    associate, account, _ = get_assoc_acc_curr(Sber, account='1234')
    payments = [get_source_payment(), get_source_payment()]
    bundle = get_payment_bundle(payments, associate, account)  # type: PaymentsBundle

    assoc = TstAssociate()
    creator = assoc.payment_dispatcher.get_creator(bundle)
    result = creator.create_bundle(return_encoded=True)
    cat = b'\xea\xee\xf2'
    assert result == cat

    result = creator.create_bundle()
    assert result == 'кот'
    assert creator.get_bundle_contents() == b'\xea\xee\xf2'
    assert creator.get_bundle_contents(decoded=True) == 'кот'

    payments = [
        get_source_payment({'f_acc': account.number}, associate=assoc),
        get_source_payment({'f_acc': account.number}, associate=assoc),
        get_source_payment({'f_acc': account.number}, associate=assoc),
    ]

    dispatcher = assoc.payment_dispatcher

    filename, contents = dispatcher.bundles_to_file(
        dispatcher.bundle_compose([str(payment.id) for payment in payments]))

    assert 'pbundle_testas_' in filename
    assert '.zip' in filename
    assert contents

    for payment in payments:
        payment.refresh_from_db()
        assert payment.status == states.EXPORTED_ONLINE

    with pytest.raises(UserHandledException):
        TstAssociate().payment_dispatcher.bundle_compose([str(payment.id) for payment in payments])

    filename, contents = dispatcher.bundles_to_file(
        dispatcher.bundle_compose([get_source_payment({'f_acc': account.number}, associate=assoc).id]))

    assert '.txt' in filename
    assert contents == cat


def test_export_different_account(get_assoc_acc_curr, get_source_payment):
    associate, account_1, _ = get_assoc_acc_curr(TstAssociate, account='1234')
    associate, account_2, _ = get_assoc_acc_curr(TstAssociate, account='1236')
    payments = [
        get_source_payment({'f_acc': account_1.number}, associate=TstAssociate),
        get_source_payment({'f_acc': account_2.number}, associate=TstAssociate)
    ]

    assert not len(PaymentsBundle.objects.all())

    TstAssociate().payment_dispatcher.bundle_compose([str(payment.id) for payment in payments])

    assert len(PaymentsBundle.objects.all()) == 2


@pytest.mark.parametrize('associate', [Unicredit, JpMorgan])
@pytest.mark.parametrize('empty_field', ['t_name', 't_bankname', 't_acc', 't_bic'])
def test_many_payment_creator(empty_field, associate, get_source_payment, get_payment_bundle):

    if empty_field == 't_bic' and associate is JpMorgan:
        empty_field = 't_swiftcode'

    incorrect_payment = get_source_payment({empty_field: ''})
    bundle = get_payment_bundle([incorrect_payment, get_source_payment()])
    creator = associate.payment_dispatcher.get_creator(bundle)

    compiled = creator.create_bundle()

    bundle = PaymentsBundle.objects.filter(id=bundle.id)[0]
    incorrect_payment.refresh_from_db()

    assert '<NbOfTxs>1</NbOfTxs>' in compiled
    assert len(bundle.payments) == 1
    assert incorrect_payment.status == states.BCL_INVALIDATED


def test_associate_automations(get_assoc_acc_curr, get_payment_bundle, get_source_payment, init_user):

    init_user(robot=True)
    associate, account, _ = get_assoc_acc_curr(Sber, account='1234')

    payments = [get_source_payment(), get_source_payment()]
    bundle = get_payment_bundle(payments, associate, account)  # type: PaymentsBundle

    assert len(TstAssociate.automate_statements()) == 1

    automated = TstAssociate.automate_payments(bundle.number)
    assert automated.status == states.EXPORTED_H2H

    payments[0].refresh_from_db()
    payments[1].refresh_from_db()

    assert payments[0].status == states.EXPORTED_H2H
    assert payments[1].status == states.EXPORTED_H2H

    with pytest.raises(BclException):
        TstAssociate.run_procedure('bogus')  # неизвестная процедура

    with pytest.raises(KeyError):
        TstAssociate.run_procedure('automate_statements')  # упущена дата

    assert TstAssociate.run_procedure('automate_statements', {'on_date': '2017-05-03'}) == []

    with pytest.raises(ValidationError):
        TstAssociate.run_procedure('automate_payments', {'bundle_number': bundle.number})

    automated = TstAssociate.run_procedure('automate_payments_sync', {'bundle_number': bundle.number})
    assert automated


@pytest.fixture
def get_payment_data(get_assoc_acc_curr):
    """Возвращает данные, необходимые для создания платежа и связанных объектов методом register_payment."""

    def wrapper(associate, *, number_src):
        _, account, _ = get_assoc_acc_curr(associate, account='123')

        payment_data = {
            'number_src': number_src,
            'currency_id': Currency.RUB,
            'summ': 100,
            'ground': 'some',
            'f_acc': account.number,
            't_acc': 'some@accname',
            'associate_id': associate.id,
            'account_id': account.id,
        }

        return payment_data

    return wrapper


def test_register_payment(get_payment_data):
    payment, created = Sber.register_payment(
        get_payment_data(associate=Sber, number_src='123321'), service_id=Service.OEBS)

    assert payment.status == states.NEW
    assert created

    yoomoney_number_src = '123444'
    payment, created = YooMoney.register_payment(
        get_payment_data(associate=YooMoney, number_src=yoomoney_number_src), service_id=Service.TOLOKA)

    payment.refresh_from_db()

    assert payment.status == states.BUNDLED
    assert created
    assert PaymentsBundle.objects.filter(status=PaymentsBundle.state_processing_ready).count() == 1

    pay = Payment.objects.filter(number_src=yoomoney_number_src)[0]
    assert pay.status == states.BUNDLED
    assert pay.currency_id == Currency.RUB

    # Синхронизация статусов.
    pay.status = states.ERROR
    pay.save()

    payment.refresh_from_db()

    assert payment.status == pay.status


def test_register_service_payment_exception(get_payment_data, monkeypatch, mocker):
    monkeypatch.setattr(PaymentsBundle, 'save', None)
    mocker.patch('bcl.core.models.PaymentsBundle', PaymentsBundle)

    params = get_payment_data(associate=YooMoney, number_src='123321')

    with pytest.raises(TypeError):
        YooMoney().register_payment(params, service_id=Service.TOLOKA)

    with pytest.raises(TypeError):
        YooMoney().register_payment(params, service_id=Service.TOLOKA)


def test_register_service_payment_yoomoney_exception(get_payment_data, monkeypatch, mocker, init_user):
    init_user(robot=True)

    # создаем платеж в платежной системе с ожидаемой ошибкой при связывании с пакетом
    old_save = PaymentsBundle.save
    monkeypatch.setattr(PaymentsBundle, 'save', None)
    mocker.patch('bcl.core.models.PaymentsBundle', PaymentsBundle)

    params = get_payment_data(associate=YooMoney, number_src='125321')

    with pytest.raises(TypeError):
        YooMoney().register_payment(params, service_id=Service.TOLOKA)

    # проверяем, что ни платеж, ни пакет не создались из-за исключения.
    payment = Payment.objects.filter(
        service_id=Service.TOLOKA,
        number_src='125321'
    ).first()
    assert payment is None

    # проверяем, что при повторном вызове создается пакет и проставляется в очередь
    monkeypatch.setattr(PaymentsBundle, 'save', old_save)
    payment, created = YooMoney.register_payment(params, service_id=Service.TOLOKA)
    assert created is True

    queue_obj = PaymentsBundle.objects.filter(status=PaymentsBundle.state_processing_ready)
    assert len(queue_obj) == 1


def test_downloader(init_user, get_assoc_acc_curr):

    init_user(robot=True)

    with pytest.raises(LogicError):
        downloader = type(str('RuntimeDownloader'), (StatementDownloader,), {'associate': TstAssociate})
        downloader.process()

    statements = TstStatementDownloader.process()
    assert statements == []  # Неизвестный счёт.

    _, account, _ = get_assoc_acc_curr(TstAssociate, account='1234')

    statements = TstStatementDownloader.process()
    assert len(statements) == 1

    statements = TstStatementDownloader.process()
    assert len(statements) == 0  # Загружена ранее.


def test_auto_urgent(get_assoc_acc_curr, build_payment_bundle):
    assoc = JpMorgan

    _, acc, _ = get_assoc_acc_curr(assoc)

    acc.auto_urgent = True
    acc.save()

    bundle = build_payment_bundle(assoc, payment_dicts=[
        {
            'autobuild': Payment.AUTO_BUILD_CPA,
            'urgent': False,
        }
    ], account=acc, h2h=True)

    assert bundle.payments[0].urgent


def test_block_account(get_source_payment, get_assoc_acc_curr, get_payment_bundle):
    """Тест блокирования выплат с заблокированных счетов."""
    associate, blocked_account, _ = get_assoc_acc_curr(Raiffeisen.id, account='407028103000010')
    associate, not_blocked_account, _ = get_assoc_acc_curr(Raiffeisen.id, account='407028103000077')

    # блокируем один счёт
    blocked_account.blocked = blocked_account.HARD_BLOCKED
    blocked_account.save()

    b_payment = get_source_payment(dict(associate_id=Raiffeisen.id, f_acc=blocked_account.number, summ=300))
    not_b_payment = get_source_payment(dict(associate_id=Raiffeisen.id, f_acc=not_blocked_account.number, summ=100))

    blocked_bundle = get_payment_bundle((b_payment, ), associate, blocked_account)
    not_b_bundle = get_payment_bundle((not_b_payment, ), associate, not_blocked_account)

    for bundle in PaymentsBundle.objects.all():
        bundle.schedule()

    # смотрим чтобы в выдаче был только незаблокированный счёт
    assert PaymentsBundle.get_scheduled() == not_b_bundle

    # снимание блокировку аккаунта
    blocked_account.blocked = blocked_account.NON_BLOCKED
    blocked_account.save()

    for bundle in PaymentsBundle.objects.all():
        bundle.schedule()

    # смотрим чтобы в выдаче был только что разблокированный счёт
    assert PaymentsBundle.get_scheduled() == blocked_bundle


def test_org_external():
    """Тест на определение  иностранных юридических лиц."""
    kaz_org, _ = Organization.objects.get_or_create(
        name='TOO «Яндекс.Казахстан»')
    # казахстанское ТОО является иностранным юр.лицом
    assert kaz_org.is_external


    ukr_org, _ = Organization.objects.get_or_create(
        name='ТОВ "Яндекс.Україна"')
    # украиноское ТОВ является иностранным юр.лицом
    assert ukr_org.is_external

    org, _ = Organization.objects.get_or_create(
        name='АО Яндекс.Технологии')
    # российское АО не является иностранным юр.лицом
    assert not org.is_external


def test_synchronizer_get_candidate_bundles(build_payment_bundle, django_assert_num_queries):

    def build_bundle(status):
        bundle = build_payment_bundle(Alfa, payment_dicts=[{}, {}], h2h=True)
        bundle.sent = True
        bundle.status = status
        bundle.save()
        return bundle

    # Кандидат.
    bundle1 = build_bundle(states.PROCESSING)

    # Кандидат. Есть платежи в обработке.
    bundle2 = build_bundle(states.ACCEPTED_H2H)
    pay1, pay2 = list(bundle2.payments.all())
    pay1.status = states.COMPLETE
    pay1.save()
    pay2.status = states.PROCESSING
    pay2.save()

    # Не кандидат: всё проведено
    bundle3 = build_bundle(states.ACCEPTED_H2H)
    bundle3.payments.update(status=states.COMPLETE)

    syncer = Alfa.payment_synchronizer()

    with django_assert_num_queries(1) as _:
        candidates = sorted(syncer.get_candidate_bundles(), key=lambda candidate: candidate.id)

    assert len(candidates) == 2
    assert candidates[0].id == bundle1.id
    assert candidates[1].id == bundle2.id


@pytest.mark.parametrize('associate', [Sber, Raiffeisen, Ing])
def test_payment_currency_validation(
    associate, get_assoc_acc_curr, get_payment_bundle, get_source_payment, build_payment_bundle,
    response_mock,
):
    _, account, _ = get_assoc_acc_curr(
        associate, account={'currency_code': Currency.USD, 'number': '12345678912345678912'}
    )
    payment_params = {
        'currency_id': Currency.USD,
        'trans_pass': '',
        'contract_num': '',
        'contract_dt': None,
        'ground': 'test',
        't_country': '826',
        'oper_code': '',
    }
    with pytest.raises(ValidationError):
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

    payment_params['trans_pass'] = '1223234'
    with pytest.raises(ValidationError):
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

    payment_params['trans_pass'] = '12345678/1234/1234/1/1'
    with pytest.raises(ValidationError):
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

    payment_params['oper_code'] = '1234'
    with pytest.raises(ValidationError):
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

    payment_params['oper_code'] = '11100'
    with pytest.raises(ValidationError):
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

    payment_params['oper_code'] = '21100'
    payment_params['expected_dt'] = datetime(2021, 6, 18)
    with pytest.raises(ValidationError):
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

    payment_params['oper_code'] = '21100'
    payment_params['expected_dt'] = datetime(2021, 6, 18)
    payment_params['advance_return_dt'] = datetime(2021, 6, 17)
    with pytest.raises(ValidationError):
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

    payment_params['oper_code'] = '21100'
    payment_params['expected_dt'] = datetime(2021, 6, 18)
    payment_params['advance_return_dt'] = datetime(2021, 6, 18)

    response = (
        'POST https://refs-test.paysys.yandex.net/api/swift -> 200:'
        '{"data": {"bics": ['
        '{"bic8": "OWHBDEFF", "bicBranch": "XXX", '
        '"addrOpRegion": "aoreg", "addrOpStreet": "aostr", "addrOpStreetNumber": "aostrn", "addrOpBuilding": "aobld", '
        '"addrOpCity": "aocit", "instName": "inst", "countryCode": "DE"'
        '}'
        ']}}'
    )
    if associate is Ing:
        response = ''

    with response_mock(response):
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

        payment_params['contract_num'] = '1223234'
        with pytest.raises(ValidationError):
            build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

        payment_params['trans_pass'] = ''
        with pytest.raises(ValidationError):
            build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

        payment_params['contract_dt'] = datetime(2021, 6, 18)
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

        payment_params['contract_num'] = ''
        with pytest.raises(ValidationError):
            build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)


@pytest.mark.parametrize('associate', [Raiffeisen, RaiffeisenSpb, Ing, Sber, Unicredit])
def test_empty_tax_field_validation(associate, get_assoc_acc_curr, build_payment_bundle):
    _, account, _ = get_assoc_acc_curr(
        associate, account={'currency_code': Currency.RUB, 'number': '12345678912345678912'}
    )
    payment_params = {
        'n_kbk': '18210102010011000110',
        'n_okato': '',
        'n_status': '',
        'n_ground': '',
        'n_period': '',
        'n_doc_num': '',
        'n_doc_date': '',
    }
    with pytest.raises(ValidationError) as e:
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate.uses_h2h, account=account)

    if associate is Raiffeisen:
        # для райфа еще и доп проверку для 1c формата делаем
        with pytest.raises(ValidationError) as e:
            build_payment_bundle(associate, payment_dicts=[payment_params], h2h=False, account=account)

    for key in payment_params:
        assert key in e.value.msg

    payment_params.update({
        'n_okato': '45383000',
        'n_status': '02',
        'n_ground': 'ТП',
        'n_period': 'МС.08.2020',
        'n_doc_num': '0',
        'n_doc_date': '31-08-2020',
    })
    build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)

    if associate is Raiffeisen:
        build_payment_bundle(associate, payment_dicts=[payment_params], h2h=associate is not Sber, account=account)


def test_remote_id_parsed(build_payment_bundle):
    remote_id = '123'

    bundle = build_payment_bundle(Raiffeisen)
    bundle.remote_id = remote_id
    bundle.save()

    assert bundle.remote_id_parsed.id == remote_id
    assert bundle.remote_id_parsed.raw == remote_id
