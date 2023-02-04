import pytest
from cryptography.fernet import InvalidToken

from billing.yandex_pay.yandex_pay.core.actions.wallet.decrypt_app_id import DecryptAppIdAction
from billing.yandex_pay.yandex_pay.core.actions.wallet.encrypt_app_id import EncryptAppIdAction
from billing.yandex_pay.yandex_pay.core.exceptions import CoreInvalidVersionError


@pytest.mark.asyncio
async def test_decrypt_app_id():
    encrypted: str = await EncryptAppIdAction('hello').run()
    decrypted: str = await DecryptAppIdAction(encrypted).run()

    assert decrypted == 'hello'


@pytest.mark.asyncio
async def test_decrypt_app_id_wrong_version():
    with pytest.raises(CoreInvalidVersionError):
        await DecryptAppIdAction('2:xxx').run()


@pytest.mark.asyncio
async def test_decrypt_app_id_wrong_data_at_tail():
    encrypted: str = await EncryptAppIdAction('hello').run()

    encrypted += 'xxx'
    decrypted = await DecryptAppIdAction(encrypted).run()

    assert decrypted == 'hello'


@pytest.mark.asyncio
async def test_decrypt_app_id_wrong_data():
    encrypted = '1:xxx'

    with pytest.raises(InvalidToken):
        await DecryptAppIdAction(encrypted).run()
