import pytest

from maps_adv.geosmb.harmonist.server.lib.enums import StepStatus

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.fixture(autouse=True)
def common_mocks(mocker, dm):
    mocker.patch(
        "maps_adv.geosmb.harmonist.server.lib.domain.Validator.validate_data",
        side_effect=[
            (
                [{"first_name": "Line1"}, {"last_name": "One1"}],
                [{"row": "row", "reason": "because"}],
            ),
            ([{"first_name": "Line2"}, {"last_name": "One2"}], []),
        ],
    )

    dm.list_unvalidated_creation_entries.coro.return_value = [
        dict(
            session_id="session_id_1",
            biz_id=123,
            parsed_input=[["Line1", "One1"], ["Second1", "line1"]],
            markup={
                "ignore_first_line": True,
                "column_type_map": [
                    {"column_type": "FIRST_NAME", "column_number": 1},
                    {"column_type": "LAST_NAME", "column_number": 2},
                ],
            },
        ),
        dict(
            session_id="session_id_2",
            biz_id=321,
            parsed_input=[["Line2", "One2"]],
            markup={
                "ignore_first_line": False,
                "column_type_map": [
                    {"column_type": "FIRST_NAME", "column_number": 3},
                    {"column_type": "LAST_NAME", "column_number": 4},
                ],
            },
        ),
    ]


async def test_fetches_unvalidated_list(domain, dm):
    await domain.process_unvalidated()

    dm.list_unvalidated_creation_entries.assert_called_once()


async def test_validates_unvalidated(domain, dm, mds):
    await domain.process_unvalidated()

    dm.list_unvalidated_creation_entries.assert_called_once()
    dm.submit_validated_clients.assert_any_call(
        session_id="session_id_1",
        biz_id=123,
        validation_step_status=StepStatus.IN_PROGRESS,
        valid_clients=[
            {"first_name": "Line1"},
            {"last_name": "One1"},
        ],
        invalid_clients=[{"row": "row", "reason": "because"}],
    )
    dm.submit_validated_clients.assert_any_call(
        session_id="session_id_2",
        biz_id=321,
        validation_step_status=StepStatus.FINISHED,
        valid_clients=[
            {"first_name": "Line2"},
            {"last_name": "One2"},
        ],
        invalid_clients=[],
    )
    mds.upload_file.assert_called_once()


async def test_returns_none(domain):
    result = await domain.process_unvalidated()

    assert result is None


async def test_not_raises_on_exception(domain, dm, mds):
    dm.submit_validated_clients.coro.side_effect = [Exception, None]

    try:
        await domain.process_unvalidated()
    except:
        pytest.fail("Should not raise")

    assert dm.submit_validated_clients.call_count == 2
