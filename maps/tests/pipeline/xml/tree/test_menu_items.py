from maps_adv.export.lib.pipeline.xml.tree.menu_items import MenuItems


def test_returns_expected_xml_string():
    menu_items = [
        dict(
            id="ac_879ac7cb6f48d8ef_11572",
            pages=["navi_zero_speed_banner", "navi_zero_speed_banner/datatesting"],
            style="campaign-11572_f9c49b9d38cf632a",
            position=14,
            title=[dict(lang="ru", value="16. İstanbul Bienali")],
            search_text="""{"text": "", "ad": {"advert_tag_id": "search-tag_cid-hash_94629eb2f9a4d7f9309239c4bcb96f0c"}}""",  # noqa: E501
            companies=[123305157958, 1003909190],
        ),
        dict(
            id="ac_879ac7cb6f48d8ef_11573",
            pages=[],
            style="campaign-11572_f9c49b9d38cf632a",
            position=14,
            title=[dict(value="16. İstanbul Bienali 2")],
            search_text="chain:1",
            companies=[],
        ),
    ]

    result = str(MenuItems.from_iterable(menu_items))

    expected_xml = """<MenuItems>
  <MenuItem id="ac_879ac7cb6f48d8ef_11572">
    <pageId>navi_zero_speed_banner</pageId>
    <pageId>navi_zero_speed_banner/datatesting</pageId>
    <style>campaign-11572_f9c49b9d38cf632a</style>
    <title xml:lang="ru">16. İstanbul Bienali</title>
    <searchText>{"text": "", "ad": {"advert_tag_id": "search-tag_cid-hash_94629eb2f9a4d7f9309239c4bcb96f0c"}}</searchText>
    <position>14</position>
    <Companies>
      <id>123305157958</id>
      <id>1003909190</id>
    </Companies>
  </MenuItem>
  <MenuItem id="ac_879ac7cb6f48d8ef_11573">
    <style>campaign-11572_f9c49b9d38cf632a</style>
    <title>16. İstanbul Bienali 2</title>
    <searchText>chain:1</searchText>
    <position>14</position>
  </MenuItem>
</MenuItems>
"""  # noqa: E501

    assert str(result) == expected_xml
