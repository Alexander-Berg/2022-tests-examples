from agency_rewards.rewards.utils.yql_crutches import export_to_yt

from billing.agency_rewards.tests_platform.common import TestBase, USD
from billing.agency_rewards.tests_platform.common import yt_type, new_reward, REWARD_YT_COLUMNS, get_bunker_calc


bunker_calc_path = '/agency-rewards/dev/regression/tasks/balance-34499'


class TestCommContractCurrency(TestBase):
    """
    Для комисс. договоров могут быть валюты, отличные от рублей.

    Проверяем, что валюта берется из бункера, если в YT нет поля currency
    Проверяем, что валюта берется из YT, если в YT есть поле currency
    """

    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()

    @classmethod
    def setup_fixtures_ext(cls, _session, yt_client, _yql_client):
        path = get_bunker_calc(bunker_calc_path).path
        export_to_yt(
            yt_client,
            path,
            lambda: (
                new_reward(cls.contract_id1, amt=100.0, reward=10.0, currency="", delkredere=1.0),
                new_reward(cls.contract_id2, amt=200.0, reward=20.0, currency=USD, delkredere=2.0),
            ),
            REWARD_YT_COLUMNS
            + [
                yt_type("delkredere", "double"),
            ],
        )


class TestCommCopyCurrency(TestBase):
    """
    Проверка, что валюту в 10/310 строку копируем из 1/301 строки
    Она есть в тестах по Беларусии и Казахстану, например:

    - tests_platform.m.test_belarus.TestPostPayment
    - tests_platform.generators.m.belarus.TestPostPayment
    """

    pass
