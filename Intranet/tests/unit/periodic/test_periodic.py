from plan.periodic.models import RunResult
from plan.periodic.utils import periodic_task


def test_decorator():

    assert not RunResult.objects.count()

    @periodic_task('my_task')
    def my_task(params):
        return {'a': 'b', 'c': params.get('c')}

    out = my_task()
    assert out is None

    results = list(RunResult.objects.all())
    assert len(results) == 1

    result = results[0]
    assert result.name == 'my_task'
    assert result.data['c'] is None

    result.data['c'] = 'other'
    result.save()

    my_task()
    result.refresh_from_db()
    assert result.data['c'] == 'other'
