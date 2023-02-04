from builtins import str
import pytest
from django.contrib.auth import get_user_model

from kelvin.results.models import LessonResult, LessonSummary


User = get_user_model()


@pytest.fixture
def lesson_result_models(jclient, lesson_models):
    jclient.login()
    lesson, problem1, problem2, problem_link1, problem_link2 = lesson_models
    summary = LessonSummary.objects.create(lesson=lesson,
                                           student=jclient.user)
    lesson_result = LessonResult.objects.create(
        answers={
            str(problem_link1.id): [
                {
                    'markers': {
                        '1': {
                            'answer_status': {'1': True},
                            'user_answer': {'1': '4.0'},
                            'mistakes': 0,
                            'max_mistakes': 1,
                        },
                    },
                    'theory': None,
                    'mistakes': 0,
                    'max_mistakes': 1,
                    'completed': False,
                    'spent_time': None,
                },
            ],
            str(problem_link2.id): [
                {
                    'markers': {
                        '1': {
                            'answer_status': [0, 1],
                            'user_answer': [0, 1],
                            'mistakes': 1,
                            'max_mistakes': 3,
                        },
                    },
                    'theory': None,
                    'mistakes': 1,
                    'max_mistakes': 3,
                    'completed': True,
                    'spent_time': None,
                },
            ],
        },
        completed=False,
        points=1,
        max_points=2,
        spent_time=100500,
        summary=summary,
    )
    return lesson_result, lesson, problem_link1, problem_link2
