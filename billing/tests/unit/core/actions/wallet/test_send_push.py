import pytest

from billing.yandex_pay.yandex_pay.core.actions.wallet.send_push import SendPushAction


@pytest.mark.asyncio
async def test_request_sent_with_expected_body(
    aioresponses_mocker,
    yandex_pay_settings,
):
    def assert_body_callback(url, **kwargs):
        body = kwargs['json']
        push_id = body['data']['push_id']
        transit_id = body['data']['transit_id']
        assert body == {
            'data': {
                'topic_push': yandex_pay_settings.THALES_SUP_TOPIC_PUSH,
                'push_id': push_id,
                'transit_id': transit_id,
            },
            'is_data_only': True,
            'receiver': ['uuid:123'],
            'notification': {'body': '456', 'title': transit_id},
            'project': 'yandexpay',
        }

    aioresponses_mocker.post(
        f'{yandex_pay_settings.SUP_URL}/pushes?dry_run=0',
        status=200,
        payload={
            'id': '123',
            'receiver': [],
            'data': {},
            'request_time': 123,
        },
        callback=assert_body_callback,
    )

    await SendPushAction(app_id='123', data='456').run()
