from datetime import datetime
from decimal import Decimal

import pytest

from bcl.banks.registry import Sber, Unicredit, Ing, PriorBy, Raiffeisen, YooMoney, IngRo
from bcl.core.models import BundleSign, Payment, PaymentsBundle, \
    StatementPayment, StatementRegister, User, states, SwiftIdentificationCode, Currency, AuditEntry, SigningRight, \
    Service, NumbersRegistry
from bcl.core.views.rpc import Rpc
from bcl.exceptions import PaymentDiscardError, BclException, ValidationError, UserHandledException
from bcl.toolbox.signatures import XmlSignature, Signature


def idfn(val):
    return f"params: {val}"


def test_numbers_registry():
    associate = Sber

    number_max = 999999

    assert associate.payment_num_max == number_max
    assert NumbersRegistry.get_number(associate) == 1

    entry = NumbersRegistry.objects.get(associate_id=associate.id)
    entry.number = 999
    entry.save()

    # Перепрыгиваем номер (для ЦБР)
    assert NumbersRegistry.get_number(associate) == 1001

    entry.number = number_max
    entry.save()

    # Перепрыгиваем для ЦБР, а потом по превышению предела.
    assert NumbersRegistry.get_number(associate) == 1

    # Перепрыгиваем для ING Румыния номера, оканчивающиеся на 0
    associate = IngRo
    assert associate.get_new_payment_number() == 1

    entry = NumbersRegistry.objects.get(associate_id=associate.id)
    entry.number = 9
    entry.save()

    assert associate.get_new_payment_number() == 11


def test_signing_right(dss_signing_right, make_org):

    right = dss_signing_right(serial='00', associate=Sber)
    assert right.dss_credentials

    login, secret = right.dss_credentials_get()

    assert login == 'tester1'
    assert secret == 'Atester1'

    right.dss_credentials_reset()

    assert not right.dss_credentials

    # Проверка фильтрации параметров подписи.
    out = SigningRight.dss_get_params(Sber, user=right.user)
    assert out[1] == '00'
    assert out[2].id == right.id

    org = make_org(name='dummy')

    right2 = dss_signing_right(serial='22', associate=Sber, org=org)
    out = SigningRight.dss_get_params(Sber, user=right.user, org=org)
    assert out[1] == '22'
    assert out[2].id == right2.id

    with pytest.raises(UserHandledException):
        # Нет подписи для данной компании.
        SigningRight.dss_get_params(Sber, user=right.user, org=make_org(name='bogus'))


class TestPaymentBundle:

    def test_cleanup_stale(self, time_shift, build_payment_bundle):

        assert not PaymentsBundle.cleanup_stale()

        def build(*, set_status=True):
            """
            :rtype: PaymentsBundle
            """
            bundle = build_payment_bundle(YooMoney, service=True, h2h=True)

            if set_status:
                bundle.set_status(bundle.state_processing_now, propagate_to_payments=False)

            return bundle

        bundle1 = build(set_status=False)
        bundle2 = build()
        bundle3 = build()

        with time_shift(4 * 60 * 60):
            bundles = PaymentsBundle.cleanup_stale()
            assert len(bundles) == 2

            bundle1.refresh_from_db()
            bundle2.refresh_from_db()
            bundle3.refresh_from_db()

            for bundle in (bundle1, bundle2, bundle3):
                assert bundle.status != bundle.state_processing_now

    def test_scheduling(self, get_payment_bundle, get_source_payment, init_user):

        user = init_user('scheduler')

        def make_bundle():
            """
            :rtype: PaymentsBundle
            """
            payments = [get_source_payment(), get_source_payment()]
            return get_payment_bundle(payments)

        bundle1 = make_bundle()
        bundle2 = make_bundle()
        bundle3 = make_bundle()

        assert PaymentsBundle.get_scheduled() is None

        assert not bundle1.is_processing_ready
        assert not bundle1.is_processing_now

        bundle1.schedule()
        bundle2.schedule()

        assert bundle1.is_processing_ready

        scheduled1 = PaymentsBundle.get_scheduled()
        assert scheduled1.id == bundle1.id

        with PaymentsBundle.scheduled(scheduled1):
            scheduled1.refresh_from_db()
            assert scheduled1.is_processing_now

            scheduled1.user = user

        bundle1.refresh_from_db()
        assert bundle1.user == user
        assert bundle1.is_processing_done

        # Проверка постановки на повторую обработку.

        with PaymentsBundle.scheduled() as scheduled2:
            assert scheduled2.id == bundle2.id
            assert scheduled2.is_processing_now
            raise Exception('bug')

        bundle2.refresh_from_db()

        assert bundle2.is_processing_ready
        assert 'присвоили номер' in bundle2.processing_notes
        assert bundle2.processing_retries == 1

        # Ускорим тесты подменой данных.
        bundle2.processing_retries = PaymentsBundle.processing_retries_max - 1
        bundle2.processing_after_dt = datetime.utcnow()

        bundle2.save()

        with PaymentsBundle.scheduled() as scheduled2:
            assert scheduled2.id == bundle2.id
            assert scheduled2.is_processing_now
            raise BclException('nice title')

        bundle2.refresh_from_db()

        assert bundle2.is_processing_failed
        assert bundle2.processing_notes == 'nice title'
        assert bundle2.processing_retries == PaymentsBundle.processing_retries_max

    def test_basic(self, get_payment_bundle, get_source_payment):
        payments = [get_source_payment(), get_source_payment()]
        bundle = get_payment_bundle(payments)  # type: PaymentsBundle

        assert payments[0].status == states.NEW
        assert payments[1].status == states.NEW

        bundle.update_payments_status(states.BUNDLED)

        payments[0].refresh_from_db()
        payments[1].refresh_from_db()

        assert payments[0].status == states.BUNDLED
        assert payments[1].status == states.BUNDLED

    def test_signing_related(self, read_fixture, get_payment_bundle, get_source_payment, mocker):
        xml_signature = XmlSignature(read_fixture('signature.xml', decode='utf-8')).as_text()

        user1 = User(username='test')
        user1.save()

        user2 = User(username='test2')
        user2.save()

        sign1 = BundleSign(user=user1, level=1)
        digit = SigningRight(
            user=user1, associate_id=Unicredit.id, serial_number='111', level=1)
        digit.save()

        sign2 = BundleSign(user=user2, level=2)
        digit2 = SigningRight(
            user=user2, associate_id=Unicredit.id, serial_number='222', level=2)
        digit2.save()

        mocker_object = mocker.patch.object(Signature, 'as_bytes')
        mocker_object.return_value = xml_signature

        Signature.serial = '111'

        bundle = get_payment_bundle(
            [get_source_payment(associate=Unicredit)], associate=Unicredit)  # type: PaymentsBundle
        bundle.signs_add(user1, Signature(sign1))
        assert len(AuditEntry.objects.filter(
            action=AuditEntry.ACTION_BUNDLE_SIGN
        )) == 1

        Signature.serial = '222'

        bundle.signs_add(user2, Signature(sign2))
        bundle.refresh_from_db()

        assert bundle.signs_check_user_sign(user1)
        assert bundle.signs_check_user_sign(user2)
        assert bundle.signs_enough
        assert len(bundle.digital_signs) == 2
        assert len(AuditEntry.objects.filter(
            action=AuditEntry.ACTION_BUNDLE_SIGN
        )) == 2

        bundle.signs_remove(user1)
        bundle.refresh_from_db()

        assert not bundle.signs_check_user_sign(user1)
        assert bundle.signs_check_user_sign(user2)
        assert not bundle.signs_enough
        assert len(bundle.digital_signs) == 1
        assert bundle.digital_signs[0].value == xml_signature.encode('utf-8')

    def test_is_changeable(self, get_payment_bundle, get_source_payment, get_proved):
        bundle = get_payment_bundle([get_source_payment({'status': states.COMPLETE}), get_source_payment()])
        assert not bundle.is_changeable

        bundle = get_payment_bundle([get_source_payment({'status': states.DECLINED_BY_BANK}), get_source_payment()])
        assert not bundle.is_changeable

        bundle = get_payment_bundle([get_source_payment(), get_source_payment()])
        assert bundle.is_changeable

        bundle = get_payment_bundle([get_source_payment(), get_source_payment()], h2h=True)
        assert bundle.is_changeable

        bundle.set_status(states.EXPORTED_H2H)
        assert not bundle.is_changeable

        bundle = get_payment_bundle([get_source_payment(service=True)], h2h=True)
        bundle.status = states.FOR_DELIVERY
        assert not bundle.is_changeable

        bundle = get_payment_bundle([get_source_payment({'status': states.NEW})])
        assert bundle.is_changeable

        payment = get_source_payment({'status': states.EXPORTED_H2H, 'statement_payment': None})
        get_payment_bundle([payment])
        assert payment.is_changeable

        proved_pay = get_proved(Sber)[0]
        payment = get_source_payment({'status': states.EXPORTED_H2H, 'statement_payment': proved_pay})
        proved_pay.payment = payment
        proved_pay.save()
        get_payment_bundle([payment])
        assert not payment.is_changeable

        bundle = get_payment_bundle([get_source_payment({'status': states.ERROR})])
        bundle.status = states.ERROR
        assert bundle.is_changeable

    def test_download_allowed(self, get_payment_bundle, get_source_payment):
        bundle = get_payment_bundle([get_source_payment()])
        assert bundle.download_allowed

        bundle = get_payment_bundle([get_source_payment()])
        bundle.destination = PaymentsBundle.DESTINATION_H2H
        bundle.save()
        bundle.refresh_from_db()
        assert not bundle.download_allowed

        bundle = get_payment_bundle([get_source_payment()])
        bundle.file = None
        bundle.save()
        bundle.refresh_from_db()

        assert not bundle.download_allowed

    def test_hide_bundle(self, get_payment_bundle, get_source_payment):
        payment = get_source_payment({'status': states.BUNDLED})
        bundle = get_payment_bundle([payment])
        bundle.hide()

        payment.refresh_from_db()
        assert payment.status == states.NEW
        assert payment.bundle is None

        assert bundle.hidden
        assert bundle.payments[0].id == payment.id

    def test_payment_property(self, get_source_payment):
        payment = get_source_payment({'status': states.PROCESSING})

        assert payment.is_processing

        payment.set_status(states.BCL_INVALIDATED)
        assert payment.is_invalidated


class TestPayment:

    @pytest.fixture(params=[
        ('МФО 320984 ОКПО 37250679   ', {'inn': '37250679', 'bik': '320984'}),
        ('ОКПО 37250679 МФО 320984   ', {'inn': '37250679', 'bik': '320984'})
    ], ids=idfn)
    def param_counter_party(self, request):
        return request.param

    def test_to_dict_oebs(self, get_proved, get_assoc_acc_curr, get_statement):

        _, account, _ = get_assoc_acc_curr(Sber, account='123456')

        statement = get_statement('dummy', Sber)
        register = StatementRegister(
            associate_id=Sber.id,
            account=account, statement=statement)

        register.save()

        payment = get_proved(Sber)[0]
        to_dict = Rpc.to_dict_proved_pay

        oebs_dict = to_dict(payment)

        assert oebs_dict['direction'] == 'IN'
        assert oebs_dict['doc_number'] == payment.number
        assert int(Decimal(oebs_dict['summ'])) == 123

        oebs_payment = Payment(number='12345', currency_id=643, associate_id=Sber.id)
        oebs_payment.save()

        payment.payment = oebs_payment
        payment.save()

        oebs_dict = to_dict(payment)

        assert oebs_dict['doc_number'] == oebs_payment.number

    def test_display_currency_op_docs(self, get_source_payment):
        payment = get_source_payment({'currency_op_docs': '4'})
        assert payment.display_currency_op_docs == '4 - Документы представлены'

    def test_is_fee(self):
        pay = StatementPayment(trans_code=SwiftIdentificationCode.CHARGE, associate_id=Sber.id)
        assert pay.is_fee()

        pay.trans_code = SwiftIdentificationCode.TRANSFER
        assert not pay.is_fee()

        pay.info['18'] = '17'
        pay.info['06'] = 'ндс по комиссии'
        assert pay.is_fee()

    def test_paid_by(self, get_source_payment):

        payment = get_source_payment()
        assert payment.paid_by == 'OUR'

        payment = get_source_payment({
            'f_iban': 'CH123',
            't_iban': 'RO456',
            'currency_id': Currency.USD,
        })
        assert payment.paid_by == 'SHA'

        payment.paid_by = 'OUR'
        payment.save()
        assert payment.paid_by == 'OUR'

        payment.paid_by = None
        payment.save()
        assert payment.paid_by == 'OUR'


class TestPaymentOEBS:

    def test_iban(self, get_source_payment):
        payment = get_source_payment({
            'f_iban': '',
            't_iban': '',
        })
        assert not payment.f_iban
        assert not payment.t_iban

        payment = get_source_payment({
            'f_iban': 'DE85503200000206701419',
            't_iban': 'DE85503200000206701412',
        })
        assert payment.f_iban
        assert payment.t_iban

    def test_is_sepa(self, get_source_payment):
        payment = get_source_payment({
            't_iban': 'RO12345',
            'summ': '30.00',
            'currency_id': Currency.EUR,
        })
        assert payment.is_sepa

        payment = get_source_payment()
        assert not payment.is_sepa

    def test_is_changeable(self, get_source_payment, get_proved):
        # new payment
        payment = get_source_payment({'status': states.NEW})
        assert payment.is_changeable

        # exported, not linked
        payment = get_source_payment({'status': states.EXPORTED_H2H, 'statement_payment': None})
        assert payment.is_changeable

        # exported, linked
        proved_pay = get_proved(Sber)[0]
        payment = get_source_payment({'status': states.EXPORTED_H2H, 'statement_payment': proved_pay})
        proved_pay.payment = payment
        proved_pay.save()
        assert not payment.is_changeable

    def test_discard_check(self, get_source_payment, get_proved):
        payment = get_source_payment({'status': states.REVOKED})
        with pytest.raises(PaymentDiscardError):
            payment.discard_check()

        payment = get_source_payment({'status': states.CANCELLED})
        with pytest.raises(PaymentDiscardError):
            payment.discard_check()

        proved_pay = get_proved(Sber)[0]
        payment = get_source_payment({'status': states.BUNDLED, 'statement_payment': proved_pay})
        proved_pay.payment = payment
        proved_pay.save()

        with pytest.raises(PaymentDiscardError):
            payment.discard_check()

    def test_reject(self, build_payment_bundle, get_proved, get_source_payment, time_shift):
        # Экспортированный платёж отклоняется с удалением пакета

        def revoke():
            with time_shift(60 * 60 * 24 * 2, backwards=True):  # Для удовлетворения фильтра по времени.
                payment.revoke()

        bundle = build_payment_bundle(Sber, payment_dicts=[{'status': states.EXPORTED_ONLINE}])
        bundle.refresh_from_db()
        payment = bundle.payments[0]
        revoke()

        assert PaymentsBundle.objects.count() == 0
        assert payment.status == states.REVOKED

        # Экспортированный проведенный платёж отклоняется без удаления пакета
        proved_pay = get_proved(Sber)[0]

        bundle = build_payment_bundle(Sber, payment_dicts=[{'id': 40, 'status': states.EXPORTED_ONLINE}])
        bundle.sent = True
        bundle.save()
        bundle.refresh_from_db()

        proved_pay.payment_id = 40
        proved_pay.save()

        payment = bundle.payments[0]
        revoke()
        assert PaymentsBundle.objects.count() == 1
        assert payment.status == states.REVOKED

        # Неэкспортированные платежи не отклоняются
        payment = get_source_payment({'status': states.NEW})
        revoke()
        assert payment.status == states.NEW

    def test_cancel(self, get_source_payment):
        # Экспортированный платёж не аннулируется
        payment = get_source_payment({'status': states.BUNDLED})
        payment.cancel()
        assert payment.status == states.BUNDLED

        # Новые платежи аннулируются
        payment = get_source_payment({'status': states.NEW})
        payment.cancel()
        assert payment.status == states.CANCELLED

    def test_get_iso_tax_period(self):

        payment = Payment()

        def check(period, expected):
            payment.n_period = period
            current = payment.iso_tax_period

            if current:
                assert current == expected
            else:
                assert current is expected

        check('0', None)
        check('', None)

        check('ГД.00.2020', ('2020-01-01', '', ''))
        check('ПЛ.01.2020', ('2020-01-01', 'HLF1', ''))
        check('КВ.03.2020', ('2020-01-01', 'QTR3', ''))
        check('МС.05.2020', ('2020-01-01', 'MM05', ''))
        check('МС.5.20', ('2020-01-01', 'MM05', ''))
        check('2.4.21', ('', '', '2021-04-02'))
        check('22/4/2020', ('', '', '2020-04-22'))

    @pytest.mark.parametrize("associate", [Unicredit, Sber, Ing])
    def test_cyrillic_in_currency_payment(self, associate, get_source_payment, get_assoc_acc_curr):

        payment = get_source_payment({
            'status': states.NEW,
            'currency_id': Currency.USD,
            'ground': 'Кириллица'},
            associate=associate)
        get_assoc_acc_curr(payment.associate_id, account={'number': payment.f_acc, 'currency_code': Currency.USD})

        with pytest.raises(ValidationError) as err:
            associate.payment_dispatcher.bundle_compose([str(payment.id)])
        assert 'Кириллица не допускается в валютных платежах' in err.value.msg

    def test_cyrillic_in_byr_payment(self, get_source_payment, get_assoc_acc_curr):
        _, account, _ = get_assoc_acc_curr(PriorBy, account='123456')
        payment = get_source_payment({
            'status': states.NEW,
            'currency_id': Currency.BYN,
            'ground': 'Кириллица',
            'f_acc': account.number},
            associate=PriorBy)
        PriorBy.payment_dispatcher.bundle_compose([str(payment.id)])


class TestPaymentService:

    def test_to_dict(self, get_source_payment, get_assoc_acc_curr):

        _, account, _ = get_assoc_acc_curr(YooMoney, account='someacc')

        payment = get_source_payment(
            {
                'f_acc': account.number,
                'status': states.NEW,
                'service_id': Service.TOLOKA,
                'processing_notes': '[TSTERR] Test error',
                'ground': 'Назначение',
                't_acc': '',
                'remote_responses': [{'status': 'ok'}]
            },
            service=True,
            associate=YooMoney
        )
        payment.summ = Decimal('101.52')
        payment.save()

        payment_dict = Rpc.serialize_service_payments([payment])[0]

        # Значения всех полей, кроме целочисленных преобразуются в строки.
        for k, v in payment_dict.items():
            if k in ('service', 'firm', 'associate_id', 'doc_number'):
                assert isinstance(v, int)
            else:
                assert isinstance(v, str)

        # Код статуса преобразуется в псевдоним.
        assert payment_dict['status'] == 'new'

        # Словарь преобразуется в json.
        assert payment_dict['payment_system_answer'] == '{"status": "ok"}'

        # service_id преобразован для обратной совместимости в service.
        assert payment_dict['service'] == 6

        # unicode, NoneType передаются без изменений
        assert payment_dict['ground'] == 'Назначение'

        assert payment_dict['summ'] == '101.52'

        ignored_fields = {'update_dt', 'id', 'dt', 'bundle'}
        assert len(set(payment_dict.keys()) & ignored_fields) == 0

        # Если статус не Error, то поля ошибки игнорируются.
        assert 'error_code' not in payment_dict
        assert 'error_message' not in payment_dict

        payment.status = states.ERROR
        payment.org = None
        payment_dict = Rpc.serialize_service_payments([payment])[0]

        assert payment_dict['error_code'] == 'TSTERR'
        assert payment_dict['error_message'] == 'Test error'
        assert 'org' not in payment_dict


class TestSourcePaymentBase:

    def test_get_n_doc_date(self):
        # arrange
        payment = Payment(currency_id=643, associate_id=Sber.id)
        payment.save()

        # act
        date_empty = payment.get_n_doc_date('')

        payment.n_doc_date = '0'
        payment.save()
        date_0 = payment.get_n_doc_date('%d-%m-%Y')

        payment.n_doc_date = '20-12-2017'
        payment.save()
        date_valid_1 = payment.get_n_doc_date('%d-%m-%Y')
        date_valid_2 = payment.get_n_doc_date('%m-%d-%Y')

        # assert
        assert date_empty == ''
        assert date_0 == '0'
        assert date_valid_1 == '20-12-2017'
        assert date_valid_2 == '12-20-2017'

    @pytest.mark.parametrize("associate", [Raiffeisen, Ing])
    def test_check_n_doc_date(self, associate, get_source_payment, get_payment_bundle):
        payment = get_source_payment({
            'status': states.NEW,
            'currency_id': Currency.RUB,
            'n_kbk': '1',
            'n_doc_date': datetime.now().strftime('%d-%m-%Y'),
            'n_okato': '45383000',
            'n_status': '02',
            'n_ground': 'ТП',
            'n_period': 'МС.08.2020',
            'n_doc_num': '0',
            'n_type': '0'
        },
            associate=associate)
        bundle = get_payment_bundle([payment])
        creator = associate.payment_dispatcher.get_creator(bundle)

        compiled = creator.create_bundle()
        expected_result = 'ПоказательДаты=' + datetime.now().strftime('%d-%m-%Y') if associate is Raiffeisen \
            else '/DATE/' + datetime.now().strftime('%y%m%d')

        assert expected_result in compiled


class TestDigitalSignature:

    @pytest.fixture()
    def signature_xml(self, read_fixture):
        return read_fixture('signature.xml', 'utf-8')

    @pytest.fixture()
    def validate_pain006(self, validate_xml):
        """Валидация схемы pain.001.001.06"""
        def wrapper(xml):
            """
            :param str xml:
            :rtype: tuple # is_valid, err_log, err_count
            """

            xml = xml.replace('pain.001.001.03.RU2014.01.xsd', '')
            xml = xml.replace('pain.001.001.03', 'pain.001.001.06')
            xml = xml.encode('utf-8')

            return validate_xml('pain.001.001.06.xsd', xml)

        return wrapper

    def test_digital_signature_xsd(self, signature_xml, validate_xml):
        ds = BundleSign()
        ds.value = XmlSignature(signature_xml).as_bytes()

        assert b'KeyInfo' in ds.value
        assert b'KeyName' in ds.value

        valid, log, err_count = validate_xml('xmldsig-core-schema.xsd', ds.value, [])
        assert err_count == 0, log

    def test_swift_xt_pain(self, signature_xml, validate_pain006, get_source_payment, get_payment_bundle):

        compiled = Unicredit.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment()])).create_bundle()
        compiled = XmlSignature.inject(compiled, signature_xml)

        valid, log, err_count = validate_pain006(compiled)

        assert err_count == 0, log

    def test_swift_xt_pain_without_from_inn(self, signature_xml, validate_pain006, get_source_payment, get_payment_bundle):
        # если нет ИНН получателя, то нужно убрать секцию Cdtr/Id
        payment_attrs = {
            't_inn': ''
        }
        compiled = Unicredit.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment(attrs=payment_attrs)])).create_bundle()
        compiled = XmlSignature.inject(compiled, signature_xml)

        valid, log, err_count = validate_pain006(compiled)

        assert err_count == 0, log

    def test_hex_sign(self, signature_xml):
        ds = XmlSignature(signature_xml)

        assert ds.serial == '12000d3ece1cbf936c80c766fb0000000d3ece'
