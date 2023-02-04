import pytest

from maps_adv.common.config_loader import (
    ConfigLoader,
    InitializationError,
    Option,
    OptionNotFound,
)


@pytest.fixture
def make_adapter(mocker):
    def _make(retval=None, *, dependencies=(), side_effect=None, name=None):
        adapter = mocker.Mock()
        adapter.dependencies = dependencies
        adapter.return_value = adapter

        if retval:
            adapter.load.return_value = retval
        elif side_effect:
            adapter.load.side_effect = side_effect

        if name:
            adapter.__name__ = name

        return adapter

    return _make


def test_uses_specified_adapters(make_adapter):
    adapter1 = make_adapter()
    adapter2 = make_adapter()

    loader = ConfigLoader(Option("EXISTING"), adapters=(adapter1, adapter2))
    loader.init()

    assert adapter1.called is True
    assert adapter2.called is True


def test_first_adapter_has_higher_priority(make_adapter):
    adapter1 = make_adapter("FROM FIRST")
    adapter2 = make_adapter("FROM SECOND")

    loader = ConfigLoader(Option("EXISTING"), adapters=(adapter1, adapter2))
    loader.init()

    assert loader["EXISTING"] == "FROM FIRST"


def test_will_load_from_specified_key(make_adapter):
    adapter1 = make_adapter("FROM FIRST")
    adapter2 = make_adapter("FROM SECOND")

    loader = ConfigLoader(
        Option("EXISTING", load_from="ANOTHER"), adapters=(adapter1, adapter2)
    )
    loader.init()

    assert "ANOTHER" in adapter1.load.call_args_list[0][0]


@pytest.mark.parametrize("kw", ({}, {"load_from": "ANOTHER"}))
def test_raises_if_option_not_found(kw, make_adapter):
    adapter1 = make_adapter(side_effect=OptionNotFound("EXISTING"))
    adapter2 = make_adapter(side_effect=OptionNotFound("EXISTING"))

    loader = ConfigLoader(Option("EXISTING", **kw), adapters=(adapter1, adapter2))
    with pytest.raises(OptionNotFound, match="EXISTING"):
        loader.init()


def test_uses_default_if_option_not_found(make_adapter):
    adapter1 = make_adapter(side_effect=OptionNotFound("EXISTING"))
    adapter2 = make_adapter(side_effect=OptionNotFound("EXISTING"))

    loader = ConfigLoader(
        Option("EXISTING", default="DEFAULT"), adapters=(adapter1, adapter2)
    )
    loader.init()

    assert loader["EXISTING"] == "DEFAULT"


def test_uses_converter_if_specified(make_adapter, mocker):
    adapter1 = make_adapter("FROM FIRST")
    adapter2 = make_adapter("FROM SECOND")
    converter = mocker.Mock(return_value="KEK")

    loader = ConfigLoader(
        Option("EXISTING", converter=converter), adapters=(adapter1, adapter2)
    )
    loader.init()

    converter.assert_called_with("FROM FIRST")
    assert loader["EXISTING"] == "KEK"


def test_converter_not_used_for_default(make_adapter, mocker):
    adapter1 = make_adapter(side_effect=OptionNotFound("EXISTING"))
    adapter2 = make_adapter(side_effect=OptionNotFound("EXISTING"))
    converter = mocker.Mock()

    loader = ConfigLoader(
        Option("EXISTING", default="DEFAULT", converter=converter),
        adapters=(adapter1, adapter2),
    )
    loader.init()

    assert converter.called is False


def test_uses_next_adapter_if_previous_not_knows_option(make_adapter):
    adapter1 = make_adapter()
    adapter1.load.side_effect = OptionNotFound
    adapter2 = make_adapter("FROM SECOND")

    loader = ConfigLoader(Option("EXISTING"), adapters=(adapter1, adapter2))
    loader.init()

    assert loader["EXISTING"] == "FROM SECOND"


def test_uses_higher_adapters_when_resolving_dependencies(make_adapter):
    adapter1 = make_adapter()
    adapter1.load.side_effect = OptionNotFound
    adapter2 = make_adapter("FROM SECOND")
    adapter3 = make_adapter("FROM THIRD", dependencies=(Option("DEP1"),))
    adapter4 = make_adapter("FROM FOURTH")

    loader = ConfigLoader(
        Option("EXISTING"), adapters=(adapter1, adapter2, adapter3, adapter4)
    )
    loader.init()

    assert "DEP1" in adapter1.load.call_args_list[0][0]
    assert "DEP1" in adapter2.load.call_args_list[0][0]
    assert adapter4.load.called is False
    adapter3.assert_called_with("FROM SECOND")


def test_uses_default_if_dependency_not_resolved(make_adapter):
    adapter1 = make_adapter(side_effect=OptionNotFound)
    adapter2 = make_adapter(side_effect=OptionNotFound)
    adapter3 = make_adapter("FROM THIRD", dependencies=(Option("DEP1", default="kek"),))
    adapter4 = make_adapter("FROM FOURTH")

    loader = ConfigLoader(
        Option("EXISTING"), adapters=(adapter1, adapter2, adapter3, adapter4)
    )
    loader.init()

    assert "DEP1" in adapter1.load.call_args_list[0][0]
    assert "DEP1" in adapter2.load.call_args_list[0][0]
    assert adapter4.load.called is False
    adapter3.assert_called_with("kek")


def test_logs_if_adapter_dependencies_are_not_resolved(make_adapter, caplog):
    adapter1 = make_adapter(
        side_effect=[OptionNotFound("DEP1"), "FROM FIRST"], name="FIRST ADAPTER"
    )
    adapter2 = make_adapter(
        side_effect=[OptionNotFound("DEP1"), "FROM SECOND"], name="SECOND ADAPTER"
    )
    adapter3 = make_adapter(
        side_effect=[OptionNotFound("DEP1"), "FROM THIRD"],
        dependencies=(Option("DEP1"),),
        name="THIRD ADAPTER",
    )

    loader = ConfigLoader(Option("EXISTING"), adapters=(adapter1, adapter2, adapter3))
    loader.init()

    assert (
        "Adapter THIRD ADAPTER omitted because dependency DEP1 not found"
        in caplog.messages
    )


def test_logs_if_adapter_initialization_errored(make_adapter, caplog):
    adapter1 = make_adapter("FROM FIRST", name="FIRST ADAPTER")
    adapter2 = make_adapter("FROM SECOND", name="SECOND ADAPTER")
    adapter2.side_effect = InitializationError("Some details")

    loader = ConfigLoader(Option("EXISTING"), adapters=(adapter1, adapter2))
    loader.init()

    assert (
        "Adapter SECOND ADAPTER omitted because initialization errored with message "
        '"Some details"' in caplog.messages
    )
