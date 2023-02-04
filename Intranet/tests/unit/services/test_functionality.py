import pytest
import mock

from django.conf import settings

from plan.services.tasks import update_functionality
from plan.services.functionality import FUNCTIONALITIES_CHECKER

from common import factories


@pytest.fixture(autouse=True)
def functionalities():
    for func_code in FUNCTIONALITIES_CHECKER:
        factories.ServiceTypeFunctionFactory(code=func_code)


@pytest.mark.parametrize('with_resource', (True, False))
@pytest.mark.parametrize('with_warden', (True, False))
@pytest.mark.parametrize('is_exportable', (True, False))
@pytest.mark.parametrize('has_duty', (None, 'abc', 'watcher'))
@pytest.mark.parametrize('has_service_roles', (True, False))
@pytest.mark.parametrize('has_functional_roles', (True, False))
@pytest.mark.parametrize('has_oebs', (True, False))
@pytest.mark.parametrize('active_related_func', (True, False))
def test_update_functionality(
    with_resource, is_exportable,
    with_warden, has_duty, has_service_roles,
    has_functional_roles, has_oebs, active_related_func,
):
    service = factories.ServiceFactory(
        is_exportable=is_exportable,
        use_for_hr=has_oebs,
        functions=[],
    )
    func = factories.ServiceTypeFunctionFactory(
        code='some_code',
        active=active_related_func,
    )
    service.service_type.functions.add(func)
    expected = [
        settings.QUOTAS_FUNCTION,  # всегда сейчас добавляем
        settings.HARDWARE_FUNCTION,  # всегда сейчас добавляем
    ]
    if active_related_func:
        expected.append(func.code)
    if is_exportable:
        expected.append(settings.STAFF_EXPORT_FUNCTION)
    if has_oebs:
        expected.append(settings.OEBS_FUNCTION)
    if has_service_roles:
        expected.append(settings.SERVICE_ROLES_FUNCTION)
        factories.ServiceMemberFactory(
            service=service,
            role=factories.RoleFactory(
                scope=factories.RoleScopeFactory(
                    utility_scope=True
                )
            )
        )

    if has_functional_roles:
        expected.append(settings.FUNCTIONAL_ROLES_FUNCTION)
        factories.ServiceMemberFactory(
            service=service,
            role=factories.RoleFactory(
                scope=factories.RoleScopeFactory(
                    utility_scope=False
                )
            )
        )

    if with_resource:
        type_code = 'some_code'
        if with_warden:
            type_code = settings.WARDEN_RESOURCE_TYPE_CODE
            expected.append(settings.WARDEN_FUNCTION)
        factories.ServiceResourceFactory(
            service=service,
            type=factories.ResourceTypeFactory(
                code=type_code,
            ),
        )
        expected.append(settings.RESOURCE_CONSUME_FUNCTION)

    watcher_rows = []
    if has_duty:
        expected.append(settings.DUTY_FUNCTION)
        if has_duty == 'abc':
            factories.ScheduleFactory(service=service)
        else:
            watcher_rows = [
                (service.id, )
            ]

    class FakeYQLRequest:
        def __init__(self):
            self.status = 'COMPLETED'
            self.run = mock.MagicMock()
            table_mock = mock.MagicMock()
            table_mock.rows = watcher_rows
            self.get_results = mock.MagicMock(return_value=[table_mock])

    with mock.patch('yql.api.v1.client.YqlClient.query', return_value=FakeYQLRequest()):
        update_functionality()

    service.refresh_from_db()
    assert len(service.functions) == len(expected)
    assert set(service.functions) == set(expected)
