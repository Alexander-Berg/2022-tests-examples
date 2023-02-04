import pytest

from review.core import const
from review.core.logic import cia
from tests import helpers


def test_get_for_person_review(
    client,
    mocked_feedback_request,
    person_review,
    person_review_role_builder,
):
    reviewer = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    ).person
    response = helpers.get_json(
        client,
        path='/external/feedback/person-reviews/{}/'.format(person_review.id),
        login=reviewer.login,
    )
    assert response['feedback'] == mocked_feedback_request


def test_no_revoked_requests(
    client,
    mocked_feedback_request_revoked,
    person_review,
    person_review_role_builder,
):
    reviewer = person_review_role_builder(
        person_review=person_review,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    ).person
    response = helpers.get_json(
        client,
        path='/external/feedback/person-reviews/{}/'.format(person_review.id),
        login=reviewer.login,
    )
    assert response['feedback'] == {'feedbacks': []}


def test_get_for_calibration_person_review(
    client,
    calibration_builder,
    calibration_person_review_builder,
    calibration_role_builder,
    mocked_feedback_request_one_person,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    cpr = calibration_person_review_builder(calibration=calibration)
    admin = calibration_role_builder(
        type=const.ROLE.CALIBRATION.ADMIN,
        calibration=calibration,
    ).person
    response = helpers.get_json(
        client,
        path='/external/feedback/calibration-person-reviews/{}/'.format(cpr.id),
        login=admin.login,
    )
    assert response['feedback'] == mocked_feedback_request_one_person


def test_get_for_calibration_person_review_mute_author(
    client,
    calibration_builder,
    calibration_person_review_builder,
    calibration_role_builder,
    mocked_feedback_request,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    cpr = calibration_person_review_builder(calibration=calibration)
    admin = calibration_role_builder(
        type=const.ROLE.CALIBRATION.ADMIN,
        calibration=calibration,
    ).person
    response = helpers.get_json(
        client,
        path='/external/feedback/calibration-person-reviews/{}/'.format(cpr.id),
        login=admin.login,
    )
    received = response['feedback']['feedbacks'][0]
    expected_empty = (
        'reporter',
        'reporter_staff',
        'reporter_type',
        'persons',
    )
    assert all(received[key] is None for key in expected_empty)
    expected_message = mocked_feedback_request['feedbacks'][0]['positive_message']
    assert received['positive_message'] == expected_message


@pytest.mark.parametrize(
    'status', [
        const.CALIBRATION_STATUS.DRAFT,
        const.CALIBRATION_STATUS.ARCHIVE
    ]
)
def test_get_for_calibration_person_review_no_access(
    client,
    calibration_builder,
    calibration_person_review_builder,
    calibration_role_builder,
    mocked_feedback_request,
    status,
):
    calibration = calibration_builder(status=status)
    cpr = calibration_person_review_builder(calibration=calibration)
    admin = calibration_role_builder(
        type=const.ROLE.CALIBRATION.ADMIN,
        calibration=calibration,
    ).person
    response = helpers.get_json(
        client,
        path='/external/feedback/calibration-person-reviews/{}/'.format(cpr.id),
        login=admin.login,
        expect_status=403,
    )
    assert response['errors']['*']['code'] == 'PERMISSION_DENIED'


@pytest.fixture
def mocked_feedback_request(monkeypatch):
    response = dict(feedbacks=[
        dict(
            reporter=1,
            reporter_staff=dict(login='asd'),
            reporter_type='some_type',
            persons=[2, 3],
            positive_message='yeah',
            is_revoked=False,
        ),
    ])

    def mock_request(*a, **kw):
        return response
    monkeypatch.setattr(cia.HttpConnector, 'get', mock_request)
    return response


@pytest.fixture
def mocked_feedback_request_one_person(monkeypatch):
    response = dict(feedbacks=[
        dict(
            reporter=1,
            reporter_staff=dict(login='asd'),
            reporter_type='some_type',
            persons=[2],
            positive_message='yeah',
            is_revoked=False,
        ),
    ])

    def mock_request(*a, **kw):
        return response
    monkeypatch.setattr(cia.HttpConnector, 'get', mock_request)
    return response


@pytest.fixture
def mocked_feedback_request_revoked(monkeypatch):
    response = dict(feedbacks=[
        dict(
            reporter=1,
            reporter_staff=dict(login='asd'),
            reporter_type='some_type',
            person=[2, 3],
            positive_message='yeah',
            is_revoked=True,
        ),
    ])

    def mock_request(*a, **kw):
        return response
    monkeypatch.setattr(cia.HttpConnector, 'get', mock_request)
    return response
