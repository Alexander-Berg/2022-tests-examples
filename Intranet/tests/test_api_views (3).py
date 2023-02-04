from typing import Iterable

from guardian.shortcuts import assign_perm

from django.conf import settings
from django.contrib.auth.models import Permission
from django.db.models import Max

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.enrollments.tests.factories import EnrolledUserFactory
from lms.moduletypes.models import ModuleType
from lms.staff.tests.factories import StaffCityFactory, StaffOfficeFactory, UserWithStaffProfileFactory
from lms.tags.tests.factories import TagFactory
from lms.users.tests.factories import GroupFactory, PermissionPresetFactory, UserFactory
from lms.utils.tests.test_decorators import parameterized_expand_doc

from ..models import Course, CourseBlock, CourseGroup, CourseModule, CourseStudent, LinkedCourse
from .factories import (
    CourseBlockFactory, CourseCategoryFactory, CourseCityFactory, CourseFactory, CourseGroupFactory,
    CourseModuleFactory, CourseStudentFactory, CourseTeamFactory, CourseVisibilityFactory, LinkedCourseFactory,
    ProviderFactory, StudentModuleProgressFactory, StudyModeFactory, TutorInternalFactory,
)


class StudyModeListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:studymode-list'

    def setUp(self) -> None:
        self.study_modes = StudyModeFactory.create_batch(2)
        self.inactive_mode = StudyModeFactory(is_active=False)

    def build_expected(self, studymodes):
        return [{
            'id': sm.id,
            'slug': sm.slug,
            'name': sm.name,
        } for sm in studymodes]

    def test_url(self):
        self.assertURLNameEqual('studymodes/', base_url=settings.API_BASE_URL)

    def test_list(self):
        expected = self.build_expected(self.study_modes)
        self.list_request(self.get_url(), expected=expected, pagination=False, num_queries=1)


class ProviderListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:provider-list'

    def setUp(self) -> None:
        self.providers = ProviderFactory.create_batch(5)
        self.inactive = ProviderFactory.create_batch(2, is_active=False)

    def test_url(self):
        self.assertURLNameEqual('providers/', base_url=settings.API_BASE_URL)

    def build_expected(self, providers):
        return [
            {
                'id': p.id,
                'name': p.name
            } for p in sorted(providers, key=lambda x: x.name)
        ]

    def test_list(self):
        expected = self.build_expected(self.providers)
        self.list_request(self.get_url(), expected=expected, pagination=False, num_queries=1)


class CourseCityListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-city-list'

    def setUp(self) -> None:
        self.cities = CourseCityFactory.create_batch(5)
        self.inactive = CourseCityFactory.create_batch(2, is_active=False)

    def test_url(self):
        self.assertURLNameEqual('course_cities/', base_url=settings.API_BASE_URL)

    def build_expected(self, cities):
        return [
            {
                'id': c.id,
                'name': c.name,
                'slug': c.slug,
            } for c in sorted(cities, key=lambda x: x.name)
        ]

    def test_list(self):
        expected = self.build_expected(self.cities)
        self.list_request(self.get_url(), expected=expected, pagination=False, num_queries=1)


class CourseCategoryListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-category-list'

    def setUp(self) -> None:
        self.categories = CourseCategoryFactory.create_batch(5)
        self.inactive_categories = CourseCategoryFactory.create_batch(2, is_active=False)

    def test_url(self):
        self.assertURLNameEqual('course_categories/', base_url=settings.API_BASE_URL)

    def build_expected(self, categories):
        return [
            {
                'id': c.id,
                'parent_id': c.parent_id,
                'slug': c.slug,
                'name': c.name,
                'depth': c.node_depth,
                'color_theme': {
                    'id': c.color_theme.id,
                    'name': c.color_theme.name,
                    'slug': c.color_theme.slug,
                    'is_active': c.color_theme.is_active,
                    'created': serializers.DateTimeField().to_representation(c.color_theme.created),
                    'modified': serializers.DateTimeField().to_representation(c.color_theme.modified),
                    'course_card_gradient_color': c.color_theme.course_card_gradient_color,
                },
            } for c in sorted(categories, key=lambda x: x.id)  # реальная сортировка идет по node__path
        ]

    def test_list(self):
        expected = self.build_expected(self.categories)
        self.list_request(self.get_url(), expected=expected, num_queries=2)


class CourseCategoryDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-category-detail'

    def setUp(self) -> None:
        self.category = CourseCategoryFactory()

    def test_url(self):
        self.assertURLNameEqual('course_categories/{}/', args=(self.category.id,), base_url=settings.API_BASE_URL)

    def build_expected(self, category):
        return {
            'id': category.id,
            'parent_id': category.parent_id,
            'slug': category.slug,
            'name': category.name,
            'depth': category.node_depth,
            'color_theme': {
                'id': category.color_theme.id,
                'slug': category.color_theme.slug,
                'name': category.color_theme.name,
                'course_card_gradient_color': category.color_theme.course_card_gradient_color,
            },
        }

    def test_get(self):
        expected = self.build_expected(self.category)
        self.detail_request(self.get_url(self.category.id), expected=expected, num_queries=1)


class CourseCategoriesListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-categories'

    def setUp(self) -> None:
        self.course = CourseFactory(is_active=True)
        self.categories = CourseCategoryFactory.create_batch(5)
        self.inactive_categories = CourseCategoryFactory.create_batch(2, is_active=False)
        self.course.categories.add(*(self.categories + self.inactive_categories))

    def test_url(self):
        self.assertURLNameEqual('courses/{}/categories/', args=(self.course.id,), base_url=settings.API_BASE_URL)

    def build_expected(self, categories):
        return [
            {
                'id': c.id,
                'parent_id': c.parent_id,
                'slug': c.slug,
                'name': c.name,
                'depth': c.node_depth,
                'color_theme': {
                    'id': c.color_theme.id,
                    'name': c.color_theme.name,
                    'slug': c.color_theme.slug,
                    'is_active': c.color_theme.is_active,
                    'created': serializers.DateTimeField().to_representation(c.color_theme.created),
                    'modified': serializers.DateTimeField().to_representation(c.color_theme.modified),
                    'course_card_gradient_color': c.color_theme.course_card_gradient_color,
                },
            } for c in sorted(categories, key=lambda x: x.name)
        ]

    def test_list(self):
        url = self.get_url(self.course.id)
        expected = self.build_expected(self.categories)
        self.list_request(url, expected, num_queries=3)


class CourseListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.author = UserFactory(is_staff=True)
        self.courses = CourseFactory.create_batch(10, is_active=True)
        self.inactive_courses = CourseFactory.create_batch(2, is_active=False)
        self.tags = TagFactory.create_batch(10)
        for i, course in enumerate(self.courses[:5]):
            course.tags.set(self.tags[i: i + 2])

    def build_expected(self, courses):
        return [
            {
                'id': c.id,
                'slug': c.slug,
                'name': c.name,
                'shortname': c.shortname,
                'summary': c.summary,
                'city_id': c.city_id,
                'study_mode_id': c.study_mode_id,
                'image_url': c.image_url,
                'available_for_enroll': c.available_for_enroll,
                'is_full': c.is_full,
                'structure': c.structure,
                'format': c.format,
                'author': {
                    'id': c.author.id,
                    'username': c.author.username,
                    'first_name': c.author.first_name,
                    'last_name': c.author.last_name,
                },
                'categories': [cc.id for cc in c.categories.all()],
                'begin_date': c.begin_date,
                'end_date': c.end_date,
                'enroll_begin': c.enroll_begin,
                'enroll_end': c.enroll_end,
                'payment_method': c.payment_method,
                'paid_percent': c.paid_percent,
                'payment_terms': c.payment_terms,
                'price': c.price,
                'num_hours': c.num_hours,
                'status': c.status,
                'type': c.course_type,
                'tags': list(c.tags.values_list('name', flat=True)),
                'enable_groups': c.enable_groups,
                'created': serializers.DateTimeField().to_representation(c.created),
                'modified': serializers.DateTimeField().to_representation(c.modified),
            } for c in sorted(self.courses, key=lambda x: x.name)
        ]

    def test_url(self):
        self.assertURLNameEqual('courses/', base_url=settings.API_BASE_URL)

    def test_non_auth(self):
        response = self.client.get(self.get_url(), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)
        url = self.get_url()
        expected = self.build_expected(self.courses)
        self.list_request(url, expected, num_queries=10)

    def test_list_with_groups(self):
        for c in self.courses:
            CourseGroupFactory.create_batch(5, course=c)
            c.refresh_from_db()

        self.client.force_login(user=self.user)
        url = self.get_url()
        expected = self.build_expected(self.courses)
        self.list_request(url, expected, num_queries=10)


class CourseWithVisibilityListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-list'

    def setUp(self) -> None:
        self.author = UserFactory(is_staff=True)
        self.courses_with_no_visibility_rules = CourseFactory.create_batch(4, is_active=True)
        self.inactive_courses = CourseFactory.create_batch(2, is_active=False)
        self.city = StaffCityFactory()
        self.office = StaffOfficeFactory(city=self.city)
        self.user = UserWithStaffProfileFactory(staffprofile__office=self.office)
        self.available_courses = CourseFactory.create_batch(3, is_active=True)
        [
            CourseVisibilityFactory(course=course, rules={"eq": ["staff_city", self.city.id]})
            for course in self.available_courses
        ]
        self.unavailable_courses = CourseFactory.create_batch(3, is_active=True)
        [
            CourseVisibilityFactory(course=course, rules={"ne": ["staff_city", self.city.id]})
            for course in self.unavailable_courses
        ]

    def build_expected(self, courses):
        return [
            {
                'id': c.id,
                'slug': c.slug,
                'name': c.name,
                'shortname': c.shortname,
                'summary': c.summary,
                'city_id': c.city_id,
                'study_mode_id': c.study_mode_id,
                'image_url': c.image_url,
                'available_for_enroll': c.available_for_enroll,
                'is_full': c.is_full,
                'structure': c.structure,
                'format': c.format,
                'author': {
                    'id': c.author.id,
                    'username': c.author.username,
                    'first_name': c.author.first_name,
                    'last_name': c.author.last_name,
                },
                'categories': [cc.id for cc in c.categories.all()],
                'begin_date': c.begin_date,
                'end_date': c.end_date,
                'enroll_begin': c.enroll_begin,
                'enroll_end': c.enroll_end,
                'payment_method': c.payment_method,
                'paid_percent': c.paid_percent,
                'payment_terms': c.payment_terms,
                'price': c.price,
                'num_hours': c.num_hours,
                'status': c.status,
                'type': c.course_type,
                'tags': list(c.tags.values_list('name', flat=True)),
                'enable_groups': c.enable_groups,
                'created': serializers.DateTimeField().to_representation(c.created),
                'modified': serializers.DateTimeField().to_representation(c.modified),
            } for c in sorted(courses, key=lambda x: x.name)
        ]

    def test_list_with_visibilities(self):
        self.client.force_login(user=self.user)
        url = self.get_url()
        expected_courses = self.courses_with_no_visibility_rules + self.available_courses
        expected = self.build_expected(expected_courses)
        self.list_request(url, expected, num_queries=11)

    def test_list_with_view_permission(self):
        assign_perm("courses.view_course", self.user)
        self.client.force_login(user=self.user)
        url = self.get_url()
        expected_courses = self.courses_with_no_visibility_rules + self.available_courses + self.unavailable_courses
        expected = self.build_expected(expected_courses)
        self.list_request(url, expected, num_queries=9)


class CourseDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-detail'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.author = UserFactory()
        self.course = CourseFactory(author=self.author, is_active=True)
        self.inactive_courses = CourseFactory.create_batch(2, is_active=False)
        self.tags = TagFactory.create_batch(5)
        self.course.tags.set(self.tags[:3])

    def build_expected(self, course):
        return {
            'id': course.id,
            'slug': course.slug,
            'name': course.name,
            'shortname': course.shortname,
            'summary': course.summary,
            'description': course.description,
            'city_id': course.city_id,
            'study_mode_id': course.study_mode_id,
            'provider_id': course.provider_id,
            'author': {
                'id': course.author.id,
                'username': course.author.username,
                'first_name': course.author.first_name,
                'last_name': course.author.last_name,
            },
            'image_url': course.image_url,
            'structure': course.structure,
            'format': course.format,
            'categories': sorted(category.id for category in course.categories.all()),
            'available_for_enroll': course.available_for_enroll,
            'is_full': course.is_full,
            'begin_date': serializers.DateTimeField().to_representation(course.begin_date),
            'end_date': serializers.DateTimeField().to_representation(course.end_date),
            'enroll_begin': serializers.DateTimeField().to_representation(course.enroll_begin),
            'enroll_end': serializers.DateTimeField().to_representation(course.enroll_end),
            'payment_method': course.payment_method,
            'paid_percent': course.paid_percent,
            'price': course.price,
            'payment_terms': course.payment_terms,
            'num_hours': course.num_hours,
            'status': course.status,
            'enable_groups': course.enable_groups,
            'enable_followers': course.enable_followers,
            'enrollments_only': course.enrollments_only,
            'multi_enrollments': course.multi_enrollments,
            'retries_allowed': course.retries_allowed,
            'is_enroll_open': course.is_enroll_open,
            'enroll_will_begin': course.enroll_will_begin,
            'has_open_seats': course.has_open_seats,
            'type': course.course_type,
            'tags': list(course.tags.values_list('name', flat=True)),
            'created': serializers.DateTimeField().to_representation(course.created),
            'modified': serializers.DateTimeField().to_representation(course.modified),
        }

    def test_url(self):
        self.assertURLNameEqual('courses/{}/', args=(1,), base_url=settings.API_BASE_URL)

    def test_non_auth(self):
        response = self.client.get(self.get_url(self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_get(self):
        self.client.force_login(user=self.user)
        url = self.get_url(self.course.id)
        expected = self.build_expected(self.course)
        self.detail_request(url, expected=expected, num_queries=5)

    def test_get_with_groups(self):
        CourseGroupFactory.create_batch(10, course=self.course)
        self.course.refresh_from_db()

        self.client.force_login(user=self.user)
        url = self.get_url(self.course.id)
        expected = self.build_expected(self.course)
        self.detail_request(url, expected=expected, num_queries=10)


class CourseGroupListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'api:course-groups'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)
        self.course_groups: Iterable[CourseGroup] = CourseGroupFactory.create_batch(4, course=self.course)
        self.inactive_groups = CourseGroupFactory.create_batch(2, course=self.course, is_active=False)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/groups/', args=(1,), base_url=settings.API_BASE_URL)

    def test_non_auth(self):
        course = CourseFactory(author=self.user, is_active=True)
        response = self.client.get(self.get_url(course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_no_course(self):
        self.client.force_login(user=self.user)
        max_course_id = Course.objects.aggregate(max_id=Max('id')).get('max_id', 0) or 0
        with self.assertNumQueries(3):
            response = self.client.get(self.get_url(max_course_id + 1), format='json')

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        response_data = response.data
        self.assertEqual('not_found', response_data['detail'].code)

    @parameterized_expand_doc(
        [
            (False, False, False, False, False, False, 3, False),
            (False, False, True, False, False, False, 3, False),
            (False, True, False, False, False, False, 5, False),
            (False, True, True, False, False, False, 7, True),
            (True, False, False, False, False, False, 5, True),
            (True, False, True, False, False, False, 5, True),
            (True, True, False, False, False, False, 7, True),
            (True, True, True, False, False, False, 7, True),
            (False, False, False, True, False, False, 3, False),
            (False, False, False, False, True, False, 3, False),
            (False, False, False, False, False, True, 3, False),
            (False, True, False, True, False, False, 7, True),
            (False, True, False, False, True, False, 7, True),
            (False, True, False, False, False, True, 7, True),
        ]
    )
    def test_list(
        self,
        is_active,
        is_preview_mode,
        user_is_author,
        user_has_permisssion,
        user_group_has_permission,
        user_in_course_team,
        num_queries,
        has_result
    ):
        course = CourseFactory(author=self.user if user_is_author else UserFactory(), is_active=is_active)
        course_groups: Iterable[CourseGroup] = CourseGroupFactory.create_batch(4, course=course)
        CourseGroupFactory.create_batch(2, course=course, is_active=False)
        if user_has_permisssion:
            assign_perm('courses.view_course', self.user, course)
        if user_group_has_permission:
            group = GroupFactory()
            group.user_set.add(self.user)
            assign_perm('courses.view_course', group, course)
        if user_in_course_team:
            preset = PermissionPresetFactory()
            permission = Permission.objects.get(codename='view_course')
            preset.permissions.add(permission)
            course_team = CourseTeamFactory(permission_preset=preset)
            course_team.add_user(self.user.id)
            course.teams.add(course_team)
            self.assertTrue(self.user.has_perm('courses.view_course', course))
        url = self.get_url(course.id)
        if is_preview_mode:
            url += '?preview=true'
        self.client.force_login(user=self.user)
        with self.assertNumQueries(num_queries):
            response = self.client.get(url, format='json')

        expected_status = status.HTTP_200_OK if has_result else status.HTTP_404_NOT_FOUND
        self.assertEqual(response.status_code, expected_status)

        if has_result:
            self.assertEqual(response.data.keys(), {'count', 'next', 'previous', 'results'})
            self.assertEqual(response.data.get('count'), 4)
            results = response.data.get('results')

            expected = [{
                'id': cg.id,
                'name': cg.name,
                'summary': cg.summary,
                'can_join': cg.can_join,
                'num_participants': cg.num_participants,
                'max_participants': cg.max_participants,
                'available_for_enroll': cg.available_for_enroll,
                'is_enroll_open': cg.is_enroll_open,
                'enroll_will_begin': cg.enroll_will_begin,
                'has_open_seats': cg.has_open_seats,
                'is_full': cg.is_full,
                'begin_date': cg.begin_date,
                'end_date': cg.end_date,
                'enroll_begin': cg.enroll_begin,
                'enroll_end': cg.enroll_end,
                'tutor': None,
            } for cg in sorted(course_groups, key=lambda cg: cg.name)]

            self.assertEqual(results, expected, msg=results)

    def test_filter_is_full(self):
        group = CourseGroupFactory(course=self.course, max_participants=3)
        EnrolledUserFactory.create_batch(3, course=self.course, group=group)

        self.client.force_login(user=self.user)
        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(self.course.id), data={'is_full': False}, format='json')

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data.get('count'), 4)


class CourseModuleListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-modules'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)
        self.course_modules: Iterable[CourseModule] = CourseModuleFactory.create_batch(5, course=self.course)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/modules/', args=(1,), base_url=settings.API_BASE_URL)

    def build_expected(self, course_modules):
        return [{
            'id': m.id,
            'module_type': {
                'name': m.module_type.name,
                'app_label': m.module_type.app_label,
                'model': m.module_type.model,
            },
            'name': m.name,
            'description': m.description,
            'estimated_time': m.estimated_time,
            'block_id': None,
        } for m in course_modules]

    def test_non_auth(self):
        course = CourseFactory(author=self.user, is_active=True)
        response = self.client.get(self.get_url(course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        self.client.force_login(user=self.user)
        url = self.get_url(self.course.id)
        expected = self.build_expected(self.course_modules)
        self.list_request(url, expected, num_queries=4)


class CourseModuleDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-module-detail'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.course = CourseFactory(is_active=True)
        self.course_module = CourseModuleFactory(course=self.course)

    def test_url(self):
        self.assertURLNameEqual('course_modules/{}/', args=(1,), base_url=settings.API_BASE_URL)

    def build_expected(self, module: CourseModule) -> dict:
        return {
            'id': module.id,
            'module_type': {
                'name': module.module_type.name,
                'app_label': module.module_type.app_label,
                'model': module.module_type.model,
            },
            'name': module.name,
            'description': module.description,
            'estimated_time': module.estimated_time,
        }

    def test_non_auth(self):
        course_module = CourseModuleFactory(course=self.course)
        response = self.client.get(self.get_url(course_module.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_detail(self):
        self.client.force_login(user=self.user)
        url = self.get_url(self.course_module.id)
        expected = self.build_expected(self.course_module)
        self.detail_request(url, expected, num_queries=3)


class TutorDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:tutor-detail'

    def setUp(self) -> None:
        self.user = UserFactory()
        self.tutor = TutorInternalFactory()

    def test_url(self):
        self.assertURLNameEqual('tutors/{}/', args=(self.tutor.id,), base_url=settings.API_BASE_URL)

    def test_detail(self):
        expected = {
            'id': self.tutor.id,
            'is_internal': True,
            'user': {
                'id': self.tutor.user.id,
                'username': self.tutor.user.username,
                'first_name': self.tutor.user.first_name,
                'last_name': self.tutor.user.last_name,
            },
            'name': f'{self.tutor.user.first_name} {self.tutor.user.last_name}',
            'email': self.tutor.email,
            'url': f'{settings.STAFF_BASE_URL}/{self.tutor.user.username}',
            'position': self.tutor.position,
        }

        self.client.force_login(user=self.user)
        self.detail_request(url=self.get_url(self.tutor.id), expected=expected, num_queries=3)


class CourseBlockListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-blocks'

    def build_expected(self, course_blocks):
        return [{
            'id': b.id,
            'name': b.name,
            'summary': b.summary,
        } for b in course_blocks]

    def test_url(self):
        self.assertURLNameEqual('courses/{}/blocks/', args=(1,), base_url=settings.API_BASE_URL)

    def test_non_auth(self):
        user = UserFactory()
        course = CourseFactory(author=user, is_active=True)
        response = self.client.get(self.get_url(course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        user = UserFactory()
        self.client.force_login(user=user)
        course = CourseFactory(author=user, is_active=True)
        course_blocks = CourseBlockFactory.create_batch(9, course=course)
        CourseBlockFactory.create_batch(9)
        expected = self.build_expected(course_blocks)
        url = self.get_url(course.id)
        self.list_request(url, expected, num_queries=4)


class CourseBlockDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:course-block-detail'

    def build_expected(self, block: CourseBlock) -> dict:
        return {
            'id': block.id,
            'name': block.name,
            'summary': block.summary,
        }

    def test_url(self):
        self.assertURLNameEqual('course_blocks/{}/', args=(1,), base_url=settings.API_BASE_URL)

    def test_non_auth(self):
        user = UserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_block = CourseBlockFactory(course=course)
        response = self.client.get(self.get_url(course_block.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_detail(self):
        user = UserFactory()
        course = CourseFactory(author=user, is_active=True)
        course_block = CourseBlockFactory(course=course)
        expected = self.build_expected(course_block)
        self.client.force_login(user=user)
        url = self.get_url(course_block.id)
        self.detail_request(url, expected, num_queries=3)


class CourseStudentCompleteTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-course-complete'

    def test_url(self):
        self.assertURLNameEqual('my/courses/{}/complete/', args=(1,), base_url=settings.API_BASE_URL)

    def test_non_auth(self):
        course = CourseFactory()
        response = self.client.put(self.get_url(course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_complete(self):
        course = CourseFactory(
            is_active=True,
            structure=Course.StructureChoices.NO_MODULES,
        )
        student: CourseStudent = CourseStudentFactory(
            course=course,
            status=CourseStudent.StatusChoices.ACTIVE,
        )

        self.client.force_login(user=student.user)
        with self.assertNumQueries(7):
            response = self.client.put(self.get_url(course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)

        student.refresh_from_db()
        self.assertEqual(student.status, CourseStudent.StatusChoices.COMPLETED)

    def test_course_with_modules(self):
        course = CourseFactory(
            is_active=True,
            structure=Course.StructureChoices.MULTI,
        )
        student: CourseStudent = CourseStudentFactory(
            course=course,
            status=CourseStudent.StatusChoices.ACTIVE,
        )
        self.client.force_login(user=student.user)
        with self.assertNumQueries(3):
            response = self.client.put(self.get_url(course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        data = response.data
        self.assertEqual(data[0].code, 'cannot_complete')

    def test_course_not_found(self):
        wrong_course = CourseFactory(is_active=True)
        student = CourseStudentFactory(status=CourseStudent.StatusChoices.ACTIVE)
        self.client.force_login(user=student.user)
        with self.assertNumQueries(3):
            response = self.client.put(self.get_url(wrong_course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_student_already_complete(self):
        course = CourseFactory(
            is_active=True,
            structure=Course.StructureChoices.NO_MODULES,
        )
        student: CourseStudent = CourseStudentFactory(
            course=course,
            status=CourseStudent.StatusChoices.COMPLETED,
        )
        self.client.force_login(user=student.user)
        with self.assertNumQueries(3):
            response = self.client.put(self.get_url(course.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)


class MyCourseModuleListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:my-course-module-list'

    def setUp(self):
        self.course = CourseFactory(is_active=True)
        self.user = UserFactory()

    def test_url(self):
        self.assertURLNameEqual('my/courses/{}/modules/', args=(1,), base_url=settings.API_BASE_URL)

    def test_non_auth(self):
        response = self.client.get(self.get_url(self.course.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_empty_list(self):
        StudentModuleProgressFactory.create_batch(19, course=self.course)
        self.client.force_login(user=self.user)
        self.list_request(self.get_url(self.course.id), check_ids=False, expected=[], num_queries=4)

    def build_expected(self, progresses):
        return [
            {
                'module_id': p.module_id,
                'score': p.score,
            } for p in progresses]

    def test_list(self):
        StudentModuleProgressFactory.create_batch(19, course=self.course)
        student = CourseStudentFactory(course=self.course, user=self.user)
        another_course = CourseFactory(is_active=True)
        another_student = CourseStudentFactory(course=another_course, user=self.user)
        progresses = StudentModuleProgressFactory.create_batch(5, course=self.course, student=student)
        StudentModuleProgressFactory.create_batch(9, course=another_course, student=another_student)
        expected = self.build_expected(progresses)
        self.client.force_login(user=self.user)

        self.list_request(self.get_url(self.course.id), check_ids=False, expected=expected, num_queries=5)

    def test_course_not_found(self):
        not_active_course = CourseFactory()
        self.client.force_login(user=self.user)
        expected_error = {'detail': 'not_found'}

        with self.assertNumQueries(3):
            url = self.get_url(not_active_course.id)
            response = self.client.get(url, format='json')
            self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
            self.assert_errors(response.data, expected_error)

    def test_list_expelled_student_with_progress(self):
        completed_student = CourseStudentFactory(course=self.course, user=self.user)
        StudentModuleProgressFactory.create_batch(5, course=self.course, student=completed_student)
        completed_student.expell()

        CourseStudentFactory(course=self.course, user=self.user)
        self.client.force_login(user=self.user)

        self.list_request(self.get_url(self.course.id), check_ids=False, expected=[], num_queries=4)


class LinkedCourseDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'api:linked-course-detail'

    def setUp(self):
        ModuleType.objects.get_for_model(LinkedCourse)
        self.user = UserFactory()
        self.course = CourseFactory(course_type=Course.TypeChoices.TRACK.value)
        self.linked_course = CourseFactory(course_type=Course.TypeChoices.COURSE.value)
        self.student = CourseStudentFactory(course=self.course, user=self.user)
        self.linked_course_module = LinkedCourseFactory(course=self.course, linked_course=self.linked_course)

    def build_expected(self, linked_course_module: LinkedCourse):
        return {
            "id": linked_course_module.id,
            "linked_course": {
                "id": linked_course_module.linked_course.id,
                "slug": linked_course_module.linked_course.slug,
                "name": linked_course_module.linked_course.name,
                "shortname": linked_course_module.linked_course.shortname,
            },
            "name": linked_course_module.name,
            "description": linked_course_module.description,
            "estimated_time": linked_course_module.estimated_time,
        }

    def test_url(self):
        self.assertURLNameEqual(
            'linkedcourse_modules/{}/',
            args=(self.linked_course_module.id,),
            base_url=settings.API_BASE_URL
        )

    def test_no_auth(self):
        self.detail_request(
            url=self.get_url(self.linked_course_module.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=0
        )

    def test_another_course(self):
        another_course = CourseFactory()
        another_student = CourseStudentFactory(course=another_course)
        self.client.force_login(user=another_student.user)
        self.create_request(
            url=self.get_url(self.linked_course_module.id),
            status_code=status.HTTP_403_FORBIDDEN,
            num_queries=5
        )

    def test_not_exists(self):
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.linked_course_module.id + 1),
            status_code=status.HTTP_404_NOT_FOUND,
            num_queries=3
        )

    def test_get_result(self):
        self.client.force_login(user=self.user)
        self.detail_request(
            url=self.get_url(self.linked_course_module.id),
            expected=self.build_expected(self.linked_course_module),
            num_queries=6
        )
