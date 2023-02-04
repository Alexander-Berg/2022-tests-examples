# coding: utf-8
from __future__ import unicode_literals


def test_rtc_profile(project):
    if project.id in {"search-delete", "rtc-preorders-lake"}:
        return
    else:
        assert project.profile == 'flexy', \
            "Project {} has bad profile".format(project.id)


def test_rtc_profile_tags(project):
    if project.id in {"search-delete", "rtc-preorders-lake"}:
        return
    if 'yati' in project.id:
        assert 'rtc_yati' in project.profile_tags
        return
    else:
        eine_boot_tags = ('disk', 'rtc')
        assert len([tag for tag in eine_boot_tags if tag in project.profile_tags]) == 1, \
            "Project {} not found required tag profile.".format(project.id)
