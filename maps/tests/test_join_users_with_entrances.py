import data
from maps.wikimap.feedback.pushes.entrances.join_users_with_entrances.lib.join_users_with_entrances import (
    join_users_with_entrances,
)
from maps.wikimap.feedback.pushes.helpers import helpers
from nile.api.v1.clusters import MockYQLCluster
from nile.api.v1.local import StreamSource, ListSink
import pytest


@pytest.fixture
def job():
    cluster = MockYQLCluster()
    job = cluster.job()
    join_users_with_entrances(
        job,
        "fake_users_path",
        "fake_entrances_path",
        "fake_pushes_output",
        "fake_assignments_output",
    )
    return job


def test_join_users_with_entrances(job):
    pushes_output = []
    assignments_output = []
    job.local_run(
        sources={
            "entrances":    StreamSource(data.ENTRANCES, schema=data.ENTRANCES_SCHEMA),
            "users":        StreamSource(data.USERS),
        },
        sinks={
            "pushes":       ListSink(pushes_output),
            "assignments":  ListSink(assignments_output),
        },
    )
    helpers.compare_records_lists(
        sorted(pushes_output),
        sorted(data.PUSHES_OUTPUT),
    )
    for record in assignments_output:
        record["uids"].sort()
    helpers.compare_records_lists(
        sorted(assignments_output),
        sorted(data.ASSIGNMENTS_OUTPUT),
    )
