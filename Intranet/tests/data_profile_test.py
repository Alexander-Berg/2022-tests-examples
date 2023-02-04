from staff.person.models import Staff


from .factories_test import DataProfileFactory, SecurityTestCase


class DataProfileTestCase(SecurityTestCase):

    def test_to_Q_own_department(self):
        dp = DataProfileFactory(
            own_department_only=True,
            chiefed_departments_only=False,
            own_office_only=False,
            office=None,
        )
        q = dp.to_Q(staff=self.batman)
        assert set(
            Staff.objects.filter(q).values_list('login', flat=True)
        ) == {self.batman.login, self.batman_deputy.login, self.staff_13.login}

    def test_to_Q_chiefed_department(self):
        dp = DataProfileFactory(
            own_department_only=False,
            chiefed_departments_only=True,
            own_office_only=False,
            office=None,
        )
        q = dp.to_Q(staff=self.batman)
        assert set(
            Staff.objects.filter(q).values_list('login', flat=True)
        ) == {self.batman.login, self.batman_deputy.login, self.staff_03.login, self.staff_13.login}

    def test_to_Q_department(self):
        dp = DataProfileFactory(
            own_department_only=False,
            chiefed_departments_only=False,
            own_office_only=False,
            department=self.level_01,
            office=None,
        )
        q = dp.to_Q(staff=self.batman)
        assert set(
            Staff.objects.filter(q).values_list('login', flat=True)
        ) == {self.staff_01.login, self.staff_03.login}

    def test_to_Q_own_office(self):
        dp = DataProfileFactory(
            own_department_only=False,
            chiefed_departments_only=False,
            own_office_only=True,
            department=None,
        )
        q = dp.to_Q(staff=self.batman)
        assert set(
            Staff.objects.filter(q).values_list('login', flat=True)
        ) == {self.batman.login, self.batman_deputy.login, self.staff_01.login}

    def test_to_Q_office(self):
        dp = DataProfileFactory(
            own_department_only=False,
            chiefed_departments_only=False,
            own_office_only=False,
            department=None,
            office=self.ekb,
        )
        q = dp.to_Q(staff=self.batman)
        assert set(
            Staff.objects.filter(q).values_list('login', flat=True)
        ) == {self.staff_03.login}

    def test_to_Q_chiefed_department_with_deputy_status(self):
        dp = DataProfileFactory(
            own_department_only=False,
            chiefed_departments_only=True,
            own_office_only=False,
            office=None,
        )
        q = dp.to_Q(staff=self.batman_deputy)
        assert not Staff.objects.filter(q).exists()
