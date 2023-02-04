from mock import Mock
import unittest
from datetime import date, timedelta, datetime
import copy

from maps.infra.duty.generator.lib.duty_manager import DutyManager, DutyRank, LAST_UPDATE
from maps.infra.duty.generator.lib.const import WORKDAYS, NONWORKDAYS


class MutableArgsMock(Mock):
    def _increment_mock_call(_mock_self, *args, **kwargs):
        return super()._increment_mock_call(*copy.deepcopy(args), **copy.deepcopy(kwargs))

    def _mock_call(_mock_self, *args, **kwargs):
        return super()._mock_call(*copy.deepcopy(args), **copy.deepcopy(kwargs))


class DutyManagerTest(unittest.TestCase):
    TEAM = ['alexey-savin', 'af-arno', 'comradeandrew', 'imseleznev', 'khrolenko', 'romankh', 'tervlad', 'ykirpichev']
    ABC_SERVICE = 'maps-duty-team'
    C1 = '1'
    C2 = '2'
    TODAY = date(2018, 12, 10)

    DAY_BY_TYPE_2018_DEC_2019_JAN = {
        date(2018, 12, 16): 'weekend', date(2018, 12, 5): 'weekday', date(2019, 1, 13): 'weekend',
        date(2019, 1, 22): 'weekday', date(2018, 12, 30): 'weekend', date(2018, 12, 3): 'weekday',
        date(2019, 1, 15): 'weekday', date(2019, 1, 16): 'weekday', date(2018, 12, 28): 'weekday',
        date(2018, 12, 1): 'weekend', date(2019, 1, 9): 'weekday', date(2019, 1, 18): 'weekday',
        date(2018, 12, 26): 'weekday', date(2018, 12, 15): 'weekend', date(2019, 1, 11): 'weekday',
        date(2019, 1, 28): 'weekday', date(2018, 12, 24): 'weekday', date(2018, 12, 13): 'weekday',
        date(2019, 1, 30): 'weekday', date(2018, 12, 11): 'weekday', date(2019, 1, 24): 'weekday',
        date(2018, 12, 9): 'weekend', date(2019, 1, 26): 'weekend', date(2018, 12, 23): 'weekend',
        date(2019, 1, 4): 'holiday', date(2018, 12, 21): 'weekday', date(2019, 1, 6): 'holiday',
        date(2018, 12, 19): 'weekday', date(2019, 1, 21): 'weekday', date(2018, 12, 17): 'weekday',
        date(2018, 12, 6): 'weekday', date(2019, 1, 2): 'holiday', date(2019, 1, 23): 'weekday',
        date(2018, 12, 31): 'weekend', date(2018, 12, 4): 'weekday', date(2019, 1, 12): 'weekend',
        date(2019, 1, 17): 'weekday', date(2018, 12, 29): 'weekday', date(2018, 12, 2): 'weekend',
        date(2019, 1, 14): 'weekday', date(2019, 1, 19): 'weekend', date(2018, 12, 27): 'weekday',
        date(2019, 1, 8): 'holiday', date(2019, 1, 29): 'weekday', date(2018, 12, 25): 'weekday',
        date(2018, 12, 14): 'weekday', date(2019, 1, 10): 'weekday', date(2018, 12, 12): 'weekday',
        date(2019, 1, 25): 'weekday', date(2018, 12, 10): 'weekday', date(2019, 1, 27): 'weekend',
        date(2018, 12, 8): 'weekend', date(2019, 1, 5): 'holiday', date(2018, 12, 22): 'weekend',
        date(2019, 1, 7): 'holiday', date(2018, 12, 20): 'weekday', date(2019, 1, 1): 'holiday',
        date(2018, 12, 18): 'weekday', date(2018, 12, 7): 'weekday', date(2019, 1, 3): 'holiday',
        date(2019, 1, 20): 'weekend', date(2019, 1, 31): 'weekday'}

    def init_test(self):
        self.manager = DutyManager(calendar_uid='1', tvm_client_id=42, tvm_secret='fake_secret', oauth_token='X', dry_run=False)
        self.manager.gap.gaps = self.gap
        self.manager.dutycoins_cache.load = Mock(return_value=self.dutycoins_cache)
        self.manager.dutycoins_cache.save = self.save_dutycoins = MutableArgsMock()
        self.manager._load_duty = Mock()
        self.manager.on_duty = self.on_duty
        self.manager._publish_duties = self.publish_duties = MutableArgsMock()
        self.manager.calendar.day_by_type.day_by_type = self.DAY_BY_TYPE_2018_DEC_2019_JAN
        self.manager.calendar.day_by_type.cache = Mock()
        self.manager.today = Mock(return_value=self.TODAY)

    def generate(self, generate_from, days, type, length, week_align, override):
        self.init_test()
        self.manager.generate(generate_from, days, type, length, week_align, self.TEAM,
                              self.ABC_SERVICE, self.C1, self.C2, override)

    def check_uniqueness(self, duties):
        for day, day_duty in duties.items():
            self.assertEqual(2, len(set(day_duty.values())))  # all members in one day are unique and there are 2 of them

    def check_teamness(self, duties):
        for day, day_duty in duties.items():
            self.assertTrue(all(x in self.team for x in day_duty.values()))  # all members are from the team

    def check_no_person_on_vacation(self, duties, gaps):
        for day, day_duty in duties.items():
            date = datetime(day.year, day.month, day.day)
            for person in day_duty.values():
                self.assertFalse(gaps(person, date, date + timedelta(days=1)))

    def check_no_empty_days(self, duties, old_duties, generate_from, days, type, length, week_align, override):
        all_duties = copy.deepcopy(old_duties) if not override else {}
        all_duties.update(duties)
        period = set(generate_from + timedelta(days=d) for d in range(days))

        def complies_with_type(day_type, duty_type):
            return not (day_type == 'weekday') ^ (duty_type == WORKDAYS)

        period = set(d for d in period if complies_with_type(self.DAY_BY_TYPE_2018_DEC_2019_JAN[d], type))
        self.assertTrue(len(period) > 0)
        self.assertLessEqual(period, set(all_duties.keys()))

    def check_no_consecutive_people_on_duty(self, duties, old_duties, generate_from, days, type, length, week_align, override):
        first_day = generate_from
        if week_align and not length % 7:
            first_day -= timedelta(days=first_day.weekday())

        people_on_duty = {}
        for period in range(days // length + 1):
            duty_pairs = [list(duties.get(first_day + timedelta(days=d), {}).values())
                          for d in range(period * length, (period + 1) * length)]
            people_on_duty[period] = set([person for duty_pair in duty_pairs for person in duty_pair])
        for period in range(days // length):
            self.assertEqual(people_on_duty[period] & people_on_duty[period + 1], set())

    def simple_test(self, generate_kwargs, gap=None, check_consecutive=True):
        self.team = self.TEAM
        self.dutycoins_cache = {
            p: {DutyRank.PRIMARY: 0, DutyRank.SECONDARY: 0, LAST_UPDATE: date(2018, 11, 15)}
            for p in self.team
        }
        self.on_duty = {}
        self.gap = gap if gap else Mock(return_value=[])
        self.generate(**generate_kwargs)
        self.save_dutycoins.assert_called_once_with(
            self.ABC_SERVICE,
            {p: {DutyRank.PRIMARY: 0, DutyRank.SECONDARY: 0, LAST_UPDATE: self.TODAY} for p in self.team}
        )
        duties = self.publish_duties.call_args[0][0]
        self.check_uniqueness(duties)
        self.check_teamness(duties)
        self.check_no_empty_days(duties, {}, **generate_kwargs)
        if check_consecutive:
            self.check_no_consecutive_people_on_duty(duties, {}, **generate_kwargs)

    def test_simple(self):
        self.simple_test({'generate_from': self.TODAY, 'days': 28, 'type': WORKDAYS, 'length': 7,
                          'week_align': True, 'override': False})

    def test_simple_with_all_gaps(self):
        def gaps(person, since, till):
            if since.date() >= self.TODAY and till.date() <= self.TODAY + timedelta(days=2):
                return [{'work_in_absence': False}]
            return []
        self.simple_test({'generate_from': self.TODAY, 'days': 28, 'type': WORKDAYS, 'length': 7,
                          'week_align': True, 'override': False}, gap=gaps, check_consecutive=False)

    def test_simple_weekend(self):
        self.simple_test({'generate_from': self.TODAY, 'days': 28, 'type': NONWORKDAYS, 'length': 1,
                          'week_align': False, 'override': False})

    def test_simple_week_align(self):
        self.simple_test({'generate_from': self.TODAY + timedelta(days=2), 'days': 28, 'type': WORKDAYS, 'length': 7,
                          'week_align': True, 'override': False})

    def test_minimum(self):
        self.team = self.TEAM
        self.dutycoins_cache = {
            p: {DutyRank.PRIMARY: 5, DutyRank.SECONDARY: 5, LAST_UPDATE: date(2018, 11, 15)}
            for p in self.team
        }
        self.dutycoins_cache['imseleznev'][DutyRank.PRIMARY] = 0
        self.dutycoins_cache['khrolenko'][DutyRank.SECONDARY] = 0
        self.dutycoins_cache['romankh'][DutyRank.PRIMARY] = 1
        self.dutycoins_cache['ykirpichev'][DutyRank.SECONDARY] = 1
        self.on_duty = {}
        self.gap = Mock(return_value=[])
        generate_kwargs = {'generate_from': self.TODAY, 'days': 28, 'type': WORKDAYS, 'length': 7,
                           'week_align': True, 'override': False}
        self.generate(**generate_kwargs)
        duties = self.publish_duties.call_args[0][0]
        self.check_uniqueness(duties)
        self.check_teamness(duties)
        self.check_no_empty_days(duties, {}, **generate_kwargs)
        self.check_no_consecutive_people_on_duty(duties, {}, **generate_kwargs)
        for day in [self.TODAY + timedelta(days=d) for d in range(5)]:
            self.assertEqual(duties[day][DutyRank.PRIMARY], 'imseleznev')
            self.assertEqual(duties[day][DutyRank.SECONDARY], 'khrolenko')
        for day in [self.TODAY + timedelta(days=d) for d in range(7, 7 + 5)]:
            self.assertEqual(duties[day][DutyRank.PRIMARY], 'romankh')
            self.assertEqual(duties[day][DutyRank.SECONDARY], 'ykirpichev')

    def test_minimum_and_gaps(self):
        self.team = self.TEAM
        self.dutycoins_cache = {
            p: {DutyRank.PRIMARY: 5, DutyRank.SECONDARY: 5, LAST_UPDATE: date(2018, 11, 15)}
            for p in self.team
        }
        self.dutycoins_cache['imseleznev'][DutyRank.PRIMARY] = 0
        self.dutycoins_cache['khrolenko'][DutyRank.SECONDARY] = 0
        self.dutycoins_cache['romankh'][DutyRank.PRIMARY] = 1
        self.dutycoins_cache['ykirpichev'][DutyRank.SECONDARY] = 1
        self.on_duty = {}

        def gaps(person, since, till):
            if since.date() >= self.TODAY and till.date() <= self.TODAY + timedelta(days=2):
                if person in ['imseleznev', 'khrolenko']:
                    return [{'work_in_absence': False}]
            return []

        self.gap = gaps
        generate_kwargs = {'generate_from': self.TODAY, 'days': 28, 'type': WORKDAYS, 'length': 7,
                           'week_align': True, 'override': False}
        self.generate(**generate_kwargs)
        duties = self.publish_duties.call_args[0][0]
        self.check_uniqueness(duties)
        self.check_teamness(duties)
        self.check_no_empty_days(duties, {}, **generate_kwargs)
        self.check_no_consecutive_people_on_duty(duties, {}, **generate_kwargs)
        self.check_no_person_on_vacation(duties, gaps)
        for day in [self.TODAY + timedelta(days=d) for d in range(5)]:
            self.assertEqual(duties[day][DutyRank.PRIMARY], 'romankh')
            self.assertEqual(duties[day][DutyRank.SECONDARY], 'ykirpichev')
        for day in [self.TODAY + timedelta(days=d) for d in range(7, 7 + 5)]:
            self.assertEqual(duties[day][DutyRank.PRIMARY], 'imseleznev')
            self.assertEqual(duties[day][DutyRank.SECONDARY], 'khrolenko')

    def test_already_assigned(self):
        self.team = self.TEAM
        self.dutycoins_cache = {
            p: {DutyRank.PRIMARY: 5, DutyRank.SECONDARY: 5, LAST_UPDATE: date(2018, 11, 15)}
            for p in self.team
        }
        self.dutycoins_cache['comradeandrew'][DutyRank.PRIMARY] = 0
        self.dutycoins_cache['comradeandrew'][DutyRank.SECONDARY] = 0
        already_assigned = {}
        today = datetime(self.TODAY.year, self.TODAY.month, self.TODAY.day)
        for hour in range(9, 9 + 24):
            already_assigned[today - timedelta(days=1) + timedelta(hours=hour)] = {DutyRank.PRIMARY: 'khrolenko',
                                                                                   DutyRank.SECONDARY: 'imseleznev'}
            already_assigned[today + timedelta(hours=hour)] = {DutyRank.PRIMARY: 'imseleznev', DutyRank.SECONDARY: 'alexey-savin'}
            already_assigned[today + timedelta(days=1) + timedelta(hours=hour)] = {DutyRank.PRIMARY: 'khrolenko',
                                                                                   DutyRank.SECONDARY: 'imseleznev'}
            already_assigned[today + timedelta(days=2) + timedelta(hours=hour)] = {DutyRank.SECONDARY: 'comradeandrew'}
            already_assigned[today + timedelta(days=3) + timedelta(hours=hour)] = {DutyRank.PRIMARY: 'comradeandrew'}

        self.on_duty = copy.deepcopy(already_assigned)
        self.gap = Mock(return_value=[])
        generate_kwargs = {'generate_from': self.TODAY, 'days': 28, 'type': WORKDAYS, 'length': 7,
                           'week_align': True, 'override': False}
        self.generate(**generate_kwargs)
        dutycoins_should_be_today = {
            p: {DutyRank.PRIMARY: 5, DutyRank.SECONDARY: 5, LAST_UPDATE: self.TODAY}
            for p in self.team
        }
        dutycoins_should_be_today['khrolenko'][DutyRank.PRIMARY] = 5 + (24 - 9) / 24.
        dutycoins_should_be_today['imseleznev'][DutyRank.SECONDARY] = 5 + (24 - 9) / 24.
        dutycoins_should_be_today['comradeandrew'][DutyRank.PRIMARY] = 0
        dutycoins_should_be_today['comradeandrew'][DutyRank.SECONDARY] = 0
        self.save_dutycoins.assert_called_once_with(self.ABC_SERVICE, dutycoins_should_be_today)
        duties = self.publish_duties.call_args[0][0]
        self.check_uniqueness(duties)
        self.check_teamness(duties)
        already_assigned_by_days = {(t - timedelta(hours=9)).date(): d for t, d in already_assigned.items()}
        self.check_no_empty_days(duties, already_assigned_by_days, **generate_kwargs)
        days_when_both_ranks_were_assigned = set([day for day, duty in already_assigned_by_days.items() if len(set(duty.values())) > 1])
        self.assertEqual(days_when_both_ranks_were_assigned & set(duties.keys()), set())

    def test_consecutive_is_better_than_on_vacation(self):
        self.team = self.TEAM
        self.dutycoins_cache = {
            p: {DutyRank.PRIMARY: 5, DutyRank.SECONDARY: 5, LAST_UPDATE: date(2018, 11, 15)}
            for p in self.team
        }
        self.dutycoins_cache['imseleznev'][DutyRank.PRIMARY] -= 5
        self.dutycoins_cache['khrolenko'][DutyRank.SECONDARY] -= 5
        self.dutycoins_cache['romankh'][DutyRank.PRIMARY] -= 1
        self.dutycoins_cache['ykirpichev'][DutyRank.SECONDARY] -= 1
        self.on_duty = {}

        def gaps(person, since, till):
            if since.date() >= self.TODAY and till.date() <= self.TODAY + timedelta(days=10):
                if person in ['khrolenko']:
                    return [{'work_in_absence': False}]
            if since.date() >= self.TODAY and till.date() <= self.TODAY + timedelta(days=14):
                if person in set(self.TEAM) - set(['ykirpichev', 'romankh', 'khrolenko']):
                    return [{'work_in_absence': False}]
            return []

        self.gap = gaps
        generate_kwargs = {'generate_from': self.TODAY, 'days': 28, 'type': WORKDAYS, 'length': 7,
                           'week_align': True, 'override': False}
        self.generate(**generate_kwargs)
        duties = self.publish_duties.call_args[0][0]
        self.check_uniqueness(duties)
        self.check_teamness(duties)
        self.check_no_empty_days(duties, {}, **generate_kwargs)
        self.check_no_person_on_vacation(duties, gaps)
