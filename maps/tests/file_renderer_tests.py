from collections import defaultdict
from pathlib import Path
import pytest

import maps.infra.baseimage.template_generator.lib.template_generator as tg


@pytest.fixture
def empty_config():
    return defaultdict(lambda: '')


@pytest.fixture
def minimal_config(empty_config):
    empty_config.update({
        'app': {
            'name': 'testapp'
        },
        'logbroker': {
            'topics': [{'tag': 'TAG', 'path': '/path'}]
        },
        'push_client': {
            'logger': defaultdict(lambda: ''),
            'env_client_id': defaultdict(lambda: '')
        }
    })

    return empty_config


def default_conf_template(fakefs):
    sample = Path('etc/foo.default.conf')
    fakefs.fs.create_file(fakefs.templates / sample)

    return sample


def overwriting_template(fakefs):
    fakefs.fs.create_file(fakefs.output / default_conf_template(fakefs))


def test_renderer_dry_run(empty_config, fakefs):
    ''' Logbroker topic generation is skipped if no topics configured.
    '''
    with fakefs() as fakefs:
        renderer = tg.FileRenderer(empty_config, fakefs.templates, True)
        renderer.process_files(fakefs.output)

        assert list(fakefs.output.rglob('*')) == []


def test_link_environment(fakefs, empty_config):
    ''' Process environment specs for regular configuration files.
    '''
    with fakefs() as fakefs:
        conf_template = default_conf_template(fakefs)
        renderer = tg.FileRenderer(empty_config, fakefs.templates, True)
        renderer.process_files(fakefs.output)

        symlink = fakefs.output / "etc" / "foo.conf"
        realfile = fakefs.output / conf_template

        generated_layout = fakefs.output.rglob('*.conf')
        assert sorted([symlink, realfile]) == sorted(generated_layout)
        assert symlink.is_symlink()
        assert symlink.samefile(realfile)


def test_override_whitelisted_path(fakefs, empty_config):
    ''' Allow overwrite of whitelisted files
    '''
    with fakefs() as fakefs:
        overwriting_template(fakefs)
        empty_config.update(
            {tg.TEMPLATE_OVERWRITE_WHITELIST_PARAM_NAME: ['*.conf']})

        renderer = tg.FileRenderer(empty_config, fakefs.templates, True)
        renderer.process_files(fakefs.output)


def test_override_non_whitelited_path(fakefs, empty_config):
    ''' Raise on file overwrite if not whitelisted
    '''
    with fakefs() as fakefs:
        overwriting_template(fakefs)
        empty_config.update(
            {tg.TEMPLATE_OVERWRITE_WHITELIST_PARAM_NAME: ['*.json']})

        renderer = tg.FileRenderer(empty_config, fakefs.templates, True)
        with pytest.raises(Exception):
            renderer.process_files(fakefs.output)
