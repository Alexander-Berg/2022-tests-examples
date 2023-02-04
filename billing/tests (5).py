import email
from textwrap import dedent
from pathlib import Path
from unittest import mock
from yatest.common import (
    source_path,
    output_path,
)

from billing.library.tools.d_arc.d_arc_lib.lib import (
    grow_version,
    find_last_changelog_commit,
    update_changelog_from_arc,
    GrowType
)

arc_log = [
    {
        "commit": "8d9878509d38a532b8ae3a5844150d196ae818e1",
        "parents": [
            "63e8dde0515e462d19a3051d3639c7b0cf31c65d"
        ],
        "author":"ecialo",
        "date":"2020-04-29T16:02:21+03:00",
        "message":"REPORTS-2686: inc version",
        "path":"billing/yb_reports_monitorings"
    },
    {
        "commit": "63e8dde0515e462d19a3051d3639c7b0cf31c65d",
        "parents": [
            "c66b8df623001654b27760d70eb4f83db1ad9467"
        ],
        "author":"ecialo",
        "date":"2020-04-29T16:01:13+03:00",
        "message":"REPORTS-2686: wrap 'max_identifier_length'",
        "path":"billing/yb_reports_monitorings"
    },
    {
        "commit": "c66b8df623001654b27760d70eb4f83db1ad9467",
        "parents": [
            "b46ed27ecddd3fd99baf61be5c703b49381f1aaf"
        ],
        "author":"ecialo",
        "date":"2020-04-29T15:20:52+03:00",
        "message":"REPORTS-2686: inc version",
        "path":"billing/yb_reports_monitorings"
    },
]


def test_grow_version():
    version = "1.2.3"
    z_version = "0.0.0"
    assert grow_version(version, GrowType.patch) == "1.2.4"
    assert grow_version(z_version, GrowType.patch) == "0.0.1"
    assert grow_version(version, GrowType.minor) == "1.3.0"
    assert grow_version(z_version, GrowType.minor) == "0.1.0"
    assert grow_version(version, GrowType.major) == "2.0.0"
    assert grow_version(z_version, GrowType.major) == "1.0.0"


def test_find_last_commit():
    f = dedent("""
    [ TeamCity ]
        Build new package in fabric
    [ estarchak ]
        add db objects and yql to dashboard; 7a0df08
    last commit: ff0fe599533db45e256095abab816da248875d58
    """).split("\n")
    fe = dedent("""
    [ TeamCity ]
        Build new package in fabric
    [ estarchak ]
        add db objects and yql to dashboard; 7a0df08
    """).split("\n")
    assert find_last_changelog_commit(f) == "ff0fe599533db45e256095abab816da248875d58"
    assert find_last_changelog_commit(fe) == ""


def test_update_changelog():
    source_changelog = source_path("billing/library/tools/d_arc/tests/source_changelog")
    result_changelog = Path(output_path("./result_changelog"))
    with mock.patch('billing.library.tools.d_arc.d_arc_lib.lib.get_arc_log', return_value=arc_log):
        update_changelog_from_arc(
            repo_path=".",
            changelog_path=source_changelog,
            result_changelog_path=result_changelog,
            grow_type=GrowType.patch
        )

    expected = dedent(f"""\
    yb-reports-monitorings (0.1.1) unstable; urgency=low
      [ ecialo ]
          REPORTS-2686: inc version
      last commit: 8d9878509d38a532b8ae3a5844150d196ae818e1

     -- Integrat Dostavlyaev <robot-billing-ci@yandex-team.ru>  {email.utils.formatdate()}

    yb-reports-monitorings (0.1.0) unstable; urgency=low
      [ ecialo ]
          REPORTS-2686: wrap 'max_identifier_length'
          REPORTS-2686: inc version
      last commit: 63e8dde0515e462d19a3051d3639c7b0cf31c65d

     -- Integrat Dostavlyaev <robot-billing-ci@yandex-team.ru>  Wed, 29 Apr 2020 13:01:52 -0000
    """)
    assert result_changelog.read_text() == expected
