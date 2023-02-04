from infra.deploy_notifications_controller.lib.models.action import InfraChange
from infra.deploy_notifications_controller.lib.models.stage_history_change import StageHistoryUpdate, StageHistoryChange
from test_stage_history_change_utils import create_stage, create_update, default_old_spec_revision, \
    default_old_du_revision, create_spec, create_input_data, default_update_spec_revision, \
    default_event


def test_create_change_tag():
    field_name = 'test'

    expected_tag = f'changed:{field_name}'
    actual_tag = StageHistoryUpdate.create_change_tag(field_name)

    assert actual_tag == expected_tag


def test_create_tags():
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)
    update = create_update()

    custom_tag = 'custom_tag'

    expected_tags = [
        'ya.deploy',
        f'stage:{update.tag_action}',
        f'stage:id:{stage.id}',
        f'stage:uuid:{stage.uuid}',
        f'author:{default_event.author.name}',
        custom_tag
    ]

    output_data = StageHistoryChange.OutputData()
    output_data.tags.append(custom_tag)

    actual_tags = update.create_tags(create_input_data(stage), output_data)

    assert actual_tags == expected_tags


def test_create_infra_change():
    stage = create_stage(default_old_spec_revision, default_old_du_revision, du_ldr=default_old_du_revision)

    update = create_update(
        spec=create_spec(default_update_spec_revision, default_old_du_revision),
    )

    change_kind = InfraChange.EventKind.STARTED

    expected_infra_change = InfraChange(
        stage_id=stage.id,
        timestamp=default_event.timestamp,
        service_id=stage.infra_service,
        environment_id=stage.infra_environment,
        author=default_event.author.name,
        event_kind=change_kind,
        revision=default_update_spec_revision
    )

    actual_infra_change = update.create_infra_change(
        default_update_spec_revision, stage, change_kind
    )

    assert actual_infra_change == expected_infra_change
