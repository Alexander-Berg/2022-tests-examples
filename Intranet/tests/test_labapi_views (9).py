from django.conf import settings
from django.db.models import Max

from rest_framework import status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import UrlNameMixin
from lms.users.tests.factories import LabUserFactory, UserFactory

from ..models import StaffGroup
from .factories import (
    StaffCityFactory, StaffCountryFactory, StaffGroupFactory, StaffOfficeFactory, UserWithStaffProfileFactory,
)

LABAPI_BASE_URL = settings.LABAPI_BASE_URL


class LabStaffLanguagesListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:staff-language-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.user.staffprofile.language_native = 'ru'
        self.user.staffprofile.save()

        self.user_profile_en = UserWithStaffProfileFactory(staffprofile__language_native='en')
        self.user_profile_en_again = UserWithStaffProfileFactory(staffprofile__language_native='en')

        self.user_profile_empty = UserWithStaffProfileFactory(staffprofile__language_native='')

    def test_url(self):
        self.assertURLNameEqual('staff/languages/', base_url=LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        results = response.data
        expected = [
            {'name': 'en'},
            {'name': 'ru'},
        ]
        self.assertEqual(results, expected)


class LabStaffCountryListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:staff-country-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.countries = StaffCountryFactory.create_batch(5)

    def test_url(self):
        self.assertURLNameEqual('staff/countries/', base_url=LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        results = response.data
        expected = [
            {
                'id': country.id,
                'code': country.code,
                'name': country.name,
            } for country in self.countries
        ]

        self.assertListEqual(results, expected)


class LabStaffCityListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:staff-city-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.cities = StaffCityFactory.create_batch(5)

    def test_url(self):
        self.assertURLNameEqual('staff/cities/', base_url=LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        results = response.data
        expected = [
            {
                'id': city.id,
                'country_id': city.country_id,
                'name': city.name,
            } for city in self.cities
        ]
        self.assertListEqual(results, expected)


class LabStaffOfficeListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:staff-office-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.offices = StaffOfficeFactory.create_batch(5)

    def test_url(self):
        self.assertURLNameEqual('staff/offices/', base_url=LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        results = response.data
        expected = [
            {
                'id': office.id,
                'city_id': office.city_id,
                'code': office.code,
                'name': office.name,
            } for office in self.offices
        ]
        self.assertListEqual(results, expected)


class LabStaffGroupListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:staff-department-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.staff_groups = StaffGroupFactory.create_batch(5)

    def test_url(self):
        self.assertURLNameEqual('staff/departments/', base_url=LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(6):
            response = self.client.get(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        response_data = response.data
        expected = [
            {
                'id': staff_group.id,
                'name': staff_group.name,
                'level': staff_group.level,
            } for staff_group in self.staff_groups
        ]
        self.assertEqual(response_data['count'], len(self.staff_groups))
        self.assertIsNone(response_data['next'])
        self.assertIsNone(response_data['previous'])
        self.assertListEqual(response_data['results'], expected)


class LabStaffGroupDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:staff-department-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.staff_group = StaffGroupFactory()

    def test_url(self):
        self.assertURLNameEqual('staff/departments/{pk}/', kwargs={'pk': 1}, base_url=LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(0), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_not_found(self):
        self.client.force_login(user=self.user)

        max_staff_group_id = StaffGroup.objects.aggregate(max_id=Max('id')).get('max_id', 0) or 0
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(max_staff_group_id + 1), format='json')

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        response_data = response.data
        self.assertEqual('not_found', response_data['detail'].code)

    def test_detail(self):
        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(self.staff_group.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        response_data = response.data
        expected = {
            'id': self.staff_group.id,
            'name': self.staff_group.name,
            'level': self.staff_group.level,
        }

        self.assertDictEqual(response_data, expected)
