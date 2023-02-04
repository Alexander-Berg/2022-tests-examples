from maps.garden.sdk.module_traits.module_traits import ModuleTraits, ModuleType, SortOption

from maps.garden.tools.module_monitoring.lib.module_traits_consistency import generate_module_traits_consistency_events


def test_no_event_on_consistent_traits():
    traits = [
        ModuleTraits(name="src", type=ModuleType.SOURCE, sort_options=[SortOption(key_pattern="test_pattern")]),
        ModuleTraits(name="map", sources=["src"], type=ModuleType.MAP)
    ]
    return list(generate_module_traits_consistency_events("stable", traits))


def test_crit_event_on_consistent_traits():
    traits = [
        ModuleTraits(name="src", type=ModuleType.SOURCE, sort_options=[SortOption(key_pattern="test_pattern")]),
        ModuleTraits(name="map", sources=["unknown_src"], type=ModuleType.MAP)
    ]
    return list(generate_module_traits_consistency_events("datatesting", traits))
