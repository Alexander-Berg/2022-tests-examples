import importlib

import pretend
import pytest

from django.conf import settings
from django.contrib.auth.models import Permission

from plan.services.models import Service
from common import factories


@pytest.fixture
def data(db, owner_role, deputy_role, responsible_role, superuser, staff_factory):
    # какой-то вообще левый чувак, которого нет ни в одном сервисе
    stranger = staff_factory()

    service_type = factories.ServiceTypeFactory(
        name='Сервис',
        name_en='Service',
        code='undefined'
    )

    # команда ABC
    meta_other = factories.ServiceFactory(
        slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG,
        is_exportable=False
    )

    support = staff_factory()
    support_perm = Permission.objects.get(codename='manage_responsible')
    support.user.user_permissions.add(support_perm)

    # какой-то метасервис
    big_boss = staff_factory()
    metaservice = factories.ServiceFactory(
        owner=big_boss,
        is_exportable=True,
        membership_inheritance=True,
    )
    owner_of_metaservice = factories.ServiceMemberFactory(
        service=metaservice,
        role=owner_role,
        staff=big_boss
    )

    # сервис с командой, управляющим и вложенным сервисом
    staff = staff_factory()
    service = factories.ServiceFactory(
        parent=metaservice,
        is_exportable=True,
        owner=staff,
        membership_inheritance=True,
    )
    owner_of_service = factories.ServiceMemberFactory(
        service=service,
        role=owner_role,
        staff=staff
    )
    deputy = factories.ServiceMemberFactory(
        service=service,
        role=deputy_role
    )

    # команда этого сервиса
    responsible = factories.ServiceMemberFactory(service=service, role=responsible_role, staff=staff_factory())
    team_member = factories.ServiceMemberFactory(service=service, staff=staff_factory())

    department = factories.DepartmentFactory()
    factories.StaffFactory(department=department)
    department_member = factories.ServiceMemberDepartmentFactory(
        service=service,
        department=department
    )
    factories.ServiceMemberFactory(
        service=service,
        from_department=department_member,
        role=department_member.role,
    )

    # вложенный сервис
    child = factories.ServiceFactory(
        parent=service,
        is_exportable=True,
        membership_inheritance=True,
        owner=staff_factory()
    )
    owner_of_child = factories.ServiceMemberFactory(
        service=child,
        role=owner_role,
        staff=child.owner,
    )

    # другой сервис, уже сам по себе и без команды
    other_staff = staff_factory()
    other_service = factories.ServiceFactory(
        parent=metaservice,
        owner=other_staff,
        is_exportable=True,
        membership_inheritance=True,
    )
    owner_of_other_service = factories.ServiceMemberFactory(
        service=other_service,
        role=owner_role,
        staff=other_staff
    )

    # some guy with new type of permission (for internal role)
    ya_editor = factories.StaffFactory()
    ya_editor_perm_codes = ['can_edit', 'view_kpi', 'view_own_services']
    for perm_code in ya_editor_perm_codes:
        ya_editor.user.user_permissions.add(Permission.objects.get(codename=perm_code))

    fixture = pretend.stub(
        stranger=stranger,

        moderator=superuser,
        meta_other=meta_other,

        support=support,
        support_perm=support_perm,

        big_boss=big_boss,
        metaservice=metaservice,
        owner_of_metaservice=owner_of_metaservice,

        staff=staff,
        service=service,
        owner_of_service=owner_of_service,
        service_type=service_type,

        deputy=deputy,
        responsible=responsible,
        team_member=team_member,

        department=department,
        department_member=department_member,

        child=child,
        owner_of_child=owner_of_child,

        other_staff=other_staff,
        other_service=other_service,
        owner_of_other_service=owner_of_other_service,

        ya_editor=ya_editor,
    )
    return fixture


@pytest.fixture
def services_tree(staff_factory):
    schema = [
        ('A', None),
        ('B', None),
        ('C', 'A'),
        ('D', 'C'),
        ('E', 'C'),
        ('F', 'B'),
        ('G', 'B'),
        ('H', None),
    ]
    for slug, parent_slug in schema:
        factories.ServiceFactory(
            slug=slug,
            parent=Service.objects.get(slug=parent_slug) if parent_slug else None,
            owner=staff_factory('full_access')
        )
    child = Service.objects.get(slug='G')
    parent = Service.objects.get(slug='H')
    child.parent = parent
    child.save()


@pytest.fixture()
def crowdtest_urls(settings):
    """Переопределяет настройку CROWDTEST и перезагружает модули с роутерами"""
    original_crowdtest = settings.CROWDTEST
    settings.CROWDTEST = True
    module = importlib.import_module('plan.services.router')
    importlib.reload(module)
    module = importlib.import_module('plan.api.router')
    importlib.reload(module)
    module = importlib.import_module('plan.urls')
    importlib.reload(module)
    yield
    settings.CROWDTEST = original_crowdtest
    module = importlib.import_module('plan.services.router')
    importlib.reload(module)
    module = importlib.import_module('plan.api.router')
    importlib.reload(module)
    module = importlib.import_module('plan.urls')
    importlib.reload(module)


@pytest.fixture()
def oebs_related_service(request, data):
    service = data.service
    if hasattr(request, 'param'):
        service = getattr(data, request.param)
    resource_type = factories.ResourceTypeFactory(code=settings.OEBS_PRODUCT_RESOURCE_TYPE_CODE)
    resource = factories.ResourceFactory(type=resource_type)
    factories.ServiceResourceFactory(service=service, resource=resource)
    return service


@pytest.fixture()
def oebs_agreement(request, oebs_related_service):
    return factories.OEBSAgreementFactory(service=oebs_related_service)
