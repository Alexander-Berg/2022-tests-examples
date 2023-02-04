# coding=utf-8
# TODO : we should write more tests in this module and do not use XML in any other cases...
import shapely

import yandex.maps.geolib3 as geolib

import maps.carparks.tools.dap_snippets.lib.common as common
import maps.carparks.tools.dap_snippets.lib.xml_converter as xml_converter
from maps.carparks.tools.dap_snippets.lib.driving_arrival_point import DrivingArrivalPoint


def test_anchor_only():
    actual = xml_converter.to_xml([
        DrivingArrivalPoint(anchor=geolib.Point2(1.1, 2.2))]
    )
    expected = u'''<DrivingArrivalPoints xmlns:gml="http://www.opengis.net/gml" xmlns="http://maps.yandex.ru/snippets/driving_arrival_points/1.x">
  <DrivingArrivalPoint>
    <Anchor>
      <gml:pos>1.1 2.2</gml:pos>
    </Anchor>
  </DrivingArrivalPoint>
</DrivingArrivalPoints>
'''

    # pytest has a bug when it prints a diff using non-utf-8 encoding
    # for the right side of equals operator. Temp variables workaround that
    # issue
    assert actual == expected


def test_linestring_geometry():
    linestring = shapely.geometry.LineString([(0, 0), (0, 4), (4, 4), (4, 0)])
    dap = DrivingArrivalPoint(
        anchor=geolib.Point2(1.1, 2.2),
        geometry=linestring)
    actual = xml_converter.to_xml([dap])
    expected = u'''<DrivingArrivalPoints xmlns:gml="http://www.opengis.net/gml" xmlns="http://maps.yandex.ru/snippets/driving_arrival_points/1.x">
  <DrivingArrivalPoint>
    <Anchor>
      <gml:pos>1.1 2.2</gml:pos>
    </Anchor>
    <Geometry>
      <gml:LineString>
        <gml:posList>0.0 0.0 0.0 4.0 4.0 4.0 4.0 0.0</gml:posList>
      </gml:LineString>
    </Geometry>
  </DrivingArrivalPoint>
</DrivingArrivalPoints>
'''
    assert actual == expected


def test_all_fields():
    actual = xml_converter.to_xml([DrivingArrivalPoint(
        anchor=geolib.Point2(0, 1),
        id='some_id',
        geometry=shapely.geometry.Polygon(((0, 0), (0, 2), (1, 3), (0, 0))),
        walking_time=common.LocalizedValue(45, u'2 мин'),
        price=common.Money(100, u'100 \u20BD', 'RUB'),
        description=u'улица Иванова',
        tags=['tag1', 'tag2', 'tag3'],
        rating=0.5)
    ])

    expected = u'''<DrivingArrivalPoints xmlns:gml="http://www.opengis.net/gml" xmlns="http://maps.yandex.ru/snippets/driving_arrival_points/1.x">
  <DrivingArrivalPoint>
    <id>some_id</id>
    <Anchor>
      <gml:pos>0.0 1.0</gml:pos>
    </Anchor>
    <Geometry>
      <gml:Polygon>
        <gml:exterior>
          <gml:LinearRing>
            <gml:posList>0.0 0.0 0.0 2.0 1.0 3.0 0.0 0.0</gml:posList>
          </gml:LinearRing>
        </gml:exterior>
      </gml:Polygon>
    </Geometry>
    <WalkingTime>
      <value>45</value>
      <text>2 мин</text>
    </WalkingTime>
    <Price>
      <value>100</value>
      <text>100 ₽</text>
      <currency>RUB</currency>
    </Price>
    <description>улица Иванова</description>
    <Tags>
      <tag>tag1</tag>
      <tag>tag2</tag>
      <tag>tag3</tag>
    </Tags>
    <rating>0.5</rating>
  </DrivingArrivalPoint>
</DrivingArrivalPoints>
'''
    assert actual == expected


def test_special_chars():
    SPECIAL_CHARS = u'> < &'

    actual = xml_converter.to_xml([DrivingArrivalPoint(
        anchor=geolib.Point2(0, 1),
        id=SPECIAL_CHARS,
        walking_time=common.LocalizedValue(45, SPECIAL_CHARS),
        price=common.Money(100, SPECIAL_CHARS, 'RUB'),
        description=SPECIAL_CHARS,
        tags=[SPECIAL_CHARS])]
    )

    expected = u'''<DrivingArrivalPoints xmlns:gml="http://www.opengis.net/gml" xmlns="http://maps.yandex.ru/snippets/driving_arrival_points/1.x">
  <DrivingArrivalPoint>
    <id>{escaped_special_chars}</id>
    <Anchor>
      <gml:pos>0.0 1.0</gml:pos>
    </Anchor>
    <WalkingTime>
      <value>45</value>
      <text>{escaped_special_chars}</text>
    </WalkingTime>
    <Price>
      <value>100</value>
      <text>{escaped_special_chars}</text>
      <currency>RUB</currency>
    </Price>
    <description>{escaped_special_chars}</description>
    <Tags>
      <tag>{escaped_special_chars}</tag>
    </Tags>
  </DrivingArrivalPoint>
</DrivingArrivalPoints>
'''.format(escaped_special_chars=u'&gt; &lt; &amp;')

    assert actual == expected
