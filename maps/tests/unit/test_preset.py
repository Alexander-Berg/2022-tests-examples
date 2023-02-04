import re
import yaml
import pytest
import json
from library.python import resource

from maps.analytics.tools.lama.lib.preset import apply_presets


preset_files_regex = re.compile(r'^maps\/analytics\/tools\/lama\/tests\/unit\/fixtures\/presets\/.+\.(in\.yaml)$')
filtered_file_paths = list(filter(preset_files_regex.fullmatch, resource.resfs_files()))
fixture_items = list(map(lambda input: {"input": input, "output": input.replace(preset_files_regex.fullmatch(input)[1], 'out.json')}, filtered_file_paths))


@pytest.mark.parametrize('fixture_item', fixture_items)
def test_preset(fixture_item):
    input = fixture_item['input']
    output = fixture_item['output']

    config_content = resource.resfs_read(input)
    config = yaml.safe_load(config_content)
    config = apply_presets(config)

    config_expected = json.loads(resource.resfs_read(output))
    assert config == config_expected
