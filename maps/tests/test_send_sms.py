import pytest
from aiohttp.web import Response

from maps_adv.common.yasms.client import IncorrectParams, YasmsClient, YasmsError

pytestmark = [pytest.mark.asyncio]


success_response = b"""\
<?xml version="1.0" encoding="windows-1251"?>
<doc>
    <message-sent id="127000000003456" />
    <gates ids="15" />
</doc>"""

fail_response = b"""\
<?xml version="1.0" encoding="windows-1251"?>
<doc>
    <error>User does not have an active phone to receive messages</error>
    <errorcode>NOCURRENT</errorcode>
</doc>
"""


async def test_sends_correct_request(yasms_client, mock_yasms_api):
    request_path = None
    request_headers = None
    request_params = None

    async def yasms_handler(request):
        nonlocal request_path, request_headers, request_params
        request_path = request.path
        request_params = dict(request.query)
        request_headers = dict(request.headers)
        return Response(status=200, body=success_response)

    mock_yasms_api(yasms_handler)

    await yasms_client.send_sms(
        phone=71234567890, text="we will always kek", identity="id-123"
    )

    assert request_path == "/sendsms"
    assert request_params == {
        "phone": "+71234567890",
        "sender": "awesome_sender",
        "text": "we will always kek",
        "identity": "id-123",
    }


async def test_returns_sms_id_if_success(yasms_client, mock_yasms_api):
    mock_yasms_api(Response(status=200, body=success_response))

    sms_id = await yasms_client.send_sms(phone=71234567890, text="we will always kek")

    assert sms_id == "127000000003456"


@pytest.mark.parametrize(
    "phone, message", [(None, "meow"), ("", "meow"), (23432, None), (23432, "")]
)
async def test_raises_if_incorrect_input(phone, message, yasms_client):
    with pytest.raises(
        IncorrectParams,
        match="Phone number and message text must be set.",
    ):
        await yasms_client.send_sms(phone=phone, text=message)


@pytest.mark.parametrize("sender", [None, ""])
async def test_raises_if_incorrect_sender(sender, aiotvm):
    with pytest.raises(IncorrectParams, match="Sender must be specified."):
        YasmsClient(
            url="http://yasms.api",
            tvm=aiotvm,
            tvm_destination="anywhere",
            sender=sender,
        )


async def test_raises_if_sending_fails(yasms_client, mock_yasms_api):
    mock_yasms_api(Response(status=200, body=fail_response))

    with pytest.raises(YasmsError) as exc_info:
        await yasms_client.send_sms(phone=71234567890, text="message")

    assert exc_info.value.args == (
        "NOCURRENT",
        "User does not have an active phone to receive messages",
    )
