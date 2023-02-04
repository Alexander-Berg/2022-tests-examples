from infra.deploy_notifications_controller.lib.models.action import DummyQnotifierMessage
from infra.deploy_notifications_controller.lib.models.stage import StageState, Meta, Spec, Status, Stage
from infra.deploy_notifications_controller.lib.models.stage_history_change import StageHistoryUpdate
from test_stage_history_change_utils import create_stage, create_update, default_old_spec_revision, \
    default_old_du_revision, create_spec, create_input_data, default_update_spec_revision, \
    default_event, create_expected_message, create_meta, create_status, default_update_du_revision


def test_update_update_stage():
    actual_stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)

    update_project_id = actual_stage.project_id + '_new'
    update_acl = [{'test': 'value'}, {'test2': 'value2'}]
    update_meta = create_meta(
        project_id=update_project_id,
        acl=update_acl,
    )

    update_spec = create_spec(
        revision=default_update_spec_revision,
        du_revision=default_old_du_revision,
        revision_info=None,
        additional_field='new_spec_value',
    )

    update_status = create_status(
        revision=default_old_spec_revision,
        du_revision=default_old_du_revision,
        du_ldr=default_old_du_revision,
        yasm_itype=None,
        additional_field='new_status_value',
    )

    update = create_update(
        meta=update_meta,
        spec=update_spec,
        status=update_status,
    )

    expected_stage_state = StageState(
        meta=Meta(values=actual_stage.meta, project_id=update_project_id, acl=update_acl),
        spec=Spec(values=update_spec, revision=default_update_spec_revision),
        status=Status(values=update_status)
    )

    expected_stage = Stage(
        state=expected_stage_state,
        last_timestamp=update.timestamp
    )

    expected_message_state = expected_stage_state.to_dict()

    actual_state = update.update_stage(create_input_data(actual_stage))

    assert actual_state == expected_message_state
    assert actual_stage == expected_stage


def test_update_same_timestamp():
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision, last_timestamp=default_event.timestamp)
    update = create_update()

    actual_message, output_data = update.process_changes(create_input_data(stage))
    assert actual_message is None
    assert output_data.infra_changes == []


def test_update_nothing_changed():
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)
    update = create_update()

    expected_message = DummyQnotifierMessage(
        stage_id=stage.id,
        timestamp=default_event.timestamp,
    )

    actual_message, output_data = update.process_changes(create_input_data(stage))
    assert actual_message == expected_message
    assert output_data.infra_changes == []


def test_update_project_id_changed():
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)
    update = create_update(
        meta=create_meta(
            project_id=stage.project_id + '_new',
        ),
    )

    expected_message = create_expected_message(
        stage, update,
        custom_tags=[StageHistoryUpdate.create_change_tag('project_id')],
        revisions={stage.revision},
    )

    actual_message, output_data = update.process_changes(create_input_data(stage))

    assert actual_message == expected_message
    assert output_data.infra_changes == []


def test_update_acl_changed():
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)
    update = create_update(
        meta=create_meta(
            acl=[{'test': 'value'}, {'test2': 'value2'}],
        ),
    )

    expected_message = create_expected_message(
        stage, update,
        custom_tags=[StageHistoryUpdate.create_change_tag('acl')],
        revisions={stage.revision},
    )

    actual_message, output_data = update.process_changes(create_input_data(stage))

    assert actual_message == expected_message
    assert output_data.infra_changes == []


def test_update_spec_changed():
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)

    update = create_update(
        spec=create_spec(default_update_spec_revision, default_update_du_revision),
    )

    expected_message = create_expected_message(
        stage, update,
        custom_tags=[StageHistoryUpdate.create_change_tag('spec')],
        revisions={default_update_spec_revision},
    )

    actual_message, output_data = update.process_changes(create_input_data(stage))

    assert actual_message == expected_message
    assert output_data.infra_changes == []


def test_update_no_infras_only_spec_revision():
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)

    update = create_update(
        spec=create_spec(default_update_spec_revision, default_old_du_revision),
        status=create_status(default_update_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)
    )

    _, output_data = update.process_changes(create_input_data(stage))
    assert output_data.infra_changes == []
