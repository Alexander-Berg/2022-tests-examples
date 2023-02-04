from yatest.common import source_path
import pytest

import maps.analyzer.toolkit.lib as tk


def test_config():
    config_dict = tk.config.read_json_config(
        source_path('maps/analyzer/toolkit/tests/data/config/config.json'),
    )

    assert tk.paths.Common.SIGNALS.value == '//home/production/tmp/signals'
    assert tk.paths.Assessors.USERS.value == '//home/production/tmp/users'
    assert tk.models.Models.DEFAULT.value == '//home/production/pools/default'
    assert config_dict['fake'] == 10
    assert tk.config.paths_config(config_dict['source']) == tk.paths.Assessors.USERS


def test_config_fail():
    with pytest.raises(ValueError):
        tk.config.read_json_config(
            source_path('maps/analyzer/toolkit/tests/data/config/config.corrupted.json'),
        )


def test_paths_config():
    assert tk.config.paths_config('Common') == tk.paths.Common
    assert tk.config.paths_config('Common.TRAVEL_TIMES') == tk.paths.Common.TRAVEL_TIMES


def test_yt_path():
    assert tk.config.yt_path('Common.TRAVEL_TIMES') == tk.paths.Common.TRAVEL_TIMES
    assert tk.config.yt_path('TRAVEL_TIMES', tk.paths.Common) == tk.paths.Common.TRAVEL_TIMES
    assert tk.config.yt_path('//home', tk.paths.Assessors) == '//home'
    with pytest.raises(AttributeError):
        tk.config.yt_path('TAVEL_TIMES', tk.paths.Common)
    with pytest.raises(ValueError):
        tk.config.yt_path('Comon.TRAVEL_TIMES')


def test_propagate_property_in_all_sections():
    config = {
        'common': {
            'year': 2021
        },
        'name': 'John',
        'marriage': {
            'year': 1999,
            'reason': 'pregnancy'
        },
        'divorce': {
            'reason': 'infidelity'
        }
    }

    config = tk.config.propagate_property_in_all_sections(config, name='common')
    assert config['marriage']['year'] == 1999
    assert config['divorce']['year'] == 2021
    assert 'common' not in config
