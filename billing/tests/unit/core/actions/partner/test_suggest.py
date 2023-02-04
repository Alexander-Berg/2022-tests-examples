import pytest

from sendr_interactions.clients.spark_suggest import SparkSuggestItem

from hamcrest import assert_that, equal_to

from billing.yandex_pay_admin.yandex_pay_admin.core.actions.partner.suggest import PartnerSuggestAction
from billing.yandex_pay_admin.yandex_pay_admin.interactions import SparkSuggestClient


class TestPartnerSuggestAction:
    @pytest.fixture
    def suggest_list(self):
        return [
            SparkSuggestItem(
                spark_id=i,
                name=f'suggested_name_{i}',
                full_name=f'suggested_full_name_{i}',
                inn=f'suggested_inn_{i}' if i != 2 else '',
                ogrn=f'suggested_ogrn_{i}',
                address=f'suggested_address_{i}',
                leader_name=f'suggested_leader_name_{i}',
                region_name=f'suggested_region_name_{i}',
            )
            for i in range(3)
        ]

    @pytest.mark.asyncio
    async def test_result(self, mocker, suggest_list):
        query = 'ИП Иванов 047...'
        client_mock = mocker.patch.object(SparkSuggestClient, 'get_hint', mocker.AsyncMock(return_value=suggest_list))

        returned = await PartnerSuggestAction(query=query).run()

        assert_that(returned, equal_to(suggest_list[:2]))
        client_mock.assert_called_once_with(query=query)

    @pytest.mark.asyncio
    async def test_empty_request(self):
        assert [] == await PartnerSuggestAction(query='').run()
