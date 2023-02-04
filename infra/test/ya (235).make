OWNER(g:hostman)

GO_TEST_FOR(infra/hostctl/internal/units/env/pacman/dpkgutil)

RESOURCE(
    status /var/lib/dpkg/status
)
END()
