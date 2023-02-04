import re
from datetime import timedelta
from decimal import Decimal

from sendr_utils import utcnow

from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.mocks.base import BaseMocker


class YandexDeliveryMocker(BaseMocker):
    def mock_delivery_methods(self, express: bool = False, same_day: bool = False):
        now = utcnow()
        return self.mocker.post(
            re.compile(self.endpoint_url('/v1/delivery-methods')),
            payload={
                'express_delivery': {
                    'allowed': express,
                },
                'same_day_delivery': {
                    'allowed': same_day,
                    'available_intervals': [
                        {
                            'from': (now + timedelta(hours=1)).isoformat(),
                            'to': (now + timedelta(hours=3)).isoformat(),
                        }
                    ]
                },
            },
        )

    def mock_check_price(self, price: Decimal = Decimal('15')):
        return self.mocker.post(
            re.compile(self.endpoint_url('/v1/check-price')),
            payload={
                'price': str(price),
            },
        )

    def mock_create_claim(self, claim_id: str):
        return self.mocker.post(
            re.compile(self.endpoint_url('/v2/claims/create')),
            payload={
                'id': claim_id,
                'version': 1,
                'revision': 1,
                'status': 'estimating',
                'updated_ts': '2022-02-22T00:00:00+00:00',
            },
        )

    def mock_cancel_info(self, claim_id: str, cancel_state: str):
        return self.mocker.post(
            re.compile(self.endpoint_url('/v2/claims/cancel-info') + rf'\?.*claim_id={claim_id}.*'),
            payload={
                'cancel_state': cancel_state,
            },
        )
