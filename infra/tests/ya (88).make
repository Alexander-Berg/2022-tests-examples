OWNER(g:hostman)

GO_TEST_FOR(infra/hostctl/internal/units/tasks)

GO_XTEST_SRCS(
    conflicts_test.go
    file_manage_test.go
    file_remove_test.go
    package_install_test.go
    package_uninstall_test.go
    porto_run_test.go
    porto_shutdown_test.go
    systemd_run_test.go
    systemd_shutdown_test.go
    condition_test.go
)

END()
