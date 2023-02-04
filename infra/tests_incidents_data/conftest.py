import time

import mongomock
import pytest

from freezegun import freeze_time

from infra.rtc_sla_tentacles.backend.lib.config.interface import ConfigInterface
from infra.rtc_sla_tentacles.backend.lib.incidents.storage import Incident, IncidentStorage
from infra.rtc_sla_tentacles.backend.lib.metrics.types import SloType


@freeze_time("1970-01-01 00:00:00")
def _get_sample_incidents(config_interface: ConfigInterface):
    fifteen_minutes = 60 * 15
    return [
        Incident(
            None,
            int(time.time()),
            int(time.time()) + fifteen_minutes,
            allocation_zone,
            slo_type,
            assignee,
            10,
            "EXAMPLE-1"
        )
        for allocation_zone in config_interface.get_allocation_zones()
        for slo_type in [SloType.availability, SloType.redeployed_on_time]
        for assignee in config_interface.get_incidents_config()["assignees"]
    ]


def _fill_mongomock_database_with_sample_incidents(config_interface: ConfigInterface,
                                                   incident_storage: IncidentStorage) -> None:
    for incident in _get_sample_incidents(config_interface):
        incident_storage.create_incident(incident)


@pytest.fixture
def incident_storage(config_interface: ConfigInterface) -> IncidentStorage:
    """
        Creates and returns an instance of 'IncidentStorage'
        with mocked MongoClient.
    """
    mongomock_client = mongomock.MongoClient(**config_interface.get_mongo_url_dict())
    incident_storage = IncidentStorage(config_interface, mongomock_client)
    _fill_mongomock_database_with_sample_incidents(config_interface, incident_storage)
    return incident_storage
