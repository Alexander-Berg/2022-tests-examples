from unittest.mock import patch

import pytest
from django.utils.dateparse import parse_datetime

from intranet.femida.src.contest.helpers import get_contest_results_by_participants, sync_contest
from intranet.femida.src.contest.models import Contest, ContestProblem
from intranet.femida.tests.mock.contest import ContestAnswers
from integration.contest.mock import FakeContestAPI, FakeContestTVMApi

pytestmark = pytest.mark.django_db


def fake_service_ticket():
    return "ticket"


@patch('intranet.femida.src.contest.helpers.ContestAPI', FakeContestAPI)
def test_get_contest_results_by_participants():
    result = get_contest_results_by_participants(contest_id=1, participant_ids={1, 2, 3})
    assert set(result.keys()) == {1, 2}

    assert result[1]['participation'] == {
        'id': 1,
        'login': 'login1',
        'name': 'name1',
        'start_time': FakeContestAPI.START_TIME,
        'finish_time': FakeContestAPI.FINISH_TIME,
    }
    assert result[1]['results']['score'] == 1
    assert [i['status'] for i in result[1]['results']['problems']] == ['ACCEPTED', 'NOT_ACCEPTED']

    assert result[2]['participation'] == {
        'id': 2,
        'login': 'login2',
        'name': 'name2',
        'start_time': FakeContestAPI.START_TIME,
        'finish_time': FakeContestAPI.FINISH_TIME,
    }
    assert result[2]['results']['score'] == 1
    assert [i['status'] for i in result[2]['results']['problems']] == ['ACCEPTED', 'NOT_SUBMITTED']


@patch('intranet.femida.src.contest.helpers.ContestTVMApi', FakeContestTVMApi)
def test_sync_contest_meta_info():
    sync_contest(Contest(contest_id=1))
    contest_1 = Contest.objects.filter(contest_id=1)[0]

    expected_time = parse_datetime("2022-01-01T12:03:00.000Z")
    assert contest_1.is_infinite is False
    assert contest_1.finishing_at == expected_time

    actual_problem_1 = ContestProblem.objects.filter(problem_origin_id='1/1/1')[0]
    actual_problem_2 = ContestProblem.objects.filter(problem_origin_id='2/2/2')[0]
    assert actual_problem_1.html_text == ContestAnswers.tex_statement_html
    assert actual_problem_2.html_text == ContestAnswers.md_statement_html


@patch('intranet.femida.src.contest.helpers.ContestTVMApi', FakeContestTVMApi)
def test_sync_contest_meta_info_infinite():
    sync_contest(Contest(contest_id=2))
    contest_2 = Contest.objects.filter(contest_id=2)[0]

    assert contest_2.is_infinite is True
    assert contest_2.finishing_at is None
