import json

import mock
import pytest
from bson import ObjectId

from staff.budget_position.const import WORKFLOW_STATUS
from staff.budget_position.models.workflow import Workflow
from staff.departments.edit.proposal_mongo import MONGO_COLLECTION_NAME
from staff.departments.models import ProposalMetadata
from staff.departments.tests.factories import VacancyMemberFactory

from staff.proposal import views


@pytest.mark.django_db
def test_create_proposal(company, post_request, proposal_params, robot_staff_user, mocked_mongo):
    proposal_front_json, proposal_mongo_object = proposal_params

    if proposal_front_json['headcounts']['actions']:
        author = company.persons['dep1-hr-analyst']
    else:
        author = company.persons['dep12-chief']
    VacancyMemberFactory(vacancy=company.vacancies['dep111-vac'], person=author)
    request = post_request('proposal-api:add-proposal', proposal_front_json)
    request.user = author.user

    with mock.patch('staff.proposal.views.views.tasks.create_proposal_tickets') as create_tickets_function:
        response = views.add_proposal(request)

    assert response.status_code == 200
    response_content = json.loads(response.content)
    assert 'proposal_uid' in response_content
    proposal_id = response_content['proposal_uid']

    # функция создания тикетов вызвана правильно
    create_tickets_function.assert_called_once_with(proposal_id)

    # в монге лежит правильный объект
    mongo_object = mocked_mongo.db[MONGO_COLLECTION_NAME].find_one({'_id': ObjectId(proposal_id)})
    for field, value in proposal_mongo_object.items():
        assert field in mongo_object
        assert mongo_object[field] == value

    # в постгресе тоже лежит что надо
    assert ProposalMetadata.objects.filter(proposal_id=proposal_id).exists()
    assert Workflow.objects.filter(proposal__proposal_id=proposal_id, status=WORKFLOW_STATUS.PENDING).exists()


def test_creating_proposal_linked_to_existing_ticket(
        company,
        proposal4,
        proposal2_front_json,
        post_request,
        mocked_mongo):

    old_r15n_ticket = 'TSALARY-123456'
    mocked_mongo.db[MONGO_COLLECTION_NAME].update_one(
        {'_id': ObjectId(proposal4)},
        {'$set': {'tickets.restructurisation': old_r15n_ticket}}
    )
    ProposalMetadata.objects.filter(proposal_id=proposal4).update(applied_at='2020-10-10T12:34:56.789')
    Workflow.objects.filter(proposal__proposal_id=proposal4).update(status=WORKFLOW_STATUS.CONFIRMED)

    proposal2_front_json['link_to_ticket'] = old_r15n_ticket

    request = post_request('proposal-api:add-proposal', proposal2_front_json)
    author_user = company.persons['dep12-chief'].user
    request.user = author_user

    response = views.add_proposal(request)
    assert response.status_code == 400
    assert json.loads(response.content)['errors'] == {
        'link_to_ticket': [{'code': 'no_permission'}]
    }

    author_user.is_superuser = True
    author_user.save()
    response = views.add_proposal(request)
    assert response.status_code == 400
    errors = json.loads(response.content)['errors']
    assert 'link_to_ticket' in errors
    error = errors['link_to_ticket'][0]
    assert error['code'] == 'proposal_with_personal_tickets_cannot_be_linked'
    assert error['params']['message'] == 'proposal with personal tickets cannot be linked to another ticket'
    assert set(
        error['params']['personal_ticket_logins']
    ) == {'dep11-person', 'dep12-person', 'dep1-person', 'dep2-person'}
