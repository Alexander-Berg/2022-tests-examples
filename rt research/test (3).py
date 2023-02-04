import pytest

import irt.config
import irt.utils


def test_config():
    assert irt.config.__version__ == irt.utils.__version__

    with pytest.raises(TypeError):
        class Config(irt.config.Config):
            pass

    with pytest.raises(RuntimeError):
        irt.config.Config()

    config = irt.config.Config('example',
                               initial_config={
                                   'key1': 'value1',
                                   'key2': 'value2',
                                   'key4': irt.utils.read_env_or_file(None, 'file.ext'),
                                   'section1': {
                                       'section_key1': 'section_value1',
                                       'key1': '1',
                                       'key2': '2',
                                   }
                               },
                               config_patch={'key2': 'value', 'key3': 'value3'})

    with pytest.raises(RuntimeError):
        irt.config.Config('test')

    with pytest.raises(RuntimeError):
        irt.config.Config.attr = 'try'

    with pytest.raises(RuntimeError):
        config.attr = 'try'

    assert config['key1'] == 'value1'
    assert config['key2'] == 'value'
    assert config['key3'] == 'value3'
    assert config['key4'] == 'file_ext'
    assert config['key5'] == 'example'
    assert config['key6'] == 'example_conf'
    assert config['section1'] == {
        'section_key1': 'section_value1',
        'key1': '1',
        'key2': '2'
    }

    assert config is irt.config.Config()

    assert config.section1['section_key1'] == 'section_value1'
    assert irt.config.Config.section1['section_key1'] == 'section_value1'
    assert irt.config.Config().section1['section_key1'] == 'section_value1'
