# coding: utf-8
from __future__ import unicode_literals

from infra.rtc.walle_validator.lib.transform import get_max_fixes


def test_automation_limits_dns_fixes(project, host_counts):
    if project.id not in host_counts or project.id in ["rtc-preorders-lake", "search-delete", "rtc-yt-inbox"]:
        return

    total_hosts = host_counts[project.id]
    if total_hosts == 0:
        return

    max_fixes = get_max_fixes(total_hosts)
    [dns_thresholds] = project.automation_limits["max_dns_fixes"]
    assert dns_thresholds["period"] == "1d"
    assert dns_thresholds["limit"] <= max_fixes
