"""
Example which doesn't work is a most shameful thing.
Make sure we haven't break it accidentally.

"""
from infra.reconf_juggler.examples.declared.builder import Builder


def test_default_target(canonized_resolver, diff_canonized):
    """
    Compare final full JSON with canonized.

    """
    builder = Builder(resolver=canonized_resolver)

    tree = builder.build()
    json = builder.dump(tree)

    diff_canonized(json)


def test_jsdk_dump_target(canonized_resolver, diff_canonized):
    """
    Compare final full JSON with canonized.

    """
    builder = Builder(resolver=canonized_resolver)

    tree = builder.build('jsdk_dump')
    json = builder.dump(tree)

    diff_canonized(json)
