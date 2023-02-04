from decimal import Decimal
import json

import pytest
from mock import Mock

from django_yauth.user import Application

from django.core.urlresolvers import reverse

from staff.departments.models import Department
from staff.lib.testing import StaffFactory
from staff.budget_position.workflow_service import gateways, entities


@pytest.fixture(autouse=True)
def mock_oebs_hire_service(monkeypatch):
    monkeypatch.setattr(
        gateways.OebsHireService,
        '_try_send',
        Mock(return_value={'id': 1, 'errors': []}),
    )


@pytest.fixture(autouse=True)
def mock_oebs_service(monkeypatch):
    monkeypatch.setattr(
        gateways.OEBSService,
        'send_request',
        Mock(return_value={'ID': 123}),
    )
    monkeypatch.setattr(
        gateways.OEBSService,
        'get_transaction_status',
        Mock(return_value=('UPLOADED', 1, None, None)),
    )
    monkeypatch.setattr(
        gateways.OEBSService,
        'get_salary_data',
        Mock(return_value=entities.SalaryData(salary=Decimal(100500), rate=Decimal(0.5))),
    )


@pytest.fixture
def company(company_with_module_scope):
    return company_with_module_scope


@pytest.fixture
def settings(settings_with_module_scope):
    return settings_with_module_scope


@pytest.fixture
def femida_post_request(rf, company, settings):
    def make_request(to_reverse, post_kwargs):
        url = reverse(to_reverse)
        request = rf.post(
            url,
            json.dumps(post_kwargs),
            'application/json',
        )
        robot_department = Department.objects.get(id=settings.ROBOT_DEPARTMENT_ID)
        request.user = StaffFactory(login='robot-femida', department=robot_department).user
        setattr(request, 'service_is_readonly', False)
        setattr(request, 'client_application', Application(id='', name='femida', home_page=''))
        return request
    return make_request


@pytest.fixture
def startrek_post_request(rf):
    def make_request(to_reverse, post_kwargs):
        url = reverse(to_reverse)
        request = rf.post(
            url,
            json.dumps(post_kwargs),
            'application/json',
        )
        return request
    return make_request


@pytest.fixture
def femida_user(db, company, settings):
    robot_department = Department.objects.get(id=settings.ROBOT_DEPARTMENT_ID)
    return StaffFactory(login='robot-femida', department=robot_department).user
