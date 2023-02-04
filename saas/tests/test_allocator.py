import yatest.common as yc
import subprocess


def test_allocator():
    rtys_path = yc.binary_path('saas/rtyserver_sys/rtyserver')
    out = subprocess.check_output("nm -u %s | egrep ' (malloc|realloc|free)$' | wc -l" % rtys_path, shell=True)
    assert out == "3\n"
