import balancer.test.configs.lib.cfg as cfg


def test_admin(balancer_fixture):
    balancer_fixture.start()


def test_sane_timeouts(balancer_parsed_config):
    cfg.sane_timeouts(balancer_parsed_config)


def test_all_weights_non_zero(balancer_parsed_config):
    cfg.all_weights_non_zero(balancer_parsed_config)


def test_sane_attempts_count(balancer_parsed_config):
    cfg.sane_attempts_count(balancer_parsed_config)


def test_service_alerts(balancer_parsed_config):
    cfg.service_alerts(balancer_parsed_config, 'knoss_search')
