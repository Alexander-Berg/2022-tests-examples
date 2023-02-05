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
    # http://maps-wiki2.algol.maps.yandex.ru/actions/get-suggest.xml?layer-id=2&name=tags&depend-name=type&tags=%D1%81&type=%D0%B3%D0%BE%D1%80%D0%BE%D0%B4
    print core.GetSuggest(2, 'tags', 'с', 'type', 'город')
    print core.GetSuggest(2, 'tags', '', 'type', 'город')


if __name__ == '__main__':
    main()
