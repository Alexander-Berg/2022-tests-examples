# coding: utf-8

from yatest import common


def test_clemmer():
    with open(common.source_path("ads/clemmer/tests/test/in.txt")) as stdin:
        return common.canonical_execute(
            common.binary_path("ads/clemmer/tests/test/clemmer_test/clemmer_test"),
            ["notrace"],
            stdin=stdin,
            check_exit_code=False,
            timeout=120,
        )
