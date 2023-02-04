from review.core import const
from tests.helpers import get_json


def test_api_review_stats(
    client,
    review,
    person_review_builder,
    review_role_builder,
    test_person
):
    review_role_builder(
        review=review,
        person=test_person,
        type=const.ROLE.REVIEW.SUPERREVIEWER
    )
    wait_approve = [
        person_review_builder(
            review=review,
            status=const.PERSON_REVIEW_STATUS.EVALUATION,
        )
        for _ in range(3)
    ]

    result = get_json(
        client=client,
        path='/v1/reviews/{}/stats/'.format(review.id),
        login=test_person.login,
    )

    assert result['all'] == len(wait_approve)
    assert result['waiting_for_approve_total'] == len(wait_approve)
