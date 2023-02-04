from walle.scenario.constants import ScenarioFsmStatus
from walle.scenario.script import noop_script
from walle.util.misc import drop_none


def get_scenario_json(
    name="test",
    ticket_key="WALLE-2413",
    script=None,
    script_name=None,
    issuer=None,
    script_args=None,
    labels=None,
    status=None,
    autostart=None,
    hosts=None,
    next_check_time=None,
    uses_uuid_keys=None,
    project_id=None,
    data_storage=None,
):
    if not labels:
        labels = {}
    if script:
        script_name = script.name
    data = {
        "name": name,
        "scenario_type": script.name if script else script_name,
        "ticket_key": ticket_key,
        "stage_info": script(script_args).serialize() if script else None,
        "hosts": hosts,
        "next_check_time": next_check_time,
        "issuer": issuer,
        "script_args": script_args,
        "labels": labels,
        "status": status,
        "autostart": autostart,
        "uses_uuid_keys": script.uses_uuids if script else uses_uuid_keys,
        "project_id": project_id,
        "data_storage": data_storage,
    }
    return drop_none(data)


def create_scenarios(
    walle_test,
    size,
    start_idx=0,
    name="test{}",
    issuer="tester",
    labels=None,
    status=ScenarioFsmStatus.CREATED,
    scenario_type=noop_script.name,
    reverse=True,
):
    dicts = []
    labels = labels or {}
    for idx in range(start_idx, start_idx + size):
        scenario = walle_test.mock_scenario(
            dict(
                scenario_id=idx,
                name=name.format(idx),
                next_check_time=0,
                issuer=issuer,
                ticket_key="TEST-{}".format(idx),
                labels=labels,
                status=status,
                scenario_type=scenario_type,
            )
        )
        dicts.append(scenario.to_api_obj())

    if reverse:
        return dicts[::-1]
    return dicts
