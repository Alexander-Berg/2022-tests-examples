import faker

from django.test import TestCase

from lms.courses.tests.factories import CourseFactory

from ..models import CourseMailing
from .factories import CourseMailingFactory, MailingFactory

fake = faker.Faker()


class SyncCourseMailingSignalTestCase(TestCase):
    def setUp(self) -> None:
        self.course = CourseFactory()
        self.mailing1, self.mailing2 = MailingFactory.create_batch(2, is_active=True)
        self.inactive_mailing = MailingFactory(is_active=False)

    def test_set_enable_followers(self):
        self.course.enable_followers = False
        self.course.save()

        CourseMailingFactory(course=self.course, mailing=self.mailing1, is_active=True)

        with self.assertNumQueries(13):
            self.course.enable_followers = True
            self.course.save()

        self.assertEqual(CourseMailing.objects.filter(course=self.course, is_active=True).count(), 2)
        self.assertEqual(
            CourseMailing.objects.filter(course=self.course, mailing=self.mailing1, is_active=True).count(), 1,
        )
        self.assertEqual(
            CourseMailing.objects.filter(course=self.course, mailing=self.mailing2, is_active=True).count(), 1,
        )

    def test_un_set_enable_followers(self):
        self.course.enable_followers = True
        self.course.save()

        with self.assertNumQueries(12):
            self.course.enable_followers = False
            self.course.save()

        self.assertFalse(CourseMailing.objects.filter(course=self.course).exists())

    def test_not_changed_set_enable_followers(self):
        CourseMailingFactory(course=self.course, mailing=self.mailing1, is_active=True)

        with self.assertNumQueries(10):
            self.course.save()

        self.assertEqual(CourseMailing.objects.filter(course=self.course, is_active=True).count(), 1)
        self.assertEqual(
            CourseMailing.objects.filter(course=self.course, mailing=self.mailing1, is_active=True).count(), 1,
        )
