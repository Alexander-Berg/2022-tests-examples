import pytest

from hamcrest import assert_that, equal_to, instance_of

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.refund.map_statuses import (
    map_trust_refund_status_to_refund_status,
)
from billing.yandex_pay_plus.yandex_pay_plus.interactions.trust_payments.enums import TrustRefundStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import CashbackRefundStatus


@pytest.mark.parametrize('trust_status', list(TrustRefundStatus))
def test_maps_all_statuses(trust_status):
    result = map_trust_refund_status_to_refund_status(trust_status)
    assert_that(
        result,
        instance_of(CashbackRefundStatus),
    )


@pytest.mark.parametrize('trust_status, expected_db_status', {
    (TrustRefundStatus.SUCCESS, CashbackRefundStatus.SUCCESS),
    (TrustRefundStatus.WAIT_FOR_NOTIFICATION, CashbackRefundStatus.IN_PROGRESS),
    (TrustRefundStatus.ERROR, CashbackRefundStatus.ERROR),
    (TrustRefundStatus.FAILED, CashbackRefundStatus.FAILED),
})
def test_status_mapping_result(trust_status, expected_db_status):
    result = map_trust_refund_status_to_refund_status(trust_status)
    assert_that(result, equal_to(expected_db_status))


def test_unknown_status():
    with pytest.raises(ValueError):
        map_trust_refund_status_to_refund_status('unknown')
