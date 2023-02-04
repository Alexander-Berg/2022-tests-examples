from datetime import timedelta

import factory
from django.conf import settings
from django.utils import timezone

from ok.approvements.choices import APPROVEMENT_HISTORY_EVENTS, APPROVEMENT_STAGE_STATUSES
from ok.scenarios.choices import SCENARIO_STATUSES
from ok.staff.choices import GROUP_TYPES


class UserFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'users.User'

    username = factory.Sequence(lambda n: f'username-{n}')
    staff_id = factory.Sequence(lambda n: n)
    uid = factory.Sequence(lambda n: str(n))
    email = factory.LazyAttribute(lambda obj: f'{obj.username}@yandex-team.ru')
    affiliation = 'yandex'


class ApprovementFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'approvements.Approvement'

    author = factory.Sequence(lambda n: 'author{}'.format(n))
    text = factory.Sequence(lambda n: 'Text{}'.format(n))
    uid = factory.Sequence(lambda n: 'uid{}'.format(n))
    object_id = factory.Sequence(lambda n: 'JOB-{}'.format(n))

    @factory.post_generation
    def groups(self, create, group_urls, **kwargs):
        if create and group_urls:
            for group_url in group_urls:
                approvement_group = ApprovementGroupFactory(approvement=self, group__url=group_url)
                GroupMembershipFactory(group=approvement_group.group, login=f'{group_url}_member')


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


class ApprovementGroupFactory(factory.DjangoModelFactory):

    approvement = factory.SubFactory('tests.factories.ApprovementFactory')
    group = factory.SubFactory('tests.factories.GroupFactory')

    class Meta:
        model = 'approvements.ApprovementGroup'


class ApprovementHistoryFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'approvements.ApprovementHistory'


class QueueFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'tracker.Queue'

    name = factory.Sequence(lambda n: ''.join(chr(ord('A') + int(i)) for i in str(n)))


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
    staff_id = factory.Sequence(lambda n: n)
    type = GROUP_TYPES.department

    class Meta:
        model = 'staff.Group'


class GroupMembershipFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'staff.GroupMembership'

    group = factory.SubFactory('tests.factories.GroupFactory')


class ScenarioFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'scenarios.Scenario'

    slug = factory.Sequence(lambda n: f'slug-{n}')
    name = factory.Sequence(lambda n: f'Scenario #{n}')
    author = factory.Sequence(lambda n: f'author-{n}')


class ScenarioTrackerMacroFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'scenarios.ScenarioTrackerMacro'

    scenario = factory.SubFactory('tests.factories.ScenarioFactory')
    name = factory.LazyAttribute(lambda obj: obj.scenario.name)
    tracker_queue = factory.SubFactory('tests.factories.QueueFactory')
    tracker_id = factory.Sequence(lambda n: n)
    is_active = factory.LazyAttribute(lambda obj: obj.scenario.status == SCENARIO_STATUSES.active)


class ScenarioResponsibleGroupFactory(factory.DjangoModelFactory):

    class Meta:
        model = 'scenarios.ScenarioResponsibleGroup'

    scenario = factory.SubFactory('tests.factories.ScenarioFactory')
    group = factory.SubFactory('tests.factories.GroupFactory')


def create_users(*usernames, **kwargs):
    return [UserFactory(username=username, **kwargs) for username in usernames]


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
        if complex_stage.status == APPROVEMENT_STAGE_STATUSES.current and i == 1:
            child_kwargs['status'] = APPROVEMENT_STAGE_STATUSES.current
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


def create_history_entries(objects, event, user, **kwargs):
    for obj in objects:
        ApprovementHistoryFactory(
            event=event,
            user=user,
            content_object=obj,
            status=obj.status,
            resolution=getattr(obj, 'resolution', ''),
            **kwargs
        )


def approve_stage(stage):
    stage.is_approved = True
    stage.status = APPROVEMENT_STAGE_STATUSES.approved
    stage.save()


def create_pinged_approvement(stages_count=None, approvers=None, ping_time=None, **kwargs):
    approvement = create_approvement(stages_count, approvers, **kwargs)
    create_history_entries(
        objects=approvement.stages.current().leaves(),
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
