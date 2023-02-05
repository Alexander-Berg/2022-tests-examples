#!/usr/bin/python
# -*- coding: utf-8 -*-

from ytools.corba import ServantFactory, CORBA
from ytools.config import Config
import omniORB
import Yandex

def main():
    config = Config()
    servants = ServantFactory(config)
    core = servants.get('core/bindname')
    #geometry:<?xml version="1.0"?><ym:ymaps xmlns:gml="http://www.opengis.net/gml" xmlns:ym="http://maps.yandex.ru/ymaps/1.x" xmlns:wm="http://maps.yandex.ru/wikimap/1.x"><ym:GeoObjectCollection><gml:featureMembers><ym:GeoObject><gml:metaDataProperty><wm:ObjectMetaData><wm:id>0</wm:id><wm:layerId>12</wm:layerId><wm:Revision><wm:id>0</wm:id></wm:Revision><wm:description><![CDATA[]]></wm:description><wm:Attributes></wm:Attributes></wm:ObjectMetaData></gml:metaDataProperty><gml:MultiGeometry><gml:geometryMembers><gml:Polygon><gml:exterior><gml:LinearRing><gml:posList>37.077967599034345 55.89549411633773 37.10131354629995 55.87832127920358 37.04191870987416 55.86963547931136 37.077967599034345 55.89549411633773</gml:posList></gml:LinearRing></gml:exterior></gml:Polygon></gml:geometryMembers></gml:MultiGeometry></ym:GeoObject></gml:featureMembers></ym:GeoObjectCollection></ym:ymaps>
    #zoom:12
    #lon:37.14594550430776
    #lat:55.66861159789524
    #ticket:u89f84bec225747e5406bd927ef6cccc2
    print core.GetObjectTemplate(0, 12, '<?xml version="1.0"?><ym:ymaps xmlns:gml="http://www.opengis.net/gml" xmlns:ym="http://maps.yandex.ru/ymaps/1.x" xmlns:wm="http://maps.yandex.ru/wikimap/1.x"><ym:GeoObjectCollection><gml:featureMembers><ym:GeoObject><gml:metaDataProperty><wm:ObjectMetaData><wm:id>0</wm:id><wm:layerId>12</wm:layerId><wm:Revision><wm:id>0</wm:id></wm:Revision><wm:description><![CDATA[]]></wm:description><wm:Attributes></wm:Attributes></wm:ObjectMetaData></gml:metaDataProperty><gml:MultiGeometry><gml:geometryMembers><gml:Polygon><gml:exterior><gml:LinearRing><gml:posList>37.077967599034345 55.89549411633773 37.10131354629995 55.87832127920358 37.04191870987416 55.86963547931136 37.077967599034345 55.89549411633773</gml:posList></gml:LinearRing></gml:exterior></gml:Polygon></gml:geometryMembers></gml:MultiGeometry></ym:GeoObject></gml:featureMembers></ym:GeoObjectCollection></ym:ymaps>', 37.14594550430776, 55.66861159789524)


if __name__ == '__main__':
    main()
