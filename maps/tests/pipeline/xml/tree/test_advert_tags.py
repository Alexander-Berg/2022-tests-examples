from maps_adv.export.lib.pipeline.xml.tree.advert_tags import AdvertTags


def test_returns_expected_xml_string():
    advert_tags = [
        dict(id="action_search_tag_e0d69fb8ef84e26b2626367b8a7b4e39", companies=[]),
        dict(
            id="action_search_tag_dddfa402022aefbb85ca525e545cf55a",
            companies=[90319553535, 1133966351],
        ),
    ]

    result = str(AdvertTags.from_iterable(advert_tags))

    expected_xml = """<AdvertTags>
  <AdvertTag id="action_search_tag_dddfa402022aefbb85ca525e545cf55a">
    <Companies>
      <id>1133966351</id>
      <id>90319553535</id>
    </Companies>
  </AdvertTag>
</AdvertTags>
"""

    assert str(result) == expected_xml
