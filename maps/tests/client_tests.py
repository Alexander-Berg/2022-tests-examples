from maps.garden.libs.auth.auth_client import HttpAuth
from maps.garden.libs.jns_client import client


class AuthMock(HttpAuth):
    def get_garden_server_auth_headers(self):
        return {"X-Ya-Service-Ticket": "service_ticket"}


def test_send_to_garden_dev(requests_mock):
    request_data = {
        "project": client.JNS_GARDEN_PROJECT,
        "template": client.JNS_DEFAULT_TEMPLATE,
        "target_project": client.JNS_GARDEN_PROJECT,
        "channel": client.JNS_GARDEN_DEVELOPMENT_CHANNEL,
        "params": {
            "message": "message",
        }
    }

    def matcher(data):
        return data.json() == request_data

    requests_mock.post(
        client.JNS_CHANNEL_URL,
        status_code=200,
        request_headers={
            "Content-Type": "application/json",
            "X-Ya-Service-Ticket": "service_ticket"
        },
        additional_matcher=matcher,
    )

    client.notify_garden_dev(AuthMock(), "message")

    assert requests_mock.called
