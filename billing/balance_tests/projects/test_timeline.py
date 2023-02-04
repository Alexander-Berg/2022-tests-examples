# -*- coding: utf-8 -*-

from balance.contracts.timeline import ProjectTimeline, Interval

from balance import contractpage

from tests.base import BalanceTest
from tests.object_builder import *
from datetime import datetime

from collections import namedtuple

df = lambda sdt: datetime.strptime(sdt, '%Y-%m-%d')


_Collateral = namedtuple(
    'Collateral', ('id', 'dt', 'finish_dt', 'contract_projects', 'signed'))


class _Contract(object):
    def __init__(self, _id, client_id, collaterals, cs=None):
        self.id = _id
        self.client_id = client_id
        self.collaterals = collaterals
        self.cs = cs or collaterals[-1]

    def current_signed(self):
        return self.cs


class TestTimeline(BalanceTest):
    def test_timeline_base_case(self):
        """Базовый случай: оферта закрыта 5го числа, договор создан 3го числа,
        подписан 5го числа. Открутки за 1-4 числа мапятся на оферту,
        открутки 5-10 числа мапятся на договор."""
        session = self.session
        client = ClientBuilder().build(session).obj
        person = PersonBuilder(client=client).build(client.session).obj
        services = {143}
        projects = ('test-project-01', )

        offer = ContractBuilder(
            client=person.client,
            person=person,
            services=services,
            dt=df('2018-01-01'),
            sign_dt=df('2018-01-01'),
            finish_dt=df('2018-01-05'),
            contract_projects=dict(enumerate(projects)),
            firm_id=1,
        ).build(session).obj

        offer_timeline = ProjectTimeline.build_from_contract(offer)
        expected_timeline = ProjectTimeline([('2018-01-01', '2018-01-05')])
        self.assertEqual(offer_timeline, expected_timeline)

    def test_offer_not_closed(self):
        """Оферта не закрыта, договор создан 3 числа. При попытке подписать
        договор кидаем ошибку. Все открутки мапятся на оферту."""

        session = self.session
        client = ClientBuilder().build(session).obj
        person = PersonBuilder(client=client).build(client.session).obj
        services = {143}
        projects = ('test-project-01', )

        offer = ContractBuilder(
            client=person.client,
            person=person,
            services=services,
            dt=df('2018-01-01'),
            sign_dt=df('2018-01-01'),
            contract_projects=dict(enumerate(projects)),
            firm_id=1,
        ).build(session).obj

        contract = ContractBuilder(
            client=person.client,
            person=person,
            services=services,
            dt=df('2018-01-03'),
            contract_projects=dict(enumerate(projects)),
            firm_id=1,
        ).build(session).obj

        contract.col0.firm = 1
        contract.col0.manager_code = 1122
        contract.col0.commission = 0
        contract.col0.payment_type = POSTPAY_PAYMENT_TYPE
        contract.col0.personal_account = 1
        contract.col0.currency = 810
        contract.col0.payment_term = 15
        contract.col0.is_signed = False

        offer_timeline = ProjectTimeline.build_from_contract(offer)
        contract_timeline = ProjectTimeline.build_from_contract(contract)

        conflicts = offer_timeline.conflicts(contract_timeline)
        self.assertRaises(StopIteration, lambda: next(conflicts))  # no conflicts

        cp = contractpage.ContractPage(self.session, contract.id)
        self.assertRaises(Exception, cp.sign())

    def test_build_timeline_from_contracts(self):
        project_id = '00000000-0000-0000-0000-000000000001'

        contracts = [
            _Contract(_id=1, client_id=1,
                      collaterals=[
                          _Collateral(
                              id=1,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=datetime(year=2015, month=1, day=2),
                              contract_projects={0: project_id},
                              signed=True
                          ),
                          _Collateral(
                              id=2,
                              dt=datetime(year=2015, month=1, day=2),
                              finish_dt=datetime(year=2015, month=1, day=5),
                              contract_projects={},
                              signed=True
                          ),
                          _Collateral(
                              id=3,
                              dt=datetime(year=2015, month=1, day=7),
                              finish_dt=datetime(year=2015, month=1, day=10),
                              contract_projects={0: project_id},
                              signed=True
                          ),

                          # current signed
                          _Collateral(
                              id=4,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=datetime(year=2015, month=1, day=10),
                              contract_projects=None,
                              signed=True
                          ),
                      ])
        ]

        timelines = ProjectTimeline.build_from_contracts(contracts)
        timeline = timelines[project_id]

        expected_intervals = (
            Interval(start_dt=datetime(2015, 1, 1), finish_dt=datetime(2015, 1, 2)),
            Interval(start_dt=datetime(2015, 1, 7), finish_dt=datetime(2015, 1, 10)),
        )

        self.assertEqual(len(timeline), len(expected_intervals))
        for i, interval in enumerate(timeline):
            expected = expected_intervals[i]
            self.assertEqual(interval.start_dt, expected.start_dt)
            self.assertEqual(interval.finish_dt, expected.finish_dt)

    def test_build_timeline_from_contracts_with_open_finish_dt(self):
        project_id = '00000000-0000-0000-0000-000000000001'

        contracts = [
            _Contract(_id=1, client_id=1,
                      collaterals=[
                          _Collateral(
                              id=1,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=None,
                              contract_projects={0: project_id},
                              signed=True
                          ),
                          _Collateral(
                              id=2,
                              dt=datetime(year=2015, month=1, day=2),
                              finish_dt=None,
                              contract_projects={},
                              signed=True
                          ),
                          _Collateral(
                              id=3,
                              dt=datetime(year=2015, month=1, day=7),
                              finish_dt=None,
                              contract_projects={0: project_id},
                              signed=True
                          ),

                          # current signed
                          _Collateral(
                              id=4,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=datetime(year=2015, month=1, day=20),
                              contract_projects=None,
                              signed=True
                          ),
                      ])
        ]

        timelines = ProjectTimeline.build_from_contracts(contracts)
        timeline = timelines[project_id]

        expected_intervals = (
            Interval(start_dt=datetime(2015, 1, 1), finish_dt=datetime(2015, 1, 2)),
            Interval(start_dt=datetime(2015, 1, 7), finish_dt=datetime(2015, 1, 20)),
        )

        self.assertEqual(len(timeline), len(expected_intervals))
        for i, interval in enumerate(timeline):
            expected = expected_intervals[i]
            self.assertEqual(interval.start_dt, expected.start_dt)
            self.assertEqual(interval.finish_dt, expected.finish_dt)

    def test_build_timeline_from_contracts_with_all_open_finish_dt_and_nonsigned_collateral(self):
        project_id = '00000000-0000-0000-0000-000000000001'

        contracts = [
            _Contract(_id=1, client_id=1,
                      collaterals=[
                          _Collateral(
                              id=1,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=None,
                              contract_projects={0: project_id},
                              signed=True
                          ),
                          _Collateral(
                              id=2,
                              dt=datetime(year=2015, month=1, day=2),
                              finish_dt=None,
                              contract_projects={},
                              signed=True
                          ),
                          _Collateral(
                              id=3,
                              dt=datetime(year=2015, month=1, day=3),
                              finish_dt=None,
                              contract_projects=None,
                              signed=True
                          ),
                          _Collateral(
                              id=4,
                              dt=datetime(year=2015, month=1, day=3),
                              finish_dt=datetime(year=2015, month=1, day=5),
                              contract_projects=None,
                              signed=False
                          ),
                          _Collateral(
                              id=5,
                              dt=datetime(year=2015, month=1, day=7),
                              finish_dt=None,
                              contract_projects={0: project_id},
                              signed=True
                          ),

                          # current signed
                          _Collateral(
                              id=6,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=None,
                              contract_projects=None,
                              signed=True
                          ),
                      ])
        ]

        timelines = ProjectTimeline.build_from_contracts(contracts)
        timeline = timelines[project_id]

        expected_intervals = (
            Interval(start_dt=datetime(2015, 1, 1), finish_dt=datetime(2015, 1, 2)),
            Interval(start_dt=datetime(2015, 1, 7), finish_dt=None),
        )

        self.assertEqual(len(timeline), len(expected_intervals))
        for i, interval in enumerate(timeline):
            expected = expected_intervals[i]
            self.assertEqual(interval.start_dt, expected.start_dt)
            self.assertEqual(interval.finish_dt, expected.finish_dt)

    def test_build_timeline_from_contracts_with_open_finish_dt_and_several_projects(self):
        project_id1 = '00000000-0000-0000-0000-000000000001'
        project_id2 = '6666666666'
        project_id3 = '33333333333333'

        contracts = [
            _Contract(_id=1, client_id=1,
                      collaterals=[
                          _Collateral(
                              id=1,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=None,
                              contract_projects={0: project_id1, 1: project_id2},
                              signed=True
                          ),
                          _Collateral(
                              id=2,
                              dt=datetime(year=2015, month=1, day=2),
                              finish_dt=None,
                              contract_projects={0: project_id2},
                              signed=True
                          ),
                          _Collateral(
                              id=3,
                              dt=datetime(year=2015, month=1, day=7),
                              finish_dt=None,
                              contract_projects={0: project_id3, 1: project_id1},
                              signed=True
                          ),

                          # current signed
                          _Collateral(
                              id=4,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=datetime(year=2015, month=1, day=20),
                              contract_projects=None,
                              signed=True
                          ),
                      ])
        ]
        timelines = ProjectTimeline.build_from_contracts(contracts)

        expected_map = {project_id1: (Interval(start_dt=datetime(2015, 1, 1), finish_dt=datetime(2015, 1, 2)),
                                      Interval(start_dt=datetime(2015, 1, 7), finish_dt=datetime(2015, 1, 20))),
                        project_id2: (Interval(start_dt=datetime(2015, 1, 1), finish_dt=datetime(2015, 1, 7), ), ),
                        project_id3: (Interval(start_dt=datetime(2015, 1, 7), finish_dt=datetime(2015, 1, 20), ), ),
                        }

        for project_id, expected_intervals in expected_map.items():
            timeline = timelines[project_id]

            self.assertEqual(len(timeline), len(expected_intervals))
            for i, interval in enumerate(timeline):
                expected = expected_intervals[i]
                self.assertEqual(interval.start_dt, expected.start_dt)
                self.assertEqual(interval.finish_dt, expected.finish_dt)

    def test_timeline_from_contracts_limited_by_current_sign(self):
        project_id = '00000000-0000-0000-0000-000000000001'

        contracts = [
            _Contract(_id=1, client_id=1,
                      collaterals=[
                          _Collateral(
                              id=1,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=datetime(year=2015, month=1, day=2),
                              contract_projects={0: project_id},
                              signed=True
                          ),
                          _Collateral(
                              id=2,
                              dt=datetime(year=2015, month=1, day=2),
                              finish_dt=datetime(year=2015, month=1, day=5),
                              contract_projects={},
                              signed=True
                          ),
                          _Collateral(
                              id=3,
                              dt=datetime(year=2015, month=1, day=7),
                              finish_dt=datetime(year=2015, month=1, day=10),
                              contract_projects={0: project_id},
                              signed=True
                          ),

                          # current signed
                          _Collateral(
                              id=4,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=datetime(year=2015, month=1, day=5),
                              contract_projects=None,
                              signed=True
                          ),
                      ])
        ]

        timelines = ProjectTimeline.build_from_contracts(contracts)
        timeline = timelines[project_id]

        expected_intervals = (
            Interval(start_dt=datetime(2015, 1, 1), finish_dt=datetime(2015, 1, 2)),
        )

        self.assertEqual(len(timeline), len(expected_intervals))
        for i, interval in enumerate(timeline):
            expected = expected_intervals[i]
            self.assertEqual(interval.start_dt, expected.start_dt)
            self.assertEqual(interval.finish_dt, expected.finish_dt)

    def test_build_timeline_from_contracts_with_contract_extensions(self):
        # Смотри BALANCE-35585 - Не учитывается ДС на продление договора в расчете таймлайна
        project_id = '00000000-0000-0000-0000-000000000001'

        contracts = [
            _Contract(_id=1, client_id=1,
                      collaterals=[
                          _Collateral(
                              id=0,
                              dt=datetime(year=2015, month=1, day=1),
                              finish_dt=None,  # sic!
                              contract_projects={0: project_id},
                              signed=True
                          ),
                          # имитируем соглашение о продлении договора
                          _Collateral(
                              id=1,
                              dt=datetime(year=2015, month=1, day=10),
                              finish_dt=datetime(year=2016, month=1, day=10),
                              contract_projects=None,  # sic!
                              signed=True
                          ),],
                      cs=_Collateral(
                          id=3,
                          dt=datetime(year=2015, month=1, day=1),
                          finish_dt=datetime(year=2016, month=1, day=10),
                          contract_projects={0: project_id},
                          signed=True
                      ),)
        ]

        timelines = ProjectTimeline.build_from_contracts(contracts)
        timeline = timelines[project_id]

        expected_intervals = (
            Interval(start_dt=datetime(2015, 1, 1), finish_dt=datetime(2016, 1, 10)),
        )

        self.assertEqual(len(timeline), len(expected_intervals))
        for i, interval in enumerate(timeline):
            expected = expected_intervals[i]
            self.assertEqual(interval.start_dt, expected.start_dt)
            self.assertEqual(interval.finish_dt, expected.finish_dt)


if __name__ == '__main__':
    import nose.core
    nose.core.runmodule()
