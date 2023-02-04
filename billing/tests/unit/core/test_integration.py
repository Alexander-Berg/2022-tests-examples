from uuid import uuid4

from sendr_utils import json_value

from hamcrest import all_of, assert_that, contains_string, has_entry, has_key, has_value, not_

from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.enums import IntegrationStatus
from billing.yandex_pay_plus.yandex_pay_plus.storage.entities.integration import Integration


def test_integration_entity_does_not_expose_creds():
    integration = Integration(
        merchant_id=uuid4(),
        psp_id=uuid4(),
        status=IntegrationStatus.READY,
        creds='secret',
    )

    assert_that(
        str(integration),
        all_of(
            contains_string('ready'),
            not_(contains_string('creds')),
            not_(contains_string('secret')),
        )
    )
    assert_that(
        repr(integration),
        all_of(
            contains_string('ready'),
            not_(contains_string('creds')),
            not_(contains_string('secret')),
        )
    )
    assert_that(
        json_value(integration),
        all_of(
            has_entry('status', 'ready'),
            not_(has_key('creds')),
            not_(has_value('secret')),
        )
    )
