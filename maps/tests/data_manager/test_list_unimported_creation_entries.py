from operator import itemgetter

import pytest
from smb.common.testing_utils import dt

from maps_adv.geosmb.harmonist.server.lib.enums import PipelineStep, StepStatus

pytestmark = [pytest.mark.asyncio]


async def test_return_data(factory, dm):
    session_id = await factory.create_log(
        biz_id=123,
        markup={"segment": "orange"},
        valid_clients=[{"email": "some@yandex.ru"}, {"phone": 322}],
    )
    await factory.add_history_record(
        session_id=session_id,
        step=PipelineStep.IMPORTING_CLIENTS,
        status=StepStatus.IN_PROGRESS,
    )

    result = await dm.list_unimported_creation_entries()

    assert result == [
        dict(
            session_id=session_id,
            biz_id=123,
            segment="orange",
            clients=[{"email": "some@yandex.ru"}, {"phone": 322}],
        )
    ]


async def test_return_segment_as_none_if_no_segment_in_markup(factory, dm):
    session_id = await factory.create_log(
        biz_id=123,
        markup={},
        valid_clients=[{"email": "some@yandex.ru"}, {"phone": 322}],
    )
    await factory.add_history_record(
        session_id=session_id,
        step=PipelineStep.IMPORTING_CLIENTS,
        status=StepStatus.IN_PROGRESS,
    )

    result = await dm.list_unimported_creation_entries()

    assert result[0]["segment"] is None


async def test_returns_all_suitable_entries(factory, dm):
    for biz_id in (123, 123, 321):
        session_id = await factory.create_log(
            biz_id=biz_id,
            markup={"segment": "orange"},
            valid_clients=[{"email": "some@yandex.ru"}, {"phone": 322}],
        )
        await factory.add_history_record(
            session_id=session_id,
            step=PipelineStep.IMPORTING_CLIENTS,
            status=StepStatus.IN_PROGRESS,
        )

    result = await dm.list_unimported_creation_entries()

    assert len(result) == 3


async def test_returns_entries_sorted_by_created_at(factory, dm):
    session_id1 = await factory.create_log(
        biz_id=123,
        markup={"segment": "orange"},
        valid_clients=[{"email": "some@yandex.ru"}, {"phone": 322}],
        created_at=dt("2020-02-02 15:00:00"),
    )
    session_id2 = await factory.create_log(
        biz_id=123,
        markup={"segment": "orange"},
        valid_clients=[{"email": "some@yandex.ru"}, {"phone": 322}],
        created_at=dt("2020-02-02 11:00:00"),
    )
    session_id3 = await factory.create_log(
        biz_id=123,
        markup={"segment": "orange"},
        valid_clients=[{"email": "some@yandex.ru"}, {"phone": 322}],
        created_at=dt("2020-02-02 13:00:00"),
    )
    for session_id in [session_id1, session_id2, session_id3]:
        await factory.add_history_record(
            session_id=session_id,
            step=PipelineStep.IMPORTING_CLIENTS,
            status=StepStatus.IN_PROGRESS,
        )

    result = await dm.list_unimported_creation_entries()

    assert list(map(itemgetter("session_id"), result)) == [
        session_id2,
        session_id3,
        session_id1,
    ]


async def test_does_not_return_already_imported_entries(factory, dm):
    session_id = await factory.create_log(
        biz_id=123,
        markup={"segment": "orange"},
        valid_clients=[{"email": "some@yandex.ru"}, {"phone": 322}],
        import_result={"created_amount": 10, "updated_amount": 20},
    )
    await factory.add_history_record(
        session_id=session_id,
        step=PipelineStep.IMPORTING_CLIENTS,
        status=StepStatus.IN_PROGRESS,
    )

    result = await dm.list_unimported_creation_entries()

    assert result == []


async def test_does_not_return_not_validated_entries(factory, dm):
    session_id = await factory.create_log(
        biz_id=123,
        markup={"segment": "orange"},
        valid_clients=[{"email": "some@yandex.ru"}, {"phone": 322}],
        invalid_clients=[{"row": ["some", "values"], "reason": "because"}],
        import_result={"created_amount": 10, "updated_amount": 20},
    )
    await factory.add_history_record(
        session_id=session_id,
        step=PipelineStep.VALIDATING_DATA,
        status=StepStatus.IN_PROGRESS,
    )
    await factory.create_log(
        biz_id=123,
        markup={"segment": "orange"},
        import_result={"created_amount": 10, "updated_amount": 20},
    )

    result = await dm.list_unimported_creation_entries()

    assert result == []


async def test_does_not_return_entries_without_valid_clients(factory, dm):
    session_id = await factory.create_log(
        biz_id=123,
        markup={"segment": "orange"},
        valid_clients=[],
        invalid_clients=[{"row": ["some", "values"], "reason": "because"}],
        import_result={"created_amount": 10, "updated_amount": 20},
    )
    await factory.add_history_record(
        session_id=session_id,
        step=PipelineStep.IMPORTING_CLIENTS,
        status=StepStatus.IN_PROGRESS,
    )

    result = await dm.list_unimported_creation_entries()

    assert result == []


async def test_does_not_return_entries_not_requested_for_import(factory, dm):
    # VALIDATING_DATA FINISHED history records created implicitly
    session_id = await factory.create_log(
        biz_id=123,
        markup={"segment": "orange"},
        valid_clients=[{"email": "some@yandex.ru"}, {"phone": 322}],
        created_at=dt("2020-02-02 15:00:00"),
    )
    await factory.add_history_record(
        session_id=session_id,
        step=PipelineStep.VALIDATING_DATA,
        status=StepStatus.FINISHED,
    )

    result = await dm.list_unimported_creation_entries()

    assert result == []
