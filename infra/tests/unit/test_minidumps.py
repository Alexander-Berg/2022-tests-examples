from instancectl.config.config import RunConfig


def test_environment_pathcing_for_breakpad():
    config = RunConfig()

    env = {config.MINIDUMPS_PATH_ENV_VAR: 'incorrect_path'}
    config_dict = config.total_defaults
    config_dict['minidumps_push'] = True
    section = 'fake_section'
    config.add_minidumps_environment('fake_section', config_dict, env)
    assert env == {
        config.LD_PRELOAD_ENV_VAR: config_dict['minidumps_library'],
        config.MINIDUMPS_PATH_ENV_VAR: './minidumps/{}/'.format(section),
        config.DISABLE_INSTANCE_SIGNAL_HANDLER_ENV_VAR: '1',
    }

    old_ld_preload = env[config.LD_PRELOAD_ENV_VAR]
    config_dict['minidumps_path'] = 'custom_path'
    config_dict['minidumps_library'] = 'custom_library'
    config.add_minidumps_environment('fake_section', config_dict, env)
    assert env == {
        config.LD_PRELOAD_ENV_VAR: ' '.join((old_ld_preload, config_dict['minidumps_library'])),
        config.MINIDUMPS_PATH_ENV_VAR: config_dict['minidumps_path'],
        config.DISABLE_INSTANCE_SIGNAL_HANDLER_ENV_VAR: '1',
    }
