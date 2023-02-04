# coding: utf-8

import pytest
import walletest

from infra.rtc.walle_validator.lib.filters import Or, And, HasTag, HasLabel
from infra.rtc.walle_validator.lib.constants import (
    KNOWN_TAGS, KNOWN_LABEL_KEYS, RTC_TAG, RUNTIME_TAG, QLOUD_TAG, YT_TAG,
    YP_TAG, YP_DEV_TAG, YP_ISS_TAG, SKYNET_INSTALLED_TAG, PROJECT_TAG_SET,
    YP_MASTERS_TAG, YP_MASTER_TESTING_TAG, YP_MASTER_PRESTABLE_TAG
)


def expect_tag(tag, project):
    assert tag in project.tags, "{!r} hasn't tag {}".format(project, tag)


def test_for_project_existence(all_projects):
    assert any(RUNTIME_TAG in project.tags for project in all_projects)
    assert any(QLOUD_TAG in project.tags for project in all_projects)
    assert any(YT_TAG in project.tags for project in all_projects)
    assert any(YP_ISS_TAG in project.tags for project in all_projects)
    assert any(YP_MASTERS_TAG in project.tags for project in all_projects)


def test_for_stage_existence(project):
    # every project should have some stage
    assert project.labels.get("stage") in ("experiment", "testing", "prestable", "production", "core"), \
        "No found require tag rtc.stage-* for project {}".format(project.id)


@walletest.xfail
def test_for_project_tags(project):
    project_tags = set(project.tags) & PROJECT_TAG_SET
    assert project_tags


def test_for_unknown_tags(project):
    tags = set(project.cleaned_tags)
    assert tags and tags.issubset(KNOWN_TAGS), "{!r} has unknown tags {!r}".format(project, tags - KNOWN_TAGS)


def test_for_unknown_label_keys(project):
    keys = set(project.labels)
    assert keys.issubset(KNOWN_LABEL_KEYS), "{!r} has unknown label keys {!r}".format(project, keys - KNOWN_LABEL_KEYS)


def test_for_obsolete_tags(all_projects):
    actual_tags = set()
    for project in all_projects:
        actual_tags.update(project.cleaned_tags)
    removed_tags = KNOWN_TAGS - actual_tags
    assert not removed_tags, "there are obsolete tags, remove them: {}".format(", ".join(removed_tags))


def test_rtc_tag_presence(project):
    expect_tag(RTC_TAG, project)


@pytest.mark.project_filter(Or([
    HasTag(YT_TAG),
    HasTag(RUNTIME_TAG)
]))
def test_yt_runtime_tags_together(project):
    if project.id in ('rtc-yt-mtn', 'rtc-yt-mtn-amd'):
        return

    project_tags = set(project.tags)
    assert not set((YT_TAG, RUNTIME_TAG)).issubset(project_tags), \
            "{} and {} tags should not be used together".format(YT_TAG, RUNTIME_TAG)


def test_skynet_tag_presence(project):
    if project.id == "search-delete":
        return
    expect_tag(SKYNET_INSTALLED_TAG, project)


@pytest.mark.project_filter(Or([
    HasTag(YP_MASTERS_TAG),
    HasTag(YP_MASTER_TESTING_TAG),
    HasTag(YP_MASTER_PRESTABLE_TAG)
]))
def test_yt_tag_presence_for_yp_masters(project):
    # RUNTIMECLOUD-13669: install sysmond to yp masters
    expect_tag(YT_TAG, project)


@pytest.mark.project_filter(HasTag(YP_ISS_TAG))
def test_yp_tag_presence(project):
    # RX-931: yp tag used to export hosts to ALL_YP GenCfg group
    expect_tag(YP_TAG, project)


@pytest.mark.project_filter(And([HasTag(YP_DEV_TAG), HasLabel("stage", "production")]))
def test_special_reboot_for_yp_dev(project):
    expect_tag("special_reboot", project)


@pytest.mark.project_filter(HasTag(YP_ISS_TAG))
def test_yp_tag_stage(project):
    err = "Incorrect stage tag on project {}".format(project.id)
    # Experiment
    if project.id in ('yp-man-pre', 'yp-sas-test', 'yp-iss-vla-noclab') or "testing" in project.id:
        assert project.labels.get('stage') == 'experiment', err
    # Prestable
    elif "prestable" in project.id:
        assert project.labels.get('stage') == 'prestable', err
    # Production
    else:
        assert project.labels.get('stage') == 'production', err


@pytest.mark.project_filter(And([HasTag(YT_TAG), HasTag(YP_TAG), HasLabel("stage", "production")]))
def test_infiniband_check_schemas(project):
    if "bert" in project.id and 'man' not in project.id:
        assert project.labels.get("infiniband") == "enabled"
