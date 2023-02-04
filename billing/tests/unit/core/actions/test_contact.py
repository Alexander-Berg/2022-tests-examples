import pytest
from pay.lib.interactions.passport_addresses.entities import Contact

from billing.yandex_pay.yandex_pay.core.actions.contact import GetUserContactsAction
from billing.yandex_pay.yandex_pay.core.entities.user import User
from billing.yandex_pay.yandex_pay.interactions import PassportContactsClient


@pytest.fixture
def user():
    return User(42, 'tvm_ticket')


@pytest.mark.asyncio
@pytest.mark.parametrize('invalid_email', ['', '@yandex.ru', 'a@b@c.d', 'foo'])
async def test_contact_with_invalid_email_filtered(invalid_email, user, mocker):
    contacts = [
        Contact(email='valid@email.tld'),
        Contact(email=invalid_email),
    ]
    mock = mocker.patch.object(
        PassportContactsClient, 'list', mocker.AsyncMock(return_value=contacts)
    )

    returned = await GetUserContactsAction(user=user).run()

    assert returned == contacts[:1]
    mock.assert_awaited_once_with(uid=user.uid, user_ticket=user.tvm_ticket)
