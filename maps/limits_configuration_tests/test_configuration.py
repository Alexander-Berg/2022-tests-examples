import os
from collections import defaultdict

import yaml
import yatest.common

from maps.infra.ratelimiter2.tools.pyhelpers.limits.configuration import (
    ResourceValidator, serializeLimitsToProto
)


class TestConfiguration:
    def test_resources_unique(self):
        # check no resource duplicates in configuration
        root_path = yatest.common.source_path('maps/config/ratelimiter')
        config_to_projects = defaultdict(list)
        for project in os.listdir(root_path):
            if not os.path.isdir(os.path.join(root_path, project)):
                continue

            for config in os.listdir(os.path.join(root_path, project)):
                if not config.endswith('.yaml'):
                    continue
                config_to_projects[config].append(project)

        for config in config_to_projects:
            validator = ResourceValidator()
            for project in config_to_projects[config]:
                with open(os.path.join(root_path, project, config)) as yamlfile:
                    yaml_config = yaml.safe_load(yamlfile.read()) or {}
                    validator.add_project(project, yaml_config)
            # expect no duplicates
            assert validator.duplicates() == []

    def test_configuration_valid(self):
        root_path = yatest.common.source_path('maps/config/ratelimiter')
        for project in os.listdir(root_path):
            if not os.path.isdir(os.path.join(root_path, project)):
                continue
            for config in os.listdir(os.path.join(root_path, project)):
                if not config.endswith('.yaml'):
                    continue

                with open(os.path.join(root_path, project, config)) as yamlfile:
                    serializeLimitsToProto(yaml.safe_load(yamlfile.read()) or {})
