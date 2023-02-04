from yandex_io.pylibs.functional_tests.utils import regression
from testlib import equalizer_enabled, wait_equalizer
import logging

logger = logging.getLogger(__name__)

EQUALIZER_BAND_COUNT = 5
EQUALIZER_BAND_GAIN = 1
EQUALIZER_DEVICE_CONFIG = {
    "active_preset_id": "custom",
    "bands": [
        {"freq": 60, "gain": EQUALIZER_BAND_GAIN, "width": 90},
        {"freq": 230, "gain": EQUALIZER_BAND_GAIN, "width": 340},
        {"freq": 910, "gain": EQUALIZER_BAND_GAIN, "width": 1340},
        {"freq": 3600, "gain": EQUALIZER_BAND_GAIN, "width": 5200},
        {"freq": 14000, "gain": EQUALIZER_BAND_GAIN, "width": 13000},
    ],
    "custom_preset_bands": [0, 0, 0, 0, 0],
    "enabled": True,
    "smartEnabled": False,
}


@regression
def test_set_gstreamer_equalizer(device, backend_client):
    device.wait_for_authenticate_completion()

    with device.get_service_connector("iohub_services") as iohub_services_connector:

        # check current equalizer and reset it if needed
        config = backend_client.getDeviceConfig()
        if config is None:
            config = {}
        elif equalizer_enabled(config):
            config["equalizer"] = {}
            device.stage_logger.test_stage("Reset equalizer before main tests")
            backend_client.setDeviceConfig(config)
            wait_equalizer(iohub_services_connector, device, 0)

        # set some new equalizer
        config["equalizer"] = EQUALIZER_DEVICE_CONFIG
        backend_client.setDeviceConfig(config)
        device.stage_logger.test_stage("Device config with equalizer have been set")
        wait_equalizer(iohub_services_connector, device, EQUALIZER_BAND_COUNT, EQUALIZER_BAND_GAIN)

        # reset
        config["equalizer"] = {}
        backend_client.setDeviceConfig(config)
        device.stage_logger.test_stage("Device config with empty equalizer have been set")
        wait_equalizer(iohub_services_connector, device, 0)
