import pytest
from django.core.exceptions import ValidationError
from unittest.mock import patch, Mock

from intranet.femida.src.candidates.choices import (
    ROTATION_STATUSES,
    SUBMISSION_SOURCES,
    SUBMISSION_STATUSES,
    ROTATION_REASONS,
)
from intranet.femida.src.candidates.rotations.controllers import (
    create_rotation,
    rotation_approve,
    rotation_reject,
)
from intranet.femida.src.startrek.utils import StatusEnum

from intranet.femida.tests import factories as f
from intranet.femida.tests.utils import assert_not_raises


pytestmark = pytest.mark.django_db


@pytest.fixture
def new_rotation():
    submission = f.create_submission(
        source=SUBMISSION_SOURCES.rotation,
        status=SUBMISSION_STATUSES.draft,
    )
    return submission.rotation


@patch('intranet.femida.src.candidates.rotations.controllers.create_rotation_issue', Mock())
@patch('intranet.femida.src.candidates.rotations.controllers.create_myrotation_issue', Mock())
def test_create_rotation():
    data = {
        'publications': [f.create_publication()],
        'reason': ROTATION_REASONS.other,
        'comment': 'Comment #1',
        'is_privacy_needed': True,
    }
    initiator = f.create_user()
    rotation = create_rotation(data, initiator)

    assert rotation.created_by == initiator
    assert rotation.status == ROTATION_STATUSES.new
    assert rotation.submission.source == SUBMISSION_SOURCES.rotation
    assert rotation.submission.status == SUBMISSION_STATUSES.draft


@patch('intranet.femida.src.candidates.rotations.controllers.get_issue')
def test_rotation_approve_success(mocked_get_issue, new_rotation):
    mocked_get_issue().status.key = StatusEnum.validated
    rotation = new_rotation
    with assert_not_raises(ValidationError):
        rotation_approve(rotation)

    rotation.refresh_from_db()
    assert rotation.status == ROTATION_STATUSES.approved
    assert rotation.submission.status == SUBMISSION_STATUSES.new


@patch('intranet.femida.src.candidates.rotations.controllers.get_issue')
def test_rotation_approve_failure(mocked_get_issue, new_rotation):
    mocked_get_issue().status.key = StatusEnum.new
    rotation = new_rotation
    with pytest.raises(ValidationError):
        rotation_approve(rotation)

    rotation.refresh_from_db()
    assert rotation.status == ROTATION_STATUSES.new
    assert rotation.submission.status == SUBMISSION_STATUSES.draft


@patch('intranet.femida.src.candidates.rotations.controllers.get_issue')
def test_rotation_reject_success(mocked_get_issue, new_rotation):
    mocked_get_issue().status.key = StatusEnum.closed
    rotation = new_rotation
    with assert_not_raises(ValidationError):
        rotation_reject(rotation)

    rotation.refresh_from_db()
    assert rotation.status == ROTATION_STATUSES.rejected
    assert rotation.submission.status == SUBMISSION_STATUSES.closed


@patch('intranet.femida.src.candidates.rotations.controllers.get_issue')
def test_rotation_reject_failure(mocked_get_issue, new_rotation):
    mocked_get_issue().status.key = StatusEnum.new
    rotation = new_rotation
    with pytest.raises(ValidationError):
        rotation_reject(rotation)

    rotation.refresh_from_db()
    assert rotation.status == ROTATION_STATUSES.new
    assert rotation.submission.status == SUBMISSION_STATUSES.draft
