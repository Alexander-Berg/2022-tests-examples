EMPTY_LOGS = [
    {'date': b'2021-10-30', 'checks': b'test', 'event_type': b'message_processed', 'login': b'not-dummy-user', 'method': b'telegram'},
]

EMPTY_RESULT = [
    {
        'alerts': [],
        'alerts_count': 0,
        'alerts_count_stable': 0,
        'alerts_count_distinct': 0,
        'alerts_count_distinct_stable': 0,
        'date': b'2021-10-30'
    },
]

NON_EMPTY_LOGS = [
    {'date': b'2021-10-30', 'checks': b'testing', 'event_type': b'message_processed', 'login': b'dummy-user', 'method': b'telegram'},
    {'date': b'2021-10-30', 'checks': b'testing2', 'event_type': b'failed', 'login': b'dummy-user', 'method': b'telegram'},
    {'date': b'2021-10-30', 'checks': b'testing3', 'event_type': b'message_processed', 'login': b'dummy-user2', 'method': b'telegram'},
    {'date': b'2021-10-30', 'checks': b'testing5,testing4', 'event_type': b'message_processed', 'login': b'dummy-user', 'method': b'telegram'},
    {'date': b'2021-10-30', 'checks': b'testing4', 'event_type': b'message_processed', 'login': b'dummy-user', 'method': b'telegram'},
    {'date': b'2021-10-30', 'checks': b'test4', 'event_type': b'message_processed', 'login': b'dummy-user', 'method': b'telegram'},
    {'date': b'2021-10-30', 'checks': b'testing6', 'event_type': b'message_processed', 'login': b'dummy-user', 'method': b'not_telegram'},
    {'date': b'2021-10-31', 'checks': b'testing', 'event_type': b'message_processed', 'login': b'dummy-user', 'method': b'telegram'},
    {'date': b'2021-11-01', 'checks': b'unstable', 'event_type': b'message_processed', 'login': b'dummy-user', 'method': b'telegram'},
    {'date': b'2021-11-01', 'checks': b'lOaD', 'event_type': b'message_processed', 'login': b'dummy-user', 'method': b'telegram'},
]

NON_EMPTY_RESULT = [
    {
        'alerts': sorted([b'testing', b'testing5', b'testing4', b'testing4', b'test4']),
        'alerts_count': 5,
        'alerts_count_stable': 1,
        'alerts_count_distinct': 4,
        'alerts_count_distinct_stable': 1,
        'date': b'2021-10-30'
    },
    {
        'alerts': [b'testing'],
        'alerts_count': 1,
        'alerts_count_stable': 0,
        'alerts_count_distinct': 1,
        'alerts_count_distinct_stable': 0,
        'date': b'2021-10-31'
    },
    {
        'alerts': sorted([b'lOaD', b'unstable']),
        'alerts_count': 2,
        'alerts_count_stable': 0,
        'alerts_count_distinct': 2,
        'alerts_count_distinct_stable': 0,
        'date': b'2021-11-01'
    }
]
