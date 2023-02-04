# coding: utf-8
from btestlib import reporter
from test_audit_collector import get_diff, load_value, BUILD_NUM_KEY


if __name__ == "__main__":
    build_num = load_value(BUILD_NUM_KEY)
    diff = get_diff(build_num)
    added_tests = diff['add_to_all_stats']
    deleted_tests = diff['add_to_deleted_stats']

    if added_tests:
        reporter.log(u"Добавленные и размеченные тесты: название \t метка")
        for test in sorted(added_tests.keys()):
            reporter.log(u"{}\t{}".format(test[0], test[1]))
    if deleted_tests:
        reporter.log(u"Удаленные тесты: название \t метка")
        for test in sorted(deleted_tests.keys()):
            reporter.log(u"{}\t{}".format(test[0], test[1]))

    if added_tests or deleted_tests:
        raise Exception("Есть удаленные или добавленные тесты, посмотри Build Log")

