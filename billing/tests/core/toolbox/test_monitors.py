from datetime import datetime

from dwh.core.toolbox.monitors import monitor_works


def test_works(mock_solomon, init_work):

    work1 = init_work()
    work2 = init_work({'meta': {'task_name': 'echo'}, 'params': {'b': 'someval', 'a': 555}})

    for work in [work1, work2]:
        work.status = work.Status.STARTED
        work.dt_start = datetime(2022, 2, 5)
        work.target = 'vla'
        work.save()

    metrics, sent = monitor_works()

    assert sent
    assert len(metrics) == 2

    for idx, metric in enumerate(metrics):

        assert metric.name == 'works'

        labels = metric.labels
        assert labels['status'] == 'started'
        assert labels['dc'] == 'vla'

        assert metric.value > 0

        if idx == 1:
            assert labels['params'] == 'a=555;b=someval'
