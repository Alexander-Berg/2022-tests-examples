import asyncio
from .fail_wrapper import FailWrapper

import logging

logger = logging.getLogger(__name__)


class ConnectorsHolder(object):
    def __init__(self, devices, service_name):
        self.connectors = [device.get_service_connector(service_name, False) for device in devices]

    def __enter__(self):
        for connector in self.connectors:
            connector.start()
        return self

    def __getitem__(self, index):
        return self.connectors[index]

    def wait_all(self, matcher, timeout=30):

        results = [0] * len(self.connectors)

        async def wait_message(index, connector, matcher, timeout=30):
            results[index] = connector.wait_for_message(matcher, timeout)
            return

        futures = [wait_message(index, connector, matcher, timeout) for index, connector in enumerate(self.connectors)]
        loop = asyncio.get_event_loop()
        loop.run_until_complete(asyncio.wait(futures))
        return results

    def __exit__(self, exc_type, exc_val, exc_tb):
        for connector in self.connectors:
            connector.close()


class MultiDeviceController(object):
    def __init__(self, devices, stage_logger):
        self.devices = devices
        self.failer = FailWrapper(stage_logger)

    def __getitem__(self, index):
        return self.devices[index]

    def get_connectors(self, service_name):
        connectors = ConnectorsHolder(self.devices, service_name)
        return connectors

    def wait_devices_ready(self):
        for device in self.devices:
            logger.info(f"Awaiting device {device.device_id}...")
            device.wait_for_authenticate_completion()
            device.wait_for_listening_start()
        logger.info("All devices are ready")

    def wait_all(self, connectors, matcher, fail_message, timeout=30):
        results = connectors.wait_all(matcher, timeout)
        logger.error(results)
        fails = [index for index, result in enumerate(results) if result is None]
        if fails:
            self.failer.fail("Devices: {}, {}".format(fails, fail_message))
        return results

    def say_to_any_device(self, wav):
        self.devices[0].start_conversation()
        self.devices[0].say_to_mic(wav)

    def stop_all(self):
        for device in self.devices:
            device.stop()
            device.stage_logger.end_logging()
