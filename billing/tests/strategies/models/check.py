from hypothesis import strategies as st

from billing.dcsaap.backend.core import models, enum

from ..common import optional, non_null_text, ascii_text

__all__ = ['check', 'raw_check']

raw_check = {
    'title': st.text(max_size=256),
    'cluster': st.sampled_from(enum.CLUSTERS),
    'table1': non_null_text(min_size=1, max_size=1024),
    'ticket1': st.text(max_size=1024),
    'table2': non_null_text(min_size=1, max_size=1024),
    'ticket2': st.text(max_size=1024),
    'keys': non_null_text(min_size=1, max_size=1024),
    'columns': non_null_text(min_size=1, max_size=1024),
    'result': non_null_text(min_size=1, max_size=1024),
    'status': st.integers(min_value=0, max_value=1),
    'workflow_id': st.text(max_size=72),
    'instance_id': st.text(max_size=72),
    'debrief_queue': st.text(max_size=72),
    'is_sox': st.booleans(),
    'change_ticket': optional(st.text(max_size=1024)),
    'created_login': ascii_text(min_size=1, max_size=20),
}

check = st.builds(models.Check.objects.create, **raw_check)
