import pytest

from maps_adv.geosmb.harmonist.proto.pipeline_pb2 import CreateClientsInput, Error, File

pytestmark = [pytest.mark.asyncio]

URL = "/v1/submit_data/"


@pytest.mark.parametrize(
    "invalid_field, expected_description",
    [
        (dict(biz_id=0), "biz_id: ['Must be at least 1.']"),
        (dict(text="", file=None), "text: ['Shorter than minimum length 1.']"),
        (
            dict(
                text=None,
                file=File(
                    content=b"",
                    extension=File.Extension.CSV,
                ),
            ),
            "file: {'content': ['Shorter than minimum length 1.']}",
        ),
    ],
)
async def test_errored_on_incorrect_input(invalid_field, expected_description, api):
    input_params = dict(
        biz_id=123,
        text="Literally, any, separated, text",
        file=File(
            content=b"Literally, any, separated, text", extension=File.Extension.CSV
        ),
    )
    input_params.update(**invalid_field)

    got = await api.post(
        URL,
        proto=CreateClientsInput(**input_params),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(code=Error.VALIDATION_ERROR, description=expected_description)


async def test_returns_error_if_pass_no_data(api):
    got = await api.post(
        URL,
        proto=CreateClientsInput(biz_id=123),
        decode_as=Error,
        expected_status=400,
    )

    assert got == Error(
        code=Error.VALIDATION_ERROR,
        description="_schema: ['One of data fields must be set: text, file']",
    )
