import pytest

from django.conf import settings
from waffle.testutils import override_switch

from plan.services.tasks import calculate_gradient_fields
from plan.suspicion.models import ServiceIssue
from plan.suspicion.tasks import find_issues
from plan.services.models import Service

from common import factories

pytestmark = [pytest.mark.django_db]


@pytest.fixture
def issue_codes():
    return [
        'wrong_base_parent',
        'empty_description_ru',
        'empty_description_en',
        'no_owner',
        'owner_is_robot',
        'crit_descendants'
    ]


@pytest.fixture
def search_portal():
    return factories.ServiceFactory(is_base=True, slug=settings.ABC_SEARCH_PORTAL_SLUG)


def test_meta_other_find_issues(issue_codes, search_portal):
    meta_other = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)
    factories.ServiceFactory()

    for code in issue_codes:
        factories.IssueFactory(code=code)

    find_issues()

    assert ServiceIssue.objects.filter(service=meta_other).count() == 0


@pytest.mark.parametrize('manual', [True, False])
@pytest.mark.parametrize('issue_code', ['wrong_base_parent', 'some_code'])
def test_manual_issues(search_portal, manual, issue_code):
    issue_group = factories.IssueGroupFactory(manual_resolving=manual)
    issue = factories.IssueFactory(code=issue_code, issue_group=issue_group)
    factories.ServiceIssueFactory(service=search_portal, issue=issue)
    find_issues()
    expected = 1
    if issue_code == 'wrong_base_parent' and not manual:
        expected = 0
    assert ServiceIssue.objects.filter(
        service=search_portal,
        state=ServiceIssue.STATES.ACTIVE
    ).count() == expected


@pytest.mark.parametrize('wrong_base_parent', [True, False])
def test_wrong_base_parent(wrong_base_parent, django_assert_num_queries, search_portal):
    issue = factories.IssueFactory(code='wrong_base_parent')
    department = factories.DepartmentFactory()
    department2 = factories.DepartmentFactory()
    department3 = factories.DepartmentFactory()

    base_parent_service = factories.ServiceFactory(is_base=True)
    factories.ServiceFactory(parent=base_parent_service, owner=None)
    service = factories.ServiceFactory(parent=base_parent_service)

    base_parent_service.departments.add(department)
    base_parent_service.departments.add(department2)
    base_parent_service.departments.add(department3)
    if not wrong_base_parent:
        service.owner.department = department
        service.owner.save(update_fields=['department'])

    issue_finder_class = issue.get_issue_finder()
    issue_finder = issue_finder_class(Service.objects.active())
    with django_assert_num_queries(7):
        for _ in issue_finder():
            pass
    find_issues()
    if wrong_base_parent:
        service_issue = ServiceIssue.objects.get()
        assert service_issue.service == service
        assert service_issue.context == {}
    else:
        assert ServiceIssue.objects.count() == 0


@pytest.mark.parametrize('wrong_parent_type', [True, False])
@pytest.mark.parametrize('switch_active', [True, False])
def test_wrong_parent_type(wrong_parent_type, switch_active):
    factories.IssueFactory(code='wrong_parent_type')
    service_type = factories.ServiceTypeFactory()
    allowed_parent = factories.ServiceTypeFactory()
    parent_service_type = factories.ServiceTypeFactory()
    parent = factories.ServiceFactory(service_type=parent_service_type)
    service = factories.ServiceFactory(service_type=service_type, parent=parent)
    service_type.available_parents.add(allowed_parent)
    if not wrong_parent_type:
        service_type.available_parents.add(parent_service_type)
    with override_switch(settings.SWITCH_CHECK_ALLOWED_PARENT_TYPE, active=switch_active):
        find_issues()
    if wrong_parent_type and switch_active:
        service_issue = ServiceIssue.objects.get()
        assert service_issue.service == service
        assert service_issue.context == {
            'allowed': [{'en': allowed_parent.name_en, 'ru': allowed_parent.name}],
            'current': {'en': parent_service_type.name_en, 'ru': parent_service_type.name}
        }
    else:
        assert ServiceIssue.objects.count() == 0


@pytest.mark.parametrize('extra_functionality', [True, False])
@pytest.mark.parametrize('switch_active', [True, False])
def test_extra_functionality(extra_functionality, switch_active):
    factories.IssueFactory(code='extra_functionality')
    service_type = factories.ServiceTypeFactory()

    function = factories.ServiceTypeFunctionFactory()
    allowed_function = factories.ServiceTypeFunctionFactory()

    service = factories.ServiceFactory(
        service_type=service_type
    )
    service_type.functions.add(allowed_function)
    if extra_functionality:
        service.functions = [function.code, allowed_function.code]
    else:
        service.functions = [allowed_function.code]
    service.save()

    with override_switch(settings.SWITCH_CHECK_FUNCTIONALITY, active=switch_active):
        find_issues()

    if extra_functionality and switch_active:
        service_issue = ServiceIssue.objects.get()
        assert service_issue.service == service
        assert service_issue.context == {
            'extra': [{'ru': function.name, 'en': function.name_en}],
            'allowed': [{'ru': allowed_function.name, 'en': allowed_function.name_en}],
        }
    else:
        assert ServiceIssue.objects.count() == 0


@pytest.mark.parametrize('add_base_department', [True, False])
def test_wrong_base_parent_set_of_departments(add_base_department, search_portal):
    factories.IssueFactory(code='wrong_base_parent')
    department_base = factories.DepartmentFactory()
    department_other = factories.DepartmentFactory()

    base_parent_service = factories.ServiceFactory(is_base=True)
    service = factories.ServiceFactory(parent=base_parent_service)

    base_parent_service.departments.add(department_base)

    service.owner.department = department_other
    service.owner.save(update_fields=['department'])
    if add_base_department:
        factories.DepartmentStaffFactory(
            staff=service.owner,
            department=department_base,
        )

    find_issues()
    if add_base_department:
        assert ServiceIssue.objects.count() == 0
    else:
        assert ServiceIssue.objects.count() == 1


def test_wrong_base_parent_in_sandbox(search_portal):
    factories.IssueFactory(code='wrong_base_parent')
    factories.DepartmentFactory()

    meta_other = factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG, owner=None)
    factories.ServiceFactory(parent=meta_other, owner=None)
    service = factories.ServiceFactory(parent=meta_other)

    find_issues()

    service_issue = ServiceIssue.objects.get()
    assert service_issue.service == service
    assert service_issue.context == {}


def test_wrong_base_parent_without_owner(search_portal):
    factories.IssueFactory(code='wrong_base_parent')
    department = factories.DepartmentFactory()

    base_parent_service = factories.ServiceFactory(is_base=True)
    middle_service = factories.ServiceFactory(parent=base_parent_service)
    factories.ServiceFactory(parent=base_parent_service, owner=None)

    middle_service.owner.department = department
    middle_service.owner.save(update_fields=['department'])
    base_parent_service.departments.add(department)

    find_issues()

    assert ServiceIssue.objects.count() == 0


def test_wrong_base_parent_exclude_search_portal(search_portal):
    factories.IssueFactory(code='wrong_base_parent')

    department_base = factories.DepartmentFactory()
    department_other = factories.DepartmentFactory()

    service = factories.ServiceFactory(parent=search_portal)

    search_portal.departments.add(department_base)

    service.owner.department = department_other
    service.owner.save(update_fields=['department'])

    find_issues()
    assert ServiceIssue.objects.count() == 0


def test_empty_description_ru():
    factories.IssueFactory(code='empty_description_ru')
    factories.ServiceFactory(description='x', description_en='x')
    factories.ServiceFactory(description='x', description_en='')
    service_without_description = factories.ServiceFactory(description='', description_en='x')

    find_issues()

    service_issue = ServiceIssue.objects.get()
    assert service_issue.service == service_without_description
    assert service_issue.context == {}


def test_empty_description_en():
    factories.IssueFactory(code='empty_description_en')
    factories.ServiceFactory(description='x', description_en='x')
    factories.ServiceFactory(description='', description_en='x')
    service_without_description = factories.ServiceFactory(description='x', description_en='')

    find_issues()

    service_issue = ServiceIssue.objects.get()
    assert service_issue.service == service_without_description
    assert service_issue.context == {}


def test_no_owner():
    factories.IssueFactory(code='no_owner')
    factories.ServiceFactory()
    service_without_owner = factories.ServiceFactory(owner=None)

    find_issues()

    service_issue = ServiceIssue.objects.get()
    assert service_issue.service == service_without_owner
    assert service_issue.context == {}


def test_owner_is_robot():
    factories.IssueFactory(code='owner_is_robot')
    service_with_robot_owner = factories.ServiceFactory()
    service_with_robot_owner.owner.is_robot = True
    service_with_robot_owner.owner.save()
    factories.ServiceFactory()

    find_issues()

    service_issue = ServiceIssue.objects.get()
    assert service_issue.service == service_with_robot_owner
    assert service_issue.context == {'owner': service_with_robot_owner.owner.login}


def test_crit_descendants(django_assert_num_queries):
    factories.IssueFactory(code='crit_descendants')
    parent_service = factories.ServiceFactory()
    service_1 = factories.ServiceFactory(parent=parent_service)
    closed_service = factories.ServiceFactory(parent=parent_service, state=Service.states.CLOSED)
    factories.ServiceTrafficStatusFactory(service=closed_service, level='critical')

    # Количество SELECT запросов не зависит от количества объектов
    # SELECT - 7
    # UPDATE - 3
    # INSERT - 1
    # SAVEPOINT - 2
    # RELEASE SAVEPOINT - 2
    with django_assert_num_queries(15):
        find_issues()

    assert ServiceIssue.objects.count() == 0

    factories.ServiceTrafficStatusFactory(service=service_1, level='critical')

    # SELECT - 7
    # INSERT - 1
    # UPDATE - 4
    # SAVEPOINT - 1
    # RELEASE SAVEPOINT - 1
    with django_assert_num_queries(14):
        find_issues()

    service_issue = ServiceIssue.objects.get()
    assert service_issue.service == parent_service
    assert service_issue.context == {}
    assert service_issue.percentage_of_completion == 1

    service_2 = factories.ServiceFactory(parent=parent_service)
    # Добавляем потомка для service_2 чтобы проверить, что считаются только прямые потомки
    factories.ServiceFactory(parent=service_2)

    # SELECT - 7
    # UPDATE - 5
    # SAVEPOINT - 1
    # RELEASE SAVEPOINT - 1
    with django_assert_num_queries(14):
        find_issues()

    service_issue = ServiceIssue.objects.get()
    assert service_issue.service == parent_service
    assert service_issue.context == {}
    assert service_issue.percentage_of_completion == 0.5


@pytest.mark.parametrize('roles,issue_created,have_resource', (
    ([], False, False),
    (['hardware_resources_manager'], True, True),
    (['hardware_resources_owner'], True, True),
    (['hardware_resources_owner', 'hardware_resources_manager'], False, True),
))
def test_hardware_managers(django_assert_num_queries_lte, roles, issue_created, have_resource):
    factories.IssueFactory(code='hardware_managers')
    roles_by_code = {
        'hardware_resources_manager': factories.RoleFactory(code='hardware_resources_manager'),
        'hardware_resources_owner': factories.RoleFactory(code='hardware_resources_owner'),
    }
    service = factories.ServiceFactory()
    if have_resource:
        assert isinstance(settings.RESOURCE_SUPPLIERS_SLUGS['hardware'], list)
        hardware_supplier = factories.ServiceFactory(slug=settings.RESOURCE_SUPPLIERS_SLUGS['hardware'][0])
        resource_type = factories.ResourceTypeFactory(supplier=hardware_supplier)
        resource = factories.ResourceFactory(type=resource_type)
        factories.ServiceResourceFactory(resource=resource, service=service)
    staff = factories.StaffFactory()
    for role_code in roles:
        factories.ServiceMemberFactory(staff=staff, service=service, role=roles_by_code[role_code])
    # 2 for suspicion_serviceissue select
    # 1 for suspicion_issue select
    # 2 for services_service select
    # 1 for resources_resourcetype select (single run only)
    # 1 for roles_role select
    # 1 for services_service select (2 when single run)
    # 3 for suspicion_serviceissue update (4 when single run)
    # 2 for taskmetric
    # 4 for savepoint
    with django_assert_num_queries_lte(19):
        find_issues()
    if issue_created:
        service_issue = ServiceIssue.objects.get()
        assert service_issue.service == service
        assert service_issue.context == {}
        assert service_issue.percentage_of_completion == 1
    else:
        assert not ServiceIssue.objects.exists()


def test_hardware_managers_double_code():
    """
    Тестируем наличие двух ролей с одинаковым кодом, но одна не глобальная
    """

    factories.IssueFactory(code='hardware_managers')
    # это кастомная роль в каком-то сервисе
    factories.RoleFactory(
        code='hardware_resources_manager',
        service=factories.ServiceFactory()
    )
    roles_by_code = {
        'hardware_resources_manager': factories.RoleFactory(code='hardware_resources_manager'),
        'hardware_resources_owner': factories.RoleFactory(code='hardware_resources_owner'),
    }

    service = factories.ServiceFactory()
    hardware_supplier = factories.ServiceFactory(slug=settings.RESOURCE_SUPPLIERS_SLUGS['hardware'][0])
    resource_type = factories.ResourceTypeFactory(supplier=hardware_supplier)
    resource = factories.ResourceFactory(type=resource_type)
    factories.ServiceResourceFactory(resource=resource, service=service)
    for role_code in roles_by_code:
        factories.ServiceMemberFactory(service=service, role=roles_by_code[role_code])

    find_issues()
    assert not ServiceIssue.objects.exists()


@pytest.mark.parametrize(('roles', 'issue_created', 'have_cert'), (
    ([], False, False),
    ([], True, True),
    (['certs-resp'], False, False),
    (['certs-resp'], False, True),
))
def test_cert_managers(django_assert_num_queries_lte, roles, issue_created, have_cert):
    """
    Тест проверяет корректное обнаружение проблемы у сервиса, где есть сертификат и нету ответственных за сертификаты
    """
    factories.IssueFactory(code='cert_managers')
    roles_by_code = {
        'certs-resp': factories.RoleFactory(code='certs-resp')
    }
    service = factories.ServiceFactory()
    if have_cert:
        assert isinstance(settings.RESOURCE_SUPPLIERS_SLUGS['cert'], list)
        cert_supplier = factories.ServiceFactory(slug=settings.RESOURCE_SUPPLIERS_SLUGS['cert'][0])
        resource_type = factories.ResourceTypeFactory(supplier=cert_supplier)
        resource = factories.ResourceFactory(type=resource_type)
        factories.ServiceResourceFactory(resource=resource, service=service)
    staff = factories.StaffFactory()
    for role_code in roles:
        factories.ServiceMemberFactory(staff=staff, service=service, role=roles_by_code[role_code])
    # 2 for suspicion_serviceissue select
    # 1 for suspicion_issue select
    # 2 for services_service select
    # 1 for resources_resourcetype select (single run only)
    # 1 for roles_role select
    # 1 for services_service select (2 when single run)
    # 3 for suspicion_serviceissue update (4 when single run)
    # 2 for taskmetric
    # 4 for savepoint
    with django_assert_num_queries_lte(19):
        find_issues()
    if issue_created:
        service_issue = ServiceIssue.objects.get()
        assert service_issue.service == service
        assert service_issue.context == {}
        assert service_issue.percentage_of_completion == 1
    else:
        assert not ServiceIssue.objects.exists()


@pytest.mark.parametrize(('roles', 'have_robot', 'issue_created', ), (
    ([], False, False),
    ([], True, True),
    (['robots_manager'], True, False),
    (['robots_custom'], True, False),
    (['robots_manager'], False, False),
    (['robots_custom'], False, False),
))
def test_robots_managers(django_assert_num_queries_lte, roles, issue_created, have_robot, robot_resource_type):
    """
    При наличии привязанного робота и отсутствии отвественных - формируем проблему.
    Если нет роботы, но есть ответственные - это ОК.
    """

    issue = factories.IssueFactory(code='robot_managers')
    service = factories.ServiceFactory()
    roles_by_code = {
        'robots_manager': factories.RoleFactory(code='robots_manager'),
        'robots_custom': factories.RoleFactory(code='robots_manager', service=service)
    }
    if have_robot:
        factories.StaffFactory(login='login')
        resource = factories.ResourceFactory(
            type=robot_resource_type,
            external_id='login',
            attributes={'secret_id': {'value': 'value'}}
        )
        factories.ServiceResourceFactory(resource=resource, service=service)

    staff = factories.StaffFactory()
    for role_code in roles:
        factories.ServiceMemberFactory(
            staff=staff,
            service=service,
            role__code=roles_by_code[role_code],
            role__scope__slug=settings.ABC_ROBOTS_MANAGEMENT_SCOPE
        )

    with django_assert_num_queries_lte(19):
        # 2 for suspicion_serviceissue select
        # 1 for suspicion_issue select
        # 2 for services_service select
        # 1 for resources_resourcetype select (single run only)
        # 1 for roles_role select
        # 1 for services_service select (2 when single run)
        # 3 for suspicion_serviceissue update (4 when single run)
        # 2 for taskmetric
        # 4 for savepoint
        find_issues()

    if issue_created:
        service_issue = ServiceIssue.objects.get(issue=issue, service=service)
        assert service_issue.service == service
        assert service_issue.context == {}
        assert service_issue.percentage_of_completion == 1
    else:
        assert not ServiceIssue.objects.exists()


def test_gradient_vs(django_assert_num_queries):
    factories.IssueFactory(code='gradient_vs')

    meta_search = factories.ServiceFactory(slug=settings.ABC_SEARCH_PORTAL_SLUG)

    vs = factories.ServiceFactory(parent=meta_search)
    vs.valuestream = vs
    vs.save()

    factories.ServiceFactory(parent=vs, valuestream=vs)
    non_vs_service = factories.ServiceFactory(parent=meta_search)
    non_vs_child = factories.ServiceFactory(parent=non_vs_service)

    with django_assert_num_queries(17):
        # 2 for suspicion_serviceissue select
        # 1 for suspicion_issue select
        # 3 for services_service select
        # 2 for suspicion_serviceissue insert
        # 3 for suspicion_serviceissue update (4 when single run)
        # 2 for taskmetric
        # 4 for savepoint
        find_issues()

    issued_services_ids = {issue.service_id for issue in ServiceIssue.objects.all()}
    assert issued_services_ids == {non_vs_service.id, non_vs_child.id}


def test_gradient_structure(django_assert_num_queries):
    factories.IssueFactory(code='gradient_structure')
    meta_search = factories.ServiceFactory(slug=settings.ABC_SEARCH_PORTAL_SLUG)

    vs_tag = factories.ServiceTagFactory(slug=settings.GRADIENT_VS)
    umbrella_tag = factories.ServiceTagFactory(slug=settings.GRADIENT_UMB)
    contour_tag = factories.ServiceTagFactory(slug=settings.GRADIENT_CONTOUR)

    vs = factories.ServiceFactory(parent=meta_search)
    vs.tags.add(vs_tag)

    good_umbrella = factories.ServiceFactory(parent=vs)
    good_umbrella.tags.add(umbrella_tag)

    vs_subservice = factories.ServiceFactory(parent=vs)

    bad_umbrella = factories.ServiceFactory(parent=vs_subservice, valuestream=vs)
    bad_umbrella.tags.add(umbrella_tag)

    search_subservice = factories.ServiceFactory(parent=meta_search)

    bad_vs = factories.ServiceFactory(parent=search_subservice)
    bad_vs.tags.add(vs_tag)

    contour_without_umbrella = factories.ServiceFactory(parent=vs)
    contour_without_umbrella.tags.add(contour_tag)

    good_contour = factories.ServiceFactory(parent=good_umbrella)
    good_contour.tags.add(contour_tag)

    contour_subservice = factories.ServiceFactory(parent=good_contour)

    contour_under_contour = factories.ServiceFactory(parent=contour_subservice)
    contour_under_contour.tags.add(contour_tag)

    outside_service = factories.ServiceFactory()
    outside_vs = factories.ServiceFactory(parent=outside_service)
    outside_vs.tags.add(vs_tag)

    double_trouble_contour = factories.ServiceFactory(parent=contour_without_umbrella)
    double_trouble_contour.tags.add(contour_tag)

    calculate_gradient_fields(meta_search.id)

    with django_assert_num_queries(20):
        # 3 for serviceissue select
        # 2 for service select
        # 6 for serviceissue create
        # 3 for serviceissue update
        # 2 for taskmetric
        # 4 for savepoint
        find_issues()

    expected_issued_services_ids = {
        bad_umbrella.id,
        bad_vs.id,
        contour_without_umbrella.id,
        contour_under_contour.id,
        double_trouble_contour.id,
    }

    for service_id in expected_issued_services_ids:
        assert ServiceIssue.objects.filter(service_id=service_id).count() == 1
