from agency_rewards.rewards.scheme import base_rewards
from billing.agency_rewards.tests_platform.common import TestBase, RUR, USD

import sqlalchemy as sa


class TestCommContractCurrency(TestBase):
    """
    Для комисс. договоров могут быть валюты, отличные от рублей.

    Проверяем, что валюта берется из бункера, если в YT нет поля currency
    Проверяем, что валюта берется из YT, если в YT есть поле currency

    Имитуруем результаты расчета в YT, чтобы они просто были скачаны в БД.
    В БД проверем.

    Данные генерятся тут:
    :class:`tests_platform.generators.tasks.balance34499.TestCommContractCurrency`

    Заодно проверяем доработки по задаче:
    - BALANCE-34789 (НДС зависит от признака "Резидент")
    """

    contract_id1 = 0  # будет настроено позже
    contract_id2 = 0  # будет настроено позже

    def test_check_currency_from_bunker(self):
        self.load_pickled_data(self.session)

        cls_rewards = base_rewards

        # валюта должна быть из конфига, т.к. в таблице она не заполнена
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        cls_rewards.c.contract_id == self.contract_id1,
                        cls_rewards.c.currency == RUR,
                        cls_rewards.c.delkredere_to_charge == 1.0,
                        cls_rewards.c.delkredere_to_pay == 1.0,
                    )
                )
            ).scalar(),
            1,
            self.contract_id1,
        )

    def test_check_currency_from_yt(self):
        self.load_pickled_data(self.session)

        cls_rewards = base_rewards

        # валюта должна быть из YT
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        cls_rewards.c.contract_id == self.contract_id2,
                        cls_rewards.c.currency == USD,
                        cls_rewards.c.delkredere_to_charge == 2.0,
                        cls_rewards.c.delkredere_to_pay == 2.0,
                    )
                )
            ).scalar(),
            1,
            self.contract_id2,
        )

    def test_check_nds(self):
        self.load_pickled_data(self.session)

        cls_rewards = base_rewards

        # НДС должен быть 0, т.к. в расчете стоит "нерезидент"
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(
                        cls_rewards.c.contract_id.in_((self.contract_id1, self.contract_id2)),
                        cls_rewards.c.nds == 0,
                    )
                )
            ).scalar(),
            2,
            (self.contract_id2, self.contract_id1),
        )
