from infra.rtc.juggler.reconf.builders.projects.all_infra_prestable.builder import Builder


def test_default(canonized_resolver, diff_canonized):
    """
    Compare final full JSON with canonized one.

    """
    builder = Builder(resolver=canonized_resolver)
    json = builder.dump(builder.build())

    diff_canonized(json)
