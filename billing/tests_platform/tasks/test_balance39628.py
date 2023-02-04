import datetime

import sqlalchemy as sa

from billing.agency_rewards.tests_platform.common import TestBase
from billing.agency_rewards.tests_platform.common.scheme import v_ar_rewards, v_ar_rewards_history


class TestRewardsHistoryDbViewHasCalcColumn(TestBase):
    """
    BALANCE-39628
    Проверяем наличие поля `calc` во вьюхе v_ar_rewards_history.
    Добавляем в таблицы t_comm_base_src и t_comm_prof_src по одной записи.
    """

    contract_id1: int

    def setUp(self):
        self.load_pickled_data(self.session)

    def test_v_ar_rewards_history_has_calc_column(self):
        """
        Проверяем, что v_ar_rewards_history подтягивает из t_comm_***_src таблиц название расчёта
        в столбец calc.
        """

        res = self.session.execute(
            sa.select([v_ar_rewards_history.c.calc])
            .where(v_ar_rewards_history.c.contract_id == self.contract_id1)
            .order_by(v_ar_rewards_history.c.calc)
        ).fetchall()

        self.assertEqual(
            [row[0] for row in res],
            [f'tasks/balance39628/base_{self.contract_id1}', f'tasks/balance39628/prof_{self.contract_id1}'],
        )


class TestRewardsDbViewSelectsRewardsFromPreviousRuns(TestBase):
    """
    BALANCE-39628
    Проверяем наличие поля `calc` во вьюхе v_ar_rewards для всех случаев:
    1. Вознаграждение
    2. Корректировка
    3. Инлайн корректировка
    Проверяем, что полный запуск через час обновляет insert_dt вознаграждениям.
    Проверяем, что частичный запуск (участвовали не все расчёты) позволяет получать вознаграждения
    по расчётам, которые в запуске не участвовали.
    """

    contract_id1: int
    contract_id2: int
    contract_id3: int
    contract_id4: int
    contract_id5: int
    contract_id6: int
    contract_id7: int
    contract_id8: int
    contract_id9: int
    contract_id10: int
    contract_id11: int
    contract_id12: int

    insert_dt3: datetime.datetime
    insert_dt4: datetime.datetime
    insert_dt5: datetime.datetime
    insert_dt6: datetime.datetime

    def setUp(self):
        self.load_pickled_data(self.session)

    def test_calc_column_has_correct_value_from_v_rewards_history(self):
        """
        Проверяем, что v_rewards корректно подтягивает из v_rewards_history значения из calc столбца
        и возвращает их.
        """

        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count()])
                .select_from(v_ar_rewards)
                .where(
                    sa.and_(
                        v_ar_rewards.c.contract_id == self.contract_id1,
                        v_ar_rewards.c.calc == f'tasks/balance39628/correct_calc_column_base_{self.contract_id1}',
                    )
                )
            ).scalar(),
            1,
        )
        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count()])
                .select_from(v_ar_rewards)
                .where(
                    sa.and_(
                        v_ar_rewards.c.contract_id == self.contract_id2,
                        v_ar_rewards.c.calc == f'tasks/balance39628/correct_calc_column_prof_{self.contract_id2}',
                    )
                )
            ).scalar(),
            1,
        )

    def test_calc_column_is_actual_calc_name_for_inline_corrections(self):
        """
        Проверяем, что inline корректировки остаются прозрачными и при их наличии в
        столбце calc сохраняется оригинальное название расчёта.
        """

        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count()])
                .select_from(v_ar_rewards)
                .where(
                    sa.and_(
                        v_ar_rewards.c.contract_id == self.contract_id3,
                        v_ar_rewards.c.calc == f'tasks/balance39628/check_inline_correction_{self.contract_id3}',
                        v_ar_rewards.c.reward_to_charge == 2345,
                    )
                )
            ).scalar(),
            1,
        )

    def test_calc_column_is_correction_for_corrections(self):
        """
        Проверяем, что корректировка полностью перекрывает название расчёта и
        заменяет его на correction в столбце calc.
        """

        self.assertEqual(
            self.session.execute(
                sa.select([sa.func.count()])
                .select_from(v_ar_rewards)
                .where(
                    sa.and_(
                        v_ar_rewards.c.contract_id == self.contract_id4,
                        v_ar_rewards.c.calc == 'correction',
                    )
                )
            ).scalar(),
            1,
        )

    def test_returns_rewards_from_single_run_two_calcs(self):
        """
        Проверяем, что ничего не сломалось в простейшем случае, когда
        просто были запущены два расчёта.
        """

        res = self.session.execute(
            sa.select(
                [
                    v_ar_rewards.c.contract_id,
                    v_ar_rewards.c.insert_dt,
                ]
            )
            .where(v_ar_rewards.c.contract_id.in_([self.contract_id5, self.contract_id6]))
            .order_by(v_ar_rewards.c.contract_id)
        ).fetchall()

        self.assertEqual(
            [dict(row) for row in res],
            [
                {
                    'contract_id': self.contract_id5,
                    'insert_dt': self.insert_dt3,
                },
                {
                    'contract_id': self.contract_id6,
                    'insert_dt': self.insert_dt3,
                },
            ],
        )

    def test_returns_rewards_from_new_full_run(self):
        """
        Проверяем, что при нормальном полном перезапуске расчётов через час
        оба расчёта будут возвращать новое значение insert_dt запуска.
        """

        res = self.session.execute(
            sa.select(
                [
                    v_ar_rewards.c.contract_id,
                    v_ar_rewards.c.insert_dt,
                ]
            )
            .where(v_ar_rewards.c.contract_id.in_([self.contract_id7, self.contract_id8]))
            .order_by(v_ar_rewards.c.contract_id)
        ).fetchall()

        self.assertEqual(
            [dict(row) for row in res],
            [
                {
                    'contract_id': self.contract_id7,
                    'insert_dt': self.insert_dt4 + datetime.timedelta(hours=1),
                },
                {
                    'contract_id': self.contract_id8,
                    'insert_dt': self.insert_dt4 + datetime.timedelta(hours=1),
                },
            ],
        )

    def test_returns_rewards_from_old_calc_and_newly_ran_calc(self):
        """
        Проверяем случай, когда есть старый запуск с одним расчётом, но добавили
        новый расчёт и сделали неполный запуск только по нему.
        Должно вернуть оба расчёта с правильными insert_dt.
        """

        res = self.session.execute(
            sa.select(
                [
                    v_ar_rewards.c.contract_id,
                    v_ar_rewards.c.insert_dt,
                ]
            )
            .where(v_ar_rewards.c.contract_id.in_([self.contract_id9, self.contract_id10]))
            .order_by(v_ar_rewards.c.contract_id)
        ).fetchall()

        self.assertEqual(
            [dict(row) for row in res],
            [
                {
                    'contract_id': self.contract_id9,
                    'insert_dt': self.insert_dt5,
                },
                {
                    'contract_id': self.contract_id10,
                    'insert_dt': self.insert_dt5 + datetime.timedelta(hours=1),
                },
            ],
        )

    def test_returns_rewards_if_one_of_existing_calcs_is_reran(self):
        """
        Проверяем случай, когда был запуск по двум расчётам, а потом один из них
        прогнали еще раз в неполном запуске.
        Только у перезапущенного расчёта должно обновиться insert_dt.
        """

        res = self.session.execute(
            sa.select(
                [
                    v_ar_rewards.c.contract_id,
                    v_ar_rewards.c.insert_dt,
                ]
            )
            .where(v_ar_rewards.c.contract_id.in_([self.contract_id11, self.contract_id12]))
            .order_by(v_ar_rewards.c.contract_id)
        ).fetchall()

        self.assertEqual(
            [dict(row) for row in res],
            [
                {
                    'contract_id': self.contract_id11,
                    'insert_dt': self.insert_dt6,
                },
                {
                    'contract_id': self.contract_id12,
                    'insert_dt': self.insert_dt6 + datetime.timedelta(hours=1),
                },
            ],
        )
