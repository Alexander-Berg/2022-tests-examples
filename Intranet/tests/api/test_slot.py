from fastapi import status
import pytest
from watcher.enums import RevisionState
from watcher.db.from_transfer import Role


@pytest.mark.parametrize(
    'type_error',
    (
        None, 'no_role', 'cannot_issue',
        'different_service', 'disabled_revision',
    )
)
def test_patch_changing_role_and_showing_on_staff(
    client, slot_factory, role_factory, scope_session, type_error,
    service_factory, assert_count_queries
):
    slot = slot_factory()
    slot_id = slot.id
    role = role_factory()
    role_id = role.id
    queries_count = 3
    if type_error is None:
        queries_count = 5
    if type_error == 'no_role':
        scope_session.query(Role).filter(Role.id == role.id).delete()
    if type_error == 'cannot_issue':
        role.scope.can_issue_at_duty_time = False
    if type_error == 'different_service':
        role.service_id = service_factory().id
    if type_error == 'disabled_revision':
        slot.interval.revision.state = RevisionState.disabled
    scope_session.commit()
    with assert_count_queries(queries_count):
        # select staff
        # select slot with joined load
        # select role
        # if exception wasn't raised
        # update slot
        # select slot (refresh)
        response = client.patch(
            f'/api/watcher/v1/slot/{slot_id}',
            json={
                'role_on_duty_id': role_id,
                'show_in_staff': True,
            }
        )
    if type_error == 'no_role':
        assert response.json()['context']['message']['en'] == 'The specified role was not found'
    elif type_error == 'cannot_issue':
        assert response.json()['context']['message']['en'] == 'Specified role cannot be assigned to duty'
    elif type_error == 'different_service':
        assert response.json()['context']['message']['en'] == 'Specified role does not correspond to this service'
    elif type_error == 'disabled_revision':
        assert response.json()['context']['message']['en'] == 'Editing a disabled revision is prohibited'
    else:
        assert response.status_code == status.HTTP_200_OK, response.text
        scope_session.refresh(slot)
        assert slot.role_on_duty_id == role.id
        assert slot.show_in_staff is True


def test_patch_show_in_staff(client, slot_factory, scope_session, role_factory):
    role = role_factory()
    slot = slot_factory(show_in_staff=False, role_on_duty=role)
    response = client.patch(
        f'/api/watcher/v1/slot/{slot.id}',
        json={
            'show_in_staff': True,
        }
    )

    assert response.status_code == status.HTTP_200_OK, response.text
    scope_session.refresh(slot)
    assert slot.show_in_staff is True
    assert slot.role_on_duty_id == role.id
