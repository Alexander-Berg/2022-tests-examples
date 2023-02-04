from maps_adv.export.lib.pipeline.xml.tree.polygons import Polygons


def test_returns_expected_xml_string():
    polygons = [
        dict(id="id:1.1", polygon="WKT polygon 1"),
        dict(id="id:1.2", polygon="WKT polygon 2"),
        dict(id="id:2", polygon="WKT polygon 3"),
    ]

    result = str(Polygons.from_iterable(polygons))

    expected_xml = """<Polygons>
  <Polygon>
    <id>id:1.1</id>
    <polygon>WKT polygon 1</polygon>
  </Polygon>
  <Polygon>
    <id>id:1.2</id>
    <polygon>WKT polygon 2</polygon>
  </Polygon>
  <Polygon>
    <id>id:2</id>
    <polygon>WKT polygon 3</polygon>
  </Polygon>
</Polygons>
"""

    assert str(result) == expected_xml


def test_returns_empty_places_tag():
    polygons = []

    result = str(Polygons.from_iterable(polygons))
    expected_xml = "<Polygons/>\n"

    assert str(result) == expected_xml
