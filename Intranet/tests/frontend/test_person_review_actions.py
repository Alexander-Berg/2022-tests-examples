# coding: utf-8

from decimal import Decimal
import mock
import pytest
from celery.result import AsyncResult
from django.test import override_settings

from review.core import const
from review.core import models
from review.gradient import models as gradient_models

from tests import helpers


STATUSES = const.PERSON_REVIEW_STATUS


@pytest.fixture(autouse=True)
def mocked_xiva():
    with mock.patch('review.xiva.update_calibration.post_new_action_to_xiva.delay') as m:
        yield m


def test_edit_mark_response(client, person_review_role_reviewer):
    subject = person_review_role_reviewer.person
    person_review = person_review_role_reviewer.person_review
    review = person_review.review
    helpers.update_model(person_review, mark='bad')
    helpers.update_model(review, mark_mode=const.REVIEW_MODE.MODE_MANUAL)

    result = helpers.post_json(
        client=client,
        login=subject.login,
        path='/frontend/person-reviews/{}/actions/'.format(person_review.id),
        request={
            'mark': 'good',
            'comment': 'asd',
        },
    )

    _check_success_result_detail(person_review, result, expected={
        'mark': 'good',
    })
    _check_last_change(
        person_review,
        expected_fields={
            'subject': subject,
        },
        expected_diff={
            'mark': {'old': 'bad', 'new': 'good'},
        }
    )


def test_edit_approve_response(client, person_review_role_builder):
    first_reviewer = person_review_role_builder(
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )
    person_review = first_reviewer.person_review
    helpers.update_model(
        person_review,
        mark='extraordinary',
        status=const.PERSON_REVIEW_STATUS.EVALUATION,
        approve_level=0,
    )
    person_review_role_builder(
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
        person_review=first_reviewer.person_review,
        position=1,
    )
    result = helpers.post_json(
        client=client,
        login=first_reviewer.person.login,
        path='/frontend/person-reviews/{}/actions/'.format(person_review.id),
        request={
            const.PERSON_REVIEW_ACTIONS.APPROVE: True,
        },
    )

    _check_success_result_detail(person_review, result, expected={
        'status': STATUSES.APPROVAL,
    })
    _check_last_change(
        person_review,
        expected_fields={
            'subject': first_reviewer.person,
        },
        expected_diff={
            'status': {'new': STATUSES.APPROVAL},
        }
    )


def test_edit_approve_by_superreviewer_in_chain(
    client,
    person_review_role_builder,
    review_role_builder
):
    super_reviewer_role = person_review_role_builder(
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
    )
    person_review = super_reviewer_role.person_review
    person_review_role_builder(
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
        person_review=person_review,
        position=1,
    )
    person_review.mark = 'extraordinary'
    person_review.status = const.PERSON_REVIEW_STATUS.APPROVAL
    person_review.approve_level = 0
    person_review.save()
    super_reviewer = super_reviewer_role.person
    review_role_builder(
        review=person_review.review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
        person=super_reviewer,
    )

    result = helpers.post_json(
        client=client,
        login=super_reviewer.login,
        path='/frontend/person-reviews/{}/actions/'.format(person_review.id),
        request={
            const.PERSON_REVIEW_ACTIONS.APPROVE: True,
        },
    )

    _check_success_result_detail(
        person_review,
        result,
        expected=dict(approve_level=1),
    )
    _check_last_change(
        person_review,
        expected_fields=dict(subject=super_reviewer),
        expected_diff=dict(approve_level=dict(new=1)),
    )


def test_edit_mark_bulk_response(client, person_review_role_reviewer):
    subject = person_review_role_reviewer.person
    person_review = person_review_role_reviewer.person_review
    review = person_review.review
    helpers.update_model(person_review, mark='bad')
    helpers.update_model(review, mark_mode=const.REVIEW_MODE.MODE_MANUAL)

    result = helpers.post_json(
        client=client,
        login=subject.login,
        path='/frontend/person-reviews/bulk/actions/'.format(person_review.id),
        request={
            'ids': [person_review.id],
            'mark': 'good',
            'comment': 'asd',
        },
    )

    _check_success_result_detail(person_review, result, expected={
        'mark': 'good',
    })
    _check_last_change(
        person_review,
        expected_fields={
            'subject': subject,
        },
        expected_diff={
            'mark': {'old': 'bad', 'new': 'good'},
        }
    )


def test_edit_tag_average_mark_bulk(client, person_review_role_top_reviewer):
    subject = person_review_role_top_reviewer.person
    person_review = person_review_role_top_reviewer.person_review
    old_tag = 'A nu ka'
    new_tag = 'davay ka'
    helpers.update_model(person_review, tag_average_mark=old_tag)

    result = helpers.post_json(
        client=client,
        login=subject.login,
        path='/frontend/person-reviews/bulk/actions/',
        request={
            'ids': [person_review.id],
            'tag_average_mark': new_tag,
            'comment': 'asd',
        },
    )

    _check_success_result_detail(person_review, result, expected={
        'tag_average_mark': new_tag,
    })
    _check_last_change(
        person_review,
        expected_fields={
            'subject': subject,
        },
        expected_diff={
            'tag_average_mark': {'old': old_tag, 'new': new_tag},
        }
    )


@pytest.mark.parametrize(
    'action_url',
    [
        '/frontend/person-reviews/bulk/actions/',
        '/frontend/person-reviews/{}/actions/',
    ],
)
def test_edit_umbrella(action_url, client, umbrella_builder, person_review_builder, review_role_accompanying_hr):
    old_umbrella = umbrella_builder()
    new_umbrella = umbrella_builder(name='new')

    person = review_role_accompanying_hr.person
    person_review = person_review_builder(umbrella=old_umbrella, review=review_role_accompanying_hr.review)

    result = helpers.post_json(
        client=client,
        login=person.login,
        path=action_url.format(person_review.id),
        request={
            'ids': [person_review.id],
            'umbrella': new_umbrella.id,
        },
    )

    _check_success_result_detail(person_review, result, expected={
        'umbrella': new_umbrella,
    })
    _check_last_change(
        person_review,
        expected_fields={
            'subject': person,
        },
        expected_diff={
            'umbrella': {'old': old_umbrella.id, 'new': new_umbrella.id},
        }
    )


@pytest.mark.parametrize(
    'action_url',
    [
        '/frontend/person-reviews/bulk/actions/',
        '/frontend/person-reviews/{}/actions/',
    ],
)
def test_edit_main_product(action_url, client, main_product_builder, person_review_builder, review_role_accompanying_hr):
    old_main_product = main_product_builder()
    new_main_product = main_product_builder(name='new')

    person = review_role_accompanying_hr.person
    person_review = person_review_builder(main_product=old_main_product, review=review_role_accompanying_hr.review)

    result = helpers.post_json(
        client=client,
        login=person.login,
        path=action_url.format(person_review.id),
        request={
            'ids': [person_review.id],
            'main_product': new_main_product.id,
        },
    )

    _check_success_result_detail(person_review, result, expected={
        'main_product': new_main_product,
    })
    _check_last_change(
        person_review,
        expected_fields={
            'subject': person,
        },
        expected_diff={
            'main_product': {'old': old_main_product.id, 'new': new_main_product.id},
        }
    )


@pytest.mark.parametrize(
    'action_url',
    [
        '/frontend/person-reviews/bulk/actions/',
        '/frontend/person-reviews/{}/actions/',
    ],
)
def test_edit_umbrella_delete(action_url, client, umbrella_builder, person_review_builder, review_role_accompanying_hr):
    person = review_role_accompanying_hr.person
    old_umbrella = umbrella_builder()
    person_review = person_review_builder(umbrella=old_umbrella, review=review_role_accompanying_hr.review)

    helpers.post_json(
        client=client,
        login=person.login,
        path=action_url.format(person_review.id),
        request={
            'ids': [person_review.id],
            'umbrella': -1,
        },
    )

    _check_last_change(
        person_review,
        expected_fields={
            'subject': person,
        },
        expected_diff={
            'umbrella': {'old': old_umbrella.id, 'new': None},
        }
    )


@pytest.mark.parametrize(
    'action_url',
    [
        '/frontend/person-reviews/bulk/actions/',
        '/frontend/person-reviews/{}/actions/',
    ],
)
def test_edit_main_product_delete(action_url, client, main_product_builder, person_review_builder, review_role_accompanying_hr):
    person = review_role_accompanying_hr.person
    old_main_product = main_product_builder()
    person_review = person_review_builder(main_product=old_main_product, review=review_role_accompanying_hr.review)

    helpers.post_json(
        client=client,
        login=person.login,
        path=action_url.format(person_review.id),
        request={
            'ids': [person_review.id],
            'main_product': -1,
        },
    )

    _check_last_change(
        person_review,
        expected_fields={
            'subject': person,
        },
        expected_diff={
            'main_product': {'old': old_main_product.id, 'new': None},
        }
    )


@pytest.mark.parametrize(
    'action_url',
    [
        '/frontend/person-reviews/bulk/actions/',
        '/frontend/person-reviews/{}/actions/',
    ],
)
def test_add_umbrella(action_url, client, umbrella_builder, person_review_builder, review_role_accompanying_hr):
    person = review_role_accompanying_hr.person
    new_umbrella = umbrella_builder()
    person_review = person_review_builder(umbrella=None, review=review_role_accompanying_hr.review)

    helpers.post_json(
        client=client,
        login=person.login,
        path=action_url.format(person_review.id),
        request={
            'ids': [person_review.id],
            'umbrella': new_umbrella.id,
        },
    )

    person_review.refresh_from_db()
    assert person_review.umbrella == new_umbrella
    _check_last_change(
        person_review,
        expected_fields={
            'subject': person,
        },
        expected_diff={
            'umbrella': {'old': None, 'new': new_umbrella.id},
        }
    )


@pytest.mark.parametrize(
    'action_url',
    [
        '/frontend/person-reviews/bulk/actions/',
        '/frontend/person-reviews/{}/actions/',
    ],
)
def test_add_main_product(action_url, client, main_product_builder, person_review_builder, review_role_accompanying_hr):
    person = review_role_accompanying_hr.person
    new_main_product = main_product_builder()
    person_review = person_review_builder(main_product=None, review=review_role_accompanying_hr.review)

    helpers.post_json(
        client=client,
        login=person.login,
        path=action_url.format(person_review.id),
        request={
            'ids': [person_review.id],
            'main_product': new_main_product.id,
        },
    )

    person_review.refresh_from_db()
    assert person_review.main_product == new_main_product
    _check_last_change(
        person_review,
        expected_fields={
            'subject': person,
        },
        expected_diff={
            'main_product': {'old': None, 'new': new_main_product.id},
        }
    )


@pytest.mark.parametrize(
    'flag_field', const.FIELDS.FLAG_FIELDS,
)
def test_edit_flag_true(client, person_review_role_reviewer, flag_field):
    subject = person_review_role_reviewer.person
    person_review = person_review_role_reviewer.person_review

    result = helpers.post_json(
        client=client,
        login=subject.login,
        path='/frontend/person-reviews/{}/actions/'.format(person_review.id),
        request={
            flag_field: True,
        },
    )

    _check_success_result_detail(person_review, result, expected={
        flag_field: True,
    })
    _check_last_change(
        person_review,
        expected_fields={
            'subject': subject,
        },
        expected_diff={
            flag_field: {'old': False, 'new': True},
        }
    )


@pytest.mark.parametrize(
    'flag_field', const.FIELDS.FLAG_FIELDS,
)
def test_edit_flag_false(client, person_review_role_reviewer, flag_field):
    subject = person_review_role_reviewer.person
    person_review = person_review_role_reviewer.person_review
    helpers.update_model(person_review, **{flag_field: True})

    result = helpers.post_json(
        client=client,
        login=subject.login,
        path='/frontend/person-reviews/{}/actions/'.format(person_review.id),
        request={
            'flagged': False,
        },
    )

    _check_success_result_detail(person_review, result, expected={
        flag_field: False,
    })
    _check_last_change(
        person_review,
        expected_fields={
            'subject': subject,
        },
        expected_diff={
            flag_field: {'old': True, 'new': False},
        }
    )


def test_edit_taken_in_average_bulk(
        client,
        review_role_accompanying_hr,
        person_review_builder,
        person_builder,
):
    person = person_builder()
    review = review_role_accompanying_hr.review
    person_review = person_review_builder(
        review=review,
        person=person,
        taken_in_average=False,
    )

    result = helpers.post_json(
        client=client,
        login=review_role_accompanying_hr.person.login,
        path='/frontend/person-reviews/bulk/actions/',
        request={
            'ids': [person_review.id],
            'taken_in_average': True,
        },
    )

    _check_success_result_detail(person_review, result, expected={
        'taken_in_average': True,
    })
    _check_last_change(
        person_review,
        expected_fields={
            'subject': review_role_accompanying_hr.person,
        },
        expected_diff={
            'taken_in_average': {'old': False, 'new': True},
        }
    )


def test_edit_bonus_to_zero(
    client,
    person_review_role_builder,
    person_review_builder,
    review_builder,
):
    person_review = person_review_builder(
        review=review_builder(
            bonus_mode=const.REVIEW_MODE.MODE_MANUAL,
            mark_mode=const.REVIEW_MODE.MODE_DISABLED,
        )
    )
    subject = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
    ).person

    result = helpers.post_json(
        client=client,
        login=subject.login,
        path='/frontend/person-reviews/{}/actions/'.format(person_review.id),
        request={'bonus': '0'},
    )

    _check_success_result_detail(
        person_review,
        result,
        expected={'status': const.PERSON_REVIEW_STATUS.EVALUATION},
    )
    _check_last_change(
        person_review,
        expected_fields={
            'subject': subject,
        },
        expected_diff={
            'status': {
                'old': const.PERSON_REVIEW_STATUS.WAIT_EVALUATION,
                'new': const.PERSON_REVIEW_STATUS.EVALUATION,
            },
        },
    )
    person_review.refresh_from_db()
    assert person_review.status == const.PERSON_REVIEW_STATUS.EVALUATION
    assert person_review.bonus == Decimal('0')


def test_edit_deferred_payment(
    client,
    person_review_role_builder,
    person_review_builder,
    review_builder,
):
    person_review = person_review_builder(
        review=review_builder(
            deferred_payment_mode=const.REVIEW_MODE.MODE_MANUAL,
        )
    )
    subject = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
    ).person
    value = '100500.00'

    result = helpers.post_json(
        client=client,
        login=subject.login,
        path='/frontend/person-reviews/{}/actions/'.format(person_review.id),
        request={'deferred_payment': value},
    )

    _check_success_result_detail(
        person_review,
        result,
        expected={'deferred_payment': Decimal(value)},
    )
    _check_last_change(
        person_review,
        expected_fields={
            'subject': subject,
        },
        expected_diff={
            'deferred_payment': {
                'old': 0,
                'new': int(float(value)),
            },
        },
    )
    person_review.refresh_from_db()
    assert person_review.deferred_payment == Decimal(value)


def _check_success_result_detail(person_review, result, expected):
    assert 'errors' not in result, result['errors']
    helpers.check_db_data(person_review, expected)


def _check_last_comment(person_review, text_wiki):
    comment = models.PersonReviewComment.objects.filter(
        person_review_id=person_review.id
    ).order_by('-created_at').first()
    helpers.check_db_data(comment, text_wiki)


def _check_last_change(person_review, expected_fields, expected_diff):
    change = models.PersonReviewChange.objects.filter(
        person_review_id=person_review.id
    ).order_by('-created_at').first()
    helpers.check_db_data(change, expected_fields)
    helpers.assert_is_substructure(expected_diff, change.diff)


@pytest.mark.parametrize(
    'request_data',
    [
        ({'reviewers': {'type': 'replace', 'person': 'old_reviewer', 'person_to': 'new_reviewer'}}),
        ({'reviewers': {'type': 'delete', 'person': 'old_reviewer'}}),
        ({'reviewers': {'type': 'delete', 'persons': ['old_reviewer']}}),
        ({'reviewers': {'type': 'add', 'person': 'old_reviewer', 'position': 'start'}}),
        ({'reviewers': {'type': 'add', 'person': 'old_reviewer', 'position': 'after', 'person_to': 'new_reviewer'}}),
        ({'comment': 'test'}),
        ({'umbrella': 100500}),
    ],
)
@override_settings(BULK_ACTIONS_DELAY_THRESHOLD=0)
def test_actions_bulk_view_delay_tasks(
    request_data,
    client,
    person,
    person_review_builder,
    person_builder,
    umbrella_builder,
):
    helpers.waffle_switch('bulk_actions_enable_delay')
    person_review_ids = [person_review_builder().id]
    person_builder(login='old_reviewer')
    person_builder(login='new_reviewer')
    umbrella_builder(id=100500)

    mocked_result = AsyncResult(id='some-task-id')
    with mock.patch('review.core.tasks.bulk_same_action_set_task.delay', return_value=mocked_result) as p:
        result = helpers.post_json(
            client=client,
            login=person.login,
            path='/frontend/person-reviews/bulk/actions/',
            request={
                'ids': person_review_ids,
                **request_data,
            },
        )

    assert result['status'] == 'pending'
    p.assert_called_once_with(
        subject_id=person.id,
        ids=person_review_ids,
        params=request_data,
    )
    assert result['task_id'] == mocked_result.id


def test_add_to_emptry_reviewers_chain(
    client,
    person_review,
    review_role_builder,
    test_person,
):
    review_role_builder(
        person=test_person,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
        review=person_review.review,
    )

    result = helpers.post_json(
        client=client,
        login=test_person.login,
        path='/frontend/person-reviews/bulk/actions/',
        request={
            'ids': [person_review.id],
            'reviewers': {
                'type': 'add',
                'position': 'end',
                'person': test_person.login,
            },
        },
    )
    received = result[str(person_review.id)]['person_review']['reviewers']
    assert received
    assert received[0]['login'] == test_person.login


def test_action_replace_all_reviewers(
    client,
    person_review_builder,
    review_role_builder,
    review_role_superreviewer,
    person_review_role_builder,
    person_builder,
):
    person_review = person_review_builder(review=review_role_superreviewer.review)
    # старые ревьюеры
    person_review_role_builder(type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER, person_review=person_review)
    person_review_role_builder(type=const.ROLE.PERSON_REVIEW.REVIEWER, person_review=person_review)
    person_review_role_builder(type=const.ROLE.PERSON_REVIEW.REVIEWER, person_review=person_review)

    new_reviewer = person_builder()

    result = helpers.post_json(
        client=client,
        login=review_role_superreviewer.person.login,
        path='/frontend/person-reviews/bulk/actions/',
        request={
            'ids': [person_review.id],
            'reviewers': {
                'type': 'replace_all',
                'person_to': new_reviewer.login,
            },
        },
    )
    received = result[str(person_review.id)]['person_review']['reviewers']
    assert received
    assert received[0]['login'] == new_reviewer.login

    reviewers = (
        models.PersonReviewRole.objects
        .filter(
            person_review=person_review,
            type__in=(const.ROLE.PERSON_REVIEW.TOP_REVIEWER, const.ROLE.PERSON_REVIEW.REVIEWER),
        )
    )
    assert len(reviewers) == 1
    assert reviewers[0].person == new_reviewer
    assert reviewers[0].type == const.ROLE.PERSON_REVIEW.TOP_REVIEWER
