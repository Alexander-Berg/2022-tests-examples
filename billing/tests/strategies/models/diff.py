from hypothesis import strategies as st

from billing.dcsaap.backend.core import models

from billing.dcsaap.backend.tests.utils import const

from .run import run
from ..common import optional, utc_datetimes

__all__ = ['diff', 'diffs', 'raw_diff']

raw_diff = st.fixed_dictionaries(
    {
        'run': run,
        'key1_name': st.text(max_size=128),
        'key2_name': optional(st.text(max_size=128)),
        'key3_name': optional(st.text(max_size=128)),
        'key4_name': optional(st.text(max_size=128)),
        'key5_name': optional(st.text(max_size=128)),
        'key1_value': st.text(max_size=4000),
        'key2_value': optional(st.text(max_size=4000)),
        'key3_value': optional(st.text(max_size=4000)),
        'key4_value': optional(st.text(max_size=4000)),
        'key5_value': optional(st.text(max_size=4000)),
        'column_name': st.text(max_size=128),
        'column_value1': optional(st.text(max_size=4000)),
        'column_value2': optional(st.text(max_size=4000)),
        'type': st.sampled_from(models.Diff.TYPES_RAW),
        'issue_key': optional(st.text(max_size=256)),
        'close_dt': optional(utc_datetimes(min_value=const.MIN_DT, max_value=const.MAX_DT)),
    }
)

diff = raw_diff.map(lambda k: models.Diff.objects.create(**k))
diffs = st.lists(diff, max_size=100)
