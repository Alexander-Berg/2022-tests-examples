from maps_adv.export.lib.pipeline.xml.transform.place import GenericPlace
from maps_adv.export.lib.pipeline.xml.tree.root import Root


def test_returns_expected_xml_string():
    data = dict(
        advert_data_list=[
            dict(
                log_id="ac_879ac7cb6f48d8ef_11572",
                pages=["navi_menu_icon_1", "navi_menu_icon_1/datatesting"],
                title="Mizzi Köşkü",
                text="16. İstanbul Bienali 14 Eylül’den 10 Kasım’a dek açık.",
                phone=dict(
                    telephone="+7(111)2223344",
                    formatted="+7 (111) 222-33-44",
                    country="7",
                    prefix="111",
                    number="2223344",
                ),
                fields=dict(
                    advert_type="menu_icon",
                    anchorDust="0.5 0.5",
                    styleLogo="geoadv-ext--1993488--2a0000016d15b--logo",
                ),
                companies=[123305157958, 123305157959],
            ),
            dict(
                log_id="ac_879ac7cb6f48d8ef_11572",
                pages=[],
                title="Mizzi Köşkü",
                text="16. İstanbul Bienali 14 Eylül’den 10 Kasım’a dek açık.",
                phone=dict(
                    telephone="+7(111)2223344",
                    formatted="+7 (111) 222-33-44",
                    number="2223344",
                ),
                fields=dict(),
                companies=[],
            ),
        ],
        menu_items=[
            dict(
                id="ac_879ac7cb6f48d8ef_11572",
                pages=["navi", "navi/datatesting"],
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
        ],
        pin_data_list=[
            dict(
                pages=["navi_billboard_7"],
                places=["altay:1235768811"],
                polygons=[],
                disclaimer="",
                fields=dict(
                    campaignId="10496",
                    product="pin_on_route_v2",
                    ageCategory="",
                    title="Скидки до 50% на шубы от фабрики «Елена Фурс»",
                    styleSelected="bc50bbd177b11f0852c8145a794685058368ab3c514ea7e141d9ec6892b16df2",  # noqa: E501
                ),
                creatives=[
                    dict(
                        id="banner:id:fdbce49dbb44eeb08700feb6e",
                        type="banner",
                        fields=dict(
                            styleBalloonBanner="fdbce49dbb44eeb08700feb6e6c1242fe99002caf0ea4dce4916c32bc4aad081"  # noqa: E501
                        ),
                    ),
                    dict(
                        id="logo:id:000000000000000",
                        type="logo",
                        fields=dict(styleLogo="000000000000000", text="logo text"),
                    ),
                ],
                actions=[],
                limits={},
                target={
                    "not": False,
                    "tag": "and",
                    "items": [
                        {
                            "not": False,
                            "tag": "segment",
                            "attributes": {"id": "582", "keywordId": "217"},
                        },
                        {
                            "not": False,
                            "tag": "or",
                            "items": [
                                {"not": False, "tag": "age", "content": ["25-34"]}
                            ],
                        },
                        {"not": True, "tag": "gender", "content": ["male"]},
                        {"not": True, "tag": "or", "items": []},
                    ],
                },
                log_info={
                    "advertiserId": None,
                    "campaignId": 10496,
                    "product": "pin_on_route_v2",
                },
            ),
            dict(
                pages=["navi_zero_speed_banner_2"],
                places=["10000"],
                polygons=["campaign:11506.1"],
                disclaimer="",
                fields=dict(
                    campaignId="11506",
                    description="Устанавливайте голос и курсор от меня, кота Гурмэ, и испытывайте восторг от поездки каждый день, как я от Гурмэ!\n\nГурмяяу!",  # noqa: E501
                ),
                creatives=[],
                actions=[
                    dict(type="Call", fields=dict(phone="+7 (111) 222-33-44")),
                    dict(
                        type="OpenSite",
                        fields=dict(
                            url="https://elenafurs.ru/sale?specs_values=394,397,403,430,405,578,398&sort=price-asc&utm_source=yandex&utm_medium=navi&utm_campaign=pin",  # noqa: E501
                            title="На сайт",
                        ),
                    ),
                ],
                limits=dict(displayProbability=0.887278),
                target={},
                log_info={
                    "advertiserId": None,
                    "campaignId": "11506",
                    "product": "zero_speed_banner",
                },
            ),
        ],
        places=[
            GenericPlace(
                id="1000",
                latitude=54.3747711182,
                longitude=37.5217437744,
                address=[],
                title=[],
                permalink=None,
            ),
            GenericPlace(
                id="altay:100002340873",
                latitude=39.57265600,
                longitude=32.12236510,
                address=[dict(value="Hürriyet Mahallesi Yeni Sanayi Sitesi No:1")],
                title=[dict(value="Bridgestone Yetkili Bayii")],
                permalink=100002340873,
            ),
            GenericPlace(
                id="altay:100002340874",
                latitude=39.57265600,
                longitude=32.12236510,
                address=[dict(lang="tr", value="Hürriyet Mahallesi Yeni")],
                title=[dict(lang="en", value="Bridgestone Yetkili Bayii")],
                permalink=100002340874,
            ),
        ],
        polygons=[dict(id="campaign:11506.1", polygon="POLYGON ((1 2, 2 3))")],
        advert_tags=[
            dict(id="action_search_tag_e0d69fb8ef84e26b2626367b8a7b4e39", companies=[]),
            dict(
                id="action_search_tag_dddfa402022aefbb85ca525e545cf55a",
                companies=[90319553535, 1133966351],
            ),
        ],
    )

    result = str(Root.from_dict(data))

    expected_xml = """<?xml version='1.0' encoding='utf-8'?>
<AdvertDataList xmlns="http://maps.yandex.ru/advert/1.x">
  <MenuItems>
    <MenuItem id="ac_879ac7cb6f48d8ef_11572">
      <pageId>navi</pageId>
      <pageId>navi/datatesting</pageId>
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
  <AdvertData>
    <pageId>navi_menu_icon_1</pageId>
    <pageId>navi_menu_icon_1/datatesting</pageId>
    <logId>ac_879ac7cb6f48d8ef_11572</logId>
    <Tags>
      <field name="advert_type">menu_icon</field>
      <field name="anchorDust">0.5 0.5</field>
      <field name="styleLogo">geoadv-ext--1993488--2a0000016d15b--logo</field>
    </Tags>
    <Advert>
      <title>Mizzi Köşkü</title>
      <text>16. İstanbul Bienali 14 Eylül’den 10 Kasım’a dek açık.</text>
      <OverrideMetadata>
        <Phones>
          <Phone type="phone">
            <formatted>+7 (111) 222-33-44</formatted>
            <country>7</country>
            <prefix>111</prefix>
            <number>2223344</number>
          </Phone>
        </Phones>
      </OverrideMetadata>
    </Advert>
    <Companies>
      <id>123305157958</id>
      <id>123305157959</id>
    </Companies>
  </AdvertData>
  <AdvertData>
    <logId>ac_879ac7cb6f48d8ef_11572</logId>
    <Advert>
      <title>Mizzi Köşkü</title>
      <text>16. İstanbul Bienali 14 Eylül’den 10 Kasım’a dek açık.</text>
      <OverrideMetadata>
        <Phones>
          <Phone type="phone">
            <formatted>+7 (111) 222-33-44</formatted>
            <number>2223344</number>
          </Phone>
        </Phones>
      </OverrideMetadata>
    </Advert>
  </AdvertData>
  <AdvertTags>
    <AdvertTag id="action_search_tag_dddfa402022aefbb85ca525e545cf55a">
      <Companies>
        <id>1133966351</id>
        <id>90319553535</id>
      </Companies>
    </AdvertTag>
  </AdvertTags>
  <Places>
    <Place>
      <id>1000</id>
      <latitude>54.3747711182</latitude>
      <longitude>37.5217437744</longitude>
    </Place>
    <Place>
      <id>altay:100002340873</id>
      <latitude>39.572656</latitude>
      <longitude>32.1223651</longitude>
      <address>Hürriyet Mahallesi Yeni Sanayi Sitesi No:1</address>
      <title>Bridgestone Yetkili Bayii</title>
      <permalink>100002340873</permalink>
    </Place>
    <Place>
      <id>altay:100002340874</id>
      <latitude>39.572656</latitude>
      <longitude>32.1223651</longitude>
      <address xml:lang="tr">Hürriyet Mahallesi Yeni</address>
      <title xml:lang="en">Bridgestone Yetkili Bayii</title>
      <permalink>100002340874</permalink>
    </Place>
  </Places>
  <Polygons>
    <Polygon>
      <id>campaign:11506.1</id>
      <polygon>POLYGON ((1 2, 2 3))</polygon>
    </Polygon>
  </Polygons>
  <PinDataList>
    <PinData>
      <pageId>navi_billboard_7</pageId>
      <disclaimer></disclaimer>
      <Places>
        <id>altay:1235768811</id>
      </Places>
      <Tags>
        <field name="ageCategory"></field>
        <field name="campaignId">10496</field>
        <field name="product">pin_on_route_v2</field>
        <field name="styleSelected">bc50bbd177b11f0852c8145a794685058368ab3c514ea7e141d9ec6892b16df2</field>
        <field name="title">Скидки до 50% на шубы от фабрики «Елена Фурс»</field>
      </Tags>
      <Creatives>
        <Creative>
          <id>banner:id:fdbce49dbb44eeb08700feb6e</id>
          <type>banner</type>
          <field name="styleBalloonBanner">fdbce49dbb44eeb08700feb6e6c1242fe99002caf0ea4dce4916c32bc4aad081</field>
        </Creative>
        <Creative>
          <id>logo:id:000000000000000</id>
          <type>logo</type>
          <field name="styleLogo">000000000000000</field>
          <field name="text">logo text</field>
        </Creative>
      </Creatives>
      <Target>
        <And>
          <segment id="582" keywordId="217"/>
          <Or>
            <age>25-34</age>
          </Or>
          <Not>
            <gender>male</gender>
          </Not>
        </And>
      </Target>
      <logInfo>{"advertiserId": "None", "campaignId": "10496", "product": "pin_on_route_v2"}</logInfo>
    </PinData>
    <PinData>
      <pageId>navi_zero_speed_banner_2</pageId>
      <disclaimer></disclaimer>
      <Places>
        <id>10000</id>
      </Places>
      <Polygons>
        <id>campaign:11506.1</id>
      </Polygons>
      <Tags>
        <field name="campaignId">11506</field>
        <field name="description">Устанавливайте голос и курсор от меня, кота Гурмэ, и испытывайте восторг от поездки каждый день, как я от Гурмэ!

Гурмяяу!</field>
      </Tags>
      <Actions>
        <Action>
          <type>Call</type>
          <field name="phone">+7 (111) 222-33-44</field>
        </Action>
        <Action>
          <type>OpenSite</type>
          <field name="title">На сайт</field>
          <field name="url">https://elenafurs.ru/sale?specs_values=394,397,403,430,405,578,398&amp;sort=price-asc&amp;utm_source=yandex&amp;utm_medium=navi&amp;utm_campaign=pin</field>
        </Action>
      </Actions>
      <Limits>
        <displayProbability>0.887278</displayProbability>
      </Limits>
      <logInfo>{"advertiserId": "None", "campaignId": "11506", "product": "zero_speed_banner"}</logInfo>
    </PinData>
  </PinDataList>
</AdvertDataList>
"""  # noqa: E501

    assert str(result) == expected_xml


def test_roots_tags_without_children_will_removed():
    payload = dict(
        advert_data_list=[],
        menu_items=[],
        pin_data_list=[],
        places={},
        polygons={},
        advert_tags=[],
    )

    result = str(Root.from_dict(payload))

    expected_xml = """<?xml version='1.0' encoding='utf-8'?>
<AdvertDataList xmlns="http://maps.yandex.ru/advert/1.x"/>
"""

    assert str(result) == expected_xml
