from agency_rewards.rewards.scheme import base_rewards, paid_periods
from billing.agency_rewards.tests_platform.common import TestBase

import sqlalchemy as sa


class TestDiscardTK0inKO(TestBase):
    """
    Проверяет начисление премии после покрытия периода

    Данные генерятся тут:

    tests_platform.generators.tasks.balance34491.TestDiscardTK0inKO

    Чтобы убедиться, что тест актуален, надо раскоментировать строки:

        https://a.yandex-team.ru/review/1248662/files/1

    После этого тест, доолжен упасть.
    """

    contract_id1 = 0  # будет настроено позже

    def test_post_payment(self):
        self.load_pickled_data(self.session)

        cls_rewards = base_rewards

        # по этому договору ничего не должно быть
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(cls_rewards.c.contract_id)]).where(
                    sa.and_(cls_rewards.c.contract_id == self.contract_id1)
                )
            ).scalar(),
            0,
            self.contract_id1,
        )

        # факта оплаты быть не должно
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count(paid_periods.c.id)]).where(
                    sa.and_(
                        paid_periods.c.contract_id == self.contract_id1,
                        paid_periods.c.from_dt == self.from_dt_1m_ago,
                        paid_periods.c.paid_dt == self.from_dt,
                    )
                )
            ).scalar(),
            0,
            self.contract_id1,
        )
