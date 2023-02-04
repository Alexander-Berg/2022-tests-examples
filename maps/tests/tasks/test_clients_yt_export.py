import pytest

from maps_adv.geosmb.doorman.server.lib.enums import ClientGender
from maps_adv.geosmb.doorman.server.lib.tasks import ClientsYtExportTask

pytestmark = [pytest.mark.asyncio, pytest.mark.usefixtures("mock_yt")]


async def test_creates_table(dm, mock_yt):
    task = ClientsYtExportTask(
        config={
            "YT_CLUSTER": "hahn",
            "CLIENT_YT_EXPORT_TABLE": "//path/to/table",
            "CLIENT_YT_EXPORT_TOKEN": "fake_token",
        },
        dm=dm,
    )

    await task

    assert mock_yt["create"].called
    assert mock_yt["create"].call_args[0][1] == "//path/to/table"


async def test_writes_data_as_expected(factory, dm, mock_yt):
    client1_id = await factory.create_client(
        biz_id=11,
        phone=322223,
        email="email1@yandex.ru",
        passport_uid=456,
        first_name="Василий",
        last_name="Пупкин",
        gender=ClientGender.MALE,
        labels=["mark-2021", "mark-2022"],
    )
    client2_id = await factory.create_client(
        biz_id=22,
        phone=222333,
        email="email2@yandex.ru",
        passport_uid=654,
        first_name="Василиса",
        last_name="Пупкина",
        gender=ClientGender.FEMALE,
        labels=[],
    )

    task = ClientsYtExportTask(
        config={
            "YT_CLUSTER": "hahn",
            "CLIENT_YT_EXPORT_TABLE": "//path/to/table",
            "CLIENT_YT_EXPORT_TOKEN": "fake_token",
        },
        dm=dm,
    )

    await task

    assert mock_yt["write_table"].called
    assert mock_yt["write_table"].call_args[0][1] == [
        {
            "doorman_id": client1_id,
            "biz_id": 11,
            "phone": "322223",
            "email": "email1@yandex.ru",
            "passport_uid": 456,
            "first_name": "Василий",
            "last_name": "Пупкин",
            "gender": "MALE",
            "segments": ["NO_ORDERS"],
            "labels": ["mark-2021", "mark-2022"],
        },
        {
            "doorman_id": client2_id,
            "biz_id": 22,
            "phone": "222333",
            "email": "email2@yandex.ru",
            "passport_uid": 654,
            "first_name": "Василиса",
            "last_name": "Пупкина",
            "gender": "FEMALE",
            "segments": ["NO_ORDERS"],
            "labels": [],
        },
    ]
