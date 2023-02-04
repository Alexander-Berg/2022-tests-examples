import pytest
import kern


@pytest.mark.xfail(not kern.kernel_in('4.19.144', '5.4.64'), reason='not fixed')
def test_mpls_modules():
    # https://st.yandex-team.ru/KERNEL-422
    # https://st.yandex-team.ru/KERNEL-478
    for m in ['mpls_router', 'mpls_iptunnel', 'mpls_gso']:
        if not kern.module_loaded(m):
            kern.load_module(m)
            kern.remove_module(m)
