import pytest

from billing.apikeys.apikeys import mapper


@pytest.fixture(params=[
    (1000, None, 864),
    (1000, 1000, 86),
    (9000, 3200, 27),
], ids=lambda p: '{} limit with {} min checks'.format(p[0], p[1] if p[1] else 'DEFAULT'))
def limit_with_parametrized_min_checks(request, simple_service):
    (limit, min_checks, expected_max_delay) = request.param
    limit_config = mapper.LimitConfig(
        name=simple_service.cc + '_hits',
        service_id=simple_service.id,
        unit_id=1,
        cron_string='0 0 * * * *',
        cron_timezone='+0300',
        limit=limit,
        min_checks_for_period=min_checks,
    ).save()
    yield (limit_config, expected_max_delay)
    limit_config.delete()


def test_limit_check_period(mongomock, limit_with_parametrized_min_checks):
    limit_config, expected_max_delay = limit_with_parametrized_min_checks
    link = mapper.ProjectServiceLink(
        id='test_link_id',
        project_id='fake_project',
        service_id=limit_config.service_id,
        config={},
    ).save()
    limit_instance = link.limits[limit_config.name]
    zero_point_date = limit_instance.zero_point_date()
    next_check_dt = limit_instance.get_next_check(0, now=zero_point_date)
    check_delay = next_check_dt - zero_point_date
    assert check_delay.seconds == expected_max_delay


def test_check_limit_withoud_lock_reason_is_always_non_blocking(mongomock, simple_link):
    service_id = simple_link.service.id
    service_cc = simple_link.service.cc
    limit_config = mapper.LimitConfig(
        name=f'{service_cc}_hits',
        service_id=service_id,
        unit_id=1,
        cron_string='0 0 * * * *',
        cron_timezone='+0300',
        limit=10,
        min_checks_for_period=1,
    ).save()
    limit_checker = mapper.ProjectLinkLimitChecker(
        check_cache={
            f'{service_cc}_hits': {
                '_value_rolled': 10,
                '_value_unrolled': 0,
                'counters_cache': {

                }
            }
        },
        link_id=simple_link.id
    ).save()
    limit_checker.exceed([
        mapper.LinkLimitConfigInstance(limit_config)
    ])
    simple_link.reload()
    assert not simple_link.config.banned


def test_check_limit_with_lock_reason_and_zero_limit_is_permanently_blocking(mongomock, simple_link):
    service_id = simple_link.service.id
    service_cc = simple_link.service.cc
    limit_config = mapper.LimitConfig(
        name=f'{service_cc}_hits',
        service_id=service_id,
        unit_id=1,
        cron_string='0 0 * * * *',
        cron_timezone='+0300',
        limit=0,
        lock_reason=1,
        min_checks_for_period=1,
    ).save()
    limit_checker = mapper.ProjectLinkLimitChecker(
        check_cache={
            f'{service_cc}_hits': {
                '_value_rolled': 0,
                '_value_unrolled': 0,
                'counters_cache': {

                }
            }
        },
        link_id=simple_link.id
    ).save()
    limit_checker.exceed([
        mapper.LinkLimitConfigInstance(limit_config)
    ])
    simple_link.reload()
    assert simple_link.config.banned
    assert simple_link.config.ban_reason_id == 1
