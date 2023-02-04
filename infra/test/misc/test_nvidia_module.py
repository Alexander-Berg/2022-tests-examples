import pytest
import kern
import os


@pytest.fixture(scope='session')
def prepare_nvidia_module(run, lsb_codename, logger):

    if lsb_codename in ('precise', 'bionic'):
        pytest.xfail('broken')

    # FIXME remove this from image
    run(['apt-get', 'remove', '--yes', '--purge', 'cuda-repo-ubuntu1604'], check=False)

    repo = 'deb http://nvidia-drivers.dist.yandex.ru/nvidia-drivers/' + lsb_codename + '/amd64 /'
    logger.info('use apt repository: %s', repo)
    with open('/etc/apt/sources.list.d/nvidia-drivers.list', mode='x') as f:
        f.write(repo + '\n')

    run(['apt-get', 'update'])

    # makes directory /usr/lib/nvidia
    run(['apt-cache', 'policy', 'nvidia-common'])
    run(['apt-get', 'install', '--yes', '--no-install-recommends', 'nvidia-common'])

    # patches for nvidia dkms modules
    # https://a.yandex-team.ru/arc/trunk/arcadia/infra/rtc/packages/yandex-linux-nvidia-patches
    run(['apt-cache', 'policy', 'yandex-linux-nvidia-patches'])
    run(['apt-get', 'install', '--yes', '--no-install-recommends', 'yandex-linux-nvidia-patches'])

    yield

    os.unlink('/etc/apt/sources.list.d/nvidia-drivers.list')
    run(['apt-get', 'update'])


@pytest.mark.parametrize('nvidia_module,nvidia_version', [
    ('nvidia-418', '418.67-0ubuntu1'),
])
@pytest.mark.usefixtures("disable_update_initramfs")
@pytest.mark.usefixtures("prepare_nvidia_module")
@pytest.mark.skipif(kern.kernel_in("5.4.80-6"), reason="not required, https://st.yandex-team.ru/KERNEL-437")
def test_nvidia_module(kernel_release, nvidia_module, nvidia_version, run):
    assert os.path.exists('/lib/modules/{}/build'.format(kernel_release))

    run(['apt-cache', 'policy', nvidia_module])

    nvidia_env = os.environ.copy()
    nvidia_env['DONT_BUILD_MODULE'] = '1'
    nvidia_env['DEBIAN_FRONTEND'] = "noninteractive"
    run(['apt-get', 'install', '--yes', '--no-install-recommends', nvidia_module + '=' + nvidia_version], env=nvidia_env)

    version = nvidia_version.split('-')[0]
    if not os.path.isdir('/usr/src/{}-{}'.format(nvidia_module, version)):
        run(['ls', '-l', '/usr/src'])
        pytest.fail("nvidia module directory not found")

    assert os.path.exists('/usr/src/{}-{}/dkms.conf'.format(nvidia_module, version))

    run(['dkms', 'remove', '-m', nvidia_module, '-v', version, '-k', kernel_release], check=False)
    run(['dkms', 'build', '-m', nvidia_module, '-v', version, '-k', kernel_release])
    run(['dkms', 'remove', '-m', nvidia_module, '-v', version, '-k', kernel_release])
    run(['apt-get', 'remove', '--yes', nvidia_module])
