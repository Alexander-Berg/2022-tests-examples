import balancer.test.configs.lib.cfg as cfg


def test_admin(balancer_fixture):
    balancer_fixture.start()


def test_sane_timeouts(balancer_parsed_config):
    cfg.sane_timeouts(balancer_parsed_config)


def test_all_weights_non_zero(balancer_parsed_config):
    cfg.all_weights_non_zero(balancer_parsed_config)


def test_sane_attempts_count(balancer_parsed_config):
    cfg.sane_attempts_count(balancer_parsed_config)


def extract_knoss_project(section_name):
    if section_name.startswith('migration_prod_'):
        return section_name[len('migration_prod_'):]
    elif section_name.startswith('migration_knoss_'):
        return section_name[len('migration_knoss_'):]
    raise ValueError('"{}" is not a valid knoss section name'.format(section_name))


def check_knoss_weights(rr):
    knoss_sections = []
    for section_name in rr:
        if 'migration' in section_name:
            knoss_sections.append(section_name)
    if len(knoss_sections) != 0:
        assert len(knoss_sections) == 2
        for knoss_section in knoss_sections:
            section = rr[knoss_section]
            knoss_project = extract_knoss_project(knoss_section)

            if 'prod' in knoss_section:
                assert section['weight'] == -1, 'knoss prod should be turned off for {}'.format(knoss_project)
            else:
                assert section['weight'] == 1, 'knoss should be turned on for {}'.format(knoss_project)


def test_knoss_weights(balancer_parsed_config):
    cfg.process_config(balancer_parsed_config, cfg.process_balancer(check_knoss_weights))


def check_srcrwr(name, params, path):
    assert name != 'srcrwr'


def test_no_srcrwr_in_term(balancer_parsed_config):
    cfg.process_config(balancer_parsed_config, check_srcrwr)


def test_service_alerts(balancer_parsed_config):
    cfg.service_alerts(balancer_parsed_config, 'kubr')
