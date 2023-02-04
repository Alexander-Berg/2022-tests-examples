# coding: utf-8

##
# printposfilter напускается на "in.txt" без каких-либо дополнительных параметров,
# берётся md5 результата и сравнивается с соответствующим каноническим.
##
# Тест утверждён Лёней Бровкиным (leo@)
##

from yatest import common


def test_printposfilter():
    return common.canonical_execute(common.binary_path("saas/tools/printposfilter/printposfilter"),
        ["-i", common.source_path("saas/tools/printposfilter/tests/in.txt")])
