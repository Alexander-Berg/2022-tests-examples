import pytest

from maps_adv.warden.server.lib.data_managers.tasks import UnknownTaskType

pytestmark = [pytest.mark.asyncio]


@pytest.mark.parametrize("restorable", [True, False])
async def test_returns_details_for_type_without_schedule(restorable, factory, dm):
    type_id = await factory.create_task_type(name="task_type", restorable=restorable)

    got = await dm.retrieve_task_type_details("task_type")

    assert got == dict(
        id=type_id, time_limit=300, schedule="* * * * *", restorable=restorable
    )


@pytest.mark.parametrize("restorable", [True, False])
async def test_returns_details_for_type_with_schedule(restorable, factory, dm):
    type_id = await factory.create_task_type(
        name="task_type", schedule="* * * * *", restorable=restorable
    )

    got = await dm.retrieve_task_type_details("task_type")

    assert got == dict(
        id=type_id, time_limit=300, schedule="* * * * *", restorable=restorable
    )


async def test_raises_for_unknown_task_type(dm):
    with pytest.raises(UnknownTaskType):
        await dm.retrieve_task_type_details("task_type")
