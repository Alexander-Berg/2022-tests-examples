import pretend
import pytest

from django.core.urlresolvers import reverse

from plan.roles.models import Role

from common import factories

pytestmark = pytest.mark.django_db


@pytest.fixture
def role_suggest_data():
    Role.objects.all().delete()
    service = factories.ServiceFactory()
    scope = factories.RoleScopeFactory()
    role1 = factories.RoleFactory(name='г', name_en='z', scope=scope)
    role2 = factories.RoleFactory(name='ж', name_en='h', scope=scope)
    role3 = factories.RoleFactory(name='я', service=service)

    return pretend.stub(
        role1=role1, role2=role2, role3=role3, service=service
    )


def check_suggest(client, params, roles):
    response = client.json.get(reverse('api-v4-common:suggests:roles'), params)
    assert response.status_code == 200
    assert sorted([x['id'] for x in response.json()['results']]) == sorted([x.id for x in roles])


def test_role_suggest(client, role_suggest_data):
    check_suggest(client, {}, [role_suggest_data.role1, role_suggest_data.role2])


def test_lang(client, role_suggest_data):
    check_suggest(client, {'lang': 'en'}, [role_suggest_data.role2, role_suggest_data.role1])


def test_service(client, role_suggest_data):
    check_suggest(
        client,
        {'service': role_suggest_data.service.id},
        [role_suggest_data.role1, role_suggest_data.role2, role_suggest_data.role3]
    )


def test_id_filter(client, role_suggest_data):
    check_suggest(client, {'exclude_id': role_suggest_data.role1.id}, [role_suggest_data.role2])
    another_role = factories.RoleFactory()
    check_suggest(
        client,
        {'id': [role_suggest_data.role1.id, another_role.id]},
        [role_suggest_data.role1, another_role]
    )


def test_service_with_scope_can_issue_at_duty_time(client, role_suggest_data,
                                                   owner_role, deputy_role, responsible_role):
    """
    Саджест с параметром scope__can_issue_at_duty_time=True возвращает все существующие роли кроме
    ролей с кодом из CAN_NOT_USE_FOR_DUTY.
    """
    check_suggest(
        client,
        {'service': role_suggest_data.service.id, 'scope__can_issue_at_duty_time': True},
        [role_suggest_data.role1, role_suggest_data.role2, role_suggest_data.role3],
    )
