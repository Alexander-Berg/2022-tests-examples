# coding: utf-8
from __future__ import unicode_literals

import pytest

from infra.rtc.walle_validator.lib.filters import And, HasTag, HasNoTag
from infra.rtc.walle_validator.lib.constants import YP_TAG, YP_MASTERS_TAG, YP_MASTER_TESTING_TAG, YP_MASTER_PRESTABLE_TAG, YT_TAG

ALLOWED_MASTERS = (
    "iva",
    "man",
    "man_pre",
    "myt",
    "sas",
    "sas_test",
    "vla",
    "xdc",
)


@pytest.mark.project_filter(And([
    HasTag(YP_TAG),
    HasNoTag(YP_MASTERS_TAG),
    HasNoTag(YP_MASTER_TESTING_TAG),
    HasNoTag(YP_MASTER_PRESTABLE_TAG)
]))
def test_yp_master_label(project):
    yp_master = project.labels.get("ypmaster")
    assert yp_master is not None and yp_master in ALLOWED_MASTERS, "no proper master found"

    parts = project.id.split("-")
    if yp_master == "sas_test":
        assert "testing" in parts, "projects connected to sas-test should have testing in id"
    elif YT_TAG in project.tags and yp_master == "man_pre":
        assert "man" in parts or "sas" in parts or "prestable" in parts, "yt projects connected to man-pre should have man or sas or prestable in id"
    elif yp_master == "man_pre":
        assert "prestable" in parts, "projects connected to man-pre should have prestable in id"
    else:
        assert yp_master in parts, "projects connected to man-pre should have {} in id".format(yp_master)
