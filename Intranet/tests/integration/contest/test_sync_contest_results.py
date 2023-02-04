import pytest

from unittest import mock

from constance.test import override_config

from intranet.femida.src.candidates import choices
from intranet.femida.src.candidates.models import Challenge
from intranet.femida.src.candidates.submissions.internships import (
    sync_contest_results,
    sync_contest_participant_ids,
    get_challenges_by_contest_id,
)

from .mock import FakeContestAPI, FakeContestPrivateAPI

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_get_challenges_by_contest_id(contest_dataset):
    data = get_challenges_by_contest_id()
    assert data == {
        1: {
            2: contest_dataset['challenge_1_2'],
            3: contest_dataset['challenge_1_3'],
            4: contest_dataset['challenge_1_4'],
        },
        2: {
            5: contest_dataset['challenge_2_5'],
        }
    }


@mock.patch('intranet.femida.src.contest.helpers.ContestAPI', FakeContestAPI)
def test_sync_contest_results(contest_dataset):
    sync_contest_results(
        contest_id=1,
        challenges_by_participant_id={
            2: contest_dataset['challenge_1_2'],
            3: contest_dataset['challenge_1_3'],
            4: contest_dataset['challenge_1_4'],
        },
    )

    challenge_1_2 = Challenge.objects.get(
        answers__contest__id=1,
        answers__participation__id=2,
    )
    assert challenge_1_2.status == choices.CHALLENGE_STATUSES.pending_review
    assert 'results' in challenge_1_2.answers

    challenge_1_3 = Challenge.objects.get(
        answers__contest__id=1,
        answers__participation__id=3,
    )
    assert challenge_1_3.status == choices.CHALLENGE_STATUSES.assigned
    assert 'results' not in challenge_1_3.answers

    challenge_1_4 = Challenge.objects.get(
        answers__contest__id=1,
        answers__participation__id=4,
    )
    assert challenge_1_4.status == choices.CHALLENGE_STATUSES.assigned
    assert 'results' not in challenge_1_4.answers


@mock.patch('intranet.femida.src.contest.helpers.ContestPrivateAPI', FakeContestPrivateAPI)
def test_sync_contest_participant_ids():
    submission = f.SubmissionFactory(
        forms_data={'params': {'answer_id': '133330607'}},
    )
    challenge = f.ChallengeFactory(
        submission=submission,
        type=choices.CHALLENGE_TYPES.contest,
        answers={
            'participation': {'id': None},
        },
    )
    sync_contest_participant_ids()
    challenge.refresh_from_db()
    assert challenge.participant_id is not None


@mock.patch('intranet.femida.src.contest.helpers.ContestAPI', FakeContestAPI)
@pytest.mark.parametrize('autoestimate, result_status', (
    pytest.param('True', choices.CHALLENGE_STATUSES.finished, id='autoestimate'),
    pytest.param('False', choices.CHALLENGE_STATUSES.pending_review, id='manual'),
))
@pytest.mark.parametrize('threshold, resolution', (
    pytest.param(0, choices.CHALLENGE_RESOLUTIONS.hire, id='over_threshold'),
    pytest.param(100500, choices.CHALLENGE_RESOLUTIONS.nohire, id='below_threshold'),
))
def test_sync_onedayoffer_results(threshold, resolution, autoestimate, result_status):
    contest_id = 1
    participant_id = 2
    onedayoffer_submission = f.SubmissionFactory(
        forms_data={'params': {'passcode': 'abcde', 'autoestimate': autoestimate}},
    )
    challenge = f.ChallengeFactory(
        submission=onedayoffer_submission,
        type=choices.CHALLENGE_TYPES.contest,
        answers={
            'contest_id': contest_id,
            'participation': {'id': participant_id},
        },
    )

    with override_config(DEFAULT_ONEDAYOFFER_THRESHOLD=threshold):
        sync_contest_results(
            contest_id=contest_id,
            challenges_by_participant_id={participant_id: challenge},
        )

    challenge.refresh_from_db()
    assert challenge.status == result_status
    if challenge.status != choices.CHALLENGE_STATUSES.finished:
        resolution = ''
    assert challenge.resolution == resolution
