import pytest

import itertools
import random

from django.contrib.admin import AdminSite
from django.core.urlresolvers import reverse
from django.contrib import messages

from staff.budget_position.admin import MarketWorkflowAdmin, WorkflowAdmin
from staff.budget_position.models import Workflow
from staff.budget_position.tests.workflow_tests.utils import ChangeRegistryFactory, WorkflowModelFactory
from staff.lib.testing import DepartmentFactory, StaffFactory, DepartmentStaffFactory, UserFactory


@pytest.fixture
def market_workflow_admin():
    return MarketWorkflowAdmin(model=MarketWorkflowAdmin.MarketWorkflow, admin_site=AdminSite())


@pytest.mark.django_db
def test_market_workflow_admin_get_queryset(market_workflow_admin: MarketWorkflowAdmin, rf):
    url = reverse('admin:budget_position_marketworkflow_changelist')
    request = rf.get(url)
    market_workflows, not_market_workflows = _create_workflows(request)

    workflows = market_workflow_admin.get_queryset(request)

    assert sorted([x.pk for x in workflows]) == sorted([x.pk for x in market_workflows])


@pytest.mark.django_db
@pytest.mark.skip
def test_market_workflow_admin_push_workflows_to_oebs(market_workflow_admin: MarketWorkflowAdmin, rf):
    url = reverse('admin:budget_position_marketworkflow_changelist')
    request = rf.get(url)
    market_workflows, not_market_workflows = _create_workflows(request)
    call_invoked = False
    queryset = Workflow.objects.filter(pk__in=[x.pk for x in market_workflows])

    def _success(**kwargs):
        assert kwargs['request'] == request
        assert kwargs['queryset'] == queryset
        nonlocal call_invoked
        call_invoked = True

    market_workflow_admin.message_user = _fail
    super(MarketWorkflowAdmin, market_workflow_admin).push_workflows_to_oebs = _success

    market_workflow_admin.push_workflows_to_oebs(request, queryset)

    assert call_invoked


@pytest.mark.django_db
@pytest.mark.skip
def test_market_workflow_admin_push_workflows_to_oebs_all_workflows(market_workflow_admin: MarketWorkflowAdmin, rf):
    url = reverse('admin:budget_position_marketworkflow_changelist')
    request = rf.get(url)
    _create_workflows(request)
    message_user_invoked = False

    def _check_message_user(**kwargs):
        assert kwargs["request"] == request
        assert kwargs["level"] == messages.ERROR
        nonlocal message_user_invoked
        message_user_invoked = True

    market_workflow_admin.message_user = _check_message_user
    super(WorkflowAdmin, market_workflow_admin).push_workflows_to_oebs = _fail

    market_workflow_admin.push_workflows_to_oebs(request, Workflow.objects.all())

    assert message_user_invoked


def _create_workflows(request):
    market_root_dep = DepartmentFactory(name='Yandex Market Root', url='yandex_monetize_market')
    market_department = DepartmentFactory(name='Some Market Department', parent=market_root_dep)
    not_market_department = DepartmentFactory(name='Some Not Market Department')

    market_staff = StaffFactory(department=market_department, login_ld='MarketUser')
    not_market_staff = StaffFactory(department=not_market_department, login_ld='NotMarketUser')
    unknown_staff = StaffFactory(department=None, login_ld='UnknownUser')

    market_changes = [
        {'department': market_department, 'staff': market_staff},
        {'department': market_department, 'staff': None},
        {'department': None, 'staff': market_staff},
        {'department': market_department, 'staff': not_market_staff},
        {'department': market_department, 'staff': unknown_staff},
        {'department': not_market_department, 'staff': market_staff},
    ]

    not_market_changes = [
        {'department': not_market_department, 'staff': not_market_staff},
        {'department': not_market_department, 'staff': unknown_staff},
        {'department': not_market_department, 'staff': None},
        {'department': None, 'staff': not_market_staff},
        {'department': None, 'staff': unknown_staff},
        {'department': None, 'staff': None},
    ]

    # Create Market Workflows
    market_workflows = list()
    for _ in range(random.randint(5, 40)):
        market_workflow = WorkflowModelFactory()
        market_workflows.append(market_workflow)
        good_changes = random.sample(market_changes, random.randint(1, len(market_changes)))
        bad_changes = random.sample(not_market_changes, random.randint(0, len(not_market_changes)))

        for change in itertools.chain(bad_changes, good_changes):
            ChangeRegistryFactory(**change, workflow=market_workflow)

    # Create several Not Market Workflows
    not_market_workflows = list()
    for _ in range(random.randint(5, 40)):
        not_market_workflow = WorkflowModelFactory()
        not_market_workflows.append(not_market_workflow)
        bad_changes = random.sample(not_market_changes, random.randint(0, len(not_market_changes)))

        for change in bad_changes:
            ChangeRegistryFactory(**change, workflow=not_market_workflow)

    _create_user_for_department(market_root_dep, request)

    return market_workflows, not_market_workflows


def _create_user_for_department(department, request):
    from staff.lib.testing import DepartmentRoleFactory
    from django.contrib.auth.models import Permission
    role = DepartmentRoleFactory(id='TEST_ROLE_NAME')
    role.permissions.add(Permission.objects.get(codename='can_manage_market_budget_positions'))
    staff = StaffFactory()
    DepartmentStaffFactory(staff=staff, role=role, department=department)
    request.user = UserFactory()
    request.user.get_profile = lambda: staff


def _fail(**kwargs):
    print("Call should not be invoked in this case")
    assert False
