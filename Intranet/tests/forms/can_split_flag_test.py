from waffle.models import Switch

import pytest
from assertpy import assert_that

from staff.proposal.proposal_builder import ProposalBuilder


@pytest.fixture
def all_in_one(mocked_mongo, without_workflows, tester_that_can_execute):
    Switch.objects.create(name='enable_proposal_splitting', active=True)


@pytest.mark.django_db
def test_split_flag_when_only_person_actions_in_form(company, all_in_one, edit_proposal_get_view):
    proposal_id = (
        ProposalBuilder()
        .with_person(
            company.persons['dep11-person'].login,
            lambda person: person.staff_position('dark lord').with_ticket('T-1'),
        )
        .build(author_login='dep1-chief')
    )

    res = edit_proposal_get_view(proposal_id)

    assert_that(res['state']).has_can_split(True)


@pytest.mark.django_db
def test_split_flag_when_proposal_has_vacancy(company, all_in_one, edit_proposal_get_view):
    proposal_id = (
        ProposalBuilder()
        .with_vacancy(
            company['dep11-vac'].id,
            lambda vacancy: vacancy.with_ticket('T-1'),
        )
        .build(author_login='dep1-chief')
    )

    res = edit_proposal_get_view(proposal_id)

    assert_that(res['state']).has_can_split(False)


@pytest.mark.django_db
def test_split_flag_when_moving_into_existing_department(company, all_in_one, edit_proposal_get_view):
    proposal_id = (
        ProposalBuilder()
        .for_existing_department(
            company.dep1.url,
            lambda department: (
                department
                .use_for_person(
                    company.persons['dep11-person'].login,
                    lambda person: person.with_ticket('T-1'),
                )
                .use_for_person(
                    company.persons['dep111-person'].login,
                    lambda person: person.with_ticket('T-2'),
                )
            )
        )
        .build(author_login='dep1-chief')
    )

    res = edit_proposal_get_view(proposal_id)

    assert_that(res['state']).has_can_split(True)
