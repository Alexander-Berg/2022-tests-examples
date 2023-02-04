import pytest
from django.urls.base import reverse

from intranet.femida.src.candidates.choices import CONSIDERATION_STATUSES, ROTATION_STATUSES
from intranet.femida.src.core.switches import TemporarySwitch
from intranet.femida.src.vacancies.choices import (
    VACANCY_STATUSES,
    VACANCY_ROLES,
)
from intranet.femida.tests import factories as f
from intranet.femida.tests.clients import APIClient

pytestmark = pytest.mark.django_db


dynamic_roles = [
    VACANCY_ROLES.main_recruiter,
    VACANCY_ROLES.recruiter,
    VACANCY_ROLES.hiring_manager,
    VACANCY_ROLES.responsible,
    VACANCY_ROLES.head,
    VACANCY_ROLES.interviewer,
    VACANCY_ROLES.observer,
    VACANCY_ROLES.auto_observer
]


@pytest.mark.parametrize(
    'role, vacancy_status, http_response_code',
    [(role, status, response)
     for role in dynamic_roles
     for status, response in [(VACANCY_STATUSES.closed, 403),
                              (VACANCY_STATUSES.in_progress, 200)]]
)
def test_access_to_employee_if_member(client, role, vacancy_status, http_response_code):
    vacancy_factory = f.make_vacancy_with_status_factory(vacancy_status)
    user = f.create_user()
    employee_candidate_factory = f.make_employee_candidate_factory()
    vacancy, consideration = f.create_simple_vacancy_with_candidate(
        vacancy_factory=vacancy_factory,
        candidate_factory=employee_candidate_factory
    )
    f.VacancyMembershipFactory(member=user, vacancy=vacancy, role=role)
    url = reverse('api:candidates:detail', kwargs={'pk': consideration.candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == http_response_code


def test_allow_aa_access_to_employee(client):
    user = f.create_aa_interviewer('canonical', 'aa_perm')
    employee_candidate_factory = f.make_employee_candidate_factory()
    vacancy, consideration = f.create_simple_vacancy_with_candidate(
        candidate_factory=employee_candidate_factory,
        aa_interviewer=user,
        regular_interview_factories=[]
    )
    url = reverse('api:candidates:detail', kwargs={'pk': consideration.candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == 200


@pytest.mark.parametrize('perm,switch_state,status_code', [
    ('recruiter_perm', True, 403),
    ('recruiting_manager_perm', True, 403),
    ('recruiter_assessor_perm', True, 403),
    ('aa_perm', True, 403),
    ('hrbp_perm', True, 200),
    ('auditor_perm', True, 403),
    ('recruiter_perm', False, 200),
    ('recruiting_manager_perm', False, 200),
    ('recruiter_assessor_perm', False, 403),
    ('aa_perm', False, 403),
    ('hrbp_perm', False, 200),
    ('auditor_perm', False, 200),
])
def test_static_roles_access_to_non_rotating_employees(client, perm, switch_state, status_code):
    f.create_waffle_switch(TemporarySwitch.DISABLE_ACCESS_TO_EMPLOYEES, switch_state)
    user = f.create_user_with_perm(perm)
    candidate = f.make_employee_candidate_factory()()
    url = reverse('api:candidates:detail', kwargs={'pk': candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == status_code


@pytest.mark.parametrize('perm,switch_state,status_code', [
    ('recruiter_perm', True, 200),
    ('recruiting_manager_perm', True, 200),
    ('recruiter_assessor_perm', True, 403),
    ('aa_perm', True, 403),
    ('hrbp_perm', True, 200),
    ('auditor_perm', True, 200),
    ('recruiter_perm', False, 200),
    ('recruiting_manager_perm', False, 200),
    ('recruiter_assessor_perm', False, 403),
    ('aa_perm', False, 403),
    ('hrbp_perm', False, 200),
    ('auditor_perm', False, 200),
])
def test_static_roles_access_to_new_rotating_candidate(client, perm, switch_state, status_code):
    f.create_waffle_switch(TemporarySwitch.DISABLE_ACCESS_TO_EMPLOYEES, switch_state)
    user = f.create_user_with_perm(perm)
    candidate, _ = f.make_rotating_employee(
        employee_candidate_factory=f.make_employee_candidate_factory(status=f.CANDIDATE_STATUSES.closed),
        rotation_factory=f.factory_maker(f.RotationFactory, status=ROTATION_STATUSES.new)
    )
    url = reverse('api:candidates:detail', kwargs={'pk': candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == status_code


@pytest.mark.parametrize('perm,switch_state,status_code', [
    ('recruiter_perm', True, 200),
    ('recruiting_manager_perm', True, 200),
    ('recruiter_assessor_perm', True, 403),
    ('aa_perm', True, 403),
    ('hrbp_perm', True, 200),
    ('auditor_perm', True, 200),
    ('recruiter_perm', False, 200),
    ('recruiting_manager_perm', False, 200),
    ('recruiter_assessor_perm', False, 403),
    ('aa_perm', False, 403),
    ('hrbp_perm', False, 200),
    ('auditor_perm', False, 200),
])
def test_static_roles_access_to_approved_rotating_candidate(client, perm, switch_state, status_code):
    f.create_waffle_switch(TemporarySwitch.DISABLE_ACCESS_TO_EMPLOYEES, switch_state)
    user = f.create_user_with_perm(perm)
    candidate, rotation = f.make_rotating_employee(
        employee_candidate_factory=f.make_employee_candidate_factory(status=f.CANDIDATE_STATUSES.closed),
        rotation_factory=f.factory_maker(f.RotationFactory, status=ROTATION_STATUSES.approved)
    )
    f.SubmissionFactory(candidate=candidate, status=f.SUBMISSION_STATUSES.new, rotation=rotation)
    url = reverse('api:candidates:detail', kwargs={'pk': candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == status_code


@pytest.mark.parametrize('perm,switch_state,status_code', [
    ('recruiter_perm', True, 403),
    ('recruiting_manager_perm', True, 403),
    ('recruiter_assessor_perm', True, 403),
    ('aa_perm', True, 403),
    ('hrbp_perm', True, 200),
    ('auditor_perm', True, 403),
    ('recruiter_perm', False, 200),
    ('recruiting_manager_perm', False, 200),
    ('recruiter_assessor_perm', False, 403),
    ('aa_perm', False, 403),
    ('hrbp_perm', False, 200),
    ('auditor_perm', False, 200),
])
def test_static_roles_access_to_finished_rotating_candidate(client, perm, switch_state, status_code):
    f.create_waffle_switch(TemporarySwitch.DISABLE_ACCESS_TO_EMPLOYEES, switch_state)
    user = f.create_user_with_perm(perm)
    candidate, rotation = f.make_rotating_employee(
        employee_candidate_factory=f.make_employee_candidate_factory(status=f.CANDIDATE_STATUSES.closed),
        rotation_factory=f.factory_maker(f.RotationFactory, status=ROTATION_STATUSES.approved)
    )
    f.SubmissionFactory(candidate=candidate, status=f.SUBMISSION_STATUSES.closed, rotation=rotation)
    url = reverse('api:candidates:detail', kwargs={'pk': candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == status_code


@pytest.mark.parametrize('perm,switch_state,status_code', [
    ('recruiter_perm', True, 403),
    ('recruiting_manager_perm', True, 403),
    ('recruiter_assessor_perm', True, 403),
    ('aa_perm', True, 403),
    ('hrbp_perm', True, 200),
    ('auditor_perm', True, 403),
    ('recruiter_perm', False, 200),
    ('recruiting_manager_perm', False, 200),
    ('recruiter_assessor_perm', False, 403),
    ('aa_perm', False, 403),
    ('hrbp_perm', False, 200),
    ('auditor_perm', False, 200),
])
def test_static_roles_access_to_rejected_rotating_candidate(client, perm, switch_state, status_code):
    f.create_waffle_switch(TemporarySwitch.DISABLE_ACCESS_TO_EMPLOYEES, switch_state)
    user = f.create_user_with_perm(perm)
    candidate, _ = f.make_rotating_employee(
        rotation_factory=f.factory_maker(f.RotationFactory, status=ROTATION_STATUSES.rejected)
    )
    url = reverse('api:candidates:detail', kwargs={'pk': candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == status_code


@pytest.mark.parametrize(
    'role, vacancy_status',
    [(role, status)
     for role in dynamic_roles
     for status in [VACANCY_STATUSES.closed, VACANCY_STATUSES.in_progress]]
)
def test_allow_access_to_non_employee_if_member(client, role, vacancy_status):
    vacancy_factory = f.make_vacancy_with_status_factory(vacancy_status)
    user = f.create_user()
    vacancy, consideration = f.create_simple_vacancy_with_candidate(
        vacancy_factory=vacancy_factory
    )
    f.VacancyMembershipFactory(member=user, vacancy=vacancy, role=role)
    url = reverse('api:candidates:detail', kwargs={'pk': consideration.candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == 200


@pytest.mark.parametrize(
    'role, vacancy_status',
    [(role, status)
     for role in dynamic_roles
     for status in [VACANCY_STATUSES.closed, VACANCY_STATUSES.in_progress]]
)
def test_deny_access_to_non_employee_if_not_member(client, role, vacancy_status):
    vacancy_factory = f.make_vacancy_with_status_factory(vacancy_status)
    user = f.create_user()
    vacancy, consideration = f.create_simple_vacancy_with_candidate(
        vacancy_factory=vacancy_factory
    )
    url = reverse('api:candidates:detail', kwargs={'pk': consideration.candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == 403


@pytest.mark.parametrize(
    'role, vacancy_status',
    [(role, status)
     for role in dynamic_roles
     for status in [VACANCY_STATUSES.closed, VACANCY_STATUSES.in_progress]]
)
def test_deny_access_to_employee_if_not_member_of_vacancy(client, role, vacancy_status):
    vacancy_factory = f.make_vacancy_with_status_factory(vacancy_status)
    user = f.create_user()
    employee_candidate_factory = f.make_employee_candidate_factory()
    vacancy, consideration = f.create_simple_vacancy_with_candidate(
        vacancy_factory=vacancy_factory,
        candidate_factory=employee_candidate_factory
    )
    url = reverse('api:candidates:detail', kwargs={'pk': consideration.candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == 403


@pytest.mark.parametrize('role', dynamic_roles)
def test_access_to_employee_if_member_of_second_vacancy(client, role):
    vacancy_factory1 = f.make_vacancy_with_status_factory(VACANCY_STATUSES.closed)
    consideration_factory1 = f.make_consideration_factory_with_status(CONSIDERATION_STATUSES.archived)
    vacancy_factory2 = f.make_vacancy_with_status_factory(VACANCY_STATUSES.in_progress)
    consideration_factory2 = f.make_consideration_factory_with_status(CONSIDERATION_STATUSES.in_progress)
    user = f.create_user()
    constant_employee_factory = f.make_constant_factory(f.make_employee_candidate_factory())
    vacancy1, consideration1 = f.create_simple_vacancy_with_candidate(
        vacancy_factory=vacancy_factory1,
        candidate_factory=constant_employee_factory,
        consideration_factory=consideration_factory1
    )
    vacancy2, consideration2 = f.create_simple_vacancy_with_candidate(
        vacancy_factory=vacancy_factory2,
        candidate_factory=constant_employee_factory,
        consideration_factory=consideration_factory2
    )
    f.VacancyMembershipFactory(member=user, vacancy=vacancy2, role=role)
    url = reverse('api:candidates:detail', kwargs={'pk': consideration1.candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == 200


@pytest.mark.parametrize(
    'vacancy_status, interviews, http_response', [
        (VACANCY_STATUSES.closed, 1, 403),
        (VACANCY_STATUSES.in_progress, 1, 200),
        (VACANCY_STATUSES.in_progress, 0, 403)
])
def test_access_non_vacancy_interviewer_to_employee_candidate(client, vacancy_status,
                                                              interviews, http_response):
    user = f.create_user()
    vacancy_factory = f.make_vacancy_with_status_factory(vacancy_status)
    interview_factory = f.make_regular_interview_with_state_factory(user)
    candidate_factory = f.make_employee_candidate_factory()
    _vacancy, consideration = f.create_simple_vacancy_with_candidate(
        vacancy_factory=vacancy_factory,
        regular_interview_factories=[interview_factory]*interviews,
        candidate_factory=candidate_factory
    )
    url = reverse('api:candidates:detail', kwargs={'pk': consideration.candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == http_response


@pytest.mark.parametrize(
    'vacancy_status, interviews, http_response', [
        (VACANCY_STATUSES.closed, 1, 403),
        (VACANCY_STATUSES.in_progress, 1, 200),
        (VACANCY_STATUSES.in_progress, 0, 403)
])
def test_access_non_vacancy_interviewer_to_candidate(client, vacancy_status,
                                                     interviews, http_response):
    user = f.create_user()
    vacancy_factory = f.make_vacancy_with_status_factory(vacancy_status)
    interview_factory = f.make_regular_interview_with_state_factory(user)
    _vacancy, consideration = f.create_simple_vacancy_with_candidate(
        vacancy_factory=vacancy_factory,
        regular_interview_factories=[interview_factory]*interviews
    )
    url = reverse('api:candidates:detail', kwargs={'pk': consideration.candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == http_response


@pytest.mark.parametrize(
    'vacancy_status, interviews', [
        (vacancy_status, interviews)
        for vacancy_status in [VACANCY_STATUSES.closed, VACANCY_STATUSES.in_progress]
        for interviews in range(2)
])
def test_access_vacancy_interviewer_to_non_employee_candidate(client, vacancy_status,
                                                              interviews):
    user = f.create_user()
    vacancy_factory = f.make_vacancy_with_status_factory(vacancy_status)
    interview_factory = f.make_regular_interview_with_state_factory(user)
    vacancy, consideration = f.create_simple_vacancy_with_candidate(
        vacancy_factory=vacancy_factory,
        regular_interview_factories=[interview_factory]*interviews
    )
    f.VacancyMembershipFactory(member=user, vacancy=vacancy, role=VACANCY_ROLES.interviewer)
    url = reverse('api:candidates:detail', kwargs={'pk': consideration.candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == 200


@pytest.mark.parametrize(
    'vacancy_status, interviews, http_response', [
        (VACANCY_STATUSES.closed, 1, 403),
        (VACANCY_STATUSES.closed, 0, 403),
        (VACANCY_STATUSES.in_progress, 1, 200),
        (VACANCY_STATUSES.in_progress, 0, 200)
])
def test_access_vacancy_interviewer_to_employee_candidate(client, vacancy_status,
                                                          interviews, http_response):
    user = f.create_user()
    vacancy_factory = f.make_vacancy_with_status_factory(vacancy_status)
    interview_factory = f.make_regular_interview_with_state_factory(user)
    candidate_factory = f.make_employee_candidate_factory()
    vacancy, consideration = f.create_simple_vacancy_with_candidate(
        vacancy_factory=vacancy_factory,
        regular_interview_factories=[interview_factory]*interviews,
        candidate_factory=candidate_factory
    )
    f.VacancyMembershipFactory(member=user, vacancy=vacancy, role=VACANCY_ROLES.interviewer)
    url = reverse('api:candidates:detail', kwargs={'pk': consideration.candidate.pk})
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == http_response


@pytest.mark.parametrize(
    'consideration_data, count', [
        ([(CONSIDERATION_STATUSES.in_progress, 5)], 5),
        ([(CONSIDERATION_STATUSES.archived, 6)], 6),
        ([(CONSIDERATION_STATUSES.archived, 1), (CONSIDERATION_STATUSES.in_progress, 6)], 7),
        ([(CONSIDERATION_STATUSES.archived, 3), (CONSIDERATION_STATUSES.archived, 5)], 8)
])
def test_access_non_vacancy_member_interviewer_to_interviews(client, consideration_data, count):
    user = f.create_user()
    interview_factory = f.make_regular_interview_with_state_factory(user)
    for consideration_status, cons_count in consideration_data:
        consideration_factory = f.make_consideration_factory_with_status(consideration_status)
        _vacancy, _consideration = f.create_simple_vacancy_with_candidate(
            consideration_factory=consideration_factory,
            regular_interview_factories=[interview_factory]*cons_count
        )
    url = reverse('api:interviews:list')
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == 200
    data = response.json()
    assert data['count'] == count


@pytest.mark.parametrize('is_employee, vacancy_status', [
    (is_employee, vacancy_status)
    for is_employee in [True, False]
    for vacancy_status in [VACANCY_STATUSES.closed, VACANCY_STATUSES.in_progress]
])
def test_allow_aa_access_to_candidate_aa_interviews_not_changed(client, is_employee,
                                                                vacancy_status):
    user = f.create_aa_interviewer('canonical', 'aa_perm')
    recruiter = f.create_recruiter()
    candidate_factory = f.make_employee_candidate_factory() if is_employee else f.CandidateFactory
    vacancy_factory = f.make_vacancy_with_status_factory(vacancy_status)
    _vacancy, _consideration = f.create_simple_vacancy_with_candidate(
        candidate_factory=candidate_factory,
        aa_interviewer=user,
        regular_interview_factories=[f.InterviewFactory],
        hr_screeneer=recruiter,
        vacancy_factory=vacancy_factory
    )
    url = reverse('api:interviews:list')
    client.login(user.username)
    response = client.get(url, {})
    assert response.status_code == 200
    data = response.json()
    assert data['count'] == 1


@pytest.mark.parametrize('method, body, status_code', [
    (APIClient.get, {}, 200),
    (APIClient.patch, {'first_name': 'I cant'}, 403),
])
def test_auditor_access_to_candidate(client, method, body, status_code):
    user = f.create_user_with_perm('auditor_perm')
    candidate = f.CandidateFactory()
    url = reverse('api:candidates:detail', kwargs={'pk': candidate.pk})
    client.login(user.username)
    response = method(client, url, body)
    assert response.status_code == status_code
