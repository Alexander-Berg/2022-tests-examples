import mock


def test_worker_stat():
    from staff.monitorings.unistat import _workers_stat
    uwsgi_stat_json = {
        'workers': [
            {'status': 'busy'},
            {'status': 'busy'},
            {'status': 'idle'},
            {'status': 'busy'},
        ]
    }
    with mock.patch('staff.monitorings.unistat.get_uwsgi_stats_json', return_value=uwsgi_stat_json):
        assert _workers_stat() == [
            ['staff_uwsgi_workers_total_ammx', 4],
            ['staff_uwsgi_workers_busy_count_ammx', 3],
            ['staff_uwsgi_workers_busy_percent_axxx', 75]
        ]
