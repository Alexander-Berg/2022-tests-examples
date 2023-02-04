import logging
import json
import yandex_io.protos.model_objects_pb2 as model_objects
from .quasar_configuration_http_api import QuasarFirstrundBackendClient

logger = logging.getLogger(__name__)


def test_firstrund_http_connect(device, another_auth_code):
    firstrund_http_port = device.config["firstrund"]["httpPort"]
    backend_url = f"http://localhost:{firstrund_http_port}"
    backendClient = QuasarFirstrundBackendClient(backend_url)
    device.wait_for_listening_start()
    device.wait_for_authenticate_completion()

    with device.get_service_connector("firstrund") as firstrund:

        device.start_setup_mode()
        device.wait_for_message(
            firstrund,
            lambda m: m.HasField('configuration_state')
            and m.configuration_state == model_objects.ConfigurationState.CONFIGURING,
            "Device didn't enter configure mode",
        )

        payload = {"ssid": "none", "password": "password", "plain": True, "xtoken_code": another_auth_code}
        resp = backendClient.connect(json.dumps(payload))
        assert resp.status_code == 200
        result = resp.json()
        assert result["status"] == "ok"
        assert "has_critical_update" in result
        device.wait_for_message(
            firstrund,
            lambda m: m.HasField('configuration_state')
            and m.configuration_state == model_objects.ConfigurationState.CONFIGURED,
            "Device didn't finished set up",
        )
