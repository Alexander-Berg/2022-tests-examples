from datacloud.features.time_hist.helpers import ActivityHistReducer


def test_activity_reducer():
    recs = [
        {'timestamp': 1546372800},   # 2019-01-01T23:00
        {'timestamp': 1546372800},   # 2019-01-01T23:00
        {'timestamp': 1546372800},   # 2019-01-01T23:00
        {'timestamp': 1546459200},   # 2019-01-02T23:00
        {'timestamp': 946731600},    # 2000-01-01T16:00
        {'timestamp': 1546419600},   # 2019-01-02T12:00
        {'timestamp': 1546506000},   # 2019-01-03T12:00
        {'timestamp': 1546592400},   # 2019-01-04T12:00
    ]
    key = {'external_id': '1_2019-02-02'}
    expected_row = {
        'external_id': '1_2019-02-02',
        'total_activity_days': {'holiday': 4},
        'hist_activity_count': {'holiday': {'23': 2, '12': 3}},
        'hist_activity_rate': {'holiday': {'23': 0.5, '12': 0.75}},
        'timezone_name': None,
    }
    reducer = ActivityHistReducer(
        holidays={'2019-01-01', '2019-01-02', '2019-01-03', '2019-01-04'},
        days_to_take=175,
        date_format='%Y-%m-%d',
        ext_id_key='external_id'
    )
    assert list(reducer(key, recs)) == [expected_row]
