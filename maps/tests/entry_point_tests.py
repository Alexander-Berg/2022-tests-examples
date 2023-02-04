import pytest

import maps.infra.baseimage.template_generator.lib.template_generator as tg
from maps.infra.baseimage.template_generator.lib.config import CONFIG_VARS_DIR_NAME
from unittest.mock import (ANY, create_autospec)


TOPLEVEL_CONFIG_CONTENT = '''
app:
  foo:
    bar: True
'''


DEPENDENT_CONFIG_CONTENT = '''
app:
  baz: {{app.foo.bar}}
'''


@pytest.fixture(autouse=True)
def autouse(mock_subprocess):
    pass


@pytest.fixture
def mock_renderer(monkeypatch):
    renderer = create_autospec(tg.FileRenderer)
    monkeypatch.setattr(tg, 'FileRenderer', renderer)

    return renderer


def sample_config(fakefs):
    configfile = fakefs.configs / "config.yaml"
    fakefs.fs.create_file(configfile, contents=TOPLEVEL_CONFIG_CONTENT)

    return configfile


def invalid_nested_config(fakefs):
    invalid_config = fakefs.configs / CONFIG_VARS_DIR_NAME / "config.jaml"
    fakefs.fs.create_file(invalid_config)
    return invalid_config


def ordered_nested_configs(fakefs):
    root = fakefs.configs / CONFIG_VARS_DIR_NAME
    initial = root / "01-config.yaml"
    additional = root / "02-additional.yaml"

    fakefs.fs.create_file(initial, contents=TOPLEVEL_CONFIG_CONTENT)
    fakefs.fs.create_file(additional, contents=DEPENDENT_CONFIG_CONTENT)

    return [initial, additional]


def default_generate(filesystem, config_files: list[str] = None):
    tg.generate(config_files or [],
                filesystem.output,
                filesystem.configs,
                filesystem.templates,
                False)


def test_dry_run(fakefs, mock_renderer):
    with fakefs() as fakefs:
        default_generate(fakefs)
        mock_renderer.assert_called_once_with(
            ANY, fakefs.templates, False)
        mock_renderer.return_value.process_files.assert_called_once_with(
            fakefs.output)


def test_template_dir_not_set(fakefs, mock_renderer):
    with fakefs() as fakefs:
        tg.generate([], fakefs.output, fakefs.configs, None, False)
        mock_renderer.assert_called_once_with(
            ANY, fakefs.configs / tg.TEMPLATES_DIR_NAME, ANY)


def test_raise_missing_mandatory_config_file(fakefs, mock_renderer):
    with fakefs() as fakefs:
        with pytest.raises(FileNotFoundError):
            default_generate(fakefs, [str(fakefs.configs / "config.yaml")])


def test_process_mandatory_config_file(fakefs, mock_renderer):
    with fakefs() as fakefs:
        sample_config_fakefs = sample_config(fakefs)
        default_generate(fakefs, [str(sample_config_fakefs)])

        config, *_ = mock_renderer.call_args.args
        assert config.get('app', {}).get('foo', {}).get('bar')


def test_missing_optional_config_file(fakefs, mock_renderer):
    with fakefs() as fakefs:
        default_generate(fakefs, [f'?{fakefs.configs / "config.yaml"}'])

        config, *_ = mock_renderer.call_args.args
        assert 'foo' not in config


def test_present_optional_config_file(fakefs, mock_renderer):
    with fakefs() as fakefs:
        sample_config_fakefs = sample_config(fakefs)
        default_generate(fakefs, [f'?{sample_config_fakefs}'])

        config, *_ = mock_renderer.call_args.args
        assert 'foo' in config['app'] and 'bar' in config['app']['foo']


def test_raise_invalid_config_extension(fakefs):
    with fakefs() as fakefs:
        invalid_nested_config(fakefs)
        with pytest.raises(Exception):
            default_generate(fakefs)


def test_successive_config_load(fakefs, mock_renderer):
    with fakefs() as fakefs:
        ordered_nested_configs(fakefs)
        default_generate(fakefs)

        config, *_ = mock_renderer.call_args.args
        assert config['app']['foo']['bar'] == config['app']['baz']
