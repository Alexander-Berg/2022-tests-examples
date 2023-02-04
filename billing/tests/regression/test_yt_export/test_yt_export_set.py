from datetime import (
    datetime as dt
)

import luigi
from yt.wrapper import escape_ypath_literal
import pytest

from dwh.grocery.task.yt_export_task import YTExportSetTask
from dwh.grocery.targets import (
    YTMapNodeTarget,
    ChunkParam,
)
from dwh.grocery.tools import (
    to_iso,
)

from .. import is_eq


@pytest.mark.skip(reason="need fix requires method for YtExportTask due to self.requires().complete()")
def make_export_task_class(root, sources):
    class TestExportSet(YTExportSetTask):
        target = (root / YTMapNodeTarget("export_set_test/")).path
        cluster = root.cluster or ""
        source_uri = ""

        def requires(self):
            return []

        def get_sources_for_export(self):
            return sources
    return TestExportSet


# Когда-нибудь я наберусь духа и солью эти 2 теста в один, с генерациями случайных наборов случайных чанков и таблиц
@pytest.mark.skip(reason="need fix requires method for YtExportTask due to self.requires().complete()")
def test_export_nothing(yt_test_root, workers_fix):
    workers = workers_fix

    task_class = make_export_task_class(yt_test_root, [])
    task = task_class()

    is_success = luigi.build([task], local_scheduler=True, workers=workers)
    assert is_success
    assert task.complete()


@pytest.mark.skip(reason="need fix requires method for YtExportTask due to self.requires().complete()")
def test_export_some_sources(yt_test_root, workers_fix, fake_t_shipment, fake_t_product):
    workers = workers_fix

    t_shipmet_chunks = fake_t_shipment.chunk_by('dt', ChunkParam.month)
    exportables = [fake_t_product] + t_shipmet_chunks[:3:]

    update_id = to_iso(dt.now())

    sources = [
        (escape_ypath_literal(e.uri.replace(".", "_")), update_id, e)
        for e in exportables
    ]

    task_class = make_export_task_class(yt_test_root, sources)
    task = task_class()

    is_success = luigi.build([task], local_scheduler=True, workers=workers)
    assert is_success
    for ex, yt in zip(exportables, task_class().output()):
        assert is_eq(ex, yt)
    assert task.complete()

    new_task = task_class()
    is_success = luigi.build([new_task], local_scheduler=True, workers=workers)
    assert is_success
    assert new_task.complete()
