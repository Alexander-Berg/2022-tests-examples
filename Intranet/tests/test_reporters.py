import csv
import io
from builtins import object, range, str
from uuid import uuid4

from mock import MagicMock

from django.core.files.storage import default_storage

from kelvin.reports.reports import CsvReporter, FilesReporter


class TestReporters(object):
    COLS_COUNT = 2
    ROWS_COUNT = 3

    def test_files(self, mocker):

        data = []
        for _ in range(TestReporters.ROWS_COUNT):
            data.append((str(uuid4()), str(uuid4())))

        iterator = iter(data)

        saved_content = []

        def mock_save(filename, content):
            saved_content.append(content.file.getvalue())
            return filename

        mocker.patch.object(
            default_storage,
            'save',
            new=MagicMock(side_effect=mock_save)
        )

        reporter = FilesReporter()
        reporter.from_iterable(str(uuid4()), iterator)

        assert default_storage.save.call_count == TestReporters.ROWS_COUNT
        assert tuple(saved_content) == tuple(item[1] for item in data)
