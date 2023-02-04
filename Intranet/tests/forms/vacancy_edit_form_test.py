import pytest

from staff.departments.tests.factories import VacancyFactory, VacancyMemberFactory
from staff.lib.testing import StaffFactory

from staff.proposal.forms.proposal import ProposalForm
from staff.proposal.forms.vacancy import VacancyEditForm


@pytest.mark.django_db
@pytest.mark.parametrize('is_fake', (True, False))
def test_vacancy_edit_form_department(company, is_fake):
    author = StaffFactory()
    vacancy = VacancyFactory()
    VacancyMemberFactory(vacancy=vacancy, person=author)

    fake_dep = 'dep_001'
    form_data = {
        'action_id': 'act_007',
        'vacancy_id': vacancy.id,
        'department': '' if is_fake else company.dep2.url,
        'fake_department': fake_dep if is_fake else '',
    }

    form = VacancyEditForm(data=form_data, base_initial={'author_user': author.user})

    assert form.is_valid()
    assert form.cleaned_data['vacancy_id'] == vacancy.id
    if is_fake:
        assert form.cleaned_data['fake_department'] == fake_dep
    else:
        assert form.cleaned_data['department'] == company.dep2.url


@pytest.mark.django_db
def test_vacancy_edit_form_is_valid_on_editing(company, mocked_mongo, robot_staff_user):
    author = StaffFactory()
    vacancy = VacancyFactory()

    form_data = {
        'vacancies': {
            'actions': [{
                'action_id': 'act_007',
                'vacancy_id': vacancy.id,
                'department': company.dep2.url,
            }],
        },
        'apply_at': '2032-12-01',
    }

    form = ProposalForm(
        data=form_data,
        base_initial={'author_user': author.user},
        initial=form_data,
    )

    assert form.is_valid()
