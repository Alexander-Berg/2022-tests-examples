from maps_adv.export.lib.pipeline.xml.transform.place import GenericPlace
from maps_adv.export.lib.pipeline.xml.tree.places import Places


def test_returns_expected_xml_string():
    places = [
        GenericPlace(
            id="1000",
            latitude=54.3747711182,
            longitude=37.5217437744,
            address=[],
            title=[],
            permalink=None,
        ),
        GenericPlace(
            id="altay:100002340874",
            latitude=39.57265600,
            longitude=32.12236510,
            address=[dict(value="Hürriyet Mahallesi Yeni Sanayi Sitesi No:1")],
            title=[dict(value="Bridgestone Yetkili Bayii")],
            permalink=100002340874,
        ),
        GenericPlace(
            id="altay:100002340873",
            latitude=39.57265600,
            longitude=32.12236510,
            address=[dict(lang="tr", value="Hürriyet Mahallesi Yeni")],
            title=[dict(lang="en", value="Bridgestone Yetkili Bayii")],
            permalink=100002340873,
        ),
    ]

    result = str(Places.from_iterable(places))

    expected_xml = """<Places>
  <Place>
    <id>1000</id>
    <latitude>54.3747711182</latitude>
    <longitude>37.5217437744</longitude>
  </Place>
  <Place>
    <id>altay:100002340873</id>
    <latitude>39.572656</latitude>
    <longitude>32.1223651</longitude>
    <address xml:lang="tr">Hürriyet Mahallesi Yeni</address>
    <title xml:lang="en">Bridgestone Yetkili Bayii</title>
    <permalink>100002340873</permalink>
  </Place>
  <Place>
    <id>altay:100002340874</id>
    <latitude>39.572656</latitude>
    <longitude>32.1223651</longitude>
    <address>Hürriyet Mahallesi Yeni Sanayi Sitesi No:1</address>
    <title>Bridgestone Yetkili Bayii</title>
    <permalink>100002340874</permalink>
  </Place>
</Places>
"""

    assert str(result) == expected_xml


def test_returns_empty_places_tag():
    places = []

    result = str(Places.from_iterable(places))
    expected_xml = "<Places/>\n"

    assert str(result) == expected_xml
