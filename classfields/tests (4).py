import unittest

import mock
from django.core.exceptions import ValidationError
from django.test import TestCase

from django_dbq.models import Job, JobType
from django_dbq.utils import JobStates


class TestJob(TestCase):

    def setUp(self) -> None:
        self.settings, *_ = JobType.objects.get_or_create(name='TestJob', active=True)

    def tearDown(self) -> None:
        JobType.objects.all().delete()
        Job.objects.all().delete()

    @mock.patch('django_dbq.models.publish')
    def test_save(self, publish):
        job = Job.objects.create(name=self.settings.name, settings=self.settings)
        self.assertEqual(1, publish.call_count)
        publish.reset_mock()
        for state in JobStates:
            if state == JobStates.NEW:
                continue
            job.state = state
            print(job.state)
            job.save()

            self.assertEqual(0, publish.call_count)
        publish.reset_mock()

        job.state = 'NEW'
        job.save()
        self.assertEqual(1, publish.call_count)
        publish.reset_mock()

        job.state = 'COMPLETE'
        job.save(enque=True)
        self.assertEqual(1, publish.call_count)

    def test_clear(self):
        self.settings.ignore_test = True
        self.settings.save()
        import django_dbq.models
        django_dbq.models.IS_TESTING = True
        self.assertRaises(ValidationError, Job.objects.create, name=self.settings.name, settings=self.settings)
        django_dbq.models.IS_TESTING = False
        self.settings.active = False
        self.settings.save()
        self.assertRaises(ValidationError, Job.objects.create, name=self.settings.name, settings=self.settings)
        self.settings.active = True
        self.settings.save()
