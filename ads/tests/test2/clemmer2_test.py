# coding: utf-8

from yatest import common


def test_clemmer2():
    with open(common.source_path("ads/clemmer/tests/test2/in.txt")) as stdin:
        return common.canonical_execute(
            common.binary_path("ads/clemmer/tests/test2/clemmer2_test/clemmer2_test"),
            [],
            stdin=stdin,
            check_exit_code=False,
            timeout=120,
        )
