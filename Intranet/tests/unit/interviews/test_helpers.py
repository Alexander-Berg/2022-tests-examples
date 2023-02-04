import json

import pytest

from constance.test import override_config
from unittest.mock import patch

from intranet.femida.src.interviews.choices import INTERVIEW_TYPES, AA_TYPES
from intranet.femida.src.interviews.helpers import is_interview_review_needed

from intranet.femida.tests import factories as f


@pytest.mark.parametrize('interview_type, aa_type, rnd_result, expected_result', (
    pytest.param(INTERVIEW_TYPES.aa, AA_TYPES.canonical, 99, False, id='AAcanonical[missed]'),
    pytest.param(INTERVIEW_TYPES.aa, AA_TYPES.canonical, 0, True, id='AAcanonical[reached]'),
    pytest.param(INTERVIEW_TYPES.aa, AA_TYPES.dev_ops, 75, False, id='AAsub[missed]'),
    pytest.param(INTERVIEW_TYPES.aa, AA_TYPES.dev_ops, 25, True, id='AAsub[reached]'),
    pytest.param(INTERVIEW_TYPES.aa, AA_TYPES.analytic, 99, True, id='AAsub[always]'),
    pytest.param(INTERVIEW_TYPES.aa, AA_TYPES.to, 0, False, id='AAsub[never]'),
))
@patch('intranet.femida.src.interviews.helpers.random')
@override_config(**{
    'REVIEW_AA_TYPE_PROBABILITY': json.dumps({
        'canonical': 10,
        'to': 0,
        'analytic': 100,
        'dev_ops': 50,
    }),
})
def test_is_interview_review_needed(random_mock, interview_type, aa_type, rnd_result, expected_result):
    random_mock.randrange.return_value = rnd_result
    interview = f.InterviewFactory(
        type=interview_type,
        aa_type=aa_type,
    )
    assert is_interview_review_needed(interview) == expected_result
