import pytest

from django.conf import settings
from django.core.urlresolvers import reverse

from plan.monitorings.views import BaseMonitoringView
from common import factories

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize(
    'exists, staff_role', (
        (True, 'own_only_viewer'),
        (False, 'own_only_viewer'),
    )
)
def test_missing_resources_robots(client, exists, staff_role, staff_factory):
    """
    Проверим, что мониторинг сработает, если есть робот, но ресурса такого нет.
    Если ресурса есть, мониторинг срабатывать не должен.
    """

    robot = factories.StaffFactory(login='my_robot', is_robot=True)
    resource_type = factories.ResourceTypeFactory(supplier_plugin='robots', code=settings.ROBOT_RESOURCE_TYPE_CODE)

    if exists:
        factories.ResourceFactory(
            type=resource_type,
            external_id=robot.login,
        )

    client.login(staff_factory(staff_role).login)
    response = client.get(reverse('monitorings:missing-resources-robots'))

    if exists:
        assert response.status_code == BaseMonitoringView.ok_code
        assert response.content == b'ok'
    else:
        assert response.status_code == BaseMonitoringView.crit_code
        assert response.content == b'Does not exist resources: 1. Logins robots: [\'my_robot\']'
