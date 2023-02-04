import pytest

pytestmark = [pytest.mark.asyncio]

url = "/api/send-message"


async def test_returns_correct_status(api):
    await api.post(
        url,
        json=dict(
            user_id=1234,
            message='text'
        ),
        expected_status=200,
    )


async def test_bot_sends_message(api, bot):
    await api.post(
        url,
        json=dict(
            user_id=1234,
            message='text'
        ),
        expected_status=200,
    )

    bot.send_message.assert_called_with(1234, 'text', disable_notification=False)
