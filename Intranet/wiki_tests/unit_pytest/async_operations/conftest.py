import pytest

from wiki.async_operations.consts import OperationOwner
from wiki.async_operations.progress_storage import ASYNC_OP_PROGRESS_STORAGE


@pytest.fixture(scope='function', autouse=True)
def clear_storage():
    ASYNC_OP_PROGRESS_STORAGE.clear()


@pytest.fixture
def owner(wiki_users, test_org_id) -> OperationOwner:
    return OperationOwner(org_inner_id=test_org_id, user_id=wiki_users.thasonic.id)
