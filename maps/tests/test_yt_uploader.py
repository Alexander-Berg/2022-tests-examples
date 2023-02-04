from datetime import datetime

import yt.wrapper as yt
import maps.analyzer.pylibs.envkit as envkit
import maps.analyzer.toolkit.lib as tk
from maps.analyzer.services.eta_comparison.lib.yt_uploader import (
    YtUploader, PROCESS_NAME,
    EtaQualityCmpTool
)


def test_yt_uploader():
    EtaQualityCmpTool.LOGS.path = '//dir'
    conf = {
        'rows_in_buffer': 2
    }
    dt = datetime.utcnow().date()
    logs_path = tk.sources.path_for_date(EtaQualityCmpTool.LOGS, dt)

    expected_rows = [{'foo': 'bar'}, {'bar': 'baz'}]
    with YtUploader(conf) as uploader:
        for row in expected_rows:
            uploader.upload(row)

        assert uploader.buffer == []
        yt_rows = list(yt.read_table(logs_path, format='yson'))
        assert expected_rows == yt_rows
        assert yt.get(f'{logs_path}/@{envkit.yt.PROCESS_ATTRIBUTE_NAME}') == PROCESS_NAME
