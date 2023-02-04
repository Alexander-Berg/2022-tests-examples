from datetime import timedelta
from typing import Sequence

import faker
from guardian.shortcuts import assign_perm

from django.conf import settings
from django.contrib.auth import get_user_model
from django.utils import timezone
from django.utils.timezone import utc

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.tests.factories import CourseBlockFactory, CourseFactory, CourseGroupFactory
from lms.moduletypes.models import ModuleType
from lms.users.tests.factories import LabUserFactory, UserFactory

from ..models import Classroom, Timeslot
from .factories import ClassroomFactory, StudentSlotFactory, TimeslotFactory

fake = faker.Faker()
User = get_user_model()


class LabClassroomCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-create'

    def setUp(self):
        # fill the cache
        ModuleType.objects.get_for_model(Classroom)
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()

    def test_url(self):
        self.assertURLNameEqual('classroom_modules/', args=(), base_url=settings.LABAPI_BASE_URL)

    def test_create(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        url = self.get_url()
        classroom = ClassroomFactory.build(course=self.course, is_active=False)

        def make_expected(response):
            return {
                'id': response.data['id'],
                'name': classroom.name,
                'description': classroom.description,
                'is_active': classroom.is_active,
                'block_id': None,
                'estimated_time': classroom.estimated_time,
                'tracker_queue_id': None,
                'calendar_enabled': classroom.calendar_enabled,
                'created': response.data['created'],
                'modified': response.data['modified'],
            }

        data = {
            'is_active': classroom.is_active,
            'name': classroom.name,
            'description': classroom.description,
            'estimated_time': classroom.estimated_time,
            'course_id': self.course.id,
        }
        self.create_request(url=url, data=data, expected=make_expected, num_queries=14)

    def test_create_with_block(self):
        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)

        url = self.get_url()
        classroom = ClassroomFactory.build(course=self.course, is_active=False)
        course_block = CourseBlockFactory(course=self.course)

        def make_expected(response):
            return {
                'id': response.data['id'],
                'name': classroom.name,
                'description': classroom.description,
                'is_active': classroom.is_active,
                'block_id': course_block.id,
                'estimated_time': classroom.estimated_time,
                'tracker_queue_id': None,
                'calendar_enabled': classroom.calendar_enabled,
                'created': response.data['created'],
                'modified': response.data['modified'],
            }

        data = {
            'is_active': classroom.is_active,
            'name': classroom.name,
            'description': classroom.description,
            'estimated_time': classroom.estimated_time,
            'course_id': self.course.id,
            'block_id': course_block.id,
        }
        self.create_request(url=url, data=data, expected=make_expected, num_queries=17)


class LabCourseClassroomListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:course-classroom-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory()
        self.count_classrooms = 5
        self.classrooms: Sequence[Classroom] = ClassroomFactory.create_batch(self.count_classrooms, course=self.course)

    def test_url(self):
        self.assertURLNameEqual('courses/{}/classroom_modules/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=False))

        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(1), format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def build_expected(self, items):
        return [{
            'id': obj.id,
            'name': obj.name,
            'description': obj.description,
            'estimated_time': obj.estimated_time,
            'is_active': obj.is_active,
            'block_id': obj.block_id,
            'calendar_enabled': obj.calendar_enabled,
            'created': serializers.DateTimeField().to_representation(obj.created),
            'modified': serializers.DateTimeField().to_representation(obj.modified),
            'timeslots': [{
                'id': slot.id,
                'title': slot.title,
                'summary': slot.summary,
                'begin_date': serializers.DateTimeField().to_representation(slot.begin_date),
                'end_date': serializers.DateTimeField().to_representation(slot.end_date),
                'num_participants': slot.num_participants,
                'max_participants': slot.max_participants,
                'course_groups': [],
                'created': serializers.DateTimeField().to_representation(slot.created),
                'modified': serializers.DateTimeField().to_representation(slot.modified),
            } for slot in obj.timeslots.all()],
        } for obj in items]

    def test_list(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.view_course', self.user, self.course)

        url = self.get_url(self.course.id)
        expected = self.build_expected(self.classrooms)
        self.list_request(url, expected, num_queries=11)

    def test_list_with_slots(self):
        classroom = self.classrooms[0]
        TimeslotFactory.create_batch(5, classroom=classroom)
        classroom.refresh_from_db()

        self.client.force_login(user=self.user)
        assign_perm('courses.view_course', self.user, self.course)

        url = self.get_url(self.course.id)
        expected = self.build_expected(self.classrooms)
        self.list_request(url, expected, num_queries=12)


class LabClassroomTimeslotDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-timeslot-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.classroom = ClassroomFactory()
        self.timeslot = TimeslotFactory(classroom=self.classroom)

    def test_url(self):
        self.assertURLNameEqual('classroom_timeslots/{}/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=False))

        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(1), format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def build_expected(self, timeslot, show_course_groups=True):
        data = {
            'id': timeslot.id,
            'title': timeslot.title,
            'summary': timeslot.summary,
            'begin_date': serializers.DateTimeField().to_representation(timeslot.begin_date),
            'end_date': serializers.DateTimeField().to_representation(timeslot.end_date),
            'num_participants': timeslot.num_participants,
            'max_participants': timeslot.max_participants,
            'created': serializers.DateTimeField().to_representation(timeslot.created),
            'modified': serializers.DateTimeField().to_representation(timeslot.modified),
        }

        if show_course_groups:
            data['course_groups'] = list(timeslot.course_groups.order_by('id').values_list('id', flat=True))

        return data

    def test_get(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.view_course', self.user, self.classroom.course)

        url = self.get_url(self.timeslot.id)
        expected = self.build_expected(self.timeslot)
        self.detail_request(url, expected=expected, num_queries=9)

    def test_put(self, method='put'):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        assign_perm('courses.change_course', self.user, course)

        updated_timeslot: Timeslot = TimeslotFactory.build(
            id=self.timeslot.id, course=course,
            begin_date=timezone.now() + timedelta(1),
        )
        payload = {
            'begin_date': updated_timeslot.begin_date,
            'end_date': updated_timeslot.end_date,
            'title': updated_timeslot.title,
            'summary': updated_timeslot.summary,
            'max_participants': updated_timeslot.max_participants,
        }
        url = self.get_url(self.timeslot.id)

        def make_expected(response):
            response_data = response.data
            data = self.build_expected(updated_timeslot, show_course_groups=False)
            data['course_groups'] = []
            data['created'] = response_data['created']
            data['modified'] = response_data['modified']
            return data

        self.update_request(url, data=payload, expected=make_expected, num_queries=12, method=method)

    def test_put_with_groups(self):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        course_groups = CourseGroupFactory.create_batch(5, course=course)
        assign_perm('courses.change_course', self.user, course)

        updated_timeslot: Timeslot = TimeslotFactory.build(
            id=self.timeslot.id,
            course=course,
            begin_date=timezone.now() + timedelta(days=1),
        )
        course_groups_for_timeslot = course_groups[:3]
        payload = {
            'begin_date': updated_timeslot.begin_date,
            'end_date': updated_timeslot.end_date,
            'title': updated_timeslot.title,
            'summary': updated_timeslot.summary,
            'max_participants': updated_timeslot.max_participants,
            'course_groups': sorted(group.id for group in course_groups_for_timeslot)
        }
        url = self.get_url(self.timeslot.id)

        def make_expected(response):
            response_data = response.data
            data = self.build_expected(updated_timeslot, show_course_groups=True)
            data['created'] = response_data['created']
            data['modified'] = response_data['modified']
            return data

        self.update_request(url, data=payload, expected=make_expected, num_queries=15)

    def test_patch(self):
        self.test_put(method='patch')

    def test_put_with_wrong_groups(self):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        assign_perm('courses.change_course', self.user, course)

        wrong_course = CourseFactory()
        wrong_groups = CourseGroupFactory.create_batch(3, course=wrong_course)

        updated_timeslot: Timeslot = TimeslotFactory.build(
            id=self.timeslot.id, course=course,
            begin_date=timezone.now() + timedelta(days=1),
        )
        payload = {
            'begin_date': updated_timeslot.begin_date,
            'end_date': updated_timeslot.end_date,
            'title': updated_timeslot.title,
            'summary': updated_timeslot.summary,
            'max_participants': updated_timeslot.max_participants,
            'course_groups': [group.id for group in wrong_groups]
        }
        url = self.get_url(self.timeslot.id)

        response = self.update_request(url, data=payload, num_queries=9, status_code=status.HTTP_400_BAD_REQUEST)
        self.assertIn('course_groups', response.data)

    def test_patch_with_students(self):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        assign_perm('courses.change_course', self.user, course)

        new_timeslot = TimeslotFactory.build(begin_date=timezone.now() + timedelta(days=1))

        data = {
            'begin_date': new_timeslot.begin_date,
            'end_date': new_timeslot.end_date,
            'max_participants': new_timeslot.max_participants,
            'title': new_timeslot.title,
            'summary': new_timeslot.summary,
        }

        with self.assertNumQueries(12):
            response = self.client.patch(self.get_url(self.timeslot.id), data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        now_moment = fake.date_time(tzinfo=utc)
        data = {
            'begin_date': now_moment + timedelta(days=1),
        }
        with self.assertNumQueries(12):
            response = self.client.patch(self.get_url(self.timeslot.id), data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        now_moment = fake.date_time(tzinfo=utc)
        data = {
            'begin_date': now_moment - timedelta(days=1),
        }
        with self.assertNumQueries(12):
            response = self.client.patch(self.get_url(self.timeslot.id), data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        self.timeslot.max_participants = 10
        self.timeslot.save(update_fields=['max_participants'])

        StudentSlotFactory.create_batch(5, timeslot=self.timeslot)

        data = {
            'max_participants': 8,
        }
        with self.assertNumQueries(12):
            response = self.client.patch(self.get_url(self.timeslot.id), data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)

        data = {
            'max_participants': 3,
        }
        with self.assertNumQueries(9):
            response = self.client.patch(self.get_url(self.timeslot.id), data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('max_participants', response.data)

    def test_delete_with_students(self):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        assign_perm('courses.change_course', self.user, course)

        student_slot = StudentSlotFactory(timeslot=self.timeslot)

        with self.assertNumQueries(9):
            response = self.client.delete(self.get_url(self.timeslot.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data['non_field_errors'][0], 'Нельзя удалить слот, на который записаны студенты.')

        student_slot.delete()

        with self.assertNumQueries(14):
            response = self.client.delete(self.get_url(self.timeslot.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)


class LabClassroomTimeslotCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-timeslot-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.classroom = ClassroomFactory()

    def test_url(self):
        self.assertURLNameEqual('classroom_timeslots/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.post(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def build_expected(self, timeslot, show_course_groups=True):
        data = {
            'id': timeslot.id,
            'title': timeslot.title,
            'summary': timeslot.summary,
            'begin_date': serializers.DateTimeField().to_representation(timeslot.begin_date),
            'end_date': serializers.DateTimeField().to_representation(timeslot.end_date),
            'num_participants': timeslot.num_participants,
            'max_participants': timeslot.max_participants,
            'created': serializers.DateTimeField().to_representation(timeslot.created),
            'modified': serializers.DateTimeField().to_representation(timeslot.modified),
        }
        # if show_course_groups:
        #     data['course_groups'] = list(timeslot.course_groups.order_by('id').values_list('id', flat=True))

        return data

    def test_create(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user, self.classroom.course)

        new_timeslot: Timeslot = TimeslotFactory.build(classroom=self.classroom)
        payload = {
            'classroom_id': new_timeslot.classroom.id,
            'begin_date': new_timeslot.begin_date,
            'end_date': new_timeslot.end_date,
            'title': new_timeslot.title,
            'summary': new_timeslot.summary,
            'max_participants': new_timeslot.max_participants,
        }
        url = self.get_url()

        def make_expected(response):
            response_data = response.data
            data = {
                **payload,
                'begin_date': serializers.DateTimeField().to_representation(new_timeslot.begin_date),
                'end_date': serializers.DateTimeField().to_representation(new_timeslot.end_date),
                'num_participants': new_timeslot.num_participants,
                'course_groups': [],
                'id': response_data['id'],
                'created': response_data['created'],
                'modified': response_data['modified'],
            }
            del data['classroom_id']
            return data

        self.create_request(url, data=payload, expected=make_expected, num_queries=12)

    def test_create_with_groups(self):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        assign_perm('courses.change_course', self.user, course)

        course_groups = CourseGroupFactory.create_batch(3, course=course)
        new_timeslot: Timeslot = TimeslotFactory.build(classroom=self.classroom)
        payload = {
            'classroom_id': new_timeslot.classroom.id,
            'begin_date': new_timeslot.begin_date,
            'end_date': new_timeslot.end_date,
            'title': new_timeslot.title,
            'summary': new_timeslot.summary,
            'max_participants': new_timeslot.max_participants,
            'course_groups': sorted(g.id for g in course_groups)
        }
        url = self.get_url()

        def make_expected(response):
            response_data = response.data
            data = {
                **payload,
                'begin_date': serializers.DateTimeField().to_representation(new_timeslot.begin_date),
                'end_date': serializers.DateTimeField().to_representation(new_timeslot.end_date),
                'num_participants': new_timeslot.num_participants,
                # 'course_groups': [],
                'id': response_data['id'],
                'created': response_data['created'],
                'modified': response_data['modified'],
            }
            del data['classroom_id']
            return data

        self.create_request(url, data=payload, expected=make_expected, num_queries=15)

    def test_create_with_wrong_groups(self):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        assign_perm('courses.change_course', self.user, course)

        wrong_course = CourseFactory()
        wrong_course_groups = CourseGroupFactory.create_batch(3, course=wrong_course)
        new_timeslot: Timeslot = TimeslotFactory.build(classroom=self.classroom)
        payload = {
            'classroom_id': new_timeslot.classroom.id,
            'begin_date': new_timeslot.begin_date,
            'end_date': new_timeslot.end_date,
            'title': new_timeslot.title,
            'summary': new_timeslot.summary,
            'max_participants': new_timeslot.max_participants,
            'course_groups': sorted(g.id for g in wrong_course_groups)
        }
        url = self.get_url()

        response = self.create_request(url, data=payload, num_queries=10, status_code=status.HTTP_400_BAD_REQUEST)
        self.assertEqual(response.data.keys(), {'course_groups'})


class LabClassroomTimeslotStudentListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-timeslot-student-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.timeslot = TimeslotFactory()
        self.student_slots = StudentSlotFactory.create_batch(10, timeslot=self.timeslot)

    def test_url(self):
        self.assertURLNameEqual('classroom_timeslots/{}/students/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def build_expected(self, student_slots):
        sorted_items = sorted(student_slots, key=lambda x: x.student_id)
        sorted_items = sorted(sorted_items, reverse=True, key=lambda x: x.created)

        return [{
            'id': student_slot.id,
            'student': {
                'id': student_slot.student.id,
                'user': {
                    'id': student_slot.student.user_id,
                    'username': student_slot.student.user.username,
                    'first_name': student_slot.student.user.first_name,
                    'last_name': student_slot.student.user.last_name,
                },
                'group': None,
            },
            'is_attended': student_slot.is_attended,
            'created': serializers.DateTimeField().to_representation(student_slot.created),
            'modified': serializers.DateTimeField().to_representation(student_slot.modified),
        } for student_slot in sorted_items]

    def test_list(self):
        self.client.force_login(user=self.user)
        course = self.timeslot.course
        assign_perm('courses.view_course', self.user, course)

        url = self.get_url(self.timeslot.id)
        expected = self.build_expected(self.student_slots)
        self.list_request(url, expected, num_queries=10)
