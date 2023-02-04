from maps_adv.export.lib.pipeline.xml.tree.advert_data_list import AdvertDataList


def test_returns_expected_xml_string():
    advert_data_list = [
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
    ]

    result = str(AdvertDataList.from_iterable(advert_data_list))

    expected_xml = """<AdvertDataList>
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
</AdvertDataList>
"""

    assert str(result) == expected_xml


def test_returns_expected_result_without_phone_field():
    advert_data_list = [
        dict(
            log_id="ac_879ac7cb6f48d8ef_11572",
            pages=["navi_menu_icon_1", "navi_menu_icon_1/datatesting"],
            title="Mizzi Köşkü",
            text="16. İstanbul Bienali 14 Eylül’den 10 Kasım’a dek açık.",
            fields=dict(
                advert_type="menu_icon",
                anchorDust="0.5 0.5",
                styleLogo="geoadv-ext--1993488--2a0000016d15b--logo",
            ),
            companies=[123305157958, 123305157959],
        )
    ]

    result = str(AdvertDataList.from_iterable(advert_data_list))

    expected_xml = """<AdvertDataList>
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
    </Advert>
    <Companies>
      <id>123305157958</id>
      <id>123305157959</id>
    </Companies>
  </AdvertData>
</AdvertDataList>
"""

    assert str(result) == expected_xml
