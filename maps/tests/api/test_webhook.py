import pytest

from maps_adv.geosmb.notification_bot.server.lib.bot.utils.answers import texts

pytestmark = [pytest.mark.asyncio]

url = "/webhook"


async def test_returns_correct_status(api):
    await api.post(
        url,
        json={
            "update_id": 10000,
            "message": {
                "date": 1441645532,
                "chat": {
                    "last_name": "Test Lastname",
                    "id": 1111111,
                    "first_name": "Test",
                    "username": "Test",
                    "type": "private"
                },
                "message_id": 1365,
                "from": {
                    "last_name": "Test Lastname",
                    "id": 1111111,
                    "first_name": "Test",
                    "username": "Test",
                    "is_bot": False
                },
                "text": "text"
            }
        },
        expected_status=200,
    )


async def test_bot_replies(api, bot):
    await api.post(
        url,
        json={
            "update_id": 10000,
            "message": {
                "date": 1441645532,
                "chat": {
                    "last_name": "Test Lastname",
                    "id": 1111111,
                    "first_name": "Test",
                    "username": "Test",
                    "type": "private"
                },
                "message_id": 1365,
                "from": {
                    "last_name": "Test Lastname",
                    "id": 1111111,
                    "first_name": "Test",
                    "username": "Test",
                    "is_bot": False
                },
                "text": "text"
            }
        },
        expected_status=200,
    )

    bot.send_message.assert_called_with(1111111, texts['echo'])
