import factory

from plan.unistat.models import TaskMetric


class TaskMetricFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = TaskMetric

    task_name = factory.Sequence(lambda n: 'name_%s' % n)
    send_to_unistat = False
