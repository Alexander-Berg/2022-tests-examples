# flake8: noqa
import pytest

from maps_adv.geosmb.harmonist.server.lib.enums import FileExtension, InputDataType

pytestmark = [pytest.mark.asyncio, pytest.mark.mock_dm]


@pytest.mark.parametrize(
    "input_params, expected_input_data, expected_data_type",
    [
        (
            dict(text="Literally; any; separated; text"),
            "Literally; any; separated; text",
            InputDataType.TEXT,
        ),
        (
            dict(
                file_content=b"Literally; any; separated; text",
                file_extension=FileExtension.CSV,
            ),
            b"Literally; any; separated; text",
            InputDataType.CSV_FILE,
        ),
    ],
)
async def test_saves_data_in_db(
    domain, dm, input_params, expected_input_data, expected_data_type
):
    await domain.submit_data(biz_id=123, **input_params)

    dm.submit_data.assert_called_with(
        biz_id=123,
        input_data=expected_input_data,
        input_data_type=expected_data_type,
        parsed_input=[["Literally", "any", "separated", "text"]],
    )


async def test_returns_session_id(domain, dm):
    dm.submit_data.coro.return_value = "f8d049c5-3e04-4d60-9041-c056c7524e6c"

    got = await domain.submit_data(text="Literally any text", biz_id=123)

    assert got == "f8d049c5-3e04-4d60-9041-c056c7524e6c"
