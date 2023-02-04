import pytest

import infra.reconf_juggler.builders as builders


class Builder(builders.JugglerChecksBuilder):
    build_targets = ('first', 'second')
    default_target = 'first'

    def build_first(self, data):
        return ['first!']

    def build_second(self, data):
        data.append('second!')
        return data


def test_build_sequence():
    assert ['first!'] == Builder().build(target='first')
    assert ['first!', 'second!'] == \
        Builder().build(target='second')


def test_default_build_target():
    assert ['first!'] == Builder().build()


def test_undeclared_build_target():
    with pytest.raises(RuntimeError):
        Builder().build('undeclared')


def test_proxy_builder_targets():
    # there is no correct universal way to merge data from custom targets
    targets = ('initial_tree', 'checks_tree', 'checks_full', 'jsdk_dump')
    assert targets == builders.ProxyChecksBuilder.build_targets
