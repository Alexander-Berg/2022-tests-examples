import pytest
from unittest.mock import call


pytestmark = [pytest.mark.asyncio]

url = "/webhook"


def json_gen(user, date, text):
    json = {
        "update_id": 10000,
        "message": {
            "date": date,
            "chat": {
                "last_name": "Test Lastname",
                "id": user,
                "first_name": "Test",
                "username": "Test",
                "type": "private"
            },
            "message_id": 1365,
            "from": {
                "last_name": "Test Lastname",
                "id": user,
                "first_name": "Test",
                "username": "Test",
                "is_bot": False
            },
            "text": text
        }
    }
    return json


async def test_bot_antiflood(api, bot):
    for _ in range(2):
        await api.post(
            url,
            json=json_gen(1234, 1441645532, text='test'),
            expected_status=200,
        )

    calls = [
        call('1234', 'Бот запущен и готов к работе'),
        call(1234,
             'Этот бот не умеет общаться или отвечать на вопросы, он рассылает автоматические уведомления. ' +
             'Если вам нужно что-то уточнить, перейдите ' +
             '<a href="https://yandex.ru/support/business-priority/troubleshooting/favplacement.html">по ссылке</a>' +
             '.'),
        call(1234, 'Вы ввели много команд подряд — подождите пару минут, и всё заработает.')]
    bot.send_message.assert_has_calls(calls, any_order=False)


async def test_bot_antiflood_disable(api, bot):
    await api.post(
        url,
        json=json_gen(3421, 1441645532, text='test'),
        expected_status=200,
    )
    await api.post(
        url,
        json=json_gen(3421, 1441645533, text='test'),
        expected_status=200,
    )
    await api.post(
        url,
        json=json_gen(3421, 1441645540, text='/stop'),
        expected_status=200,
    )
    calls = [
        call('1234', 'Бот запущен и готов к работе'),
        call(3421,
             'Этот бот не умеет общаться или отвечать на вопросы, он рассылает автоматические уведомления. ' +
             'Если вам нужно что-то уточнить, перейдите ' +
             '<a href="https://yandex.ru/support/business-priority/troubleshooting/favplacement.html">по ссылке</a>' +
             '.'),
        call(3421, 'Вы ввели много команд подряд — подождите пару минут, и всё заработает.'),
        call(3421,
             'Вы больше не будете получать здесь уведомления о новых заявках и заказах. Чтобы включить их снова,' +
             ' нажмите /start.'
             )
    ]
    bot.send_message.assert_has_calls(calls, any_order=False)
