import re
import pytest
import os
from library.python import resource

from maps.analytics.tools.lama.lib.commands.validate import ValidateCommand


preset_files_regex = re.compile(r'^maps\/analytics\/tools\/lama\/tests\/unit\/fixtures\/validate_configs\/.+(\.yaml)$')
fixture_items = list(filter(preset_files_regex.fullmatch, resource.resfs_files()))


@pytest.mark.parametrize('fixture_path', fixture_items)
def test_validate(tmp_path, fixture_path):
    filename = os.path.basename(fixture_path)
    config_content = resource.resfs_read(fixture_path).decode('utf-8')
    config_path = f'{tmp_path}/{filename}'
    with open(config_path, 'w') as f:
        f.write(config_content)

    ValidateCommand().exec(config_path)
