import pytest

from freezegun import freeze_time

from django.core.urlresolvers import reverse
from django.utils import timezone

from django_celery_beat.models import PeriodicTask, IntervalSchedule

from plan.common.utils.tasks import lock_task
from plan.monitorings.views import BaseMonitoringView
from plan.unistat.models import TaskMetric

pytestmark = pytest.mark.django_db


@pytest.fixture
def interval():
    return IntervalSchedule.objects.create(every=1, period='days')


@lock_task
def test_task():
    pass


@lock_task
def test_task1():
    pass


class Locked(object):

    def __call__(self, *args, **kwargs):
        return self

    def __enter__(self, *args, **kwargs):
        return False

    def __exit__(self, *args, **kwargs):
        pass


def test_task_create_taskmetric():
    assert not TaskMetric.objects.exists()
    test_task.apply_async()
    assert TaskMetric.objects.count() == 1
    task_metric = TaskMetric.objects.get()
    assert task_metric.task_name == 'test_task'


def test_locked_task_dont_create_taskmetric(monkeypatch, settings):
    monkeypatch.setattr('plan.common.utils.locks.locked_context', Locked())
    settings.LOCKS_ENABLED = True
    test_task.apply_async()
    assert not TaskMetric.objects.exists()


def test_failed_tasks_warn(client, staff_factory, interval):
    staff_role = 'own_only_viewer'
    PeriodicTask.objects.create(task=f'{test_task1.__module__}.{test_task1.__name__}', interval=interval)
    url = reverse('monitorings:failed-tasks')

    # Проверим, что мониторинг возвращает warning_code, если статистика о выполнении задачи отсутсвует
    client.login(staff_factory(staff_role).login)
    response = client.get(url)
    assert response.status_code == BaseMonitoringView.warning_code
    assert response.content == b'test_task1 absent in task metrics'

    # Проверим, что мониторинг возвращает warning_code, если задачи нет в ALLOWED_TASK_DELAY_TIME
    test_task1.apply_async()

    response = client.get(url)
    assert response.status_code == BaseMonitoringView.warning_code
    assert response.content == b'test_task1 absent in ALLOWED_TASK_DELAY_TIME'


@pytest.mark.parametrize(
    'staff_role',
    ('own_only_viewer',)
)
def test_failed_tasks_crit(client, settings, staff_role, staff_factory, interval):
    PeriodicTask.objects.create(task=f'{test_task.__module__}.{test_task.__name__}', interval=interval)
    now = timezone.now()
    settings.ALLOWED_TASK_DELAY_TIME['test_task'] = 100
    url = reverse('monitorings:failed-tasks')

    # Проверим, что если последнее удачное заверешение команды было больше, чем ALLOWED_TASK_DELAY_TIME секунд
    # то мониторинг вернет crit_code
    with freeze_time(now - timezone.timedelta(seconds=100+1)):
        test_task.apply_async()
    client.login(staff_factory(staff_role).login)
    response = client.get(url)
    assert response.status_code == BaseMonitoringView.crit_code
    assert response.content == b'test_task last finished at %.2f hours ago' % (101/3600)


@pytest.mark.parametrize(
    'staff_role',
    ('own_only_viewer',)
)
def test_failed_tasks_ok_if_task_disabled(client, settings, staff_role, staff_factory, interval):
    PeriodicTask.objects.create(task=f'{test_task.__module__}.{test_task.__name__}', enabled=False, interval=interval)
    now = timezone.now()
    settings.ALLOWED_TASK_DELAY_TIME['test_task'] = 100
    url = reverse('monitorings:failed-tasks')

    # Проверим, что если последнее удачное заверешение команды было больше, чем ALLOWED_TASK_DELAY_TIME секунд
    # то мониторинг вернет crit_code
    with freeze_time(now - timezone.timedelta(seconds=100 + 1)):
        test_task.apply_async()
    client.login(staff_factory(staff_role).login)
    response = client.get(url)
    assert response.status_code == BaseMonitoringView.ok_code
    assert response.content == b'ok'


@pytest.mark.parametrize(
    'staff_role',
    ('own_only_viewer',)
)
def test_failed_tasks_ok(client, settings, staff_role, staff_factory, interval):
    PeriodicTask.objects.create(task=f'{test_task.__module__}.{test_task.__name__}', interval=interval)
    settings.ALLOWED_TASK_DELAY_TIME['test_task'] = 100
    url = reverse('monitorings:failed-tasks')
    test_task.apply_async()
    client.login(staff_factory(staff_role).login)
    response = client.get(url)
    assert response.status_code == BaseMonitoringView.ok_code
    assert response.content == b'ok'


def test_taskmetric_use_in_monitoring(client, settings, interval):
    PeriodicTask.objects.create(task=f'{test_task.__module__}.{test_task.__name__}', interval=interval)
    settings.ALLOWED_TASK_DELAY_TIME['test_task'] = 100
    with freeze_time(timezone.now() - timezone.timedelta(seconds=100 + 1)):
        test_task.apply_async()

    TaskMetric.objects.update(use_for_monitoring=False)

    response = client.get(reverse('monitorings:failed-tasks'))
    assert response.status_code == BaseMonitoringView.ok_code
    assert response.content == b'ok'
