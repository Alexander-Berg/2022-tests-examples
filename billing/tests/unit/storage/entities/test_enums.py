import pytest
from pay.lib.entities.order import PaymentMethodType, PaymentStatus

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import PaymentMethodType as StoragePaymentMethodType
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import TransactionStatus


class TestTransactionStatus:
    @pytest.mark.parametrize('status', list(TransactionStatus))
    def test_merchant_api_partner_defined(self, status: TransactionStatus):
        assert isinstance(status.to_merchant_api_status(), PaymentStatus)


class TestStoragePaymentMethodType:
    @pytest.mark.parametrize('lib_payment_method_type', list(PaymentMethodType))
    def test_is_compatible_with_lib_payment_method_type(self, lib_payment_method_type):
        converted_back_and_forth = PaymentMethodType(StoragePaymentMethodType(lib_payment_method_type.value).value)
        assert converted_back_and_forth == lib_payment_method_type
