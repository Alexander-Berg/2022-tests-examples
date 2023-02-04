from maps.garden.libs_server.build import build_defs


def _source(name, build_id):
    return build_defs.Source(
        name=name,
        version=build_defs.SourceVersion(build_id=build_id),
    )


def test_source_comparison():
    assert _source("name", 1) < _source("name", 2)
    assert _source("name", 4) > _source("name", 3)
    assert _source("name", 1) < _source("name", 11)
    assert _source("name", 2) < _source("name", 11)
