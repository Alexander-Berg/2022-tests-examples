import re
from typing import Optional

from billing.yandex_pay_plus.yandex_pay_plus.tests.lib.functional.mocks.base import BaseMocker


class PassportAddressesMocker(BaseMocker):
    def mock_get_contact(self, contact_id: Optional[str] = None):
        params_regexp = '.*'
        if contact_id is not None:
            params_regexp = fr'\?.*id={contact_id}.*'
        return self.mocker.get(
            re.compile(self.endpoint_url('/contact/get') + params_regexp),
            payload={
                'id': contact_id,
                'phone_number': '+70001112233',
                'email': 'address@email.test',
                'first_name': 'fname',
                'second_name': 'sname',
                'last_name': 'lname',
            },
        )

    def mock_get_address(self, address_id: Optional[str] = None):
        params_regexp = '.*'
        if address_id is not None:
            params_regexp = fr'\?.*id={address_id}.*'
        return self.mocker.get(
            re.compile(self.endpoint_url('/address/get') + params_regexp),
            payload={
                'id': 'ship-a-id',
                'country': 'passp-country',
                'locality': 'passp-locality',
                'street': 'passp-street',
                'building': 'passp-building',
                'location': {
                    'latitude': '30',
                    'longitude': '60',
                },
            },
        )
