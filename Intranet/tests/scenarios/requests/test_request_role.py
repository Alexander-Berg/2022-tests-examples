import pytest

from idm.core.constants.role import ROLE_STATE
from idm.core.models import Role
from idm.tests.utils import set_workflow
from idm.utils import reverse

pytestmark = [pytest.mark.django_db]


@pytest.mark.parametrize(
    'workflow,intermediate_state,final_state',
    (
        ('approvers = [approver("bilbo") | "sam"]', ROLE_STATE.GRANTED, ROLE_STATE.DEPRIVED),
        ('approvers = ["bilbo", "sam"]', ROLE_STATE.REQUESTED, ROLE_STATE.DECLINED)
    )
)
def test_request_approve_deprive(arda_users, client, simple_system, workflow, intermediate_state, final_state):
    """
    TestpalmID: 3456788-105
    TestpalmID: 3456788-107
    """
    bilbo = arda_users.bilbo
    frodo = arda_users.frodo
    sam = arda_users.sam

    set_workflow(simple_system, workflow)

    # запросим роль для frodo и запомним её id
    client.login(frodo.username)
    response = client.json.post(
        reverse('api_dispatch_list', api_name='frontend', resource_name='rolerequests'),
        {
            'user': frodo.username,
            'system': simple_system.slug,
            'path': simple_system.nodes.get(slug='manager').value_path,
        },
    ).json()
    assert response['state'] == ROLE_STATE.REQUESTED
    role_id = response['id']

    # узнаем id ApproveRequest'а для bilbo, чтобы потом подтвердить роль
    client.login(bilbo.username)
    response = client.json.get(
        reverse('api_dispatch_list',  api_name='frontend', resource_name='approverequests'),
        {
            'status': 'pending',
            'priority_type': 'primary',
            'approver': bilbo.username,
        },
    ).json()
    approverequest_id = response['objects'][0]['id']

    # подтвердим роль от bilbo
    client.login(bilbo.username)
    response = client.json.post(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='approverequests', pk=approverequest_id),
        {
            'decision': 'approve',
        },
    )
    assert response.status_code == 204

    # проверим, что роль перешла в нужное состояние
    role = Role.objects.get(pk=role_id)
    assert role.state == intermediate_state

    # отзываем роль от sam
    client.login(sam.username)
    response = client.json.delete(
        reverse('api_dispatch_detail', api_name='frontend', resource_name='roles', pk=role_id),
    )
    assert response.status_code == 204

    # проверим, что роль перешла в нужное состояние
    role.refresh_from_db()
    assert role.state == final_state
