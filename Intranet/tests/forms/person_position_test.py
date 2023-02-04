import datetime

from mock import MagicMock
import pytest

from staff.departments.controllers.proposal import ProposalCtl
from staff.departments.controllers.proposal_execution import ProposalExecution
from staff.lib.testing import StaffFactory
from staff.oebs.models import Job
from staff.proposal.forms.conversions import get_initial_old_style

from staff.proposal.forms.proposal import ProposalForm
from staff.proposal.proposal_builder import ProposalBuilder


@pytest.fixture()
def person():
    person = StaffFactory(
        login='user',
    )
    person.has_perm = lambda x: True
    person.save()
    return person


def fast_search_in_dict(dct, path='', sep='.'):
    retval = dct['data']
    for item in path.split(sep):
        if isinstance(retval, list):
            item = int(item)
        retval = retval[item]['value']
    return retval


def common_part(proposal_id, person):
    ctl = ProposalCtl(proposal_id)
    ctl._workflow_registry_updater = MagicMock()
    ProposalExecution(ctl).execute()

    proposal_ctl = ProposalCtl(proposal_id=proposal_id, author=person)
    initial_data = get_initial_old_style(proposal_ctl)

    form = ProposalForm(
        initial_old_style=initial_data,
        base_initial={
            '_id': proposal_id,
            'author_user': person.user,
            'locked_proposal': proposal_ctl.locked,
        },
    )
    return form.as_dict()


@pytest.mark.django_db
def test_that_check_that_old_proposal_wont_give_500(mocked_mongo, person, deadlines):
    proposal_id = (
        ProposalBuilder()
        .with_person(person.login, lambda person: person.staff_position('питонист').legal_position(115))
        .build(StaffFactory().login)
    )

    ctl = ProposalCtl(proposal_id)
    ctl._workflow_registry_updater = MagicMock()
    ctl.proposal_object['persons']['actions'][0]['position']['position_legal'] = 'some old string instead of job id'
    ctl.save()

    result = common_part(proposal_id, person)

    path = 'persons.actions.0.position.position_legal'
    assert fast_search_in_dict(result, path) == 'some old string instead of job id'

    path = 'persons.actions.0.position.new_position'
    assert fast_search_in_dict(result, path) == 'питонист'


@pytest.mark.django_db
def test_that_check_that_if_there_is_job_then_return_code_position(mocked_mongo, person, deadlines):
    Job(code=115, start_date=datetime.date(2019, 8, 26), name='Python developer').save()

    proposal_id = (
        ProposalBuilder()
        .with_person(person.login, lambda person: person.staff_position('питонист').legal_position(115))
        .build(StaffFactory().login)
    )

    result = common_part(proposal_id, person)

    path = 'persons.actions.0.position.position_legal'
    assert fast_search_in_dict(result, path) == 115

    path = 'persons.actions.0.position.new_position'
    assert fast_search_in_dict(result, path) == 'питонист'
