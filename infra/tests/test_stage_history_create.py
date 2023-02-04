from infra.deploy_notifications_controller.lib.models.stage import StageState, Meta, Spec, Status, Stage
from infra.deploy_notifications_controller.lib.models.stage_history_change import StageHistoryCreate
from test_stage_history_change_utils import create_stage, default_old_spec_revision, default_old_du_revision, \
    default_event, create_expected_message, create_input_data


def test_create():
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)
    create = StageHistoryCreate(event=default_event, meta=Meta(project_id=stage.project_id + '_new'))

    expected_message = create_expected_message(stage, create)

    actual_message, output_data = create.process_changes(create_input_data(stage))

    assert actual_message == expected_message
    assert output_data.infra_changes == []


def test_create_update_stage():
    actual_stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)

    update_project_id = actual_stage.project_id + '_new'

    create = StageHistoryCreate(event=default_event, meta=Meta(project_id=update_project_id))

    expected_stage_state = StageState(
        meta=Meta(values=actual_stage.meta, project_id=update_project_id),
        spec=Spec(values=actual_stage.spec),
        status=Status(values=actual_stage.status)
    )

    expected_stage = Stage(
        state=expected_stage_state,
        last_timestamp=create.timestamp
    )

    actual_state = create.update_stage(create_input_data(actual_stage))

    assert actual_state is None
    assert actual_stage == expected_stage
