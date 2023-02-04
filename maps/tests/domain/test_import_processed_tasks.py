import asyncio
from datetime import datetime, timezone

import pytest
from smb.common.testing_utils import Any

from maps_adv.common.helpers import AsyncIterator
from maps_adv.geosmb.booking_yang.server.tests.utils import (
    make_yang_list_tasks_response,
)
from maps_adv.geosmb.doorman.client import OrderEvent, Source

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.freeze_time
async def test_marks_order_as_processed(domain, dm):
    await domain.import_processed_tasks()

    dm.update_orders.assert_called_with(
        order_ids=[333],
        task_result_got_at=datetime.now(timezone.utc),
        booking_verdict="booked",
    )


async def test_does_nothing_if_no_unprocessed_orders(domain, dm):
    dm.retrieve_earliest_unprocessed_order_time.coro.return_value = None

    await domain.import_processed_tasks()

    dm.update_orders.assert_not_called()


async def test_does_nothing_if_yang_returns_nothing(domain, dm, yang):
    yang.list_accepted_assignments = AsyncIterator([])

    await domain.import_processed_tasks()

    dm.update_orders.assert_not_called()


@pytest.mark.parametrize(
    "result",
    [
        dict(
            clicked=False,
            customer_contacts_transfer=False,
            call_status="done",
            booking="rejection",
            not_booked="not_by_phone",
        ),
        dict(
            clicked=False,
            customer_contacts_transfer=False,
            call_status="done",
            booking="rejection",
            not_booked="not_cafe",
        ),
    ],
)
@pytest.mark.parametrize("disconnect", [True, False])
@pytest.mark.config(DISCONNECT_ORGS=True)
async def test_disconnects_org_if_not_cafe_or_not_by_phone(
    disconnect, result, domain, yang, geoproduct
):
    result["disconnect"] = disconnect

    yang.list_accepted_assignments = AsyncIterator(
        [make_yang_list_tasks_response(solution=result)]
    )

    await domain.import_processed_tasks()

    geoproduct.delete_organization_reservations.assert_called_with(permalink=235643)


@pytest.mark.parametrize(
    "result",
    [
        dict(
            clicked=False,
            booking="booked",
            call_status="done",
            customer_contacts_transfer=False,
        ),
        dict(
            clicked=False,
            booking=None,
            customer_contacts_transfer=False,
            call_status="cant",
            not_booked=None,
        ),
        dict(
            clicked=False,
            customer_contacts_transfer=True,
            call_status="done",
            booking="rejection",
            not_booked="no_place",
        ),
        dict(
            clicked=False,
            customer_contacts_transfer=True,
            call_status="done",
            booking="rejection",
            not_booked="not_enough_information",
            comment_not_enough_information="Нужен депозит",
        ),
    ],
)
@pytest.mark.config(DISCONNECT_ORGS=True)
async def test_disconnect_org_if_requested(result, domain, yang, geoproduct):
    result["disconnect"] = True

    yang.list_accepted_assignments = AsyncIterator(
        [make_yang_list_tasks_response(solution=result)]
    )

    await domain.import_processed_tasks()

    geoproduct.delete_organization_reservations.assert_called_with(permalink=235643)


@pytest.mark.parametrize(
    "result",
    [
        dict(
            clicked=False,
            booking="booked",
            call_status="done",
            customer_contacts_transfer=False,
        ),
        dict(
            clicked=False,
            booking=None,
            customer_contacts_transfer=False,
            call_status="cant",
            not_booked=None,
        ),
        dict(
            clicked=False,
            customer_contacts_transfer=True,
            call_status="done",
            booking="rejection",
            not_booked="no_place",
        ),
        dict(
            clicked=False,
            customer_contacts_transfer=True,
            call_status="done",
            booking="rejection",
            not_booked="not_enough_information",
            comment_not_enough_information="Нужен депозит",
        ),
    ],
)
async def test_does_not_disconnect_org_if_not_requested(
    result, domain, yang, geoproduct
):
    result["disconnect"] = False

    yang.list_accepted_assignments = AsyncIterator(
        [make_yang_list_tasks_response(solution=result)]
    )

    await domain.import_processed_tasks()

    geoproduct.delete_organization_reservations.assert_not_called()


@pytest.mark.parametrize(
    "result",
    [
        dict(
            clicked=False,
            booking="booked",
            call_status="done",
            customer_contacts_transfer=False,
        ),
        dict(
            clicked=False,
            booking=None,
            customer_contacts_transfer=False,
            call_status="cant",
            not_booked=None,
        ),
        dict(
            clicked=False,
            customer_contacts_transfer=True,
            call_status="done",
            booking="rejection",
            not_booked="no_place",
        ),
        dict(
            clicked=False,
            customer_contacts_transfer=True,
            call_status="done",
            booking="rejection",
            not_booked="not_enough_information",
            comment_not_enough_information="Нужен депозит",
        ),
        dict(
            clicked=False,
            customer_contacts_transfer=False,
            call_status="done",
            booking="rejection",
            not_booked="not_by_phone",
        ),
        dict(
            clicked=False,
            customer_contacts_transfer=False,
            call_status="done",
            booking="rejection",
            not_booked="not_cafe",
        ),
    ],
)
@pytest.mark.config(DISCONNECT_ORGS=False)
async def test_does_not_disconnect_org_in_any_case_if_disconnecting_off(
    result, domain, yang, geoproduct
):
    result["disconnect"] = True

    yang.list_accepted_assignments = AsyncIterator(
        [make_yang_list_tasks_response(solution=result)]
    )

    await domain.import_processed_tasks()

    geoproduct.delete_organization_reservations.assert_not_called()


@pytest.mark.parametrize(
    "result",
    [
        # current new format
        dict(booking="rejection", not_booked="not_cafe", call_status="done"),
        dict(booking="rejection", not_booked="not_by_phone", call_status="done"),
        # future new format
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="not_by_phone",
            alternative=None,
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="not_cafe",
            alternative=None,
            call_status="done",
        ),
    ],
)
@pytest.mark.parametrize("disconnect", [True, False])
@pytest.mark.config(NEW_YANG_FORMAT=True, DISCONNECT_ORGS=True)
async def test_disconnect_org_if_not_cafe_or_not_by_phone_in_new_format(
    disconnect, result, domain, yang, geoproduct
):
    result["disconnect"] = disconnect

    yang.list_accepted_assignments = AsyncIterator(
        [make_yang_list_tasks_response(new_yang_format=True, solution=result)]
    )

    await domain.import_processed_tasks()

    geoproduct.delete_organization_reservations.assert_called_with(permalink=235643)


@pytest.mark.parametrize(
    "result",
    [
        # current new format
        dict(booking="booked", call_status="done"),
        dict(call_status="cant"),
        # future new format
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="booked",
            not_booked=None,
            alternative=None,
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking=None,
            not_booked=None,
            alternative=None,
            call_status="cant",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="no_place",
            alternative="customer_contacts_transfer",
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="not_enough_information",
            alternative="customer_contacts_transfer",
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="another",
            alternative="customer_contacts_transfer",
            call_status="done",
        ),
    ],
)
@pytest.mark.config(NEW_YANG_FORMAT=True, DISCONNECT_ORGS=True)
async def test_disconnect_org_if_requested_in_new_format(
    result, domain, yang, geoproduct
):
    result["disconnect"] = True

    yang.list_accepted_assignments = AsyncIterator(
        [make_yang_list_tasks_response(new_yang_format=True, solution=result)]
    )

    await domain.import_processed_tasks()

    geoproduct.delete_organization_reservations.assert_called_with(permalink=235643)


@pytest.mark.parametrize(
    "result",
    [
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="booked",
            not_booked=None,
            alternative=None,
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking=None,
            not_booked=None,
            alternative=None,
            call_status="cant",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="no_place",
            alternative="customer_contacts_transfer",
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="not_enough_information",
            alternative="customer_contacts_transfer",
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="another",
            alternative="customer_contacts_transfer",
            call_status="done",
        ),
    ],
)
@pytest.mark.config(NEW_YANG_FORMAT=True, DISCONNECT_ORGS=True)
async def test_does_not_disconnect_org_if_not_requested_in_new_format(
    result, domain, yang, geoproduct
):
    result["disconnect"] = False

    yang.list_accepted_assignments = AsyncIterator(
        [make_yang_list_tasks_response(new_yang_format=True, solution=result)]
    )

    await domain.import_processed_tasks()

    geoproduct.delete_organization_reservations.assert_not_called()


@pytest.mark.parametrize(
    "result",
    [
        # current new format
        dict(call_status="cant"),
        dict(booking="booked", call_status="done"),
        dict(booking="rejection", not_booked="not_cafe", call_status="done"),
        dict(booking="rejection", not_booked="not_by_phone", call_status="done"),
        # future new format
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="booked",
            not_booked=None,
            alternative=None,
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking=None,
            not_booked=None,
            alternative=None,
            call_status="cant",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="no_place",
            alternative="customer_contacts_transfer",
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="not_enough_information",
            alternative="customer_contacts_transfer",
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="another",
            alternative="customer_contacts_transfer",
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="not_by_phone",
            alternative=None,
            call_status="done",
        ),
        dict(
            customer_contacts_transfer=False,
            clicked=True,
            booking="rejection",
            not_booked="not_cafe",
            alternative=None,
            call_status="done",
        ),
    ],
)
@pytest.mark.config(NEW_YANG_FORMAT=True, DISCONNECT_ORGS=False)
async def test_does_not_disconnect_org_in_any_case_if_disconnecting_off_in_new_format(
    result, domain, yang, geoproduct
):
    result["disconnect"] = True

    yang.list_accepted_assignments = AsyncIterator(
        [make_yang_list_tasks_response(new_yang_format=True, solution=result)]
    )

    await domain.import_processed_tasks()

    geoproduct.delete_organization_reservations.assert_not_called()


@pytest.mark.parametrize(
    "result, expected_verdict",
    [
        (
            dict(
                clicked=False,
                booking="booked",
                call_status="done",
                customer_contacts_transfer=False,
            ),
            "booked",
        ),
        (
            dict(
                clicked=False,
                customer_contacts_transfer=True,
                call_status="done",
                booking="rejection",
                not_booked="no_place",
            ),
            "no_place_contacts_transfered",
        ),
        (
            dict(
                clicked=False,
                customer_contacts_transfer=False,
                call_status="done",
                booking="rejection",
                not_booked="no_place",
            ),
            "no_place",
        ),
        (
            dict(
                clicked=False,
                customer_contacts_transfer=True,
                call_status="done",
                booking="rejection",
                not_booked="not_enough_information",
                comment_not_enough_information="Нужен депозит",
            ),
            "not_enough_information_contacts_transfered",
        ),
        (
            dict(
                clicked=False,
                customer_contacts_transfer=False,
                call_status="done",
                booking="rejection",
                not_booked="not_enough_information",
                comment_not_enough_information="Нужен депозит",
            ),
            "not_enough_information",
        ),
        (
            dict(
                clicked=False,
                booking=None,
                customer_contacts_transfer=False,
                call_status="cant",
                not_booked=None,
            ),
            "call_failed",
        ),
        (
            dict(
                clicked=False,
                customer_contacts_transfer=False,
                call_status="done",
                booking="rejection",
                not_booked="not_by_phone",
            ),
            "generic_failure",
        ),
        (
            dict(
                clicked=False,
                customer_contacts_transfer=False,
                call_status="done",
                booking="rejection",
                not_booked="not_cafe",
            ),
            "generic_failure",
        ),
    ],
)
@pytest.mark.freeze_time
async def test_updates_booking_verdict(domain, dm, yang, result, expected_verdict):
    yang.list_accepted_assignments = AsyncIterator(
        [make_yang_list_tasks_response(solution=result)]
    )

    await domain.import_processed_tasks()

    dm.update_orders.assert_called_with(
        order_ids=[333],
        task_result_got_at=datetime.now(timezone.utc),
        booking_verdict=expected_verdict,
    )


@pytest.mark.config(NEW_YANG_FORMAT=True)
@pytest.mark.parametrize(
    "result, expected_verdict",
    [
        # Current new format
        (dict(booking="booked", call_status="done"), "booked"),
        (dict(call_status="cant"), "call_failed"),
        (
            dict(booking="rejection", not_booked="not_cafe", call_status="done"),
            "generic_failure",
        ),
        (
            dict(booking="rejection", not_booked="not_by_phone", call_status="done"),
            "generic_failure",
        ),
        # Future new format
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="booked",
                not_booked=None,
                alternative=None,
                call_status="done",
            ),
            "booked",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking=None,
                not_booked=None,
                alternative=None,
                call_status="cant",
            ),
            "call_failed",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="not_by_phone",
                alternative=None,
                call_status="done",
            ),
            "generic_failure",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="no_place",
                alternative="customer_contacts_transfer",
                call_status="done",
            ),
            "no_place_contacts_transfered",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="no_place",
                alternative="call_redirect",
                call_status="done",
            ),
            "no_place",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="no_place",
                alternative="no_alternative",
                call_status="done",
            ),
            "no_place",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="not_cafe",
                alternative=None,
                call_status="done",
            ),
            "generic_failure",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="not_enough_information",
                alternative="customer_contacts_transfer",
                call_status="done",
            ),
            "not_enough_information_contacts_transfered",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="not_enough_information",
                alternative="call_redirect",
                call_status="done",
            ),
            "not_enough_information",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="not_enough_information",
                alternative="no_alternative",
                call_status="done",
            ),
            "not_enough_information",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="another",
                alternative="customer_contacts_transfer",
                call_status="done",
            ),
            "generic_failure",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="another",
                alternative="call_redirect",
                call_status="done",
            ),
            "generic_failure",
        ),
        (
            dict(
                customer_contacts_transfer=False,
                clicked=True,
                booking="rejection",
                not_booked="another",
                alternative="no_alternative",
                call_status="done",
            ),
            "generic_failure",
        ),
    ],
)
@pytest.mark.freeze_time
async def test_updates_booking_verdict_for_new_format(
    domain, dm, yang, result, expected_verdict
):
    yang.list_accepted_assignments = AsyncIterator(
        [make_yang_list_tasks_response(new_yang_format=True, solution=result)]
    )

    await domain.import_processed_tasks()

    dm.update_orders.assert_called_with(
        order_ids=[333],
        task_result_got_at=datetime.now(timezone.utc),
        booking_verdict=expected_verdict,
    )


@pytest.mark.config(DOORMAN_URL="http://doorman.server")
@pytest.mark.freeze_time
@pytest.mark.parametrize(
    "booking, event_type",
    (("booked", OrderEvent.ACCEPTED), (None, OrderEvent.REJECTED)),
)
async def test_adds_event_about_order_in_doorman(
    booking, event_type, doorman, domain, dm, yang
):
    yang.list_accepted_assignments = AsyncIterator(
        [
            make_yang_list_tasks_response(
                solution=dict(
                    clicked=False,
                    booking=booking,
                    disconnect=False,
                    call_status="done",
                    customer_contacts_transfer=False,
                )
            )
        ]
    )

    await domain.import_processed_tasks()

    await asyncio.sleep(0.1)

    doorman.add_order_event.assert_called_with(
        biz_id=222,
        client_id=111,
        event_type=event_type,
        event_timestamp=datetime.now(timezone.utc),
        source=Source.BOOKING_YANG,
        order_id=333,
    )


@pytest.mark.config(DOORMAN_URL="http://doorman.server")
async def test_works_correctly_on_any_exception_on_disconnect_attempt(
    doorman, domain, dm, yang, geoproduct
):
    geoproduct.delete_organization_reservations.side_effect = Exception
    yang.list_accepted_assignments = AsyncIterator(
        [
            make_yang_list_tasks_response(
                solution=dict(
                    clicked=False,
                    booking="booked",
                    disconnect=True,
                    call_status="done",
                    customer_contacts_transfer=False,
                )
            )
        ]
    )

    await domain.import_processed_tasks()

    await asyncio.sleep(0.1)

    doorman.add_order_event.assert_called_with(
        biz_id=222,
        client_id=111,
        event_type=OrderEvent.ACCEPTED,
        event_timestamp=Any(datetime),
        source=Source.BOOKING_YANG,
        order_id=333,
    )


@pytest.mark.parametrize("booking", ("booked", None))
async def test_does_not_add_event_about_order_in_doorman_if_not_configured(
    booking, doorman, domain, dm, yang
):
    yang.list_accepted_assignments = AsyncIterator(
        [
            make_yang_list_tasks_response(
                solution=dict(
                    clicked=False,
                    disconnect=False,
                    booking=booking,
                    call_status="done",
                    customer_contacts_transfer=False,
                )
            )
        ]
    )

    await domain.import_processed_tasks()

    await asyncio.sleep(0.1)

    assert doorman.add_order_event.called is False
