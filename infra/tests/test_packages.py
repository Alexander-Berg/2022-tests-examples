from infra.ya_salt.lib.packages import action, transaction, transaction_validator, stateutil

LO_STATE = [
    {
        "name": "nvidia_driver_installed",
        "install_recommends": False,
        "refresh": False,
        "state": "pkg",
        "__id__": "nvidia_driver_installed",
        "fun": "installed",
        "__env__": "search_runtime",
        "__sls__": "components.nvidia.driver-418-v2",
        "order": 10199,
        "pkgs": [
            {
                "nvidia-418": "418.67-0ubuntu1"
            },
            {
                "nvidia-418-dev": "418.67-0ubuntu1"
            },
            {
                "nvidia-modprobe": "418.67-0ubuntu1"
            },
            {
                "libcuda1-418": "418.67-0ubuntu1"
            },
            "nvidia-common",
            {
                "libc6-i386": "2.23-0ubuntu10"
            },
            {
                "libc6-dev": "2.23-0ubuntu10"
            },
            {
                "libc6": "2.23-0ubuntu10"
            },
            {
                "libc-bin": "2.23-0ubuntu10"
            },
            {
                "libc-dev-bin": "2.23-0ubuntu10"
            },
            {
                "locales": "2.23-0ubuntu10"
            },
            {
                "multiarch-support": "2.23-0ubuntu10"
            }
        ]
    },
    {
        "name": "yandex-hbf-agent-purged",
        "state": "pkg",
        "pkgs": [
            "yandex-hbf-agent-static",
            "yandex-hbf-agent-init"
        ],
        "fun": "purged",
        "__env__": "search_runtime",
        "__sls__": "services.yandex-hbf-agent",
        "order": 10229,
        "__id__": "yandex-hbf-agent-purged"
    }
]


class TestTransaction(object):
    def test_install(self):
        tx = transaction.Transaction()
        tx.installed(action.Install('pkg-mock', 'ver-mock', 'src-mock'))
        assert len(tx.get_installed()) == 1
        assert tx.get_installed()['pkg-mock'][0].name == 'pkg-mock'
        assert tx.get_installed()['pkg-mock'][0].version == 'ver-mock'
        assert tx.get_installed()['pkg-mock'][0].src == 'src-mock'

    def test_remove(self):
        tx = transaction.Transaction()
        tx.removed(action.Remove('pkg-mock', 'src-mock'))
        assert len(tx.get_removed()) == 1
        assert tx.get_removed()['pkg-mock'][0].name == 'pkg-mock'
        assert tx.get_removed()['pkg-mock'][0].src == 'src-mock'

    def test_get_pkg_installed_version_of_installed_package(self):
        tx = transaction.Transaction()
        tx.installed(action.Install('pkg-mock', 'ver-mock', 'src-mock'))
        assert transaction.get_pkg_installed_version('pkg-mock', tx) == ('ver-mock', None,)

    def test_get_pkg_installed_version_of_not_installed_package(self):
        tx = transaction.Transaction()
        tx.installed(action.Install('pkg-mock', 'ver-mock', 'src-mock'))
        ver, err = transaction.get_pkg_installed_version('not-installed-pkg-mock', tx)
        assert ver is None
        assert err == 'package "not-installed-pkg-mock" is not scheduled to be installed'


class TestTransactionValidator(object):
    def test_validate_packages_loop(self):
        tx = transaction.Transaction()
        tx.installed(action.Install('pkg-mock', 'ver-mock', 'src-mock'))
        tx.removed(action.Remove('pkg-mock', 'other-src-mock'))
        messages = transaction_validator.validate_packages_loop(tx)
        expected_messages = [
            'install-remove loop detected for package "pkg-mock" - '
            'Remove("pkg-mock", src: "other-src-mock"), '
            'Install("pkg-mock"="ver-mock", src: "src-mock")'
        ]
        assert messages == expected_messages

    def test_validate_packages_versions_loop(self):
        tx = transaction.Transaction()
        tx.installed(action.Install('pkg-mock', 'v1', 'src1-mock'))
        tx.installed(action.Install('pkg-mock', 'v2', 'src2-mock'))
        messages = transaction_validator.validate_packages_versions_loop(tx)
        expected_messages = [
            'version loop detected for package "pkg-mock" - '
            'Install("pkg-mock"="v1", src: "src1-mock"), '
            'Install("pkg-mock"="v2", src: "src2-mock")'
        ]
        assert messages == expected_messages

    def test_validate_packages_versions_present(self):
        tx = transaction.Transaction()
        tx.installed(action.Install('pkg-mock1', None, 'src1-mock'))
        tx.installed(action.Install('pkg-mock2', '', 'src2-mock'))
        messages = transaction_validator.validate_packages_versions_present(tx)
        expected_messages = sorted([
            'package action Install("pkg-mock1"="None", src: "src1-mock") has no version',
            'package action Install("pkg-mock2"="", src: "src2-mock") has no version',
        ])
        assert sorted(messages) == expected_messages

    def test_validate_transaction(self):
        tx = transaction.Transaction()
        tx.removed(action.Remove('pkg-mock', 'other-src-mock'))
        tx.installed(action.Install('pkg-mock', 'v1', 'src-mock'))
        tx.installed(action.Install('pkg-mock', 'v2', 'src2-mock'))
        tx.installed(action.Install('pkg-mock3', None, 'src3-mock'))
        messages = transaction_validator.validate_transaction(tx)
        expected_messages = [
            'package action Install("pkg-mock3"="None", src: "src3-mock") has no version',

            'install-remove loop detected for package "pkg-mock" - '
            'Remove("pkg-mock", src: "other-src-mock"), '
            'Install("pkg-mock"="v1", src: "src-mock"), '
            'Install("pkg-mock"="v2", src: "src2-mock")',

            'version loop detected for package "pkg-mock" - '
            'Install("pkg-mock"="v1", src: "src-mock"), '
            'Install("pkg-mock"="v2", src: "src2-mock")'
        ]
        assert messages == expected_messages


class TestStatUtil(object):
    def test_extract_lo_packages_to_transaction(self):
        tx = transaction.Transaction()
        stateutil.extract_lo_packages_to_tx(LO_STATE, tx)
        assert len(tx.get_installed().keys()) == 12
        assert len(tx.get_removed().keys()) == 2
        assert transaction.get_pkg_installed_version('nvidia-common', tx) == (None, None,)
        assert transaction.get_pkg_installed_version('nvidia-418', tx) == ('418.67-0ubuntu1', None,)
