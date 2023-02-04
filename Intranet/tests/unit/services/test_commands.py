# coding: utf-8
from __future__ import unicode_literals

import mock
import pytest

from django.conf import settings

from plan.services.models import ServiceMoveRequest
from plan.services.management.commands.autoapprove_move_requests_with_sandbox import Command

from common import factories


pytestmark = pytest.mark.django_db


@pytest.fixture
def sandbox():
    return factories.ServiceFactory(slug=settings.ABC_DEFAULT_SERVICE_PARENT_SLUG)


@pytest.fixture
def regular_service():
    parent_1 = factories.ServiceFactory()
    parent_2 = factories.ServiceFactory()
    service = factories.ServiceFactory(parent=parent_1)
    factories.ServiceMoveRequestFactory(
        service=service,
        destination=parent_2,
        state=ServiceMoveRequest.REQUESTED,
    )
    return service


@pytest.fixture
def service_going_to_sandbox(sandbox):
    parent = factories.ServiceFactory()
    service = factories.ServiceFactory(parent=parent)
    factories.ServiceMoveRequestFactory(
        service=service,
        destination=sandbox,
        state=ServiceMoveRequest.REQUESTED,
    )
    return service


@pytest.fixture
def service_going_to_sandbox_partially_approved(sandbox):
    parent = factories.ServiceFactory()
    service = factories.ServiceFactory(parent=parent)
    factories.ServiceMoveRequestFactory(
        service=service,
        destination=sandbox,
        state=ServiceMoveRequest.PARTIALLY_APPROVED,
        approver_outgoing=parent.owner,
    )
    return service


@pytest.fixture
def service_going_out_of_sandbox(sandbox):
    parent = factories.ServiceFactory()
    service = factories.ServiceFactory(parent=sandbox)
    factories.ServiceMoveRequestFactory(
        service=service,
        destination=parent,
        state=ServiceMoveRequest.REQUESTED,
    )
    return service


@pytest.fixture
def service_going_out_of_sandbox_partially_approved(sandbox):
    parent = factories.ServiceFactory()
    service = factories.ServiceFactory(parent=sandbox)
    factories.ServiceMoveRequestFactory(
        service=service,
        destination=parent,
        state=ServiceMoveRequest.PARTIALLY_APPROVED,
        approver_incoming=parent.owner,
    )
    return service


def test_autoapprove_command(
        regular_service,
        service_going_out_of_sandbox,
        service_going_out_of_sandbox_partially_approved,
        service_going_to_sandbox,
        service_going_to_sandbox_partially_approved,
):
    with mock.patch('plan.services.tasks.move_service'):
        Command().handle()
    assert regular_service.move_requests.get().state == ServiceMoveRequest.REQUESTED
    assert service_going_out_of_sandbox.move_requests.get().state == ServiceMoveRequest.REQUESTED
    assert service_going_to_sandbox.move_requests.get().state == ServiceMoveRequest.REQUESTED
    assert service_going_out_of_sandbox_partially_approved.move_requests.get().state == ServiceMoveRequest.APPROVED
    assert service_going_to_sandbox_partially_approved.move_requests.get().state == ServiceMoveRequest.APPROVED
