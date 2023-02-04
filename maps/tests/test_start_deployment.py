import enum
import datetime as dt
import http.client
import pytz
import pytest

from maps.garden.libs_server.build import build_defs
from maps.garden.sdk.module_traits import module_traits

from . import common

NOW = dt.datetime(2016, 11, 23, 12, 25, 22, tzinfo=pytz.utc)

RELEASE_NAME = "20.12.10-0"


@pytest.fixture
def prepare_data(db, module_helper):
    traits = module_traits.ModuleTraits(
        name="graph_build",
        type=module_traits.ModuleType.REDUCE,
    )
    module_helper.add_module_to_system_contour(traits)

    traits = module_traits.ModuleTraits(
        name="graph_deployment",
        type=module_traits.ModuleType.DEPLOYMENT,
        sources=["graph_build"],
        deploy_steps=[
            "testing",
            "prestable",
            "stable"
        ]
    )
    module_helper.add_module_to_system_contour(traits)

    source_build = build_defs.Build(
        id=1,
        name="graph_build",
        contour_name=common.DEFAULT_SYSTEM_CONTOUR,
        extras={
            "region": "cis1",
            "release_name": RELEASE_NAME,
        },
    )
    db.builds.insert_one(source_build.dict())


def _get_request_body(deploy_step):
    request_body = {
        "contour": common.DEFAULT_SYSTEM_CONTOUR,
        "sources": [
            {
                "name": "graph_build",
                "version": "build_id:1",
            }
        ]
    }
    if deploy_step:
        request_body["properties"] = {
            "deploy_step": deploy_step,
        }
    return request_body


def _get_expected_build_action(deploy_step):
    return [{
        "module_name": "graph_deployment",
        "build_id": 1,
        "from_build_status": None,
        "operation": build_defs.BuildOperationString.CREATE,
        "created_by": "someuser",
        "created_at": NOW,
        "action_status": build_defs.BuildActionStatus.CREATED,
        "build_creation_data": {
            "contour_name": "unittest",
            "sources": [{
                "name": "graph_build",
                "version": "build_id:1",
                "contour_name": "unittest",
                "properties": {
                    "region": "cis1",
                    "release_name": RELEASE_NAME,
                }
            }],
            "extras": {
                "region": "cis1",
                "release_name": RELEASE_NAME,
                "deploy_step": deploy_step,
                "autostarted": False
            },
            "module_version": "2",
            "external_resources": [],
            "build_version": f"{RELEASE_NAME}, {deploy_step}",
            "foreign_key": None,
        },
        "completed_at": None,
        "completion_message": None,
        "task_id": None,
    }]


@pytest.mark.freeze_time(NOW)
def test_success(garden_client, db, prepare_data):
    response = garden_client.post(
        "/modules/graph_deployment/builds/",
        json=_get_request_body(deploy_step="prestable")
    )
    assert response.status_code == http.client.CREATED

    result = list(db.build_actions.find({}, {"_id": False}))
    assert result == _get_expected_build_action(deploy_step="prestable")


@pytest.mark.freeze_time(NOW)
def test_wrong_deploy_step(garden_client, db, prepare_data):
    response = garden_client.post(
        "/modules/graph_deployment/builds/",
        json=_get_request_body(deploy_step="nonexistent")
    )
    assert response.status_code == http.client.BAD_REQUEST
    assert response.get_json() == [{
        "message": "Unsupported deploy step 'nonexistent'. Allowed deploy steps ['testing', 'prestable', 'stable']",
        "type": "error"
    }]


@pytest.mark.freeze_time(NOW)
def test_no_deploy_step(garden_client, db, prepare_data):
    response = garden_client.post(
        "/modules/graph_deployment/builds/",
        json=_get_request_body(deploy_step=None)
    )
    assert response.status_code == http.client.BAD_REQUEST
    assert response.get_json() == [{
        "message": "Build must have specified deploy step: ['testing', 'prestable', 'stable']",
        "type": "error"
    }]


class ReleaseName(enum.Enum):
    SAME = "same"
    DIFFERENT = "different"


class DeployStep(enum.Enum):
    SAME = "same"
    DIFFERENT = "different"


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    (
        "release_name_status", "deploy_step_status", "expected_code"
    ),
    [
        (ReleaseName.SAME, DeployStep.SAME, http.client.CONFLICT),  # Duplicate build
        (ReleaseName.DIFFERENT, DeployStep.SAME, http.client.CONFLICT),  # Can't run 2 builds with the same deploy step
        (ReleaseName.SAME, DeployStep.DIFFERENT, http.client.CREATED),
        (ReleaseName.DIFFERENT, DeployStep.DIFFERENT, http.client.CREATED),
    ],
)
def test_conflict_with_running_build(
    garden_client,
    db,
    prepare_data,
    release_name_status: ReleaseName,
    deploy_step_status: DeployStep,
    expected_code
):
    running_build = build_defs.Build(
        id=1,
        name="graph_deployment",
        contour_name=common.DEFAULT_SYSTEM_CONTOUR,
        status=build_defs.BuildStatus(
            string=build_defs.BuildStatusString.IN_PROGRESS,
            operation=build_defs.BuildOperationString.CREATE,
        ),
        extras={
            "release_name": RELEASE_NAME if release_name_status == ReleaseName.SAME else "some_other_name",
            "deploy_step": "prestable" if deploy_step_status == DeployStep.SAME else "stable",
        }
    )
    db.builds.insert_one(running_build.dict())

    response = garden_client.post(
        "/modules/graph_deployment/builds/",
        json=_get_request_body(deploy_step="prestable")
    )
    assert response.status_code == expected_code

    if expected_code == http.client.CONFLICT:
        assert response.get_json() == [{
            "message": "Mutually exclusive build is in progress: graph_deployment:1",
            "type": "error"
        }]


def _create_build_action(
    action_status: build_defs.BuildActionStatus,
    release_name_status: ReleaseName,
    deploy_step_status: DeployStep
):
    release_name = RELEASE_NAME if release_name_status == ReleaseName.SAME else "some_other_name"
    deploy_step = "prestable" if deploy_step_status == DeployStep.SAME else "stable"
    build_version = f"{release_name}, {deploy_step}"

    return build_defs.BuildAction(
        module_name="graph_deployment",
        build_id=1,
        operation=build_defs.BuildOperationString.CREATE,
        action_status=action_status,
        build_creation_data=build_defs.BuildCreationData(
            contour_name=common.DEFAULT_SYSTEM_CONTOUR,
            extras={
                "release_name": release_name,
                "deploy_step": deploy_step,
            },
            module_version="test",
            build_version=build_version,
        )
    )


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    (
        "release_name_status", "deploy_step_status", "expected_code"
    ),
    [
        (ReleaseName.SAME, DeployStep.SAME, http.client.CONFLICT),
        (ReleaseName.DIFFERENT, DeployStep.SAME, http.client.CREATED),
        (ReleaseName.SAME, DeployStep.DIFFERENT, http.client.CREATED),
        (ReleaseName.DIFFERENT, DeployStep.DIFFERENT, http.client.CREATED),
    ],
)
def test_conflict_with_completed_build(
    garden_client,
    db,
    prepare_data,
    release_name_status,
    deploy_step_status,
    expected_code
):
    build_action = _create_build_action(
        build_defs.BuildActionStatus.COMPLETED,
        release_name_status,
        deploy_step_status,
    )
    db.build_actions.insert_one(build_action.dict())

    response = garden_client.post(
        "/modules/graph_deployment/builds/",
        json=_get_request_body(deploy_step="prestable")
    )
    assert response.status_code == expected_code

    if expected_code == http.client.CONFLICT:
        assert response.get_json() == [{
            "message": f"A release with the name '{RELEASE_NAME}' already exists in contour '{common.DEFAULT_SYSTEM_CONTOUR}'.",
            "type": "error"
        }]


@pytest.mark.freeze_time(NOW)
@pytest.mark.parametrize(
    (
        "release_name_status", "deploy_step_status", "expected_code"
    ),
    [
        (ReleaseName.SAME, DeployStep.SAME, http.client.CONFLICT),  # Duplicate build
        (ReleaseName.DIFFERENT, DeployStep.SAME, http.client.CONFLICT),  # Can't run 2 builds with the same deploy step
        (ReleaseName.SAME, DeployStep.DIFFERENT, http.client.CREATED),
        (ReleaseName.DIFFERENT, DeployStep.DIFFERENT, http.client.CREATED),
    ],
)
def test_conflict_with_another_build_action(
    garden_client,
    db,
    prepare_data,
    release_name_status,
    deploy_step_status,
    expected_code
):
    build_action = _create_build_action(
        build_defs.BuildActionStatus.CREATED,
        release_name_status,
        deploy_step_status,
    )
    db.build_actions.insert_one(build_action.dict())

    response = garden_client.post(
        "/modules/graph_deployment/builds/",
        json=_get_request_body(deploy_step="prestable")
    )
    assert response.status_code == expected_code

    if expected_code == http.client.CONFLICT:
        assert response.get_json() == [{
            "message": "Mutually exclusive build is in progress: graph_deployment:1",
            "type": "error"
        }]


@pytest.mark.freeze_time(NOW)
def test_start_from_ui(garden_client, db, prepare_data):
    request_body = {
        "action": {
            "name":  build_defs.BuildOperationString.CREATE,
            "params": [
                {"name": "buildName", "value": "graph_deployment"},
                {"name": "contourName", "value": common.DEFAULT_SYSTEM_CONTOUR},
                {"name": "deployStep", "value": "prestable"},
                {
                    "name": "sources",
                    "value": ["graph_build:1"]
                }
            ]
        }
    }
    response = garden_client.post("/build/", json=request_body)
    assert response.status_code == http.client.CREATED

    result = list(db.build_actions.find({}, {"_id": False}))
    assert result == _get_expected_build_action(deploy_step="prestable")
