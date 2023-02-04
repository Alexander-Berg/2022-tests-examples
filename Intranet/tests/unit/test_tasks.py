import pytest
import responses

from stackbot.tasks import synchronize_bot_users
from stackbot.db import BotUser
from stackbot.enums import BotUserState
from stackbot.logic.utils import create_bot_user

from unit.utils import (
    get_staff_api_auth_url,
    staff_api_result,
)


@responses.activate
@pytest.mark.parametrize('is_dismissed_1,is_dismissed_2', [(False, False), (True, False), (True, True)])
def test_synchronize_bot_users(scope_session, staff_info, is_dismissed_1, is_dismissed_2):
    staff_1 = staff_info(is_dismissed=is_dismissed_1)
    staff_2 = staff_info(is_dismissed=is_dismissed_2)
    staff_3 = staff_info(is_dismissed=False)

    responses.add(
        responses.GET,
        get_staff_api_auth_url([staff_1.telegram_account, staff_2.telegram_account, staff_3.telegram_account]),
        json={'result': [staff_api_result(staff_1), staff_api_result(staff_2)]}
    )

    create_bot_user(
        db=scope_session,
        staff_info=staff_1,
        username=staff_1.telegram_account,
        telegram_id=1
    )
    create_bot_user(
        db=scope_session,
        staff_info=staff_2,
        username=staff_2.telegram_account,
        telegram_id=2
    )
    create_bot_user(
        db=scope_session,
        staff_info=staff_3,
        username=staff_3.telegram_account,
        telegram_id=3
    )

    synchronize_bot_users.__call__()

    bot_users = scope_session.query(BotUser).filter(BotUser.state == BotUserState.active).all()
    if not is_dismissed_1:
        assert len(bot_users) == 2
    else:
        if not is_dismissed_2:
            assert len(bot_users) == 1
        else:
            assert len(bot_users) == 0


@responses.activate
def test_synchronize_one_telegram_multiple_staff(scope_session, staff_info):
    staff_1 = staff_info()
    staff_2 = staff_info(telegram_account=staff_1.telegram_account)

    responses.add(
        responses.GET,
        get_staff_api_auth_url([staff_1.telegram_account]),
        json={'result': [staff_api_result(staff_1), staff_api_result(staff_2)]}
    )

    create_bot_user(
        db=scope_session,
        staff_info=staff_1,
        username=staff_1.telegram_account,
        telegram_id=1
    )

    synchronize_bot_users.__call__()

    bot_users = scope_session.query(BotUser).filter(BotUser.state == BotUserState.active).all()
    assert len(bot_users) == 1
    assert bot_users[0].staff_id == staff_1.id
