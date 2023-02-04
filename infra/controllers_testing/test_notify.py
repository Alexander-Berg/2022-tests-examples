import infra.callisto.controllers.sdk.notify as notify


def test_range():
    assert 1 in notify.Range(notify.NotifyLevels.IDLE, 0, 10)
    assert 0 in notify.Range(notify.NotifyLevels.IDLE, 0, 10)
    assert 1.5 in notify.Range(notify.NotifyLevels.IDLE, 0, 10)
    assert 10 not in notify.Range(notify.NotifyLevels.IDLE, 0, 10)
    assert 100 not in notify.Range(notify.NotifyLevels.IDLE, 0, 10)
    assert -100 not in notify.Range(notify.NotifyLevels.IDLE, 0, 10)

    assert -1000 in notify.Range(notify.NotifyLevels.IDLE, None, 10)
    assert 1000 in notify.Range(notify.NotifyLevels.IDLE, 10, None)
    for i in range(-100, 100, 10):
        assert i in notify.Range(notify.NotifyLevels.IDLE, None, None)


def test_levels():
    levels = notify.NotifyLevels
    assert levels.IDLE < levels.INFO < levels.WARNING < levels.ERROR
    assert levels.ERROR > levels.WARNING > levels.INFO > levels.IDLE
    assert levels.ERROR >= levels.ERROR >= levels.WARNING >= levels.WARNING >= levels.INFO > levels.IDLE
    assert levels.IDLE <= levels.IDLE <= levels.INFO <= levels.INFO <= levels.WARNING <= levels.WARNING


def test_value_notification():
    class A(notify.ValueNotification):
        name = 'A'
        ranges = [
            notify.Range(notify.NotifyLevels.IDLE, None, 0),
            notify.Range(notify.NotifyLevels.INFO, 0, 10),
            notify.Range(notify.NotifyLevels.WARNING, 10, 100),
            notify.Range(notify.NotifyLevels.ERROR, 100, None),
        ]

    assert A(-1).level == notify.NotifyLevels.IDLE
    assert A(0).level == notify.NotifyLevels.INFO
    assert A(5).level == notify.NotifyLevels.INFO
    assert A(10).level == notify.NotifyLevels.WARNING
    assert A(50).level == notify.NotifyLevels.WARNING
    assert A(100).level == notify.NotifyLevels.ERROR
    assert A(150).level == notify.NotifyLevels.ERROR

    A(100).solomon()

    assert len(A(100).message) > 0, 'empty message_template'

    class B(notify.ValueNotification):
        name = 'B'
        message_template = 'this is {value}. {extra_info}'
        ranges = [
            notify.Range(notify.NotifyLevels.IDLE, None, 0),
            notify.Range(notify.NotifyLevels.INFO, 0, None),
        ]

    value = 0
    extra_info = 'xxx'
    assert B(value, extra_info=extra_info).message == B.message_template.format(value=value, extra_info=extra_info)
    assert 'extra_info' in B(value, labels=dict(extra_info=extra_info)).solomon()['labels']
