import pytest

from django.conf import settings

from intranet.femida.src.candidates.choices import CONTACT_TYPES
from intranet.femida.src.hire_orders.models import HireOrder
from intranet.femida.src.offers import choices as offer_choices
from intranet.femida.src.utils.common import ObjectDict

from intranet.femida.tests import factories as f


# FIXME: при включении вернуть фикстуру module_db
# @pytest.fixture(scope='module')
@pytest.fixture
def hire_order_raw_data_db_values():
    return ObjectDict(
        recruiter=f.create_recruiter(),
        department=f.DepartmentFactory(
            ancestors=[settings.OUTSTAFF_DEPARTMENT_ID],
        ),
        service=f.ServiceFactory(),
        hiring_manager=f.create_user(),
        profession=f.ProfessionFactory(),
        org=f.OrganizationFactory(),
        office=f.OfficeFactory(),
        currency=f.currency(),
        position=f.PositionFactory(),
    )


@pytest.fixture
def hire_order_raw_data(hire_order_raw_data_db_values):
    db = hire_order_raw_data_db_values
    return {
        'recruiter': db.recruiter.username,
        'candidate': {
            'first_name': 'Oleg',
            'last_name': 'Bad',
            'email': 'oleg@bad.com',
            'source': 'other',
            'source_description': None,
        },
        'vacancy': {
            'name': 'Dummy Vacancy',
            'department': db.department.url,
            'abc_services': [db.service.slug],
            'hiring_manager': db.hiring_manager.username,
            'profession': db.profession.id,
        },
        'offer': {
            'form_type': offer_choices.FORM_TYPES.russian,
            'org': db.org.id,
            'professional_level': offer_choices.PROFESSIONAL_LEVELS.middle,
            'work_place': offer_choices.WORK_PLACES.office,
            'office': db.office.id,
            'payment_type': offer_choices.PAYMENT_TYPES.monthly,
            'payment_currency': db.currency.code,
            'salary': '89000.10',
            'position': db.position.id,
            'join_at': '2020-12-12',
            'grade': 20,
            'contract_type': offer_choices.CONTRACT_TYPES.indefinite,
            'probation_period_type': offer_choices.PROBATION_PERIOD_TYPES.two_weeks,
            'employment_type': offer_choices.EMPLOYMENT_TYPES.full,
            'is_main_work_place': True,
            'hardware_profile_type': offer_choices.HARDWARE_PROFILE_TYPES.token,
            'is_confirmed_by_boss': False,
            'is_internal_phone_needed': True,
            'is_sip_redirect_needed': True,
        },
    }


@pytest.fixture
def simple_hire_order(hire_order_raw_data):
    return HireOrder.objects.create(
        created_by=f.create_user(),
        raw_data=hire_order_raw_data,
        recruiter=f.create_recruiter(),
    )


@pytest.fixture
def definitely_duplicate(hire_order_raw_data):
    candidate_data = hire_order_raw_data['candidate']
    return f.CandidateContactFactory(
        type=CONTACT_TYPES.email,
        account_id=candidate_data['email'],
        normalized_account_id=candidate_data['email'],
        candidate__first_name=candidate_data['first_name'],
        candidate__last_name=candidate_data['last_name'],
        candidate__login='def-dup',
    ).candidate


@pytest.fixture
def maybe_duplicate(hire_order_raw_data):
    candidate_data = hire_order_raw_data['candidate']
    return f.CandidateContactFactory(
        type=CONTACT_TYPES.email,
        account_id=candidate_data['email'],
        normalized_account_id=candidate_data['email'],
        candidate__first_name=candidate_data['first_name'][::-1].capitalize(),
        candidate__last_name=candidate_data['last_name'][::-1].capitalize(),
    ).candidate


@pytest.fixture
def table_flow_data():
    return {
        'contract_type': offer_choices.CONTRACT_TYPES.fixed_term,
        'contract_term': 1000,
        'is_main_work_place': False,
        'probation_period_type': offer_choices.PROBATION_PERIOD_TYPES.no,
    }


@pytest.fixture
def partial_hire_order_raw_data(hire_order_raw_data, table_flow_data):
    hire_order_raw_data['use_table_flow'] = True
    for field in table_flow_data:
        hire_order_raw_data['offer'].pop(field, None)
    return hire_order_raw_data
