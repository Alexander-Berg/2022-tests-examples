from typing import List
from unittest import skip

from guardian.shortcuts import assign_perm

from django.conf import settings
from django.db import transaction
from django.db.models import Max

from rest_framework import serializers, status
from rest_framework.test import APITestCase

from lms.classrooms.tests.factories import ClassroomFactory, TimeslotFactory
from lms.core.tests.mixins import GenericRequestMixin, UrlNameMixin
from lms.courses.tests.factories import CourseBlockFactory, CourseFactory
from lms.enrollments.models import Enrollment
from lms.enrollments.tests.factories import EnrollmentFactory, NotTrackerEnrollmentFactory, TrackerEnrollmentFactory
from lms.users.tests.factories import LabUserFactory, UserFactory
from lms.utils.tests.test_decorators import parameterized_expand_doc

from ..models import ClassroomTrackerQueue, EnrollmentTracker, EnrollmentTrackerQueue
from .factories import (
    ClassroomQueueFactory, ClassroomTrackerFactory, ClassroomTrackerQueueFactory, EnrollmentQueueFactory,
    EnrollmentTrackerFactory, EnrollmentTrackerQueueFactory, TrackerQueueFactory,
)


class LabEnrollmentTrackerQueueListTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-tracker-queue-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.enrollment = TrackerEnrollmentFactory()
        self.queues = [
            EnrollmentTrackerQueueFactory(is_default=True, enrollment=self.enrollment),
        ] + EnrollmentTrackerQueueFactory.create_batch(5, is_default=False, enrollment=self.enrollment)
        self.other_queues = EnrollmentTrackerQueueFactory.create_batch(2)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/{pk}/tracker_queues/',
            kwargs={'pk': self.enrollment.id},
            base_url=settings.LABAPI_BASE_URL,
        )

    def test_list(self) -> None:
        assign_perm('courses.view_course', self.user, self.enrollment.course)
        self.client.force_login(user=self.user)
        with self.assertNumQueries(14):
            response = self.client.get(self.get_url(pk=self.enrollment.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        response_data = response.data
        expected = [
            {
                "id": queue.id,
                "name": queue.name,
                "enrollment_id": queue.enrollment_id,
                "is_default": queue.is_default,
                "summary": queue.summary,
                "description": queue.description,
                "issue_type": queue.issue_type,
                "accepted_status": queue.accepted_status,
                "rejected_status": queue.rejected_status,
                "tracker_pulling_enabled": queue.tracker_pulling_enabled,
                "cancelled_status": queue.cancelled_status,
                "can_delete": queue.can_delete,
            } for queue in self.queues
        ]
        self.assertEqual(response_data['count'], len(self.queues))
        self.assertEqual(response_data['next'], None)
        self.assertEqual(response_data['previous'], None)
        self.assertListEqual(expected, response_data['results'])

    def test_no_course(self):
        self.client.force_login(user=self.user)

        max_enrollment_id = Enrollment.objects.aggregate(max_id=Max('id')).get('max_id', 0) or 0
        with self.assertNumQueries(3):
            response = self.client.get(self.get_url(pk=max_enrollment_id + 1), format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        response_data = response.data
        self.assertEqual('not_found', response_data['detail'].code)


class LabEnrollmentTrackerQueueCreateTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-tracker-queue-create'

    def setUp(self):
        self.user = LabUserFactory(is_staff=True)

    def test_url(self):
        self.assertURLNameEqual('tracker_queues/', base_url=settings.LABAPI_BASE_URL)

    def build_new_queue_data(self, new_queue, is_default=None):
        return {
            "name": new_queue.name,
            "enrollment_id": new_queue.enrollment_id,
            "is_default": new_queue.is_default if is_default is None else is_default,
            "summary": new_queue.summary,
            "description": new_queue.description,
            "issue_type": new_queue.issue_type,
            "accepted_status": new_queue.accepted_status,
            "rejected_status": new_queue.rejected_status,
            "cancelled_status": new_queue.cancelled_status,
            "tracker_pulling_enabled": new_queue.tracker_pulling_enabled,
        }

    @parameterized_expand_doc(
        [
            (True, 14),
            (False, 14),
        ]
    )
    def test_create(self, is_default: bool, num_queries: int) -> None:
        self.client.force_login(user=self.user)

        self.enrollment = TrackerEnrollmentFactory()
        assign_perm('courses.change_course', self.user, self.enrollment.course)
        queue = EnrollmentTrackerQueueFactory.build(enrollment=self.enrollment, is_default=is_default)
        data = self.build_new_queue_data(queue, True)

        with self.assertNumQueries(num_queries):
            response = self.client.post(self.get_url(), format='json', data=data)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        expected = data
        expected['is_default'] = True
        expected['can_delete'] = True
        response_data = response.data
        expected['id'] = response_data['id']
        self.assertDictEqual(expected, response_data)

        del expected['can_delete']
        self.assertEqual(EnrollmentTrackerQueue.objects.filter(**expected).count(), 1)

    @parameterized_expand_doc(
        [
            ([False, False, False], True, [False, False, False], True, 14),
            ([True, False, False], False, [True, False, False], False, 13),
            ([True, False, False], True, [False, False, False], True, 14),
        ]
    )
    def test_create_default(
        self,
        old_defaults: List[bool],
        new_default: bool,
        changed_old_defaults: List[bool],
        changed_new_default: bool,
        num_queries: int,
    ) -> None:
        self.client.force_login(user=self.user)

        enrollment = TrackerEnrollmentFactory()
        assign_perm('courses.change_course', self.user, enrollment.course)
        old_queues = [
            EnrollmentTrackerQueueFactory(enrollment=enrollment, is_default=is_default)
            for is_default in old_defaults
        ]
        new_queue = EnrollmentTrackerQueueFactory.build(enrollment=enrollment, is_default=new_default)
        data = self.build_new_queue_data(new_queue)

        with self.assertNumQueries(num_queries):
            response = self.client.post(self.get_url(), format='json', data=data)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

        expected = data
        expected['is_default'] = changed_new_default
        expected['can_delete'] = True
        response_data = response.data
        expected['id'] = response_data['id']
        self.assertDictEqual(expected, response_data)

        del expected['can_delete']
        self.assertEqual(EnrollmentTrackerQueue.objects.filter(**expected).count(), 1)

        for old_queue, changed_old_default in zip(old_queues, changed_old_defaults):
            old_queue.refresh_from_db()
            self.assertEqual(old_queue.is_default, changed_old_default)

    def test_create_statuses_required_in_default_queue(self):
        self.client.force_login(user=self.user)

        enrollment = TrackerEnrollmentFactory()
        assign_perm('courses.change_course', self.user, enrollment.course)

        new_queue = EnrollmentTrackerQueueFactory.build(
            enrollment=enrollment,
            is_default=True,
            tracker_pulling_enabled=True,
            accepted_status=None,
            rejected_status=None,
            cancelled_status=None,
        )
        data = self.build_new_queue_data(new_queue)

        with self.assertNumQueries(9):
            response = self.client.post(self.get_url(), format='json', data=data)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        response_data = response.data

        self.assertEqual('required', response_data['accepted_status'][0].code)
        self.assertEqual('required', response_data['rejected_status'][0].code)

    def test_create_statuses_not_required_in_not_default_queue(self):
        self.client.force_login(user=self.user)

        enrollment = TrackerEnrollmentFactory()
        assign_perm('courses.change_course', self.user, enrollment.course)

        EnrollmentTrackerQueueFactory(
            enrollment=enrollment,
            is_default=True,
        )

        new_queue = EnrollmentTrackerQueueFactory.build(
            enrollment=enrollment,
            is_default=False,
            tracker_pulling_enabled=True,
            accepted_status=None,
            rejected_status=None,
            cancelled_status=None,
        )
        data = self.build_new_queue_data(new_queue)

        with self.assertNumQueries(13):
            response = self.client.post(self.get_url(), format='json', data=data)
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_create_not_tracker_enrollment(self):
        self.client.force_login(user=self.user)

        enrollment = NotTrackerEnrollmentFactory()
        assign_perm('courses.change_course', self.user, enrollment.course)
        new_queue = EnrollmentTrackerQueueFactory.build(enrollment=enrollment)
        data = self.build_new_queue_data(new_queue)

        with self.assertNumQueries(9):
            response = self.client.post(self.get_url(), data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

        response_data = response.data
        self.assertEqual('non_tracker_enrollment', response_data['enrollment'][0].code)

        del data['is_default']
        self.assertFalse(EnrollmentTrackerQueue.objects.filter(**data).exists())


class LabEnrollmentTrackerQueueDetailTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-tracker-queue-detail'

    def setUp(self):
        self.user = LabUserFactory(is_staff=True)

    def test_url(self):
        self.assertURLNameEqual('tracker_queues/{pk}/', kwargs={'pk': 1}, base_url=settings.LABAPI_BASE_URL)

    def test_retrieve(self) -> None:
        self.client.force_login(user=self.user)

        queue = EnrollmentTrackerQueueFactory()
        assign_perm('courses.view_course', self.user, queue.enrollment.course)
        with self.assertNumQueries(9):
            response = self.client.get(self.get_url(pk=queue.id), format='json')
        response_data = response.data
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        expected = {
            'id': queue.id,
            'name': queue.name,
            'enrollment_id': queue.enrollment_id,
            'is_default': False,
            'summary': queue.summary,
            'description': queue.description,
            'issue_type': queue.issue_type,
            'accepted_status': queue.accepted_status,
            'rejected_status': queue.rejected_status,
            'cancelled_status': queue.cancelled_status,
            'can_delete': queue.can_delete,
            'tracker_pulling_enabled': queue.tracker_pulling_enabled,
        }
        self.assertDictEqual(expected, response_data)

    def test_put(self) -> None:
        self.client.force_login(user=self.user)

        queue = EnrollmentTrackerQueueFactory()
        assign_perm('courses.change_course', self.user, queue.enrollment.course)
        updated_queue = EnrollmentTrackerQueueFactory.build(id=queue.id, enrollment=queue.enrollment)
        data = {
            'name': updated_queue.name,
            'is_default': True,
            'summary': updated_queue.summary,
            'description': updated_queue.description,
            'issue_type': updated_queue.issue_type,
            'accepted_status': updated_queue.accepted_status,
            'rejected_status': updated_queue.rejected_status,
            'cancelled_status': updated_queue.cancelled_status,
            'can_delete': updated_queue.can_delete,
            'tracker_pulling_enabled': updated_queue.tracker_pulling_enabled,
        }
        with self.assertNumQueries(13):
            response = self.client.put(self.get_url(pk=queue.id), format='json', data=data)

        self.assertEqual(response.status_code, status.HTTP_200_OK, msg=response.data)
        response_data = response.data

        created_queue = EnrollmentTrackerQueue.objects.get(pk=response_data['id'])
        expected = data
        expected['id'] = created_queue.pk
        expected['enrollment_id'] = queue.enrollment_id

        self.assertDictEqual(expected, response_data)
        del expected['can_delete']
        self.assertEqual(EnrollmentTrackerQueue.objects.filter(**expected).count(), 1)

    def test_patch(self) -> None:
        self.client.force_login(user=self.user)

        queue = EnrollmentTrackerQueueFactory()
        assign_perm('courses.change_course', self.user, queue.enrollment.course)
        updated_queue = EnrollmentTrackerQueueFactory.build(id=queue.id, enrollment_id=queue.enrollment_id)
        data = {
            'summary': updated_queue.summary,
            'issue_type': updated_queue.issue_type,
            'rejected_status': updated_queue.rejected_status,
        }
        with self.assertNumQueries(12):
            response = self.client.patch(self.get_url(pk=queue.id), format='json', data=data)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        response_data = response.data
        expected = data
        expected.update(
            {
                'id': response_data['id'],
                'enrollment_id': queue.enrollment_id,
                'name': queue.name,
                'is_default': False,
                'description': queue.description,
                'accepted_status': queue.accepted_status,
                'cancelled_status': queue.cancelled_status,
                'can_delete': queue.can_delete,
                'tracker_pulling_enabled': queue.tracker_pulling_enabled,
            }
        )
        self.assertEqual(expected, response_data)
        del expected['can_delete']
        self.assertEqual(EnrollmentTrackerQueue.objects.filter(**expected).count(), 1)

    def test_delete_default_queue(self) -> None:
        self.client.force_login(user=self.user)

        enrollment = TrackerEnrollmentFactory()
        default_queue = EnrollmentTrackerQueueFactory(is_default=True, enrollment=enrollment)
        not_default_queue = EnrollmentTrackerQueueFactory(is_default=False, enrollment=enrollment)
        assign_perm('courses.change_course', self.user, enrollment.course)
        with transaction.atomic():
            with self.assertNumQueries(10):
                response = self.client.delete(self.get_url(pk=default_queue.id), format='json')
        response_data = response.data
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertEqual('default_queue_is_not_last', response_data['non_field_errors'][0].code)
        self.assertTrue(EnrollmentTrackerQueue.objects.filter(id=default_queue.id, is_default=True).exists())
        self.assertTrue(EnrollmentTrackerQueue.objects.filter(id=not_default_queue.id, is_default=False).exists())

    def test_delete_last_default_queue(self) -> None:
        self.client.force_login(user=self.user)

        enrollment = TrackerEnrollmentFactory()
        default_queue = EnrollmentTrackerQueueFactory(is_default=True, enrollment=enrollment)
        assign_perm('courses.change_course', self.user, enrollment.course)
        with transaction.atomic():
            with self.assertNumQueries(15):
                response = self.client.delete(self.get_url(pk=default_queue.id), format='json')

        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertFalse(EnrollmentTrackerQueue.objects.filter(id=default_queue.id).exists())

    def test_delete_not_default_queue(self) -> None:
        self.client.force_login(user=self.user)

        enrollment = TrackerEnrollmentFactory()
        default_queue = EnrollmentTrackerQueueFactory(is_default=True, enrollment=enrollment)
        not_default_queue = EnrollmentTrackerQueueFactory(is_default=False, enrollment=enrollment)
        assign_perm('courses.change_course', self.user, enrollment.course)
        with self.assertNumQueries(14):
            response = self.client.delete(self.get_url(pk=not_default_queue.id), format='json')
        response_data = response.data
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.assertIsNone(response_data)
        self.assertTrue(EnrollmentTrackerQueue.objects.filter(id=default_queue.id, is_default=True).exists())
        self.assertFalse(EnrollmentTrackerQueue.objects.filter(id=not_default_queue.id).exists())


class LabEnrollmentTypeTrackerCreateLabTestCase(UrlNameMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/', base_url=settings.LABAPI_BASE_URL,
        )

    def test_create_with_tracker(self):
        new_enrollment = EnrollmentFactory.build(
            course=self.course,
            enroll_type=Enrollment.TYPE_TRACKER,
            is_active=True,
        )
        new_tracker_queue = EnrollmentTrackerQueueFactory.build(
            enrollment=new_enrollment, is_default=True,
        )
        data = {
            'course_id': new_enrollment.course_id,
            'enroll_type': new_enrollment.enroll_type,
            'name': new_enrollment.name,
            'is_active': new_enrollment.is_active,
            'parameters': {
                'tracker_queues': [{
                    'name': new_tracker_queue.name,
                    'summary': new_tracker_queue.summary,
                    'description': new_tracker_queue.description,
                    'issue_type': new_tracker_queue.issue_type,
                    'accepted_status': new_tracker_queue.accepted_status,
                    'rejected_status': new_tracker_queue.rejected_status,
                    'is_default': new_tracker_queue.is_default,
                }]
            }
        }

        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        with self.assertNumQueries(19):
            response = self.client.post(self.get_url(), data=data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, msg=response.data)

        enrollment = Enrollment.objects.get(pk=response.data['id'])

        self.assertEqual(len(enrollment.tracker_queues.all()), 1)
        tracker_queue = enrollment.tracker_queues.first()

        self.assertEqual(tracker_queue.name, new_tracker_queue.name)
        self.assertEqual(tracker_queue.summary, new_tracker_queue.summary)
        self.assertEqual(tracker_queue.description, new_tracker_queue.description)
        self.assertEqual(tracker_queue.issue_type, new_tracker_queue.issue_type)
        self.assertEqual(tracker_queue.accepted_status, new_tracker_queue.accepted_status)
        self.assertEqual(tracker_queue.rejected_status, new_tracker_queue.rejected_status)
        self.assertEqual(tracker_queue.is_default, new_tracker_queue.is_default)

    def test_create_many_queues(self):
        new_enrollment = EnrollmentFactory.build(
            course=self.course,
            enroll_type=Enrollment.TYPE_TRACKER,
            is_active=True,
        )
        new_tracker_queues = EnrollmentTrackerQueueFactory.build_batch(
            3, enrollment=new_enrollment, is_default=False,
        )
        new_tracker_queues[0].is_default = True

        data = {
            'course_id': new_enrollment.course_id,
            'enroll_type': new_enrollment.enroll_type,
            'name': new_enrollment.name,
            'is_active': new_enrollment.is_active,
            'parameters': {
                'tracker_queues': [{
                    'name': q.name,
                    'summary': q.summary,
                    'description': q.description,
                    'issue_type': q.issue_type,
                    'accepted_status': q.accepted_status,
                    'rejected_status': q.rejected_status,
                    'is_default': q.is_default,
                } for q in new_tracker_queues]
            }
        }

        assign_perm('courses.change_course', self.user, self.course)
        self.client.force_login(user=self.user)
        with self.assertNumQueries(25):
            response = self.client.post(self.get_url(), data=data, format='json')

        self.assertEqual(response.status_code, status.HTTP_201_CREATED, msg=response.data)

        enrollment = Enrollment.objects.get(pk=response.data['id'])
        self.assertEqual(len(enrollment.tracker_queues.all()), len(new_tracker_queues))


@skip("Deprecated Test Case")
class LabTrackerQueueListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:tracker-queue-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.queues = TrackerQueueFactory.create_batch(3)

    def test_url(self):
        self.assertURLNameEqual(
            'tracker_queues_list/', base_url=settings.LABAPI_BASE_URL,
        )

    def test_list(self):
        self.client.force_login(user=self.user)

        url = self.get_url()
        expected = [
            {
                'id': queue.id,
                'name': queue.name,
                'display_name': queue.display_name,
                'queue_name': queue.queue_name,
                'summary': queue.summary,
                'description': queue.description,
                'issue_type': queue.issue_type,
                'initial_state': queue.initial_state,
                'is_active': queue.is_active,
            } for queue in self.queues
        ]

        self.list_request(url, expected=expected, num_queries=6)


@skip("Deprecated Test Case")
class ClassroomTrackerQueueListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-tracker-queue-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.classroom = ClassroomFactory()
        self.queue = TrackerQueueFactory()
        self.other_queue = TrackerQueueFactory()
        self.classroom_queue = ClassroomTrackerQueueFactory(
            classroom=self.classroom, queue=self.queue,
        )

    def test_url(self):
        self.assertURLNameEqual(
            'classroom_modules/{}/tracker_queues/', args=(self.classroom.id,), base_url=settings.LABAPI_BASE_URL,
        )

    def test_permission_denied(self):
        self.client.force_login(user=self.user)
        url = self.get_url(self.classroom.id)
        with self.assertNumQueries(8):
            response = self.client.get(url, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_list(self):
        assign_perm('courses.view_course', self.user, self.classroom.course)
        self.client.force_login(user=self.user)

        url = self.get_url(self.classroom.id)
        expected = [
            {
                'id': self.classroom_queue.id,
                'queue': {
                    'id': self.queue.id,
                    'name': self.queue.name,
                    'display_name': self.queue.display_name,
                    'queue_name': self.queue.queue_name,
                    'summary': self.queue.summary,
                    'description': self.queue.description,
                    'issue_type': self.queue.issue_type,
                    'initial_state': self.queue.initial_state,
                    'is_active': self.queue.is_active,
                },
                'classroom_id': self.classroom.id,
            }
        ]

        self.list_request(url, expected=expected, pagination=False, num_queries=9)


@skip("Deprecated Test Case")
class ClassroomTrackerQueueCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-tracker-queue-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.classroom = ClassroomFactory()
        self.queue = TrackerQueueFactory()

    def test_url(self):
        self.assertURLNameEqual(
            'classroom_tracker_queues/', base_url=settings.LABAPI_BASE_URL,
        )

    def test_permission_denied(self):
        self.client.force_login(user=self.user)
        url = self.get_url()
        data = {
            'classroom_id': self.classroom.id,
            'queue_id': self.queue.id,
        }
        with self.assertNumQueries(8):
            response = self.client.post(url, data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_already_has_queue(self):
        self.classroom_tracker_queue = ClassroomTrackerQueueFactory(classroom=self.classroom)

        assign_perm('courses.change_course', self.user, self.classroom.course)
        self.client.force_login(user=self.user)
        url = self.get_url()
        data = {
            'classroom_id': self.classroom.id,
            'queue_id': self.queue.id,
        }
        with self.assertNumQueries(13):
            response = self.client.post(url, data=data, format='json')
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_create(self):
        assign_perm('courses.change_course', self.user, self.classroom.course)
        self.client.force_login(user=self.user)

        url = self.get_url()

        data = {
            'classroom_id': self.classroom.id,
            'queue_id': self.queue.id,
        }

        def make_expected(response):
            response_data = response.data
            expected = {
                'id': response_data['id'],
                'queue': {
                    'id': self.queue.id,
                    'name': self.queue.name,
                    'display_name': self.queue.display_name,
                    'queue_name': self.queue.queue_name,
                    'summary': self.queue.summary,
                    'description': self.queue.description,
                    'issue_type': self.queue.issue_type,
                    'initial_state': self.queue.initial_state,
                    'is_active': self.queue.is_active,
                },
                'classroom_id': self.classroom.id,
            }
            return expected

        self.create_request(url, data=data, expected=make_expected, num_queries=16)

        self.assertEqual(
            ClassroomTrackerQueue.objects.filter(classroom=self.classroom, queue=self.queue).count(),
            1,
        )


@skip("Deprecated Test Case")
class ClassroomTrackerQueueDeleteTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-tracker-queue-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.classroom = ClassroomFactory()
        self.queue = TrackerQueueFactory()
        self.classroom_tracker_queue = ClassroomTrackerQueueFactory(classroom=self.classroom, queue=self.queue)

    def test_url(self):
        self.assertURLNameEqual(
            'classroom_tracker_queues/{}/',
            args=(self.classroom_tracker_queue.id,),
            base_url=settings.LABAPI_BASE_URL,
        )

    def test_permission_denied(self):
        self.client.force_login(user=self.user)
        url = self.get_url(self.classroom_tracker_queue.id)
        with self.assertNumQueries(9):
            response = self.client.delete(url, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_delete(self):
        assign_perm('courses.change_course', self.user, self.classroom.course)
        self.client.force_login(user=self.user)

        url = self.get_url(self.classroom_tracker_queue.id)
        self.delete_request(url, num_queries=12)


@skip("Deprecated Test Case")
class ClassroomTrackerQueueDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-tracker-queue-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.classroom = ClassroomFactory()
        self.queue = TrackerQueueFactory()
        self.classroom_tracker_queue = ClassroomTrackerQueueFactory(classroom=self.classroom, queue=self.queue)

    def test_url(self):
        self.assertURLNameEqual(
            'classroom_tracker_queues/{}/',
            args=(self.classroom_tracker_queue.id,),
            base_url=settings.LABAPI_BASE_URL,
        )

    def test_permission_denied(self):
        self.client.force_login(user=self.user)
        url = self.get_url(self.classroom_tracker_queue.id)
        with self.assertNumQueries(9):
            response = self.client.get(url, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_detail(self):
        assign_perm('courses.view_course', self.user, self.classroom.course)
        self.client.force_login(user=self.user)

        expected = {
            'id': self.classroom_tracker_queue.id,
            'queue': {
                'id': self.queue.id,
                'name': self.queue.name,
                'display_name': self.queue.display_name,
                'summary': self.queue.summary,
                'description': self.queue.description,
                'issue_type': self.queue.issue_type,
                'initial_state': self.queue.initial_state,
                'is_active': self.queue.is_active,
            },
            'classroom_id': self.classroom.id,
        }

        url = self.get_url(self.classroom_tracker_queue.id)
        self.detail_request(url, expected=expected, num_queries=9)


class ClassroomQueueListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-queues-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.queues = ClassroomQueueFactory.create_batch(3)

    def test_url(self):
        self.assertURLNameEqual(
            'classroom_queues/', base_url=settings.LABAPI_BASE_URL,
        )

    def test_list(self):
        self.client.force_login(user=self.user)

        url = self.get_url()
        expected = [
            {
                'id': queue.id,
                'name': queue.name,
                'display_name': queue.display_name,
                'created': queue.created.strftime('%Y-%m-%dT%H:%M:%SZ'),
                'modified': queue.modified.strftime('%Y-%m-%dT%H:%M:%SZ'),
                'issue_type': queue.issue_type,
                'initial_state': queue.initial_state,
                'template_id': queue.template_id,
                'is_active': queue.is_active,
            } for queue in self.queues
        ]

        self.list_request(url, expected=expected, num_queries=6)


class LabClassroomDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.classroom = ClassroomFactory()
        self.timeslots = TimeslotFactory.create_batch(3, classroom=self.classroom)

    def test_url(self):
        self.assertURLNameEqual('classroom_modules/{}/', args=(1,), base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=False))

        with self.assertNumQueries(4):
            response = self.client.get(self.get_url(1), format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def build_expected(self, classroom, show_timeslots=True,):
        data = {
            'id': classroom.id,
            'name': classroom.name,
            'description': classroom.description,
            'is_active': classroom.is_active,
            'estimated_time': classroom.estimated_time,
            'block_id': classroom.block_id,
            'tracker_queue_id': None,
            'calendar_enabled': classroom.calendar_enabled,
            'created': serializers.DateTimeField().to_representation(classroom.created),
            'modified': serializers.DateTimeField().to_representation(classroom.modified),
        }

        if show_timeslots:
            data['timeslots'] = [{
                'id': timeslot.id,
                'title': timeslot.title,
                'summary': timeslot.summary,
                'begin_date': serializers.DateTimeField().to_representation(timeslot.begin_date),
                'end_date': serializers.DateTimeField().to_representation(timeslot.end_date),
                'num_participants': timeslot.num_participants,
                'max_participants': timeslot.max_participants,
                'course_groups': [],
                'created': serializers.DateTimeField().to_representation(timeslot.created),
                'modified': serializers.DateTimeField().to_representation(timeslot.modified),
            } for timeslot in classroom.timeslots.all()]

        return data

    def test_get(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.view_course', self.user, self.classroom.course)

        url = self.get_url(self.classroom.id)
        expected = self.build_expected(self.classroom)
        self.detail_request(url, expected=expected, num_queries=11)

    def test_put(self, method='put'):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        assign_perm('courses.change_course', self.user, course)

        updated_classroom = ClassroomFactory.build(id=self.classroom.id, course=course)
        payload = {
            'name': updated_classroom.name,
            'description': updated_classroom.description,
            'estimated_time': updated_classroom.estimated_time,
            'is_active': updated_classroom.is_active,
        }
        url = self.get_url(self.classroom.id)

        def make_expected(response):
            response_data = response.data
            data = self.build_expected(updated_classroom, show_timeslots=False)
            data['created'] = response_data['created']
            data['modified'] = response_data['modified']

            data['tracker_queue_id'] = None

            return data

        self.update_request(url, data=payload, expected=make_expected, num_queries=17, method=method)

    def test_patch(self):
        self.test_put(method='patch')

    def test_put_with_tracker_queue(self, method="put"):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        assign_perm('courses.change_course', self.user, course)

        classroom_queue = ClassroomQueueFactory()
        updated_classroom = ClassroomFactory.build(id=self.classroom.id, course=course)
        payload = {
            'name': updated_classroom.name,
            'description': updated_classroom.description,
            'estimated_time': updated_classroom.estimated_time,
            'is_active': updated_classroom.is_active,
            'tracker_queue_id': classroom_queue.id,
        }
        url = self.get_url(self.classroom.id)

        def make_expected(response):
            response_data = response.data
            data = self.build_expected(updated_classroom, show_timeslots=False)
            data['created'] = response_data['created']
            data['modified'] = response_data['modified']

            data['tracker_queue_id'] = classroom_queue.id

            return data

        self.update_request(url, data=payload, expected=make_expected, num_queries=20, method=method)

    def test_update_with_another_tracker_queue(self, method="put"):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        assign_perm('courses.change_course', self.user, course)

        classroom_queue1 = ClassroomQueueFactory()
        ClassroomTrackerFactory(classroom=self.classroom, queue=classroom_queue1)

        classroom_queue2 = ClassroomQueueFactory()
        updated_classroom = ClassroomFactory.build(id=self.classroom.id, course=course)
        payload = {
            'name': updated_classroom.name,
            'description': updated_classroom.description,
            'estimated_time': updated_classroom.estimated_time,
            'is_active': updated_classroom.is_active,
            'tracker_queue_id': classroom_queue2.id,
        }
        url = self.get_url(self.classroom.id)

        def make_expected(response):
            response_data = response.data
            data = self.build_expected(updated_classroom, show_timeslots=False)
            data['created'] = response_data['created']
            data['modified'] = response_data['modified']

            data['tracker_queue_id'] = classroom_queue2.id

            return data

        self.update_request(url, data=payload, expected=make_expected, num_queries=20, method=method)

    def test_put_with_no_tracker_queue(self, method="put"):
        self.client.force_login(user=self.user)
        course = self.classroom.course
        assign_perm('courses.change_course', self.user, course)

        classroom_tracker = ClassroomTrackerFactory(classroom=self.classroom)
        self.classroom.refresh_from_db()
        self.assertEqual(self.classroom.tracker, classroom_tracker)

        updated_classroom = ClassroomFactory.build(id=self.classroom.id, course=course)
        payload = {
            'name': updated_classroom.name,
            'description': updated_classroom.description,
            'estimated_time': updated_classroom.estimated_time,
            'is_active': updated_classroom.is_active,
            'tracker_queue_id': None,
        }
        url = self.get_url(self.classroom.id)

        def make_expected(response):
            response_data = response.data
            data = self.build_expected(updated_classroom, show_timeslots=False)
            data['created'] = response_data['created']
            data['modified'] = response_data['modified']

            data['tracker_queue_id'] = None

            return data

        self.update_request(url, data=payload, expected=make_expected, num_queries=18, method=method)

        self.classroom.refresh_from_db()
        self.assertFalse(hasattr(self.classroom, 'tracker'))


class LabClassroomCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:classroom-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual('classroom_modules/', base_url=settings.LABAPI_BASE_URL)

    def test_permission_denied(self):
        self.client.force_login(user=UserFactory(is_staff=True))
        with self.assertNumQueries(4):
            response = self.client.post(self.get_url(), format='json')

        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_create(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user, self.course)

        new_classroom = ClassroomFactory.build(course=self.course, is_active=True)
        payload = {
            'course_id': new_classroom.course.id,
            'name': new_classroom.name,
            'description': new_classroom.description,
            'estimated_time': new_classroom.estimated_time,
            'calendar_enabled': new_classroom.calendar_enabled,
            'is_active': new_classroom.is_active,
        }
        url = self.get_url()

        def make_expected(response):
            response_data = response.data
            data = {
                **payload,
                'block_id': None,
                'id': response_data['id'],
                'tracker_queue_id': None,
                'created': response_data['created'],
                'modified': response_data['modified'],
            }
            del data['course_id']

            return data

        self.create_request(url, data=payload, expected=make_expected, num_queries=14)

    def test_create_with_groups(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user, self.course)

        wrong_course = CourseFactory()
        new_classroom = ClassroomFactory.build(course=wrong_course, is_active=True)
        payload = {
            'course_id': new_classroom.course.id,
            'name': new_classroom.name,
            'description': new_classroom.description,
            'is_active': new_classroom.is_active,
        }
        url = self.get_url()

        self.create_request(url, data=payload, num_queries=8, status_code=status.HTTP_403_FORBIDDEN)

    def test_create_with_tracker_queue(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user, self.course)

        classroom_queue = ClassroomQueueFactory()
        new_classroom = ClassroomFactory.build(course=self.course, is_active=True)
        payload = {
            'course_id': new_classroom.course_id,
            'name': new_classroom.name,
            'description': new_classroom.description,
            'estimated_time': new_classroom.estimated_time,
            'is_active': new_classroom.is_active,
            'calendar_enabled': new_classroom.calendar_enabled,
            'tracker_queue_id': classroom_queue.id,
        }
        url = self.get_url()

        def make_expected(response):
            response_data = response.data
            data = {
                **payload,
                'block_id': None,
                'id': response_data['id'],
                'tracker_queue_id': classroom_queue.id,
                'created': response_data['created'],
                'modified': response_data['modified'],
            }
            del data['course_id']

            return data

        self.create_request(url, data=payload, expected=make_expected, num_queries=19)

    def test_create_with_block(self):
        self.client.force_login(user=self.user)
        assign_perm('courses.change_course', self.user, self.course)

        course_block = CourseBlockFactory(course=self.course)
        new_classroom = ClassroomFactory.build(course=self.course, is_active=True)
        payload = {
            'course_id': new_classroom.course.id,
            'name': new_classroom.name,
            'description': new_classroom.description,
            'estimated_time': new_classroom.estimated_time,
            'calendar_enabled': new_classroom.calendar_enabled,
            'is_active': new_classroom.is_active,
            'block_id': course_block.id,
        }
        url = self.get_url()

        def make_expected(response):
            response_data = response.data
            data = {
                **payload,
                'id': response_data['id'],
                'tracker_queue_id': None,
                'created': response_data['created'],
                'modified': response_data['modified'],
            }
            del data['course_id']

            return data

        self.create_request(url, data=payload, expected=make_expected, num_queries=17)


class CourseEnrollmentTrackerDetailTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-tracker-detail'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/tracker/{pk}/', kwargs={'pk': 1}, base_url=settings.LABAPI_BASE_URL,
        )

    def test_not_found(self) -> None:
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_MANUAL)

        self.client.force_login(user=self.user)

        with self.assertNumQueries(5):
            response = self.client.get(self.get_url(pk=enrollment.id), format='json')
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_retrieve(self) -> None:
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_TRACKER)
        enrollment_trackers = EnrollmentTrackerFactory.create_batch(3, enrollment=enrollment)

        self.client.force_login(user=self.user)

        expected = {
            'id': enrollment.id,
            'course_id': enrollment.course_id,
            'survey_id': enrollment.survey_id,
            'enroll_type': enrollment.enroll_type,
            'name': enrollment.name,
            'summary': enrollment.summary,
            'options': enrollment.options,
            'is_default': enrollment.is_default,
            'is_active': enrollment.is_active,
            'queues': [enrollment_tracker.queue_id for enrollment_tracker in enrollment_trackers],
            'created': serializers.DateTimeField().to_representation(enrollment.created),
            'modified': serializers.DateTimeField().to_representation(enrollment.modified),
        }

        assign_perm('courses.view_course', self.user, self.course)

        self.detail_request(url=self.get_url(pk=enrollment.id), expected=expected, num_queries=10)

    def test_partial_update(self):
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_TRACKER)
        old_trackers = EnrollmentTrackerFactory.create_batch(3, enrollment=enrollment)
        new_enrollment = EnrollmentFactory.build(
            course=self.course, is_active=True, enroll_type=Enrollment.TYPE_TRACKER,
        )
        new_queues = EnrollmentQueueFactory.create_batch(3)

        all_queues_ids = [new_queue.id for new_queue in new_queues] + [old_trackers[0].queue_id]

        data = {
            'survey_id': new_enrollment.survey_id,
            'name': new_enrollment.name,
            'summary': new_enrollment.summary,
            'queues': all_queues_ids,
        }

        def make_expected(response):
            return {
                **data,
                **{
                    'id': enrollment.id,
                    'course_id': enrollment.course_id,
                    'enroll_type': enrollment.enroll_type,
                    'options': enrollment.options,
                    'is_active': enrollment.is_active,
                    'is_default': enrollment.is_default,
                    'created': response.data['created'],
                    'modified': response.data['modified'],
                },
            }

        self.client.force_login(user=self.user)

        assign_perm('courses.change_course', self.user, self.course)

        self.partial_update_request(
            url=self.get_url(pk=enrollment.id), data=data, expected=make_expected, num_queries=21,
        )

        self.assertEqual(
            EnrollmentTracker.objects.filter(
                enrollment=enrollment, queue_id=all_queues_ids[0], is_default=True).count(),
            1,
        )

        self.assertEqual(
            EnrollmentTracker.objects.filter(
                enrollment=enrollment, queue_id__in=all_queues_ids[1:], is_default=False,
            ).count(),
            len(all_queues_ids) - 1,
        )

        self.assertFalse(
            EnrollmentTracker.objects.filter(
                enrollment=enrollment,
                queue_id__in=[old_tracker.queue_id for old_tracker in old_trackers[1:]],
                is_default=False,
            ).exists()
        )

    def test_delete(self) -> None:
        enrollment = EnrollmentFactory(course=self.course, is_active=True, enroll_type=Enrollment.TYPE_TRACKER)
        enrollment_trackers = EnrollmentTrackerFactory.create_batch(3, enrollment=enrollment)

        self.client.force_login(user=self.user)

        assign_perm('courses.change_course', self.user, self.course)

        self.delete_request(url=self.get_url(pk=enrollment.id), num_queries=16)

        self.assertFalse(
            EnrollmentTracker.objects.filter(
                id__in=[enrollment_tracker.id for enrollment_tracker in enrollment_trackers]
            ).exists()
        )


class CourseEnrollmentTrackerCreateTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-tracker-create'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.course = CourseFactory(is_active=True)

    def test_url(self):
        self.assertURLNameEqual(
            'enrollments/tracker/', base_url=settings.LABAPI_BASE_URL,
        )

    def test_create(self) -> None:
        enrollment = EnrollmentFactory.build(course=self.course, is_default=True, is_active=True)
        enrollment_queues = EnrollmentQueueFactory.create_batch(3)
        enrollment_queues_ids = [enrollment_queue.id for enrollment_queue in enrollment_queues]
        self.client.force_login(user=self.user)

        data = {
            'id': enrollment.id,
            'course_id': enrollment.course_id,
            'survey_id': enrollment.survey_id,
            'name': enrollment.name,
            'summary': enrollment.summary,
            'options': enrollment.options,
            'queues': enrollment_queues_ids,
            'is_active': enrollment.is_active,
            'is_default': enrollment.is_default,
        }

        def make_expected(response):
            response_data = response.data
            return {
                **data,
                **{
                    'id': response_data['id'],
                    'enroll_type': Enrollment.TYPE_TRACKER,
                    'queues': enrollment_queues_ids,
                    'created': serializers.DateTimeField().to_representation(response_data['created']),
                    'modified': serializers.DateTimeField().to_representation(response_data['modified']),
                }
            }

        assign_perm('courses.change_course', self.user, self.course)

        self.create_request(url=self.get_url(), data=data, expected=make_expected, num_queries=20)


class EnrollmentQueueListTestCase(UrlNameMixin, GenericRequestMixin, APITestCase):
    URL_NAME = 'labapi:enrollment-queue-list'

    def setUp(self) -> None:
        self.user = LabUserFactory(is_staff=True)
        self.enrollment_queues = EnrollmentQueueFactory.create_batch(5)

    def test_url(self):
        self.assertURLNameEqual('tracker/enrollment_queues/', args=(), base_url=settings.LABAPI_BASE_URL)

    def test_list(self):
        self.client.force_login(user=self.user)

        url = self.get_url()
        expected = [
            {
                'id': enrollment_queue.id,
                'name': str(enrollment_queue),
                'display_name': enrollment_queue.display_name,
                'issue_type': enrollment_queue.issue_type,
                'initial_state': enrollment_queue.initial_state,
                'is_active': enrollment_queue.is_active,
            }
            for enrollment_queue in self.enrollment_queues
        ]
        self.list_request(url, expected, num_queries=6)
