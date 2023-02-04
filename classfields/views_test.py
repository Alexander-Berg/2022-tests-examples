import pytest
from unittest import mock

from django.urls import reverse
from django.contrib.auth.models import User
from django.test import RequestFactory

from django_dbq.models import JobType, Job
from utilities import views


@pytest.fixture
def test_user(django_user_model):
    return django_user_model.objects.get_or_create(username="someone", password="something")[0]


TEST_TICKET = "test-ticket"


@pytest.fixture
def test_ticket_job():
    return JobType.objects.create(name=TEST_TICKET, active=True)


@pytest.mark.django_db
def test_run_ticket_job_creates_with_user(test_user: User, rf: RequestFactory, test_ticket_job: JobType):
    url = reverse("run-ticket", args=[test_ticket_job.name])
    request = rf.get(url)
    wanted_result_link = "/www/heh"
    with mock.patch.object(Job.objects, "create") as job_create_m, mock.patch(
        "utilities.views.redirect"
    ) as redirect_mock, mock.patch("utilities.views.current_user") as current_user_m:
        current_user_m.return_value = test_user
        results_mock = mock.MagicMock()
        results_mock.result_link = wanted_result_link
        wanted_redirect = "redirecting"
        redirect_mock.return_value = wanted_redirect
        job_create_m.return_value = results_mock
        ret_val = views.run_ticket(request, test_ticket_job.name)
        assert ret_val == wanted_redirect

        assert current_user_m.called

        _, kwargs = job_create_m.call_args
        assert "associated_user" in kwargs
        assert kwargs["associated_user"] == test_user
