from maps_adv.export.lib.pipeline.xml.tree.pin_data_list import PinDataList


def test_returns_expected_xml_string():
    pin_data_list = [
        dict(
            pages=["navi_billboard_7"],
            places=["altay:1235768811"],
            polygons=[],
            disclaimer="",
            cost="0.1",
            fields=dict(
                campaignId="10496",
                product="pin_on_route_v2",
                ageCategory="",
                title="Скидки до 50% на шубы от фабрики «Елена Фурс»",
                styleSelected="bc50bbd177b11f0852c8145a794685058368"
                "ab3c514ea7e141d9ec6892b16df2",
                # noqa: E501
            ),
            creatives=[
                dict(
                    id="banner:id:fdbce49dbb44eeb08700feb6e6",
                    type="banner",
                    fields=dict(
                        styleBalloonBanner="fdbce49dbb44eeb08700feb6e6"
                        "c1242fe99002caf0ea4dce4916"
                        "c32bc4aad081"
                        # noqa: E501
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
                            {"not": False, "tag": "age", "content": ["25-34", "45-54"]}
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
            polygons=["id:1", "id:2"],
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
    ]

    result = str(PinDataList.from_iterable(pin_data_list))

    expected_xml = """<PinDataList>
  <PinData>
    <pageId>navi_billboard_7</pageId>
    <disclaimer></disclaimer>
    <cost>0.1</cost>
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
        <id>banner:id:fdbce49dbb44eeb08700feb6e6</id>
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
          <Or>
            <age>25-34</age>
            <age>45-54</age>
          </Or>
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
      <id>id:1</id>
      <id>id:2</id>
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
"""  # noqa: E501

    assert result == expected_xml
