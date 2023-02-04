
# -*- coding: utf-8 -*-

from django.test import TestCase
from django.test.client import Client
from django.core.management import call_command
from libra.users.models import DepartmentStaffCounter, Department, Staff
from django.utils.simplejson import JSONDecoder

class CenterTestCase(TestCase):
    fixtures = ['models.xml',]

    def setUp(self):
        self.client = Client()

    def testStaffCounter(self):
        call_command('countdepstaff')

        dep = Department.objects.get(name='Группа разработки внутренних сервисов')
        staff_counter = DepartmentStaffCounter.objects.filter(department=dep)[0].staff_counter
        self.assertEquals(staff_counter, 1)

        dep = Department.objects.get(name='Служба внутренних сервисов')
        staff_counter = DepartmentStaffCounter.objects.filter(department=dep)[0].staff_counter
        self.assertEquals(staff_counter, 2)

        dep = Department.objects.get(name='Яндекс')
        staff_counter = DepartmentStaffCounter.objects.filter(department=dep)[0].staff_counter
        self.assertEquals(staff_counter, Staff.objects.filter(is_dismissed=False).count())

    def testViewAutocomleteUsers(self):
        # check autocomplete for 'tha'
        response = self.client.get('/autocomplete_users/', {'q': 'tha'})
        self.failUnlessEqual(response.status_code, 200)
        data = JSONDecoder().decode(response.content)
        self.failUnlessEqual(len(data), 1)
        self.failUnlessEqual(data[0]['login_ld'], 'thasonic')

        # check for no dismissed users in autocomplete
        s = Staff.objects.get(login='thasonic')
        s.is_dismissed = True
        s.save()
        response = self.client.get('/autocomplete_users/', {'q': 'tha'})
        data = JSONDecoder().decode(response.content)
        self.assertEquals(data, [])
        s.is_dismissed = False
        s.save()

    def testViewUser(self):
        response = self.client.get('/user/thasonic/', {'fields': 'login|departments|department|office'})
        self.failUnlessEqual(response.status_code, 200)
        data = JSONDecoder().decode(response.content)
        self.failUnlessEqual(data['login'], 'thasonic')
        self.failUnlessEqual(data['department']['name'], u'Группа разработки внутренних сервисов')
        self.failUnlessEqual(data['departments'][4]['name'], u'Группа разработки внутренних сервисов')
        self.failUnlessEqual(data['office']['name'], u'Москва, Красная Роза-1')

    def testViewDismissedUsers(self):
        s = Staff.objects.get(login_ld='thasonic')
        s.is_dismissed = True
        s.save()

        response = self.client.get('/dismissed_users/')
        self.failUnlessEqual(response.status_code, 200)
        data = JSONDecoder().decode(response.content)
        s = Staff.objects.get(wiki_name=data[0]['wiki_name'])
        self.failUnlessEqual(s.is_dismissed, True)
        s = Staff.objects.get(wiki_name=data[-1]['wiki_name'])
        self.failUnlessEqual(s.is_dismissed, True)

        s = Staff.objects.get(login_ld='thasonic')
        s.is_dismissed = False
        s.save()

    def testViewDepartment(self):
        d = Department.objects.get(name=u'Служба внутренних сервисов')
        response = self.client.get('/department/%d/' % d.id, {'fields': 'name|chief|deputy'})
        self.failUnlessEqual(response.status_code, 200)
        data = JSONDecoder().decode(response.content)
        self.failUnlessEqual(data['name'], u'Служба внутренних сервисов')
        self.failUnlessEqual(data['chief']['login_ld'], u'kolomeetz')
        self.failUnlessEqual(data['deputy'], {})

    def testViewUserCards(self):
        response = self.client.get('/user_cards/', {'logins': 'thasonic|kolomeetz'})
        self.failUnlessEqual(response.status_code, 200)
        data = JSONDecoder().decode(response.content[27:-1])
        self.failUnlessEqual(data['thasonic|kolomeetz']['_error'], u'User not found')
        response = self.client.get('/user_cards/', {'logins': 'thasonic,kolomeetz'})
        self.failUnlessEqual(response.status_code, 200)
        data = JSONDecoder().decode(response.content[27:-1])
        self.failUnlessEqual(data['thasonic']['login'], u'thasonic')
        self.failUnlessEqual(data['kolomeetz']['login'], u'kolomeetz')

    def testViewAvatar(self):
        response = self.client.get('/user/avatar/thasonic/')
        self.failUnlessEqual(response.status_code, 200)
        for i in response.items():
            if i[0] == 'Content-Type':
                self.failUnlessEqual(i[1], 'image/jpeg')
        response = self.client.get('/user/avatar/thasonic@yandex-team.ru/70/')
        self.failUnlessEqual(response.status_code, 200)
        for i in response.items():
            if i[0] == 'Content-Type':
                self.failUnlessEqual(i[1], 'image/jpeg')

    def testViewSquareAvatar(self):
        response = self.client.get('/user/avatar/thasonic/square/')
        self.failUnlessEqual(response.status_code, 200)
        for i in response.items():
            if i[0] == 'Content-Type':
                self.failUnlessEqual(i[1], 'image/jpeg')
        response = self.client.get('/user/avatar/thasonic/80/square/')
        self.failUnlessEqual(response.status_code, 200)
        for i in response.items():
            if i[0] == 'Content-Type':
                self.failUnlessEqual(i[1], 'image/jpeg')

    def testSyncProfiles(self):
        response = self.client.get('/sync_profiles/')
        self.failUnlessEqual(response.status_code, 200)

    def testSyncProfilesFields(self):
        response = self.client.get('/sync_profiles/', {'fields': 'login|work_email'})
        self.failUnlessEqual(response.status_code, 200)

    def testSyncProfilesLastModifiedAt(self):
        response = self.client.get('/sync_profiles/', {'last_updated_at': '2009-08-01'})
        self.failUnlessEqual(response.status_code, 200)

