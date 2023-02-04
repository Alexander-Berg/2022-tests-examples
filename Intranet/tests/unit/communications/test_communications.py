import pytest

from intranet.femida.src.communications.controllers import (
    update_or_create_external_message,
    identify_candidate_from_separator,
)
from intranet.femida.src.communications.workflow import NoteWorkflow

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_applications_modified_field_doesnt_change():
    candidate = f.CandidateFactory.create()
    application = f.ApplicationFactory.create(candidate=candidate)
    modified_snapshot = application.modified
    data = {
        'attachments': [],
        'candidate': candidate,
        'text': 'text',
        'type': 'outcoming',
        'email': 'email@email.com',
        'subject': 'subject',
    }
    update_or_create_external_message(data)
    assert modified_snapshot == application.modified


def test_identify_from_separator_with_appl_id_is_none():
    data = {
        'type': 'IDENTIFY',
        'messageId': '12345',
        'appl_id': None,
        'from': 'email@email.com',
    }
    assert identify_candidate_from_separator(data) is None


@pytest.mark.parametrize('user_perm, result', (
    ('recruiter_perm', True),
    ('recruiter_assessor_perm', True),
    (None, False),
))
@pytest.mark.parametrize('action', ('update', 'delete', 'reminder_create'))
def test_note_actions_permissions(user_perm, result, action):
    user = f.create_user_with_perm(user_perm)
    note = f.create_note(author=user)
    workflow = NoteWorkflow(
        instance=note,
        user=user,
    )
    assert workflow.get_action(action).has_permission() is result
