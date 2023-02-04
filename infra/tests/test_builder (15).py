from infra.rtc.juggler.reconf.builders.projects.sysdev_overall.builder import Builder


def test_default_target(canonized_resolver, diff_canonized):
    """ Compare resulting aggregates with canonized copy. """
    builder = Builder(resolver=canonized_resolver)

    tree = builder.build()
    json = builder.dump(tree)

    diff_canonized(json)
