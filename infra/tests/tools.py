

class frozen_time(object):
    def __init__(self):
        import time
        self._time = time.time()
        super(frozen_time, self).__init__()

    def time(self):
        return self._time


time = frozen_time()


def monkeypatch_hw_watcher_call(monkeypatch, module, **reports):
    def mock_get_hw_watcher_status(report_name):
        assert report_name in reports

        if isinstance(reports[report_name], Exception):
            raise reports[report_name]
        else:
            return reports[report_name]

    monkeypatch.setattr(module, "get_hw_watcher_status", mock_get_hw_watcher_status)


def mock_hw_watcher_report(reason, status="OK", timestamp_flag=True, **kwargs):
    return dict(kwargs, status=status, timestamp=time.time() if timestamp_flag else 1, reason=reason)


REAL_CONTENT = "real_content"  # return real file contents


class ReadContentMocker(object):
    def __init__(self, filename_to_content, default_content):
        self.filename_to_content = filename_to_content or {}
        self.default_content = default_content or ""

    def __call__(self, filename):
        content = self.filename_to_content.get(filename, self.default_content)
        return self._interpret_value(content, filename)

    @staticmethod
    def _interpret_value(value, filename):
        if value is REAL_CONTENT:
            with open(filename) as fh:
                return fh.read()
        elif isinstance(value, Exception):
            raise value
        else:
            return value


def mock_read_content(monkeypatch, module, filename_to_content=None, default_content=None):
    monkeypatch.setattr(module, "read_content", ReadContentMocker(filename_to_content, default_content))


def mocked_timestamp():
    return 1
