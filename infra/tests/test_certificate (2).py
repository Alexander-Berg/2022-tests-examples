# coding: utf-8
from __future__ import unicode_literals


def test_rtc_certificate_deploy(project):
    if project.id in ("yt-hahn-nirvana", "yt-hahn-mtn", "search-delete", "rtc-preorders-lake"):
        return
    assert project.certificate_deploy, \
        "Project {} certficate deploy is disabled.".format(project.id)
