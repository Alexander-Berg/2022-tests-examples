import pytest
import kern


@pytest.mark.xfail(not kern.kernel_in('4.19.119-30.1', '4.19.131-34', ('5.4.45', '5.4.184-33')), reason='not implemented')
def test_ipv6_auto_flowlabels(sysctl):
    # https://st.yandex-team.ru/KERNEL-414
    assert sysctl['net.ipv6.auto_flowlabels'] == '0'
