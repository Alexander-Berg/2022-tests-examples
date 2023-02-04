from maps.wikimap.stat.tasks_payment.dictionaries.linked_accounts.lib import linked_accounts
from cyson import UInt
from nile.api.v1 import (
    Record,
    clusters,
    local
)


def run_job(users_dump, staff_dump):
    cluster = clusters.MockCluster()
    job = cluster.job()
    job = linked_accounts.make_job(job, '2020-03-02', 'logs', 'analytics', 'results')

    result = []
    job.local_run(
        sources={
            'users_dump': local.StreamSource(users_dump),
            'staff_dump': local.StreamSource(staff_dump)
        },
        sinks={'result': local.ListSink(result)}
    )
    return result


def test():
    result = run_job(
        users_dump=[
            Record(puid=b'101', um_login=b'yndx-ivan'),
            Record(puid=b'102', um_login=b'yndx-alex'),
            Record(puid=b'103', um_login=b'yndx-petr'),
            Record(puid=b'104', um_login=b'yndx.Igor'),
            Record(puid=b'105', um_login=b'yndx-igor-world')

        ],
        staff_dump=[
            Record(login=b'alex', uid=b'202', nmaps_logins=[]),
            Record(login=b'petr', uid=b'203', nmaps_logins=[b'yndx-petr']),
            Record(login=b'igor', uid=b'204', nmaps_logins=[b'yndx-igor', b'yndx-igor-world'])
        ]
    )

    assert result == [
        Record(puid=UInt(103), login=b'yndx-petr', staff_uid=UInt(203), staff_login=b'petr',       is_primary_link=True),
        Record(puid=UInt(104), login=b'yndx-igor', staff_uid=UInt(204), staff_login=b'igor',       is_primary_link=True),
        Record(puid=UInt(105), login=b'yndx-igor-world', staff_uid=UInt(204), staff_login=b'igor', is_primary_link=False)
    ]
