import sys
import time
import os
from contextlib import contextmanager

import pytest
import six
from _pytest.outcomes import Failed
from six import reraise


IS_ARCADIA = 'ARCADIA_SOURCE_ROOT' in os.environ


@contextmanager
def raises(exception, text=None, text_startswith=None, text_contains=None, match=None, **kwargs):
    """
    Based on util.raises, but replaces regexp-only "match" with plaintext alternatives
    """

    assert len([t for t in (text, text_startswith, text_contains, match) if t is not None]) < 2, \
        u'Can only use one of "text", "text_startswith", "text_contains", or "match"'

    with pytest.raises(exception, match=match, **kwargs) as e:
        yield e
    msg = six.text_type(e.value)
    if text is not None:
        assert msg == text
    elif text_startswith is not None:
        assert msg.startswith(text_startswith)
    elif text_contains is not None:
        assert text_contains in msg
    elif match is not None:
        e.match(match)


def wait_until(func, timeout=3, interval=0.03, must_get_value=False):
    deadline = time.time() + timeout
    ret = None
    while time.time() < deadline:
        try:
            ret = func()
            if ret:
                return ret
        except:
            pass
        finally:
            time.sleep(interval)
    if must_get_value:
        assert ret
    return func()


def wait_until_passes(func, timeout=3, interval=0.03):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            return func()
        except:
            pass
        finally:
            time.sleep(interval)
    return func()


@contextmanager
def check_log(caplog, clear=True):
    if clear:
        caplog.clear()
    caplog.records_text = lambda: '\n'.join(record.message for record in caplog.records if hasattr(record, 'message'))
    yield caplog


if IS_ARCADIA:
    from freezegun import freeze_time
else:
    from dateutil import parser, tz
    from libfaketime import fake_time
    import datetime
    import pytz


    class freeze_time(fake_time):
        # https://github.com/simon-weber/python-libfaketime/issues/1
        def __init__(self, spec, tz_offset=None, **kwargs):
            dt = self._prepare(spec, tz_offset=tz_offset)
            kwargs['only_main_thread'] = False
            super(freeze_time, self).__init__(dt, **kwargs)

        def _prepare(self, spec, tz_offset=None):
            dt = spec if isinstance(spec, datetime.datetime) else parser.parse(spec)

            # If datetime currently has tzinfo, represent it as a datetime in UTC
            utc = self._convert_to_utc(dt)

            # If a tz_offset was given, subtract that also from the datetime
            if tz_offset is not None:
                utc -= datetime.timedelta(hours=tz_offset)

            # Convert the UTC datetime to the local timezone, and remove tzinfo,
            local = utc.astimezone(tz.tzlocal())
            return local.replace(tzinfo=None)

        @staticmethod
        def _convert_to_utc(dt):
            if dt.tzinfo:
                return dt.astimezone(pytz.utc)
            else:
                return dt.replace(tzinfo=pytz.utc)


class Checker(object):
    ATTEMPT_SUCCEEDED = object()
    EXCEPTION_RETRIED = object()

    def __init__(self, timeout=3, interval=0.03):
        self._checker = None
        self._timeout = timeout
        self._interval = interval

    @contextmanager
    def _attempt(self):
        try:
            yield
        except (Failed, Exception) as e:
            outcome = self._checker.throw(e)
            assert outcome is self.EXCEPTION_RETRIED
        else:
            try:
                self._checker.send(self.ATTEMPT_SUCCEEDED)
            except StopIteration:
                pass

    def __iter__(self):
        def checker():
            deadline = time.time() + self._timeout

            try:
                outcome = yield self._attempt()
            except (Failed, Exception):
                exc_info = sys.exc_info()
            else:
                assert outcome is self.ATTEMPT_SUCCEEDED
                exc_info = None
                return

            while 1:
                outcome = yield self.EXCEPTION_RETRIED
                assert outcome is None

                if time.time() > deadline:
                    if exc_info is not None:
                        reraise(*exc_info)
                    else:
                        raise AssertionError('timeout')

                try:
                    outcome = yield self._attempt()
                except (Failed, Exception):
                    exc_info = sys.exc_info()
                else:
                    exc_info = None
                    assert outcome is self.ATTEMPT_SUCCEEDED
                    return

                time.sleep(self._interval)

        self._checker = checker()
        return self._checker


def shift(x, indent=21):
    it = iter(x.splitlines())
    rv = next(it)
    for line in it:
        rv += '\n' + ' ' * indent + line
    return rv


def t(path):
    if IS_ARCADIA:
        from yatest import common
        return common.source_path(os.path.join('infra/awacs/vendor/awacs/tests', path))
    else:
        return os.path.join('./tests/', path)
