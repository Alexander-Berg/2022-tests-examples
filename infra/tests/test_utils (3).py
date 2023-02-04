# coding: utf-8
from __future__ import unicode_literals

from infra.rtc.walle_validator.dto.project import Project
from infra.rtc.walle_validator.lib.utils import ProjectTransformer

DUMMY_TAG = "dummy_test_tag"
TEST_PROJECT = 'test'


def test_project_transformer_simple():
    project = Project(id=TEST_PROJECT, name=TEST_PROJECT, tags=[])
    transform = ProjectTransformer(project)
    assert not transform.is_touched()
    transform.append_tag(DUMMY_TAG)
    assert DUMMY_TAG in project.tags
    assert transform.is_touched()
    transform.remove_tag(DUMMY_TAG)
    assert DUMMY_TAG not in project.tags
    assert transform.is_touched()


def test_project_duplicates(all_projects):
    unique_ids = {project.id for project in all_projects}
    unique_names = {project.name for project in all_projects}
    assert len(unique_ids) == len(all_projects)
    assert len(unique_names) == len(all_projects)
