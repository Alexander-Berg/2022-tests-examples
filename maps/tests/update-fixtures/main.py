import json
import os
from pathlib import Path

import yaml
from maps.analytics.tools.lama.lib.preset import apply_presets
from yalibrary.makelists import from_file


def __generate_output_path(input_path):
    filepath, filename = os.path.split(input_path)
    output = f"{filepath}/{filename.rsplit('.', 2)[0]}.out.json"
    return output


def __save_config(config, filepath):
    file_descriptor = open(filepath, 'w')
    file_descriptor.truncate(0)
    file_descriptor.write(json.dumps(config, indent=4))
    file_descriptor.close()


def __update_ya_make(resource_filepath):
    ya_make_path = Path(f"{os.getcwd()}/../unit/ya.make").resolve()
    resource_relative_filepath = resource_filepath.split('/arcadia/')[1]

    make_list = from_file(ya_make_path)
    make_list.project.resource_files().add(resource_relative_filepath)
    make_list.project.resource_files().sort_values()
    make_list.write(ya_make_path)


def save_preset_fixtures():
    directory = f"{os.getcwd()}/../unit/fixtures/presets"
    configs = Path(directory).resolve().glob('**/*.in.yaml')

    for input in configs:
        output = __generate_output_path(input)

        config_content = open(input).read()
        config = yaml.safe_load(config_content)
        config = apply_presets(config)

        __save_config(config, output)
        __update_ya_make(output)


def main():
    save_preset_fixtures()
