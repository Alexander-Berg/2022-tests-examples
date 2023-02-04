# encoding: utf-8


import factory
from django.utils import timezone

from .staff import StaffFactory
from .services import ServiceFactory
from plan.suspicion.models import (
    Complaint,
    Execution,
    ExecutionStep,
    ExecutionChain,
    Issue,
    IssueGroup,
    ServiceIssue,
    ServiceExecutionAction,
    IssueGroupThreshold,
    ServiceTrafficStatus,
    ServiceAppealIssue,
)


class ComplaintFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Complaint

    author = factory.SubFactory(StaffFactory)
    service = factory.SubFactory(ServiceFactory)
    message = factory.Sequence(lambda a: 'complaint%s' % a)


class ExecutionFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Execution

    code = factory.Sequence(lambda a: 'execution%s' % a)
    name = factory.Sequence(lambda a: 'execution%s' % a)
    name_en = factory.Sequence(lambda a: 'execution%s' % a)


class ExecutionStepFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = ExecutionStep

    execution = factory.SubFactory(ExecutionFactory)
    apply_after = timezone.timedelta()


class ExecutionChainFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = ExecutionChain

    code = factory.Sequence(lambda a: 'execution_chain%s' % a)
    name = factory.Sequence(lambda a: 'execution_chain%s' % a)
    name_en = factory.Sequence(lambda a: 'execution_chain%s' % a)


class IssueGroupFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = IssueGroup

    code = factory.Sequence(lambda a: 'issue_group%05d' % int(a))
    name = factory.Sequence(lambda a: 'issue_group%s' % a)
    name_en = factory.Sequence(lambda a: 'issue_group%s' % a)
    send_suggest = True


class IssueGroupThresholdFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = IssueGroupThreshold

    issue_group = factory.SubFactory(IssueGroupFactory)
    level = 'ok'
    threshold = 0.5
    chain = factory.SubFactory(ExecutionChainFactory)


class IssueFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = Issue

    code = factory.Sequence(lambda a: 'issue%s' % a)
    name = factory.Sequence(lambda a: 'issue%s' % a)
    name_en = factory.Sequence(lambda a: 'issue%s' % a)
    issue_group = factory.SubFactory(IssueGroupFactory)
    execution_chain = factory.SubFactory(ExecutionChainFactory)
    weight = 1
    is_active = True


class ServiceIssueFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = ServiceIssue

    service = factory.SubFactory(ServiceFactory)
    state = ServiceIssue.STATES.ACTIVE
    issue = None
    issue_group = None


class ServiceExecutionActionFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = ServiceExecutionAction

    execution = factory.SubFactory(ExecutionFactory)
    execution_chain = factory.SubFactory(ExecutionChainFactory)
    service_issue = factory.SubFactory(ServiceIssueFactory)
    should_be_applied_at = factory.Sequence(lambda a: timezone.now())


class ServiceTrafficStatusFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = ServiceTrafficStatus

    service = factory.SubFactory(ServiceFactory)
    issue_group = factory.SubFactory(IssueGroupFactory)
    issue_count = 0
    level = 'ok'


class ServiceAppealIssueFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = ServiceAppealIssue

    service_issue = factory.SubFactory(ServiceIssueFactory)
    requester = factory.SubFactory(StaffFactory)
    message = factory.Sequence(lambda a: 'appeal%s' % a)
