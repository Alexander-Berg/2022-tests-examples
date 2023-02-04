from hypothesis import strategies as st

from billing.dcsaap.backend.core import models

from billing.dcsaap.backend.tests.utils import const, tz

from .check import check
from ..common import optional, utc_datetimes, non_null_text

__all__ = ['run', 'run_status', 'raw_run']

# В некоторых местах (medium.hypothesis.api.test_run.TestRunSwitchStatus.test_switch_status)
# считается, что статусы в жизненном цикле запуска # только увеличиваются.
# Это верно только, для первых трех статусов (STARTED/FINISHED/ERROR)
run_status = st.sampled_from(models.Run.STATUS_CODES[:3])

raw_run = (
    st.datetimes(min_value=const.MIN_DT, max_value=const.MAX_DT)
    .flatmap(lambda start: st.tuples(st.just(start), optional(st.datetimes(min_value=start, max_value=const.MAX_DT))))
    .flatmap(
        lambda start_end: st.fixed_dictionaries(
            {
                'started': st.just(start_end[0]).map(tz.to_utc_tz),
                'finished': st.just(start_end[1]).map(tz.to_utc_tz),
                'updated': utc_datetimes(min_value=start_end[0], max_value=start_end[1] or const.MAX_DT),
                'check_model': check,
                'type': st.integers(min_value=0, max_value=1),
                'status': run_status,
                'error': optional(non_null_text(max_size=2024)),
                'workflow_id': optional(non_null_text(max_size=72)),
                'instance_id': optional(non_null_text(max_size=72)),
            }
        )
    )
)

raw_runs = st.lists(raw_run, max_size=1, min_size=1)
run = raw_run.map(lambda k: models.Run.objects.create(**k))
runs = st.lists(run, max_size=100)
run_twins = raw_run.map(lambda k: (models.Run.objects.create(**k), models.Run.objects.create(**k)))
