import pytest
from celery import states

from review.core import const
from tests.helpers import get_json, waffle_switch


@pytest.fixture
def db_review_state(
    calibration_builder,
    calibration_person_review_builder,
    calibration_role_builder,
    person_builder,
    person_review_builder,
    person_review_role_builder,
    review_role_builder,
):
    switches = [
        'enable_pending_tasks_api_for_active_review_page',
        'enable_pending_tasks_api_for_review_by_id_page',
        'enable_pending_tasks_api_for_review_participants_page',
        'enable_pending_tasks_api_for_calibration_page',
    ]
    for sw in switches:
        waffle_switch(sw)
    person_review_seen_1 = person_review_builder()
    person_review_seen_2 = person_review_builder()
    person_review_not_seen = person_review_builder()
    person_review_archived = person_review_builder()

    review_seen_1 = person_review_seen_1.review
    review_seen_2 = person_review_seen_2.review
    review_not_seen = person_review_not_seen.review
    review_archived = person_review_archived.review
    review_archived.status = const.REVIEW_STATUS.ARCHIVE
    review_archived.save()

    superreviewer = person_builder()
    review_role_builder(
        review=review_seen_1,
        person=superreviewer,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    review_role_builder(
        review=review_seen_2,
        person=superreviewer,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    review_role_builder(
        review=review_archived,
        person=superreviewer,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )

    reviewer = person_review_role_builder(
        person_review=person_review_seen_1,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    ).person

    calibration_seen = calibration_builder(
        status=const.CALIBRATION_STATUS.IN_PROGRESS,
    )
    calibration_not_seen = calibration_builder(
        status=const.CALIBRATION_STATUS.IN_PROGRESS,
    )
    calibration_pr_seen = calibration_person_review_builder(
        person_review=person_review_seen_1,
        calibration=calibration_seen,
    )
    calibration_pr_not_seen = calibration_person_review_builder(
        person_review=person_review_seen_2,
        calibration=calibration_not_seen,
    )
    calibrator = calibration_role_builder(
        calibration=calibration_seen,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
    ).person

    return {
        'person_review_seen_1': person_review_seen_1,
        'person_review_seen_2': person_review_seen_2,
        'person_review_not_seen': person_review_not_seen,
        'person_review_archived': person_review_archived,
        'review_seen_1': review_seen_1,
        'review_seen_2': review_seen_2,
        'review_not_seen': review_not_seen,
        'review_archived': review_archived,
        'superreviewer': superreviewer,
        'reviewer': reviewer,
        'calibration_pr_seen': calibration_pr_seen,
        'calibration_pr_not_seen': calibration_pr_not_seen,
        'calibration_seen': calibration_seen,
        'calibration_not_seen': calibration_not_seen,
        'calibrator': calibrator,
    }


def create_tasks_different_status(task_builder, task_name, **kwargs):
    states_ = [states.SUCCESS, states.FAILURE, states.PENDING]
    res_key = ['finished', 'failed', 'pending']
    return {
        k: task_builder(
            status=s,
            task_name=task_name,
            task_kwargs=kwargs,
        )
        for k, s in zip(res_key, states_)
    }


def create_bulk_tasks(task_builder, ids, params=None):
    return create_tasks_different_status(
        task_builder=task_builder,
        task_name='review.core.tasks.bulk_same_action_set_task',
        subject_id=1,
        ids=ids,
        params=params or {},
    )


def create_denormalize_tasks(
    task_builder,
    person_review_ids=None,
    review_id=None,
    person_ids=None,
    calibration_id=None,
):
    return create_tasks_different_status(
        task_builder=task_builder,
        task_name='review.core.tasks.denormalize_person_review_roles_task',
        person_review_ids=person_review_ids,
        review_id=review_id,
        person_ids=person_ids,
        calibration_id=calibration_id,
    )


def test_reviewers_changing_pending_tasks(
    client,
    db_review_state,
    task_builder,
):
    expected = create_bulk_tasks(
        task_builder,
        ids=[db_review_state['person_review_seen_1'].id],
        params={'reviewers': {'position': 'add'}},
    )['pending']
    create_bulk_tasks(
        task_builder,
        ids=[db_review_state['person_review_seen_2'].id],
        params={'reviewers': {'position': 'add'}},
    )

    response = get_json(
        client,
        '/frontend/reviews/{}/participants/pending-tasks/'.format(
            db_review_state['review_seen_1'].id,
        ),
        login=db_review_state['superreviewer'].login,
    )
    recived_tasks = response['active_tasks']['reviewers_change']

    assert recived_tasks == [expected.task_id]


def test_no_reviewers_changing_pending_tasks(
    client,
    db_review_state,
    task_builder,
):
    create_bulk_tasks(
        task_builder,
        ids=[db_review_state['person_review_seen_1'].id],
        params={'bonus': 21},
    )['pending']

    response = get_json(
        client,
        '/frontend/reviews/{}/participants/pending-tasks/'.format(
            db_review_state['review_seen_1'].id,
        ),
        login=db_review_state['superreviewer'].login,
    )

    assert not response['active_tasks']['reviewers_change']


def test_active_reviews_status_change_pending_tasks(
    client,
    db_review_state,
    task_builder,
):
    expected_1 = create_bulk_tasks(
        task_builder,
        ids=[db_review_state['person_review_seen_1'].id],
        params={'approve': True},
    )['pending']
    expected_2 = create_bulk_tasks(
        task_builder,
        ids=[db_review_state['person_review_seen_2'].id],
        params={'approve': True},
    )['pending']

    response = get_json(
        client,
        '/frontend/reviews/pending-tasks/',
        login=db_review_state['superreviewer'].login,
    )

    received_tasks = response['active_tasks']
    assert not received_tasks['grant_permissions']

    expected = {expected_1.task_id, expected_2.task_id}
    assert set(received_tasks['status_change']) == expected


def test_review_grant_permission_pending_task(
    client,
    db_review_state,
    task_builder,
):
    pr = db_review_state['person_review_seen_1']
    expected_1 = create_denormalize_tasks(
        task_builder,
        person_review_ids=[pr.id],
    )['pending']
    expected_2 = create_denormalize_tasks(
        task_builder,
        review_id=pr.review_id,
        person_ids=[pr.person_id],
    )['pending']

    pr_not_seen = db_review_state['person_review_seen_2']
    create_denormalize_tasks(
        task_builder,
        person_review_ids=[pr_not_seen.id],
    )
    create_denormalize_tasks(
        task_builder,
        review_id=pr_not_seen.review_id,
        person_ids=[pr_not_seen.person_id],
    )

    response = get_json(
        client,
        '/frontend/reviews/pending-tasks/',
        login=db_review_state['reviewer'].login,
        request={'review': pr.review_id},
    )

    received_tasks = response['active_tasks']
    assert not received_tasks['status_change']

    expected = {expected_1.task_id, expected_2.task_id}
    assert set(received_tasks['grant_permissions']) == expected


def test_calibration_pending_tasks(
    client,
    db_review_state,
    task_builder,
):
    expected_1 = create_bulk_tasks(
        task_builder,
        ids=[db_review_state['calibration_pr_seen'].person_review.id],
        params={'approve': True},
    )['pending']
    calibration = db_review_state['calibration_seen']
    expected_2 = create_denormalize_tasks(
        task_builder,
        calibration_id=calibration.id,
    )['pending']

    create_bulk_tasks(
        task_builder,
        ids=[db_review_state['calibration_pr_not_seen'].person_review.id],
        params={'approve': True},
    )
    create_denormalize_tasks(
        task_builder,
        calibration_id=db_review_state['calibration_not_seen'].id,
    )

    response = get_json(
        client,
        '/frontend/calibrations/{}/pending-tasks/'.format(calibration.id),
        login=db_review_state['calibrator'].login,
    )

    received_tasks = response['active_tasks']
    assert received_tasks == {
        'grant_permissions': [expected_2.task_id],
        'status_change': [expected_1.task_id],
    }


@pytest.mark.parametrize(
    'url,model_key,expected_result', [
        (
            '/frontend/calibrations/{}/pending-tasks/',
            'calibration_seen',
            {'grant_permissions': [], 'status_change': []},
        ),
        (
            '/frontend/reviews/{}/participants/pending-tasks/',
            'review_seen_1',
            {'reviewers_change': []},
        ),
        (
            '/frontend/reviews/pending-tasks/',
            'review_seen_1',
            {'grant_permissions': [], 'status_change': []},
        ),
    ],
)
def test_get_empty_answer(
    client,
    db_review_state,
    url,
    model_key,
    expected_result,
):
    response = get_json(
        client,
        url.format(db_review_state[model_key].id),
        login=db_review_state['calibrator'].login,
    )
    assert response['active_tasks'] == expected_result
