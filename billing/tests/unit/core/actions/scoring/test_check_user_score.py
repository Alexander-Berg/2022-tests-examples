import logging
from decimal import Decimal

import pytest

from hamcrest import assert_that, has_entries, has_item, has_properties

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.scoring.check_user_score import CheckUserScoreAction
from billing.yandex_pay_plus.yandex_pay_plus.core.entities.user import User
from billing.yandex_pay_plus.yandex_pay_plus.core.exceptions import BadUserScoreError
from billing.yandex_pay_plus.yandex_pay_plus.interactions.saturn import SaturnClient, SaturnNotFoundError
from billing.yandex_pay_plus.yandex_pay_plus.interactions.saturn.entities import UserScore
from billing.yandex_pay_plus.yandex_pay_plus.utils.stats import user_score_not_found


@pytest.fixture
def user(randn):
    return User(uid=randn())


@pytest.fixture
def user_score(rands, user):
    return UserScore(
        request_id=rands(),
        uid=user.uid,
        score=Decimal('0.851'),
        formula_id=rands(),
    )


@pytest.fixture(autouse=True)
def mock_saturn_client(mocker, user_score):
    mock = mocker.AsyncMock(return_value=user_score)
    return mocker.patch.object(SaturnClient, 'get_user_score', mock)


@pytest.mark.asyncio
async def test_user_score_passes_check(mock_saturn_client, user):
    await CheckUserScoreAction(user=user).run()

    mock_saturn_client.assert_called_once_with(uid=user.uid)


@pytest.mark.asyncio
async def test_call_logged(user, dummy_logs, user_score):
    await CheckUserScoreAction(user=user).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='USER_SCORE_CALCULATED',
                levelno=logging.INFO,
                _context=has_entries(uid=user.uid, user_score=user_score, score_value=user_score.score),
            )
        )
    )


@pytest.mark.asyncio
async def test_user_in_scoring_blacklist(user, mocker, dummy_logs):
    mocker.patch.object(CheckUserScoreAction, 'blacklist', {user.uid})

    with pytest.raises(BadUserScoreError):
        await CheckUserScoreAction(user=user).run()

    [log] = dummy_logs()
    assert_that(
        log,
        has_properties(
            message='UID_IN_SCORING_BLACKLIST',
            levelno=logging.INFO,
            _context=has_entries(uid=user.uid),
        )
    )


@pytest.mark.asyncio
async def test_user_in_scoring_whitelist(user, mocker, mock_saturn_client, dummy_logs):
    mocker.patch.object(CheckUserScoreAction, 'whitelist', {user.uid})

    await CheckUserScoreAction(user=user).run()

    mock_saturn_client.assert_not_called()
    [log] = dummy_logs()
    assert_that(
        log,
        has_properties(
            message='UID_IN_SCORING_WHITELIST',
            levelno=logging.INFO,
            _context=has_entries(uid=user.uid),
        )
    )


@pytest.mark.asyncio
async def test_user_score_below_threshold(user_score, user, dummy_logs):
    user_score.score = Decimal('0.3')

    with pytest.raises(BadUserScoreError):
        await CheckUserScoreAction(user=user).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='USER_SCORE_BELOW_THRESHOLD',
                levelno=logging.INFO,
                _context=has_entries(
                    uid=user.uid,
                    min_score_threshold=CheckUserScoreAction.min_score_threshold,
                    user_score=user_score,
                    score_value=user_score.score,
                ),
            )
        )
    )


@pytest.mark.asyncio
async def test_user_not_found(mock_saturn_client, user, mocker, dummy_logs):
    mock_saturn_client.side_effect = SaturnNotFoundError(
        status_code=404,
        method='post',
        service=SaturnClient.SERVICE,
    )
    mock_counter = mocker.patch.object(user_score_not_found, 'inc')

    with pytest.raises(BadUserScoreError):
        await CheckUserScoreAction(user=user).run()

    logs = dummy_logs()
    assert_that(
        logs,
        has_item(
            has_properties(
                message='USER_SCORE_NOT_FOUND',
                levelno=logging.WARNING,
                _context=has_entries(uid=user.uid),
            )
        )
    )

    mock_counter.assert_called_once()
