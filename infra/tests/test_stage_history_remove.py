from infra.deploy_notifications_controller.lib.models.stage import StageState, Meta, Spec, Status, Stage
from infra.deploy_notifications_controller.lib.models.stage_history_change import StageHistoryRemove
from test_stage_history_change_utils import create_stage, default_old_spec_revision, \
    default_old_du_revision, create_input_data, default_event, create_expected_message


def test_remove():
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)
    remove = StageHistoryRemove(event=default_event)

    expected_message = create_expected_message(stage, remove)

    actual_message, output_data = remove.process_changes(create_input_data(stage))

    assert actual_message == expected_message
    assert output_data.infra_changes == []


def test_remove_update_stage():
    actual_stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)
    remove = StageHistoryRemove(event=default_event)

    expected_stage_state = StageState(
        meta=Meta(values=actual_stage.meta),
        spec=Spec(values=actual_stage.spec),
        status=Status(values=actual_stage.status)
    )

    expected_stage = Stage(
        state=expected_stage_state,
        last_timestamp=remove.timestamp
    )

    actual_state = remove.update_stage(create_input_data(actual_stage))

    assert actual_state is None
    assert actual_stage == expected_stage
