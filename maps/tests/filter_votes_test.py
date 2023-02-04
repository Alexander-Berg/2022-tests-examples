from datetime import date

from maps.infopoint.tools.remove_duplicate_votes.lib.filter_votes import (
    filter_votes
)

VOTES = [
    {
        "user": "1",
        "time": date(2003, 12, 29),
        "vote": 0.3
    },
    {
        "user": "1",
        "time": date(2003, 12, 30),
        "vote": -0.3
    },
    {
        "user": "2",
        "time": date(2003, 12, 29),
        "vote": -0.3
    },
]

EXPECTED_VOTES = [
    {
        "user": "2",
        "time": date(2003, 12, 29),
        "vote": -0.3
    },
    {
        "user": "1",
        "time": date(2003, 12, 30),
        "vote": -0.3
    },
]


def test_export_infopoint():
    assert filter_votes(VOTES) == EXPECTED_VOTES
