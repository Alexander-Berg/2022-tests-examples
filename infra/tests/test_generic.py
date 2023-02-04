# coding: utf-8

import pytest

from infra.rtc.walle_validator.dto.project import Project
from infra.rtc.walle_validator.dto.constants import PROJECT_FIELDS


def test_for_non_empty_project_set(all_projects):
    assert all_projects


def test_project_to_dict():
    project_kwargs = {field: {field: None} for field in PROJECT_FIELDS}
    project = Project(**project_kwargs)
    assert project.to_dict() == project_kwargs


def test_project_without_labels():
    project = Project(id="test", name="test", tags=["something"])
    assert not project.labels


def test_project_with_labels():
    project = Project(id="test", name="test", tags=["something", "rtc.stg-something", "rtc.prj-found"])
    assert project.labels == {"stg": "something", "prj": "found"}


def test_project_with_not_orthogonal_labels():
    project = Project(id="test", name="test", tags=["something", "rtc.stg-first", "rtc.stg-second"])
    with pytest.raises(AssertionError):
        if not project.labels:
            raise RuntimeError()


def test_for_good_labels(project):
    assert project.labels is not None
