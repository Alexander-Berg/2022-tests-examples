from uuid import UUID, uuid4

import pytest

from sendr_auth import User as AuthUser

from pay.bill_payments.bill_payments.storage import Storage
from pay.bill_payments.bill_payments.storage.entities.document import Document, DocumentCode
from pay.bill_payments.bill_payments.storage.entities.user import User


@pytest.fixture
async def user(storage: Storage, unique_rand, randn) -> User:
    return await storage.user.create(User(uid=unique_rand(randn), subscription_id=str(uuid4())))


@pytest.fixture
async def document(user: User, storage: Storage) -> Document:
    return await storage.document.create(
        Document(
            uid=user.uid,
            document_id=UUID('00000000-0000-4b00-0000-000000000001'),
            code=DocumentCode.DRIVER_LICENSE,
            value='123',
        )
    )


@pytest.fixture
def auth_user(user):
    return AuthUser(uid=user.uid)
