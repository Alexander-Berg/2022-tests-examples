from copy import deepcopy
from datetime import datetime, timedelta

import pytest

from staff.departments.edit.proposal_mongo import MONGO_COLLECTION_NAME
from staff.departments.tests.factories import ProposalMetadataFactory

from staff.monitorings.views.proposals import get_proposals_without_persons_tickets


@pytest.fixture
def mongo_collection(mocked_mongo, db):
    now = datetime.now()
    outdated = {
        'actions': [],
        'persons': {
            'actions': [
                {
                    'comment': 'перевод',
                    'office': {
                        'office': 148
                    },
                    'login': 'kvronskaya',
                    'sections': [
                        'office',
                    ],
                    'action_id': 'act_96551'
                }
            ]
        },
        'apply_at_hr': '2018-05-02',
        'tickets': {
            'department_ticket': '',
            'department_linked_ticket': '',
            'persons': {},
            'deleted_persons': {}
        },
        '_prev_actions_state': {},
        'author': 24267,
        'created_at': (now - timedelta(days=367)).isoformat(),
        'pushed_to_oebs': None,
    }

    incorrect = deepcopy(outdated)
    incorrect['created_at'] = (now - timedelta(days=10)).isoformat()

    correct = deepcopy(incorrect)
    correct['tickets']['persons']['kvronskaya'] = 'SALARY-123456789'

    deleted = deepcopy(correct)
    deleted['persons']['actions'][0]['login'] = 'wlame'
    deleted['tickets']['persons']['wlame'] = 'SALARY-4234112'
    deleted['slug'] = 'deleted'

    collection = mocked_mongo.db[MONGO_COLLECTION_NAME]

    collection.insert([outdated, correct, incorrect, deleted])

    deleted_id = collection.find_one({'slug': 'deleted'})['_id']
    ProposalMetadataFactory(proposal_id=deleted_id, deleted_at=datetime.now())

    return collection


def test_get_proposals_without_persons_tickets(mongo_collection):
    suspicious = get_proposals_without_persons_tickets()
    assert len(suspicious) == 1
    assert list(suspicious.values()) == [['kvronskaya']]
