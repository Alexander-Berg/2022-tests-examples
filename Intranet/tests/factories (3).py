from datetime import timedelta

import factory
from django.conf import settings
from django.utils import timezone

from ok.approvements.choices import APPROVEMENT_HISTORY_EVENTS, APPROVEMENT_STAGE_STATUSES
from ok.approvements.models import create_history_entries


class ApprovementFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'approvements.Approvement'

    author = factory.Sequence(lambda n: 'author{}'.format(n))
    text = factory.Sequence(lambda n: 'Text{}'.format(n))
    uid = factory.Sequence(lambda n: 'uid{}'.format(n))
    object_id = factory.Sequence(lambda n: 'JOB-{}'.format(n))


def _get_stage_status(obj):
    if obj.is_approved is None:
        return APPROVEMENT_STAGE_STATUSES.pending
    return APPROVEMENT_STAGE_STATUSES.approved


class ApprovementStageFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'approvements.ApprovementStage'

    approvement = factory.SubFactory('tests.factories.ApprovementFactory')
    approver = factory.Sequence(lambda n: 'approver{}'.format(n))
    is_approved = None
    # Note: нумерация глобальная, а не в рамках одного согласования
    position = factory.Sequence(lambda n: n)
    status = factory.LazyAttribute(_get_stage_status)


class ApprovementHistoryFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'approvements.ApprovementHistory'


class QueueFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'tracker.Queue'


class SwitchFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'waffle.Switch'


class FlagFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'waffle.Flag'


class FlowFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'flows.Flow'


class GroupFactory(factory.DjangoModelFactory):

    url = factory.Sequence(lambda n: f'group_{n}')

    class Meta:
        model = 'staff.Group'


class GroupMembershipFactory(factory.DjangoModelFactory):

    group = factory.SubFactory('tests.factories.GroupFactory')

    class Meta:
        model = 'staff.GroupMembership'


def create_approvement(stages_count=None, approvers=None, **kwargs):
    """
    Создать согласование со стадиями.

    Параметры stages_count и approvers взаимоисключающие.
    :param stages_count: Число стадий в согласовании.
      По-умолчанию: 1, если не задано approvers.
    :param approvers: Для каждого согласующего будет создана одна стадия согласования.
      Порядок согласующих сохраняется.
    """
    assert not (stages_count and approvers), 'Define no more than one of stages_count and approvers'
    approvers = approvers or []
    stages_count = len(approvers) or stages_count or 1
    assert stages_count > 0, 'stages_count must be positive'

    approvement = ApprovementFactory(**kwargs)
    for position in range(stages_count):
        additional_args = {'approver': approvers[position]} if approvers else {}
        ApprovementStageFactory(
            approvement=approvement,
            position=position,
            **additional_args
        )
    approvement.next_stages.update(status=APPROVEMENT_STAGE_STATUSES.current)
    return approvement


def create_complex_stage(approvers, **kwargs):
    complex_stage = create_parent_stage(**kwargs)
    for i, approver in enumerate(approvers, start=1):
        child_kwargs = {}
        if complex_stage.is_approved:
            child_kwargs['is_approved'] = i <= complex_stage.need_approvals
            if not child_kwargs['is_approved']:
                child_kwargs['status'] = APPROVEMENT_STAGE_STATUSES.cancelled
        create_child_stage(
            parent=complex_stage,
            approver=approver,
            position=complex_stage.position + i,
            **child_kwargs,
        )
    return complex_stage


def create_parent_stage(**kwargs):
    return ApprovementStageFactory(approver='', **kwargs)


def create_child_stage(parent, **kwargs):
    return ApprovementStageFactory(
        approvement=parent.approvement,
        parent=parent,
        **kwargs,
    )


def approve_stage(stage):
    stage.is_approved = True
    stage.status = APPROVEMENT_STAGE_STATUSES.approved
    stage.save()


def create_pinged_approvement(stages_count=None, approvers=None, ping_time=None, **kwargs):
    approvement = create_approvement(stages_count, approvers, **kwargs)
    create_history_entries(
        objects=approvement.current_stages,
        event=APPROVEMENT_HISTORY_EVENTS.ping_sent,
        user=None,
        **{'created': ping_time} if ping_time else {},
    )
    return approvement


def make_approvement_stages_overdue(stages):
    for stage in stages:
        ApprovementHistoryFactory(
            content_object=stage,
            event=APPROVEMENT_HISTORY_EVENTS.ping_sent,
            created=(timezone.now() - timedelta(settings.APPROVEMENT_STAGE_OVERDUE_DAYS + 1)),
        )


def get_users(number=2):
    return ['login{}'.format(n) for n in range(number)]


def create_waffle_switch(name, active=True):
    return SwitchFactory(name=name, active=active)


def create_waffle_flag(name, active=True):
    return FlagFactory(name=name, everyone=active)
