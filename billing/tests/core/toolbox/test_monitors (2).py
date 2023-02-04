from mdh.core.importer.base import Importer
from mdh.core.actions import Action, ActionResult, ActionParams
from mdh.core.toolbox.monitors import monitor_queue, monitor_lb_write, monitor_lb_read


class ActionAParams(ActionParams):

    q: str = ''


class ActionA(Action):

    id = 90
    alias = 'A'
    params = ActionAParams

    def run(self) -> ActionResult:
        return ActionResult(success=True)


class ActionB(Action):

    id = 91
    alias = 'B'

    def run(self) -> ActionResult:
        return ActionResult(data={'x': 'y'})


def test_queue(init_user, mock_solomon):

    user = init_user()

    ActionA.run_in_background(subjects=[user])
    ActionA.run_in_background(subjects=[user], params=ActionAParams(q='w'))
    ActionB.run_in_background(subjects=[user])

    metrics, sent = monitor_queue()

    assert sent
    assert len(metrics) == 2

    for metric in metrics:
        assert metric.name == 'queue'
        labels = metric.labels
        assert labels['status'] == 'scheduled'
        action = labels['action']
        assert action in {'A', 'B'}

        if action == 'A':
            assert metric.value == 2
        else:
            assert metric.value == 1


def test_mon_lb_write(mock_solomon):

    metrics, sent = monitor_lb_write(
        stats_expected={
            'ref1': 10,
            'ref2': 10,
        },
        stats_actual={
            'ref1': 10,
            'ref2': 5,
        },
        resync=False
    )
    assert sent
    assert len(metrics) == 4
    assert metrics[0].value == 10
    assert metrics[3].labels['subj'] == 'actual'
    assert metrics[3].labels['action'] == 'export'
    assert metrics[3].labels['ref'] == 'ref2'
    assert metrics[3].value == 5

    metrics, sent = monitor_lb_write(
        stats_expected={},
        stats_actual={},
        resync=False
    )
    assert not sent


def test_mon_lb_read(mock_solomon):

    imp = Importer(records=[], batch=False)
    imp.stats_update(node='n1', reference='r1', action='add', actual=False)
    imp.stats_update(node='n1', reference='r1', action='add', actual=False)
    imp.stats_update(node='n1', reference='r2', action='add', actual=False)
    imp.stats_update(node='n2', reference='r3', action='update', actual=False)
    imp.stats_update(node='n2', reference='r3', action='update', actual=True)

    metrics, sent = monitor_lb_read(stats=list(imp.stats.values()))
    assert sent
    assert len(metrics) == 4
    assert metrics[0].labels['subj'] == 'expected'
    assert metrics[0].value == 2
    assert metrics[3].labels['subj'] == 'actual'
    assert metrics[3].value == 1

    metrics, sent = monitor_lb_read(
        stats=[]
    )
    assert not sent
