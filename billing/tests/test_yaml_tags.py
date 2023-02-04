import io
import yaml
from yatest.common import source_path


class TestYAMLTags:

    def test_include(self):
        project_root = source_path('billing/dwh/')
        yaml_content = f"""
        a: !include {project_root}/src/dwh/conf/remote/usr/bin/dwh/world_consts.yaml
        """
        c = yaml.safe_load(io.StringIO(yaml_content))
        assert c['a']['YA_TOKEN_COST'] == 30

    def test_nested_include(self):
        project_root = source_path('billing/dwh/')
        yaml_content = f"""
        a:
          z: !include {project_root}/src/dwh/conf/remote/usr/bin/dwh/world_consts.yaml
          c: 3
        """
        c = yaml.safe_load(io.StringIO(yaml_content))
        assert c['a']['z']['YA_TOKEN_COST'] == 30
