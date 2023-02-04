import pytest
import kern


@pytest.mark.xfail(not kern.kernel_in('4.19.152-38', '5.4.64'), reason='not implemented')
def test_ipv4_tcp_ya_decap_info(sysctl):
    # https://st.yandex-team.ru/NOCDEV-3083
    assert 'net.ipv4.tcp_ya_decap_info' in sysctl
