from typing import Optional

import pytest
from smb.common.testing_utils import dt

from maps_adv.common.geoproduct import GeoproductClient
from maps_adv.common.helpers import AsyncContextManagerMock, AsyncIterator, coro_mock
from maps_adv.common.yasms import YasmsClient
from maps_adv.geosmb.booking_yang.proto import orders_pb2
from maps_adv.geosmb.booking_yang.server.lib.clients import YangClient
from maps_adv.geosmb.booking_yang.server.lib.data_managers import OrdersDataManager
from maps_adv.geosmb.booking_yang.server.lib.domains import OrdersDomain


@pytest.fixture
def dm(request):
    if request.node.get_closest_marker("mock_dm"):
        return request.getfixturevalue("_mock_dm")
    return request.getfixturevalue("_dm")


@pytest.fixture
def _dm(db):
    return OrdersDataManager(db)


@pytest.fixture
def _mock_dm():
    class MockDm(OrdersDataManager):
        def __init__(self):
            pass

        create_order = coro_mock()
        fetch_pending_yang_tasks_count = coro_mock()
        iter_created_orders = AsyncIterator([])
        iter_orders_submitted_to_yang = AsyncIterator([])
        iter_orders_notified_to_users = AsyncIterator([])
        list_client_orders = coro_mock()
        list_orders_for_sending_result_event = coro_mock()
        list_orders_without_client = coro_mock()
        list_pending_orders = coro_mock()
        retrieve_earliest_unprocessed_order_time = coro_mock()
        retrieve_order_by_suite = coro_mock()
        retrieve_orders_for_sending_sms = coro_mock()
        update_orders = coro_mock()
        acquire_con_with_tx = AsyncContextManagerMock()
        fetch_actual_orders = coro_mock()
        clear_orders_by_passport = coro_mock()
        check_orders_existence_by_passport = coro_mock()
        lock_order = AsyncContextManagerMock()

    dm = MockDm()
    dm.create_order.coro.return_value = 333
    dm.retrieve_order_by_suite.coro.return_value = {
        "id": 333,
        "permalink": 235643,
        "task_completed_at": None,
        "sms_sent_at": None,
        "customer_phone": "+7 (000) 000-00-00",
        "reservation_datetime": dt("2020-11-11 07:00:00"),
        "reservation_timezone": "Europe/Moscow",
        "customer_name": "Customer",
        "client_id": 111,
        "biz_id": 222,
    }
    dm.retrieve_earliest_unprocessed_order_time.coro.return_value = dt(
        "2020-01-01 00:00:00"
    )
    dm.retrieve_orders_for_sending_sms.coro.return_value = dict(
        id=111,
        reservation_datetime=dt("2020-11-11 07:00:00"),
        reservation_timezone="Europe/Moscow",
        customer_name="Customer",
        customer_phone="+7 (000) 000-00-00",
        booking_verdict="booked",
        booking_meta={"org_name": "Кафе", "org_phone": "+7 (000) 000-00-99"},
    )

    return dm


@pytest.fixture
async def yang(mocker):
    async def list_accepted_assignments(_):
        yield make_yang_list_tasks_response()

    create_task_suite = mocker.patch(
        "maps_adv.geosmb.booking_yang.server.lib.clients.yang_client.YangClient.create_task_suite",  # noqa
        coro_mock(),
    )
    create_task_suite.coro.return_value = dict(
        id="63614047-38c3-4ad4-8a86-99c5c651a9b8", created_at=dt("2016-04-18 12:45:04")
    )
    mocker.patch(
        (
            "maps_adv.geosmb.booking_yang.server.lib"
            ".clients.yang_client.YangClient.list_accepted_assignments"
        ),
        side_effect=list_accepted_assignments,
    )

    async with YangClient(
        url="https://yang.test", token="token", pool_id="123"
    ) as client:
        yield client


@pytest.fixture
async def yasms(mocker, aiotvm):
    mocker.patch("maps_adv.common.yasms.YasmsClient.send_sms", coro_mock())

    async with YasmsClient(
        url="https://yasms.test", sender="test", tvm=aiotvm, tvm_destination="yasms"
    ) as client:
        yield client


@pytest.fixture
async def geoproduct(config, aiotvm, mocker):
    list_reservations = mocker.patch(
        ("maps_adv.common.geoproduct.GeoproductClient.list_reservations"),
        coro_mock(),
    )
    list_reservations.coro.return_value = [
        {
            "id": 123,
            "active": True,
            "permalinks": [123456],
            "data": {"phone_number": "+7 (000) 000-00-99", "some_field": "some_value"},
        }
    ]
    delete_organization_reservations = mocker.patch(
        (
            "maps_adv.common.geoproduct"
            ".GeoproductClient.delete_organization_reservations"
        ),
        coro_mock(),
    )
    delete_organization_reservations.coro.return_value = None

    async with GeoproductClient(
        url=config["GEO_PRODUCT_URL"],
        default_uid=config["GEO_PRODUCT_BIZ_OWNER_UID"],
        tvm_client=aiotvm,
        tvm_destination="geo_product",
    ) as client:
        yield client


@pytest.fixture
def domain(dm, geoproduct, geosearch, yang, yasms, doorman, config):
    return OrdersDomain(
        dm=dm,
        geoproduct=geoproduct,
        geosearch=geosearch,
        yang=yang,
        yasms=yasms,
        doorman=doorman if config.get("DOORMAN_URL") else None,
        new_yang_format=config["NEW_YANG_FORMAT"],
        disconnect_orgs=config["DISCONNECT_ORGS"],
    )


def make_yang_list_tasks_response(new_yang_format: Optional[bool] = False, **overrides):
    task = (
        {"input_values": {"cafe_name": "Кафе", "phones": ["+7 (000) 000-00-01"]}}
        if new_yang_format
        else {"input_values": {"cafe_name": "Кафе", "phone1": "+7 (000) 000-00-01"}}
    )
    solution = (
        dict(booking="booked", call_status="done")
        if new_yang_format
        else dict(
            clicked=False,
            booking="booked",
            call_status="done",
            disconnect=False,
            customer_contacts_transfer=False,
        )
    )

    response = dict(
        id=1,
        task_suite_id="task_suite_id_1",
        task=task,
        solution=solution,
        accepted_at=dt("2020-01-01 18:30:00"),
    )
    response.update(**overrides)

    return response


def make_pb_order_input(**overrides):
    params = dict(
        permalink=12345,
        reservation_datetime=dt("2020-01-01 13:00:00", as_proto=True),
        reservation_timezone="Europe/Moscow",
        person_count=3,
        customer_passport_uid=65432,
        customer_name="Клиент",
        customer_phone="+7 (000) 000-00-00",
        comment="Комментарий",
        call_agreement_accepted=True,
        biz_id=123,
    )
    params.update(overrides)

    return orders_pb2.OrderInput(**params)
