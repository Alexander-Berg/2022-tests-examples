import pytest

from datetime import datetime, timedelta
from tasha.constants import ACTION
from tasha.models import TelegramAccount
from tasha.core.dbproxy import GroupNotRegistered, UserMultipleValue

from tasha.tests import factories

pytestmark = [pytest.mark.asyncio, pytest.mark.django_db(transaction=True)]


@pytest.mark.parametrize('username', [None, 'username'])
@pytest.mark.parametrize('is_bot', [True, False])
async def test_get_user_and_username_not_found(db_proxy, username, is_bot):
    async with db_proxy.acquire_connection() as connection:
        result = await db_proxy._get_user_and_username(connection, 12345, username, is_bot)

    assert result['is_active'] is None
    assert result['is_bot'] == is_bot
    assert result['telegram_id'] == 12345
    assert result['id'] is None
    assert result['username'] == username


@pytest.mark.parametrize('username', [None, 'username', 'different_username'])
@pytest.mark.parametrize('is_bot', [True, False])
@pytest.mark.parametrize('is_active', [True, False])
async def test_get_user_and_username_found_by_telegram_id(db_proxy, username, is_bot, is_active):
    user = factories.UserFactory(is_active=is_active)
    tg_username = factories.TelegramAccountFactory(telegram_id=12345, username='username', user=user)
    async with db_proxy.acquire_connection() as connection:
        result = await db_proxy._get_user_and_username(connection, 12345, username, is_bot)

    tg_username.refresh_from_db()

    assert result['is_active'] == is_active
    assert result['is_bot'] == is_bot == tg_username.is_bot
    assert result['telegram_id'] == 12345 == tg_username.telegram_id
    assert result['id'] == user.id

    if username is None:
        assert result['username'] == 'username'
    else:
        assert result['username'] == tg_username.username == username


@pytest.mark.parametrize('telegram_id', [None, 12345, 54321])
@pytest.mark.parametrize('is_bot', [True, False])
@pytest.mark.parametrize('is_active', [True, False])
async def test_get_user_and_username_found_by_username(db_proxy, telegram_id, is_bot, is_active):
    user = factories.UserFactory(is_active=is_active)
    tg_username = factories.TelegramAccountFactory(telegram_id=12345, username='username', user=user)
    async with db_proxy.acquire_connection() as connection:
        result = await db_proxy._get_user_and_username(connection, telegram_id, 'username', is_bot)

    tg_username.refresh_from_db()

    assert result['is_active'] == is_active
    assert result['is_bot'] == is_bot == tg_username.is_bot
    assert result['telegram_id'] == tg_username.telegram_id == telegram_id
    assert result['username'] == 'username' == tg_username.username
    assert result['id'] == user.id


async def test_get_group_not_found(db_proxy):
    with pytest.raises(GroupNotRegistered):
        await db_proxy.get_group(12345)


async def test_get_group_found(db_proxy):
    group = factories.TgGroupInfoFactory(telegram_id=12345)
    result = await db_proxy.get_group(12345)

    assert dict(result) == {'id': group.id}


async def test_get_all_active_groups_with_true_bot(db_proxy, true_bot):
    group_with_bot = factories.TgGroupInfoFactory()
    another_group_with_bot = factories.TgGroupInfoFactory()
    deactivated_group_with_bot = factories.TgGroupInfoFactory(deactivated=True)
    group_without_bot = factories.TgGroupInfoFactory()  # noqa F841
    group_with_inactive_bot_membership = factories.TgGroupInfoFactory()

    factories.TgMembershipFactory(group=group_with_bot, account=true_bot, is_active=True)
    factories.TgMembershipFactory(group=another_group_with_bot, account=true_bot, is_active=True)
    factories.TgMembershipFactory(group=deactivated_group_with_bot, account=true_bot, is_active=True)
    factories.TgMembershipFactory(group=group_with_inactive_bot_membership, account=true_bot, is_active=False)

    result = await db_proxy.get_all_active_groups_with_true_bot(true_bot.username)
    assert len(result) == 2
    assert {row['telegram_id'] for row in result} == {group_with_bot.telegram_id, another_group_with_bot.telegram_id}


@pytest.mark.parametrize('god_bot_number', (None, 11))
@pytest.mark.parametrize('kick_action', ACTION.user_kicked - {ACTION.USER_BANNED_BY_USER})
async def test_get_users_to_unblock(db_proxy, kick_action, god_bot_number):
    yesterday = datetime.now() - timedelta(days=1)
    bot_account = factories.TelegramAccountFactory(username='botbot')

    groups = [
        factories.TgGroupInfoFactory(deactivated=False),
        factories.TgGroupInfoFactory(deactivated=False, god_bot_number=god_bot_number),
    ]
    accounts = [
        factories.TelegramAccountFactory(username='abc1'),
        factories.TelegramAccountFactory(username='def2'),
        factories.TelegramAccountFactory(username='ghi3'),
    ]
    for group in groups:
        factories.TgMembershipFactory(account=bot_account, group=group)

    memberships = [
        factories.TgMembershipFactory(
            account=accounts[acc_index],
            group=groups[gr_index],
        )
        for acc_index, gr_index in [(0, 0), (0, 1), (1, 0), (1, 1), (2, 1)]
    ]

    for membership in memberships:
        membership.actions.create(action=ACTION.USER_JOINED_TO_CHAT, added=yesterday)
        membership.actions.create(action=kick_action, added=datetime.now())

    result = await db_proxy.get_users_to_unblock(bot_account_id=bot_account.id, god_bot_number=god_bot_number)
    to_unblock_memberships = {res['membership_id'] for res in result}
    for membership in memberships:
        if membership.group.god_bot_number == god_bot_number:
            assert membership.id in to_unblock_memberships
        else:
            assert membership.id not in to_unblock_memberships


async def test_get_participants_count(db_proxy):
    group = factories.TgGroupInfoFactory()
    for _ in range(4):
        factories.TgMembershipFactory(group=group, is_active=True)

    for _ in range(5):
        factories.TgMembershipFactory(group=group, is_active=False)

    result = await db_proxy.get_participants_count(group.telegram_id)
    assert result == 4


async def test_get_participants_to_kick(db_proxy):
    pass


async def test_is_valid_user(db_proxy):
    pass


async def test_register_enter(db_proxy):
    pass


async def test_register_email(db_proxy):
    pass


async def test_register_enter_if_no_membership_active(db_proxy):
    pass


async def test_add_action_to_membership(db_proxy):
    pass


async def test_register_exit(db_proxy):
    pass


async def test_get_subbot_ids(db_proxy):
    tasha = factories.TelegramAccountFactory(is_tasha=True)
    another_tasha = factories.TelegramAccountFactory(is_tasha=True)
    not_tasha = factories.TelegramAccountFactory(is_tasha=False)  # noqa F841

    assert {r['telegram_id'] for r in await db_proxy.get_subbot_ids()} == {tasha.telegram_id, another_tasha.telegram_id}


async def test_get_chat_admins_emails(db_proxy):
    admin_with_email = factories.UserFactory(email='admin@yandex-team.ru', username='staff-login1')
    admin_with_no_email = factories.UserFactory(email=None, username='staff-login2')
    dismissed_admin_with_email = factories.UserFactory(
        email='dismissed@yandex-team.ru', is_active=False, username='staff-login3',
    )
    other_group_admin = factories.UserFactory(email='other@yandex-team.ru', username='staff-login4')
    not_an_admin = factories.UserFactory(email='not_an_admin@yandex-team.ru', username='staff-login5')
    robot_uhura = factories.UserFactory(email='robot-uhura@yandex-team.ru', username='robot-uhura')

    admin_with_email_username = factories.TelegramAccountFactory(user=admin_with_email)
    admin_with_no_email_username = factories.TelegramAccountFactory(user=admin_with_no_email)
    dismissed_admin_with_email_username = factories.TelegramAccountFactory(user=dismissed_admin_with_email)
    external_admin_username = factories.TelegramAccountFactory(user=None)
    other_group_admin_username = factories.TelegramAccountFactory(user=other_group_admin)
    not_an_admin_username = factories.TelegramAccountFactory(user=not_an_admin)
    some_tasha_bot_admin = factories.TelegramAccountFactory(user=robot_uhura)

    group = factories.TgGroupInfoFactory()
    other_group = factories.TgGroupInfoFactory()

    factories.TgMembershipFactory(account=admin_with_email_username, group=group, is_admin=True)
    factories.TgMembershipFactory(account=admin_with_no_email_username, group=group, is_admin=True)
    factories.TgMembershipFactory(account=dismissed_admin_with_email_username, group=group, is_admin=True)
    factories.TgMembershipFactory(account=external_admin_username, group=group, is_admin=True)
    factories.TgMembershipFactory(account=other_group_admin_username, group=other_group, is_admin=True)
    factories.TgMembershipFactory(account=not_an_admin_username, group=group, is_admin=False)
    factories.TgMembershipFactory(account=some_tasha_bot_admin, group=group, is_admin=True)

    result = await db_proxy.get_admin_emails(group.telegram_id)
    assert result == ['admin@yandex-team.ru']


async def test_valid_update_username(db_proxy):
    user = factories.UserFactory()
    tgusername_with_id = factories.TelegramAccountFactory(telegram_id=12345, user=None)
    tgusername_with_login = factories.TelegramAccountFactory(username='some_username', user=user)  # noqa F841

    await db_proxy.update_username(12345, 'some_username')
    assert TelegramAccount.objects.count() == 1
    tgusername_with_id.refresh_from_db()
    assert tgusername_with_id.user == user


async def test_update_username_with_different_users(db_proxy):
    factories.TelegramAccountFactory(telegram_id=12345)
    factories.TelegramAccountFactory(username='some_username')

    with pytest.raises(UserMultipleValue):
        await db_proxy.update_username(12345, 'some_username')


async def test_update_username_with_memberships(db_proxy):
    tgusername_with_id = factories.TelegramAccountFactory(telegram_id=12345)  # noqa F841
    tgusername_with_login = factories.TelegramAccountFactory(username='some_username')
    factories.TgMembershipFactory(account=tgusername_with_login)

    with pytest.raises(AssertionError):
        await db_proxy.update_username(12345, 'some_username')
