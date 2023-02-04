from billing.dcsaap.backend.core.models import Diff
from billing.dcsaap.backend.api.views.stat import Stat


def test_count_groups(some_diffs):
    first_diff = some_diffs[0]
    first_diff.close()
    first_diff.save()

    expected = {
        Diff.STATUS_NEW: 1,
        Diff.STATUS_CLOSED: 1,
    }

    result = Stat.count_groups(Diff, 'status')
    assert result == expected

    result = Stat.count_groups(Diff, 'status', True)
    assert result == expected
