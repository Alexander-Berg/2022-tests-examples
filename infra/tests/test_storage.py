import dataclasses
import time

import pytest

from infra.rtc_sla_tentacles.backend.lib.incidents.storage import Incident, IncidentFilter, IncidentStorage, \
    IncidentStorageException
from infra.rtc_sla_tentacles.backend.lib.metrics.types import SloType


def test_read_all_incidents(incident_storage: IncidentStorage):
    incident_filter_all_incidents = IncidentFilter()
    total, total_minutes_out_of_slo, incidents = incident_storage.read_incidents(incident_filter_all_incidents,
                                                                                 limit=100)
    actual_result = [
        incident
        for incident in incidents
    ]
    assert total == 18
    assert total_minutes_out_of_slo == 180
    assert len(actual_result) == 18


def test_create_update_delete_incident(incident_storage: IncidentStorage):
    now = int(time.time())

    new_incident_obj = Incident(
        None,
        start_time_ts=now - 60 * 60,
        end_time_ts=now - 30 * 60,
        allocation_zone="rtc_sla_tentacles_testing_yp_lite_daemonset",
        slo_type=SloType.availability,
        assignee="team_a",
        minutes_out_of_slo=5,
        startrek_ticket_id="EXAMPLE-42"
    )
    stored_id = incident_storage.create_incident(new_incident_obj)

    stored_obj = incident_storage.read_incident_data(stored_id)
    stored_data_dict = dataclasses.asdict(stored_obj)

    assert new_incident_obj == stored_obj

    stored_data_dict["startrek_ticket_id"] = "EXAMPLE-84"
    modified_obj = Incident(**stored_data_dict)
    incident_storage.update_incident(modified_obj)
    stored_obj = incident_storage.read_incident_data(stored_id)
    assert stored_obj.startrek_ticket_id == "EXAMPLE-84"

    assert incident_storage.delete_incident(stored_id) is None
    with pytest.raises(IncidentStorageException):
        incident_storage.read_incident_data(stored_id)


def test_filter_specific_incidents(incident_storage: IncidentStorage):
    incident_filter_specific_incidents = IncidentFilter(
        ended_before_ts=int(time.time()),
        assignees=["team_a", "team_b"],
        slo_types=[SloType.availability]
    )
    total, total_minutes_out_of_slo, incidents = incident_storage.read_incidents(incident_filter_specific_incidents,
                                                                                 limit=100, offset=5)
    actual_result = [
        incident
        for incident in incidents
    ]
    assert total == 6
    assert total_minutes_out_of_slo == 60
    assert len(actual_result) == 1  # 6 total, with '5' as offset
