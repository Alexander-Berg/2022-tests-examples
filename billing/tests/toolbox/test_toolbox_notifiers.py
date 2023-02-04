from bcl.exceptions import BclException
from bcl.toolbox.notifiers import SentryNotifier


def test_sentry():
    sent = SentryNotifier({'realm': 'some'}).send('text')
    assert 'text' in sent

    try:
        raise BclException('damn')

    except Exception as e:
        sent = SentryNotifier().send(e)
        assert 'damn' in sent
