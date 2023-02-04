import pytest

from staff.departments.models import VacancyMember

from staff.proposal.controllers.vacancy import VacancyDataCtl
from staff.proposal.controllers.department import order_field


@pytest.mark.django_db
def test_as_form_data(company, vacancies):
    for vacancy in vacancies.values():
        VacancyMember.objects.create(vacancy=vacancy, person=company.persons['yandex-chief'])

    data_ctl = VacancyDataCtl(vacancies.keys(), company.persons['yandex-chief'])

    form_data = [data_ctl.as_form_data(vacancy_id) for vacancy_id in vacancies]
    for item in form_data:
        assert item['vacancy_id'] in vacancies
        assert item['department'] == ''  # не подставляем подразделение
        assert item['fake_department'] == ''
        assert item['action_id'] == ''


@pytest.mark.django_db
def test_as_meta(company, vacancies):
    for vacancy in vacancies.values():
        VacancyMember.objects.create(vacancy=vacancy, person=company.persons['yandex-chief'])
    data_ctl = VacancyDataCtl(vacancies.keys(), company.persons['yandex-chief'])

    meta_data = [data_ctl.as_meta(vacancy_id) for vacancy_id in vacancies]

    for item in meta_data:
        assert item['vacancy_id']
        v = vacancies[item['vacancy_id']]
        assert item['name'] == v.name
        assert item['status'] == v.status
        assert item['ticket'] == v.ticket
        assert item['is_active'] == v.is_active
        assert item['application_id'] == v.application_id
        assert item['candidate_last_name'] == v.candidate_last_name
        assert item['candidate_first_name'] == v.candidate_first_name
        assert item['candidate_login'] == v.candidate_first_name
        assert item['department_order_field'] == order_field(v.department.tree_id, v.department.lft)
        assert item['department_id'] == v.department.id
        assert item['department_level'] == v.department.level
        assert item['department_name'] == v.department.name
        assert item['department_url'] == v.department.url
        assert item['is_published'] == v.is_published
        assert item['candidate_id'] == v.candidate_id
        assert item['preprofile_id'] == v.preprofile_id
        assert item['offer_id'] == v.offer_id
        assert item['headcount_position_code'] == v.headcount_position_code
