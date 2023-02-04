from mock import patch, Mock
import arrow

from review.lib import datetimes

from review.core.models import Review, Kpi
from tests import helpers

PERIOD_NAME = '2021_Q5'

PERSON_KPI = {
    'person1': [
        {
            'goalsName': 'Goal 1',
            'prcOfCompletion': 0,
            'weight': 99
        }
    ],
    'person2': [
        {
            'goalsName': 'Goal 2',
            'prcOfCompletion': 0,
            'weight': 90
        }
    ],
    'person3': [
        {
            'goalsName': 'Goal 3',
            'prcOfCompletion': 109,
            'weight': 65
        },
        {
            'goalsName': 'Goal 4',
            'prcOfCompletion': 80,
            'weight': 30
        }
    ],
}

MOCKED_KPI_DATA = {
    'Rewards': [
        {
            'periodName': PERIOD_NAME,
            'periodType': 'Q',
            'personAnalytics': [
                {
                    'goals': PERSON_KPI[login],
                    'login': login,
                    'stmtPrcAward': 0,
                    'stmtPrcByGoals': 0,
                    'stmtPrcOnTheScale': 0,
                    'stmtTargetBonus': 1,
                    'userComment': None,
                    'workDayRatio': 1.95
                }
                for login in PERSON_KPI
            ],
            'rewardID': 42,
            'rewardName': 'reward Q2 2021'
        }
    ]
}

MOCKED_KPI_LOADED = arrow.get('2021-08-23T21:23:58.970460').datetime


def test_review_load_kpi(client, review_role_admin, person_review_builder, person_builder, robot):
    review = review_role_admin.review
    review.evaluation_from_date = datetimes.today()
    review.evaluation_to_date = datetimes.today()
    review.save()
    persons = [person_builder(login=login) for login in PERSON_KPI]
    login_to_person_reviews = {person.login: person_review_builder(review=review, person=person) for person in persons}
    with patch('review.oebs.sync.fetch.OebsConnector.post', return_value=MOCKED_KPI_DATA):
        with patch('review.lib.datetimes.now', lambda: MOCKED_KPI_LOADED):
            helpers.post_json(
                client=client,
                path='/frontend/reviews/{}/load-kpi/'.format(review.id),
                request={},
                login=review_role_admin.person.login,
            )
    kpi_loaded = Review.objects.filter(id=review.id).first().kpi_loaded
    assert kpi_loaded == MOCKED_KPI_LOADED
    assert len(Kpi.objects.all()) == 4
    year, quarter = list(map(int, PERIOD_NAME.split('_Q')))
    for login in PERSON_KPI:
        for goal in PERSON_KPI[login]:
            assert Kpi.objects.filter(
                person_review_id=login_to_person_reviews[login].id,
                name=goal['goalsName'],
                year=year,
                quarter=quarter,
                percent=goal['prcOfCompletion'],
                weight=goal['weight'],
            ).count() == 1
