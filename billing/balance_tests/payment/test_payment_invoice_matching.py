# coding: utf-8
"""
Тестирование матчинга балансовых платежей и лицевых счетов (BALANCE-36977)
"""

import pytest
from decimal import Decimal

from balance.payments.payment_invoice_matching import PaymentInvoiceMatcher
from tests import object_builder as ob


@pytest.fixture()
def matcher():
    yield PaymentInvoiceMatcher()


class TestSearchOriginInvoice:
    """Тестирует правильность подбора инвойсов для матчинга"""

    def test_personal_account(self, session, matcher):
        invoice = ob.InvoiceBuilder.construct(session)
        assert matcher.get_original_invoice(invoice) == invoice

    def test_charge_note(self, session, matcher):
        """Тестирует, что для инвойсов-квитанций подбирается лицевой счет, который пополняет квитанция"""
        charge_note = ob.InvoiceBuilder.construct(session, type='charge_note', single_account_number=True)
        personal_account = charge_note.charge_invoice
        assert matcher.get_original_invoice(charge_note) == personal_account


class TestSearchOriginPayment:
    """Тестирует правильность подбора исходного платежа для матчинга"""

    def test_trust_payment(self, session, matcher):
        """Для обычных TRUST платежей данные берем из этого же платежа
        (кейс практикума, когда invioice_id записан прямо в трастовом платеже)"""
        payment = ob.TrustPaymentBuilder.construct(session)
        assert matcher.get_original_payment(payment) == payment

    def test_trust_api_payment(self, session, matcher):
        """Для балансовых TRUST_API платежей (логических) данные берем из TRUST платежа (физического)"""
        invoice = ob.InvoiceBuilder.construct(session)
        trust_payment = ob.TrustPaymentBuilder.construct(session)
        payment = ob.TrustApiPaymentBuilder.construct(session,
                                                      transaction_id=trust_payment.purchase_token,
                                                      invoice=invoice)
        assert matcher.get_original_payment(payment) == trust_payment

    def test_refund(self, session, matcher):
        """Для рефандов данные берем из оригинального платежа"""
        payment = ob.TrustPaymentBuilder.construct(session)
        refund = ob.RefundBuilder.construct(session, payment=payment, amount=Decimal('100'),
                                            description='', operation=None)
        assert matcher.get_original_payment(refund) == payment


class TestPaymentInvoiceMatch:
    """Тестирует логику матчинга платежей и инвойсов (BALANCE-36977)"""

    def test_match(self, session, matcher):
        invoice = ob.InvoiceBuilder.construct(session, type='charge_note', single_account_number=True,
                                              amount=Decimal('200'))
        orig_invoice = invoice.charge_invoice
        orig_payment = ob.TrustPaymentBuilder.construct(session)
        payment = ob.TrustApiPaymentBuilder.construct(session, invoice=invoice,
                                                      transaction_id=orig_payment.purchase_token)

        match_data = PaymentInvoiceMatcher().match(payment)

        expected_match_data = {
            'payment_id': orig_payment.id,
            'paysys_code': orig_payment.paysys_code,
            'trust_payment_id': orig_payment.trust_payment_id,
            'transaction_id': orig_payment.transaction_id,
            'amount': Decimal('200.00'),

            'invoice_id': orig_invoice.id,
            'invoice_type': orig_invoice.type,
            'invoice_external_id': orig_invoice.external_id,
        }
        assert match_data == expected_match_data

    def test_payment_without_invoice(self, session, matcher):
        """Тестирует возврат ошибки, если у платежа не установлен инвойс"""

        payment = ob.YandexMoneyPaymentBuilder.construct(session, invoice=None)
        with pytest.raises(RuntimeError) as err:
            matcher.match(payment)
        assert 'No invoice to match' in err.value.message
