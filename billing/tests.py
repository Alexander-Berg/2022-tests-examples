import os
import pytest
from yatest.common import test_source_path
from lxml import etree


@pytest.mark.parametrize('env_dir', ['dev', 'load', 'test', 'prod'])
def test_xml_valid(env_dir):
    for dirname, _, filenames in os.walk(test_source_path(env_dir)):
        for filename in filenames:
            if filename.endswith('.xml') or filename.endswith('.xml.sample'):
                etree.parse(os.path.join(dirname, filename))


@pytest.mark.parametrize('env_dir', ['dev', 'test', 'prod'])
def test_app_ids_are_unique(env_dir):
    with open(test_source_path(os.path.join(env_dir, 'secrets.cfg.xml')), 'r') as components_xml_file:
        tree = etree.parse(components_xml_file)
        app_ids = [i.get('id') for i in tree.xpath('/*/App')]
    assert len(app_ids) == len(set(app_ids))


@pytest.mark.parametrize('env_dir', ['dev', 'test', 'prod'])
def test_component_ids_are_unique(env_dir):
    with open(test_source_path(os.path.join(env_dir, 'components.cfg.xml')), 'r') as components_xml_file:
        tree = etree.parse(components_xml_file)
        component_ids = [i.get('id') for i in tree.xpath('/*/Component')]
    assert len(component_ids) == len(set(component_ids))


@pytest.mark.skip()
def test_something_about_components():
    """
    Автор уже ушел из компании, но вдруг здесь проверяется что-то важное, просто оставлю здесь

    (xpath -q -e '/*/Component/LPMFilters/text()' prod/components.cfg.xml 2>&1 | jq '.'); JSON_VAL="$$?" && \
    test "$$JSON_VAL" = "0"
    (xpath -q -e '/*/Component/NewRC/text()' prod/components.cfg.xml 2>&1 | jq '.'); JSON_VAL="$$?" && \
    test "$$JSON_VAL" = "0"
    (xpath -q -e '/*/Component/LPMFilters/text()' dev/components.cfg.xml 2>&1 | jq '.'); JSON_VAL="$$?" && \
    test "$$JSON_VAL" = "0"
    (xpath -q -e '/*/Component/NewRC/text()' dev/components.cfg.xml 2>&1 | jq '.'); JSON_VAL="$$?" && \
    test "$$JSON_VAL" = "0"
    (xpath -q -e '/*/Component/LPMFilters/text()' test/components.cfg.xml 2>&1 | jq '.'); JSON_VAL="$$?" && \
    test "$$JSON_VAL" = "0"
    (xpath -q -e '/*/Component/NewRC/text()' test/components.cfg.xml 2>&1 | jq '.'); JSON_VAL="$$?" && \
    test "$$JSON_VAL" = "0"
    """
    assert False
