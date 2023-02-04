from infra.reconf_juggler import Check


class cron(Check):
    validate_class = False


def test_has_subnodes():
    check = cron({'children': {'0': {}}})

    assert check.has_subnodes()
    assert not check['children']['0:cron'].has_subnodes()


def test_has_endpoints():
    check = cron({'children': {'0': {'children': {'host.example.com': None}}}})

    assert not check.has_endpoints()
    assert check['children']['0:cron'].has_endpoints()
