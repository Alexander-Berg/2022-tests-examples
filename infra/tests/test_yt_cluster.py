# coding: utf-8
from __future__ import unicode_literals

import re
import pytest

from infra.rtc.walle_validator.lib.filters import And, HasTag, HasNoTag
from infra.rtc.walle_validator.lib.constants import YT_TAG, YP_MASTERS_TAG, YP_MASTER_TESTING_TAG, YP_MASTER_PRESTABLE_TAG
from infra.rtc.walle_validator.lib.transform import get_yt_cluster_name


@pytest.mark.project_filter(And([
    HasTag(YT_TAG),
    HasNoTag(YP_MASTERS_TAG),
    HasNoTag(YP_MASTER_TESTING_TAG),
    HasNoTag(YP_MASTER_PRESTABLE_TAG)
]))
def test_yt_cluster_label(project):
    if project.id == "rtc-yt-inbox":
        return
    cluster_name = get_yt_cluster_name(project)
    assert cluster_name is not None, "cluster can't be deduced from project"
    assert cluster_name == project.labels.get("yt_cluster"), "no proper cluster tag found"


@pytest.mark.project_filter(And([
    HasTag(YT_TAG),
    HasNoTag(YP_MASTERS_TAG),
    HasNoTag(YP_MASTER_TESTING_TAG),
    HasNoTag(YP_MASTER_PRESTABLE_TAG)
]))
def test_incorrect_yt_network_tag(project):
    assert "rtc_network" not in project.tags
    for tag in project.tags:
        m = re.match("^yt_([a-z]+)_network$", tag)
        assert m is None, "obsolete {} tag detected".format(tag)
