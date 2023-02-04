import pytest

from review.core import const
from review.lib import datetimes

from tests.helpers import get_json


@pytest.fixture
def requested_mode_history(
    client,
    person_review_builder,
    review_role_builder,
):
    person_review = person_review_builder()
    person_review.tag_average_mark = 'test tag_average_mark'
    person_review.save()
    person_review_builder(
        person=person_review.person,
    )
    admin = review_role_builder(
        review=person_review.review,
        type=const.ROLE.REVIEW.ADMIN,
    ).person
    return get_json(
        client,
        '/frontend/person-reviews-mode-history/?person={}'.format(person_review.person.login),
        login=admin.login,
    )


@pytest.mark.parametrize(
    'field', [
        'join_at',
        'chief',
        'department_name',
        'department_chain_names',
        'city_name',
    ]
)
def test_person_fields_existing(requested_mode_history, field):
    assert field in requested_mode_history['person']


@pytest.mark.parametrize(
    'field', [
        'profession',
        'umbrella',
    ]
)
def test_field_existing(field, requested_mode_history):
    assert all(field in pr for pr in requested_mode_history['person_reviews'])


@pytest.mark.parametrize(
    'field', [
        'scale_id',
        'options_rsu_unit',
    ]
)
def test_review_fields_existing(requested_mode_history, field):
    assert all(field in pr['review'] for pr in requested_mode_history['person_reviews'])


@pytest.mark.parametrize(
    'checking_type', [
        const.REVIEW_TYPE.NORMAL,
        const.REVIEW_TYPE.MIDDLE,
        const.REVIEW_TYPE.BONUS,
    ]
)
def test_person_review_short_history(
    client,
    person_builder,
    person_review_builder,
    review_builder,
    marks_scale_builder,
    checking_type,
):
    today = datetimes.today()
    scale1 = marks_scale_builder(scale={'A': 0, 'B': 1, 'C': 2, 'D': 3, 'E': 4})
    scale2 = marks_scale_builder(scale={'A': 0, 'B': -1, 'C': -2, 'D': -3, 'E': -4})

    def review_dates(months):
        return dict(
            finish_date=datetimes.shifted(today, months=months),
            start_date=datetimes.shifted(today, months=months, days=-1),
        )

    mark_and_review_kwargs_list = [
        ['A', 0, dict(type=checking_type, scale_id=scale1.id, **review_dates(-1))],
        ['B', 1, dict(type=const.REVIEW_TYPE.MIDDLE, scale_id=scale2.id, **review_dates(-2))],
        ['C', 2, dict(type=const.REVIEW_TYPE.BONUS, scale_id=scale1.id, **review_dates(-3))],
        ['D', -3, dict(type=const.REVIEW_TYPE.NORMAL, scale_id=scale2.id, **review_dates(-4))],
        ['E', 4, dict(type=const.REVIEW_TYPE.NORMAL, scale_id=scale1.id, **review_dates(-5))],
        ['A', 5, dict(type=const.REVIEW_TYPE.NORMAL, scale_id=scale2.id, **review_dates(-6))],
    ]
    person = person_builder()
    person_reviews = [
        person_review_builder(
            review=review_builder(
                scale=marks_scale_builder(scale={'A': 1, 'B': 1, 'C-': 2, 'C': 3, 'D': 4, 'E': 5}),
                level_change_mode=const.REVIEW_MODE.MODE_MANUAL,
                **review_kwargs
            ),
            person=person,
            mark=mark,
            level_change=level_change,
            status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
        )
        for mark, level_change, review_kwargs in mark_and_review_kwargs_list
    ]
    checking_id = person_reviews[0].review.id

    response = get_json(
        client,
        '/frontend/person-reviews-mode-review/?reviews={}'.format(checking_id),
        login=person.login
    )
    received_history = response['person_reviews'][0]['mark_level_history']
    expecting_history = [
        dict(mark='A', level_change=5, level=const.EMPTY, scale_id=scale2.id),
        dict(mark='E', level_change=4, level=const.EMPTY, scale_id=scale1.id),
        dict(mark='D', level_change=-3, level=const.EMPTY, scale_id=scale2.id),
    ]
    assert received_history == expecting_history


def test_get_tag_average_mark_field(
    requested_mode_history,
):
    person_reviews = requested_mode_history['person_reviews']
    for person_review in person_reviews:
        assert person_review['tag_average_mark'] == 'test tag_average_mark'
