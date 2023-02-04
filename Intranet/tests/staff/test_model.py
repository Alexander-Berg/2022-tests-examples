# -*- coding: utf-8 -*-
from django.test import TestCase

from events.staff.factories import StaffGroupFactory


class TestStaffGroup(TestCase):
    fixtures = ['initial_data.json']

    def test_get_info_url_department(self):
        group = StaffGroupFactory(url='yandex', type='department')
        expected = 'https://staff.yandex-team.ru/departments/yandex/'
        self.assertEqual(group.get_info_url(), expected)

    def test_get_info_url_service(self):
        group = StaffGroupFactory(url='svc_form', type='service')
        expected = 'https://abc.yandex-team.ru/services/form'
        self.assertEqual(group.get_info_url(), expected)

    def test_get_info_url_servicerole(self):
        group = StaffGroupFactory(
            url='svc_form_development',
            type='servicerole',
            role_scope='development',
        )
        expected = 'https://abc.yandex-team.ru/services/form?scope=development'
        self.assertEqual(group.get_info_url(), expected)

    def test_get_info_url_wiki(self):
        group = StaffGroupFactory(url='job_taxi', type='wiki')
        expected = 'https://staff.yandex-team.ru/groups/job_taxi'
        self.assertEqual(group.get_info_url(), expected)
