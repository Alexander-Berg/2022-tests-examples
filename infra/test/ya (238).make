OWNER(g:hostman)

GO_TEST_FOR(infra/hostctl/units)

GO_XTEST_SRCS(
    ssh_test.go
    ntp_test.go
    hmserver_test.go
)

SIZE(MEDIUM)

# run only locally (autobuilds fails)
TAG(ya:not_autocheck)

END()
