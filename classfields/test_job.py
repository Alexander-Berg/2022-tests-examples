from unittest import mock
from django.test import TestCase

from django_dbq.models import Job, JobStates, JobType

TEST_JOB = "TestJob"


class TestJob(TestCase):
    @mock.patch("django_dbq.models.publish")
    def test_save(self, publish_mock):
        """
        Job should always be resubmitted for execution if its state changed to NEW, from any other than NEW
        """
        jt = JobType(name=TEST_JOB, tasks=[], ignore_test=False)
        jt.save()
        job = Job.objects.create(TEST_JOB, state=JobStates.NEW.value, settings=jt)
        publish_mock.assert_called()
        for state in (JobStates.PROCESSING, JobStates.COMPLETE, JobStates.FAILED, JobStates.DELETE):
            publish_mock.reset_mock()
            job.state = state.value
            job.save()
            publish_mock.assert_not_called()

            publish_mock.reset_mock()
            job.state = JobStates.NEW.value
            job.save()
            publish_mock.assert_called()

