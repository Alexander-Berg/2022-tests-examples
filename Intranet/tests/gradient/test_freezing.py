from unittest import mock

import pytest
from django.utils import timezone

from review.core import const
from review.core.logic import review_actions
from review.core.models import PersonReviewChange
from review.gradient.freezing import choose_main_umbrella, freeze_gradient_data
from tests import helpers
from tests.helpers import post_json


def _check_last_change(person_review, expected_fields, expected_diff):
    change = PersonReviewChange.objects.filter(
        person_review_id=person_review.id
    ).order_by('-created_at').first()
    helpers.check_db_data(change, expected_fields)
    helpers.assert_is_substructure(expected_diff, change.diff)


@pytest.mark.parametrize(
    'engagement_data,expected_main_umbrella',
    [
        ({}, None),
        ({'u1': 100}, 'u1'),
        ({'u1': 51, 'u2': 49}, 'u1'),
        ({'u1': 10, 'u2': 10, 'u3': 80}, 'u3'),
        ({'u1': 10, 'u2': 10, 'u3': 10}, 'default'),
        ({'default': 1}, 'default'),
        ({'default': 1, 'u1': 1, 'u2': 2}, 'default'),
        ({'default': 1, 'u1': 50}, 'default'),
        ({'default': 1, 'u1': 51}, 'u1'),
    ],
)
def test_choose_main_umbrella(
    engagement_data,
    expected_main_umbrella,
    person_builder,
    umbrella_builder,
    umbrella_person_builder,
    main_product_builder,
):
    person = person_builder()
    umbrellas = {
        'u1': umbrella_builder(main_product=main_product_builder()),
        'u2': umbrella_builder(main_product=main_product_builder()),
        'u3': umbrella_builder(main_product=main_product_builder()),
        'default': umbrella_builder(main_product=None),
    }
    umbrella_person_list = []
    for key, engagement in engagement_data.items():
        umbrella_person_list.append(
            umbrella_person_builder(person=person, umbrella=umbrellas[key], engagement=engagement)
        )

    main_umbrella = choose_main_umbrella(umbrella_person_list, umbrellas['default'])

    if expected_main_umbrella:
        assert main_umbrella == umbrellas[expected_main_umbrella]
    else:
        assert main_umbrella is None


def test_freeze_gradient_data(
    review_builder,
    person_review_builder,
    umbrella_person_builder,
    main_product_person_builder,
    robot,
):
    review = review_builder()
    freezing_datetime = timezone.now()
    review_data = []
    for i in range(3):
        person_review = person_review_builder(review=review)
        umbrella_person_review = umbrella_person_builder(person=person_review.person, engagement=100)
        main_product_person_review = main_product_person_builder(person=person_review.person)
        review_data.append((person_review, umbrella_person_review, main_product_person_review))

    freeze_gradient_data(review.id, freezing_datetime)

    review.refresh_from_db()
    assert review.product_schema_loaded == freezing_datetime

    for person_review, umbrella_person_review, main_product_person_review in review_data:
        person_review.refresh_from_db()
        assert person_review.umbrella == umbrella_person_review.umbrella
        assert person_review.main_product == main_product_person_review.main_product
        _check_last_change(
            person_review,
            expected_fields={
                'subject': robot,
            },
            expected_diff={
                'umbrella': {'old': None, 'new': umbrella_person_review.umbrella.id},
                'main_product': {'old': None, 'new': main_product_person_review.main_product.id},
            }
        )


def test_freeze_gradient_data_by_person_review_ids(
    review_builder,
    person_review_builder,
    umbrella_person_builder,
    main_product_person_builder,
    robot,
):
    review = review_builder()

    be_freezed = person_review_builder(review=review)
    umbrella_person_review = umbrella_person_builder(person=be_freezed.person, engagement=100)
    main_product_person_review = main_product_person_builder(person=be_freezed.person)

    not_be_freezed = person_review_builder(review=review)
    umbrella_person_builder(person=not_be_freezed.person, engagement=100)
    main_product_person_builder(person=not_be_freezed.person)

    freeze_gradient_data(review.id, person_review_ids=[be_freezed.id])

    be_freezed.refresh_from_db()
    assert be_freezed.umbrella == umbrella_person_review.umbrella
    assert be_freezed.main_product == main_product_person_review.main_product
    _check_last_change(
        be_freezed,
        expected_fields={
            'subject': robot,
        },
        expected_diff={
            'umbrella': {'old': None, 'new': umbrella_person_review.umbrella.id},
            'main_product': {'old': None, 'new': main_product_person_review.main_product.id},
        }
    )
    not_be_freezed.refresh_from_db()
    assert not_be_freezed.umbrella is None
    assert not_be_freezed.main_product is None


def test_freeze_gradient_data_no_data(
    review_builder,
    person_review_builder,
    robot,
):
    review = review_builder()
    person_review = person_review_builder(review=review)
    freezing_datetime = timezone.now()

    freeze_gradient_data(review.id, freezing_datetime)

    person_review.refresh_from_db()
    assert person_review.umbrella is None
    assert person_review.main_product is None
    assert not PersonReviewChange.objects.exists()
    review.refresh_from_db()
    assert review.product_schema_loaded is None


def test_freeze_gradient_data_no_main_product(
    review_builder,
    person_review_builder,
    main_product_person_builder,
    robot,
):
    review = review_builder()
    person_review = person_review_builder(review=review)
    main_product_person_review = main_product_person_builder(person=person_review.person)

    freeze_gradient_data(review.id)

    person_review.refresh_from_db()
    assert person_review.umbrella is None
    assert person_review.main_product == main_product_person_review.main_product
    _check_last_change(
        person_review,
        expected_fields={
            'subject': robot,
        },
        expected_diff={
            'main_product': {'old': None, 'new': main_product_person_review.main_product.id},
        }
    )


def test_freeze_gradient_data_no_umbrella(
    review_builder,
    person_review_builder,
    umbrella_person_builder,
    robot,
):
    review = review_builder()
    person_review = person_review_builder(review=review)
    umbrella_person_review = umbrella_person_builder(person=person_review.person, engagement=100)

    freeze_gradient_data(review.id)

    person_review.refresh_from_db()
    assert person_review.umbrella == umbrella_person_review.umbrella
    assert person_review.main_product is None
    _check_last_change(
        person_review,
        expected_fields={
            'subject': robot,
        },
        expected_diff={
            'umbrella': {'old': None, 'new': umbrella_person_review.umbrella.id},
        }
    )


def test_freeze_gradient_data_when_publish_review(review_builder):
    review = review_builder(status=const.REVIEW_STATUS.DRAFT)
    now = timezone.now()

    with mock.patch('review.core.tasks.freeze_gradient_data_task.delay') as mocked_task:
        with mock.patch('django.utils.timezone.now', return_value=now):
            review_actions.follow_workflow(review, const.REVIEW_ACTIONS.STATUS_PUBLISH)

    mocked_task.assert_called_once_with(review.id, now)
    review.refresh_from_db()


def test_freeze_gradient_data_add_persons(
    review_builder,
    person_builder,
    review_role_builder,
    staff_structure_builder,
    client,
):
    review = review_builder(
        product_schema_loaded=timezone.now(),
        status=const.REVIEW_STATUS.IN_PROGRESS,
    )
    admin = review_role_builder(review=review, type=const.ROLE.REVIEW.ADMIN)
    person = person_builder()
    structure = staff_structure_builder(persons=[person])

    with mock.patch('review.core.tasks.freeze_gradient_data_task.delay') as mocked_task:
        post_json(
            client=client,
            path='/frontend/reviews/{}/add-persons/'.format(review.id),
            request={
                'data': [{
                    'type': 'person',
                    'login': person.login,
                    'structure_change': structure['structure_change'].id,
                }],
            },
            login=admin.person.login,
        )

    person_review = review.person_reviews.get(person=person)
    mocked_task.assert_called_once_with(review.id, review.product_schema_loaded, [person_review.id])


@pytest.mark.parametrize(
    'current_status,action',
    [
        [const.REVIEW_STATUS.IN_PROGRESS, const.REVIEW_ACTIONS.STATUS_IN_DRAFT],
        [const.REVIEW_STATUS.IN_PROGRESS, const.REVIEW_ACTIONS.STATUS_ARCHIVE],
        [const.REVIEW_STATUS.ARCHIVE, const.REVIEW_ACTIONS.STATUS_UNARCHIVE],
    ],
)
def test_do_not_freeze_gradient_data_when_not_publish(current_status, action, review_builder, robot):
    review = review_builder(status=current_status)

    with mock.patch('review.core.tasks.freeze_gradient_data_task.delay') as mocked_task:
        review_actions.follow_workflow(review, action)

    assert mocked_task.call_count == 0
