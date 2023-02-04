import datetime
from decimal import Decimal
from itertools import count

from agency_rewards.rewards.butils.session import Session
from agency_rewards.rewards.mapper import Run
from agency_rewards.rewards.scheme import base_rewards, prof_rewards
from agency_rewards.rewards.utils.const import ARRunType, CommType, Scale
from billing.agency_rewards.tests_platform.common import TestBase, commission_correction, reward
from billing.agency_rewards.tests_platform.common.scheme import commission_corrections

insert_dt_counter = count()


def _get_unique_insert_dt():
    """
    Берем в прошлом, чтобы никому не мешать. Если работать от текущего времени,
    то ломается логика get_last_insert_dt в некоторых тестах.
    """
    if not hasattr(_get_unique_insert_dt, '__base_dt'):
        _get_unique_insert_dt.__base_dt = datetime.datetime.now().replace(microsecond=0)
    base_insert_dt = _get_unique_insert_dt.__base_dt
    return base_insert_dt - datetime.timedelta(days=1) + datetime.timedelta(seconds=next(insert_dt_counter))


class TestRewardsHistoryDbViewHasCalcColumn(TestBase):
    contract_id1 = TestBase.next_id()

    @classmethod
    def setup_fixtures(cls, session: Session) -> None:
        # test_v_ar_rewards_history_has_calc_column
        session.execute(
            base_rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.BaseMsk.value,
                    ct=CommType.Market.value,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    insert_dt=cls.get_insert_dt(),
                    turnover_to_charge=1000,
                    reward_to_charge=1000,
                    calc=f'tasks/balance39628/base_{cls.contract_id1}',
                ),
            ],
        )
        session.execute(
            prof_rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.Prof22.value,
                    ct=CommType.Market.value,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    insert_dt=cls.get_insert_dt(),
                    turnover_to_charge=1000,
                    reward_to_charge=1000,
                    calc=f'tasks/balance39628/prof_{cls.contract_id1}',
                ),
            ],
        )


class TestRewardsDbViewSelectsRewardsFromPreviousRuns(TestBase):
    contract_id1 = TestBase.next_id()
    contract_id2 = TestBase.next_id()
    contract_id3 = TestBase.next_id()
    contract_id4 = TestBase.next_id()
    contract_id5 = TestBase.next_id()
    contract_id6 = TestBase.next_id()
    contract_id7 = TestBase.next_id()
    contract_id8 = TestBase.next_id()
    contract_id9 = TestBase.next_id()
    contract_id10 = TestBase.next_id()
    contract_id11 = TestBase.next_id()
    contract_id12 = TestBase.next_id()

    insert_dt1 = _get_unique_insert_dt()
    insert_dt2 = _get_unique_insert_dt()
    insert_dt3 = _get_unique_insert_dt()
    insert_dt4 = _get_unique_insert_dt()
    insert_dt5 = _get_unique_insert_dt()
    insert_dt6 = _get_unique_insert_dt()

    @classmethod
    def setup_fixtures(cls, session: Session) -> None:
        cls._for_test_calc_column_has_correct_value_from_v_rewards_history(session=session)
        cls._for_test_calc_column_is_actual_calc_name_for_inline_corrections(session=session)
        cls._for_test_calc_column_is_correction_for_corrections(session=session)
        cls._for_test_returns_rewards_from_single_run_two_calcs(session=session)
        cls._for_test_returns_rewards_from_new_full_run(session=session)
        cls._for_test_returns_rewards_from_old_calc_and_newly_ran_calc(session=session)
        cls._for_test_returns_rewards_if_one_of_existing_calcs_is_reran(session=session)

    @classmethod
    def _for_test_calc_column_has_correct_value_from_v_rewards_history(cls, session: Session) -> None:
        # создаем запуск, иначе вьюха не вернет вознаграждения
        cls._create_run(session=session, insert_dt=cls.insert_dt1)

        # Вознаграждение в прошлом периоде для контракта 1 по базовой шкале
        session.execute(
            base_rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id1,
                    scale=Scale.BaseMsk.value,
                    ct=CommType.Market.value,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    insert_dt=cls.insert_dt1,
                    calc=f'tasks/balance39628/correct_calc_column_base_{cls.contract_id1}',
                ),
            ],
        )
        # Вознаграждение в прошлом периоде для контракта 2 по Проф шкале
        session.execute(
            prof_rewards.insert(),
            [
                reward(
                    contract_id=cls.contract_id2,
                    scale=Scale.Prof22.value,
                    ct=CommType.Market.value,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    insert_dt=cls.insert_dt1,
                    calc=f'tasks/balance39628/correct_calc_column_prof_{cls.contract_id2}',
                ),
            ],
        )

    @classmethod
    def _for_test_calc_column_is_actual_calc_name_for_inline_corrections(cls, session: Session) -> None:
        cls._create_run(session=session, insert_dt=cls.insert_dt2)
        cls._create_rewards(
            session=session,
            insert_dt=cls.insert_dt2,
            calc=f'tasks/balance39628/check_inline_correction_{cls.contract_id3}',
            contract_ids=[cls.contract_id3],
        )

        session.execute(
            commission_corrections.insert(),
            [
                commission_correction(
                    contract_id=cls.contract_id3,
                    type='my_test_correction_inline',
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    reward_to_charge=Decimal(2345),
                    discount_type=CommType.Market,
                ),
            ],
        )

    @classmethod
    def _for_test_calc_column_is_correction_for_corrections(cls, session: Session) -> None:
        session.execute(
            commission_corrections.insert(),
            [
                commission_correction(
                    contract_id=cls.contract_id4,
                    type='my_test_correction',
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                ),
            ],
        )

    @classmethod
    def _for_test_returns_rewards_from_single_run_two_calcs(cls, session: Session) -> None:
        cls._create_run(session=session, insert_dt=cls.insert_dt3)
        cls._create_rewards(
            session=session,
            insert_dt=cls.insert_dt3,
            calc=f'tasks/balance39628/check_simple_{cls.contract_id5}',
            contract_ids=[cls.contract_id5],
        )
        cls._create_rewards(
            session=session,
            insert_dt=cls.insert_dt3,
            calc=f'tasks/balance39628/check_simple_{cls.contract_id6}',
            contract_ids=[cls.contract_id6],
        )

    @classmethod
    def _for_test_returns_rewards_from_new_full_run(cls, session: Session) -> None:
        # old run
        insert_dt = cls.insert_dt4
        cls._create_run(session=session, insert_dt=insert_dt)
        cls._create_rewards(
            session=session,
            insert_dt=insert_dt,
            calc=f'tasks/balance39628/check_full_override_{cls.contract_id7}',
            contract_ids=[cls.contract_id7],
        )
        cls._create_rewards(
            session=session,
            insert_dt=insert_dt,
            calc=f'tasks/balance39628/check_full_override_{cls.contract_id8}',
            contract_ids=[cls.contract_id8],
        )
        # new full run 1 hour later
        new_insert_dt = insert_dt + datetime.timedelta(hours=1)
        cls._create_run(
            session=session,
            insert_dt=new_insert_dt,
        )
        cls._create_rewards(
            session=session,
            insert_dt=new_insert_dt,
            calc=f'tasks/balance39628/check_full_override_{cls.contract_id7}',
            contract_ids=[cls.contract_id7],
        )
        cls._create_rewards(
            session=session,
            insert_dt=new_insert_dt,
            calc=f'tasks/balance39628/check_full_override_{cls.contract_id8}',
            contract_ids=[cls.contract_id8],
        )

    @classmethod
    def _for_test_returns_rewards_from_old_calc_and_newly_ran_calc(cls, session: Session) -> None:
        # old calc run
        insert_dt = cls.insert_dt5
        cls._create_run(session=session, insert_dt=insert_dt)
        cls._create_rewards(
            session=session,
            insert_dt=insert_dt,
            calc=f'tasks/balance39628/check_newly_ran_{cls.contract_id9}',
            contract_ids=[cls.contract_id9],
        )
        # new calc run 1 hour later
        new_insert_dt = insert_dt + datetime.timedelta(hours=1)
        cls._create_run(session=session, insert_dt=new_insert_dt)
        cls._create_rewards(
            session=session,
            insert_dt=new_insert_dt,
            calc=f'tasks/balance39628/check_newly_ran_{cls.contract_id10}',
            contract_ids=[cls.contract_id10],
        )

    @classmethod
    def _for_test_returns_rewards_if_one_of_existing_calcs_is_reran(cls, session: Session) -> None:
        # old calcs run
        insert_dt = cls.insert_dt6
        cls._create_run(session=session, insert_dt=insert_dt)
        cls._create_rewards(
            session=session,
            insert_dt=insert_dt,
            calc=f'tasks/balance39628/check_rerun_existing_{cls.contract_id11}',
            contract_ids=[cls.contract_id11],
        )
        cls._create_rewards(
            session=session,
            insert_dt=insert_dt,
            calc=f'tasks/balance39628/check_rerun_existing_{cls.contract_id12}',
            contract_ids=[cls.contract_id12],
        )
        # rerun one old calc 1 hour later
        new_insert_dt = insert_dt + datetime.timedelta(hours=1)
        cls._create_run(session=session, insert_dt=new_insert_dt)
        cls._create_rewards(
            session=session,
            insert_dt=new_insert_dt,
            calc=f'tasks/balance39628/check_rerun_existing_{cls.contract_id12}',
            contract_ids=[cls.contract_id12],
        )

    @staticmethod
    def _create_run(session: Session, insert_dt: datetime.datetime):
        """
        Имитирует запуск расчетов на заданную дату.
        Добавляет запись в t_ar_run на заданный insert_dt, потому что иначе во вьюхе v_ar_rewards
        join не сработает. Этот запуск так же помечается продовским (тип `calc`).
        Поэтому создавать будем заведомо далеко в прошлом и без заполненного столбца start_dt.
        Но столбец finish_dt придется заполнить, потому что по нему есть фильтр в v_ar_rewards.
        """
        run = Run(
            run_dt=datetime.datetime(1990, 1, 1),
            insert_dt=insert_dt,
            type=ARRunType.Prod.value,
            finish_dt=insert_dt,
        )
        with session.begin():
            session.add(run)

    @classmethod
    def _create_rewards(cls, session: Session, insert_dt: datetime.datetime, calc: str, contract_ids: list[int]):
        session.execute(
            base_rewards.insert(),
            [
                reward(
                    contract_id=contract_id,
                    scale=Scale.BaseMsk.value,
                    ct=CommType.Market.value,
                    from_dt=cls.from_dt_1m_ago,
                    till_dt=cls.till_dt_1m_ago,
                    insert_dt=insert_dt,
                    calc=calc,
                )
                for contract_id in contract_ids
            ],
        )
