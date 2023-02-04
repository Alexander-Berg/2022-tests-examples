import uuid

import pytest

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.storage import AgentMapper
from billing.yandex_pay_admin.yandex_pay_admin.storage.entities.agent import Agent
from billing.yandex_pay_admin.yandex_pay_admin.tests.unit.storage.mappers.base import BaseMapperTests


class TestAgentMapper(BaseMapperTests):
    @pytest.fixture
    def entity(self) -> Agent:
        return Agent(
            agent_id=uuid.uuid4(),
            oauth_client_id='client_id',
            name='agent',
        )

    @pytest.fixture
    def mapper(self, storage) -> AgentMapper:
        return storage.agent

    @pytest.mark.asyncio
    async def test_find_by_oauth_client_id(self, mapper, entity):
        agent = await mapper.create(entity)

        found = await mapper.find_by_oauth_client_id(agent.oauth_client_id)

        assert_that(found, equal_to(agent))
