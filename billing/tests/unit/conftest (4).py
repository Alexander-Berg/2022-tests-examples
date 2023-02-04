from collections import deque
from inspect import getmodule
from typing import Iterable, Type

import pytest

from billing.yandex_pay_admin.yandex_pay_admin.api.handlers.base import BaseHandler  # noqa
from billing.yandex_pay_admin.yandex_pay_admin.core.actions.base import BaseAction
from billing.yandex_pay_admin.yandex_pay_admin.tests.common_conftest import *  # noqa
from billing.yandex_pay_admin.yandex_pay_admin.tests.db import *  # noqa
from billing.yandex_pay_admin.yandex_pay_admin.tests.entities import *  # noqa


@pytest.fixture
def db_engine(mocked_db_engine):
    return mocked_db_engine


@pytest.fixture(autouse=True)
def setup_context(dummy_logger, app, request_id, db_engine):
    BaseAction.context.request_id = request_id
    BaseAction.context.logger = dummy_logger
    BaseAction.context.db_engine = db_engine


@pytest.fixture
def api_handlers() -> Iterable[Type[BaseHandler]]:
    all_subclasses = []
    q = deque(BaseHandler.__subclasses__())
    while len(q) > 0:
        cls = q.pop()
        module_name = str(getmodule(cls))
        if '.test_' in module_name:
            continue

        all_subclasses.append(cls)
        subclasses = cls.__subclasses__()
        q.extend(subclasses)

    return all_subclasses


@pytest.fixture(autouse=True)
def mock_session_info(user, mocker):
    return mocker.patch('sendr_auth.BlackboxAuthenticator.get_user', mocker.AsyncMock(return_value=user))
