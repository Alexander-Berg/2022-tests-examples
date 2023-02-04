import logging

import datetime
import inject
import monotonic
import pytest

from awacs.model.domain.ctl import DomainCtl
from infra.awacs.proto import model_pb2


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.DEBUG)
    inject.clear_and_configure(binder)
    yield
    inject.clear()


def test_deadlines(cache, ctx):
    ctl = DomainCtl(namespace_id='ns', domain_id='domain')
    ctl.SELF_ACTIONS_DELAY_INTERVAL = 30
    ctl.SELF_DELETION_COOLDOWN_PERIOD = 30
    ctl._pb = model_pb2.Domain()
    ctl._pb.meta.mtime.GetCurrentTime()

    curr_time = monotonic.monotonic()
    assert not ctl._ready_to_delete(ctx)

    ctl._self_deletion_check_deadline = curr_time
    assert not ctl._ready_to_delete(ctx)

    ctl._pb.meta.mtime.FromDatetime(datetime.datetime(year=2010, month=6, day=16, hour=0, minute=0, second=0))
    assert ctl._ready_to_delete(ctx)
