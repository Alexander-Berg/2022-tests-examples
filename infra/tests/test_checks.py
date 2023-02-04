import infra.reconf_juggler.checks


def test_META():
    check = infra.reconf_juggler.checks.META({})
    expected = {
        'aggregator': 'timed_more_than_limit_is_problem',
        'aggregator_kwargs': {'limits': [{'crit': '5%',
                                          'day_end': 7,
                                          'day_start': 1,
                                          'time_end': 23,
                                          'time_start': 0,
                                          'warn': '3%'}]},

        'check_options': None,
        'creation_time': None,
        'description': '',
        'meta': {'reconf': {'class': 'infra.reconf_juggler.checks.META'}},
        'mtime': None,
        'namespace': None,
        'notifications': [],
        'refresh_time': 90,
        'tags': ['category_unreach', 'level_leaf', 'level_root'],
        'ttl': 900
    }

    assert expected == check.build()


def test_UNREACHABLE():
    check = infra.reconf_juggler.checks.UNREACHABLE({})
    expected = {
        'active': 'icmpping',
        'aggregator': 'timed_more_than_limit_is_problem',
        'aggregator_kwargs': {'limits': [{'crit': '5%',
                                          'day_end': 7,
                                          'day_start': 1,
                                          'time_end': 23,
                                          'time_start': 0,
                                          'warn': '3%'}]},
        'check_options': None,
        'creation_time': None,
        'description': '',
        'meta': {'reconf': {'class': 'infra.reconf_juggler.checks.UNREACHABLE'}},
        'mtime': None,
        'namespace': None,
        'notifications': [],
        'refresh_time': 90,
        'tags': ['category_unreach', 'level_leaf', 'level_root'],
        'ttl': 900
    }

    assert expected == check.build()


def test_ssh():
    check = infra.reconf_juggler.checks.ssh({})
    expected = {
        'active': 'ssh',
        'active_kwargs': {'timeout': 40},
        'aggregator': 'timed_more_than_limit_is_problem',
        'aggregator_kwargs': {'limits': [{'crit': '5%',
                                          'day_end': 7,
                                          'day_start': 1,
                                          'time_end': 23,
                                          'time_start': 0,
                                          'warn': '3%'}]},
        'check_options': None,
        'creation_time': None,
        'description': '',
        'meta': {'reconf': {'class': 'infra.reconf_juggler.checks.ssh'}},
        'mtime': None,
        'namespace': None,
        'notifications': [],
        'refresh_time': 90,
        'tags': ['category_infra', 'level_leaf', 'level_root'],
        'ttl': 900
    }

    assert expected == check.build()
