from yandex.maps.proto.billboard.logs_pb2 import Events
from maps.doc.proto.testhelper.common import Point
from maps.doc.proto.testhelper.validator import Validator
import json


def add_event(msg, kind, ull, place_id=None, reqid='', log_id=''):
    event = msg.event.add()
    event.reqid = reqid
    event.event = kind
    event.log_id = log_id
    event.user_location.lon = ull.lon
    event.user_location.lat = ull.lat
    data = json.loads(log_id)
    permalink = data.get('permalink')
    if place_id:
        event.place_id = place_id
    elif permalink:
        event.place_id = 'altay:' + permalink
    elif data['product'] != 'billboard':
        event.place_id = 'poly:' + data['campaignId']


def add_show_click_events(msg, ull, place_id=None, reqid='', log_id=''):
    add_event(msg, 'billboard.show', ull, place_id, reqid, log_id)
    add_event(msg, 'billboard.click', ull, place_id, reqid, log_id)


def test_logs():
    evs = Events()

    # pin_on_route
    add_show_click_events(evs, Point(37.062, 55.437),
                          reqid='1561741676538210-2873431307-vla1-6002-vla-addrs-advert-12853',
                          log_id='{"advertiserId": "3772", "campaignId": "9929", "product": "pin_on_route", "permalink": "125236040062"}')

    # billboard
    add_show_click_events(evs, Point(37.833, 55.226), place_id='point:1',
                          reqid='1561741776538210-8762387692-vla1-6002-vla-addrs-advert-12853',
                          log_id='{"advertiserId": "3778", "campaignId": "9939", "product": "billboard"}')

    # zero_speed_banner
    add_show_click_events(evs, Point(37.922, 55.628),
                          reqid='1561743676538210-7862597621-vla1-6002-vla-addrs-advert-12853',
                          log_id='{"advertiserId": "3879", "campaignId": "9849", "product": "zero_speed_banner"}')

    # actions
    add_event(evs, 'billboard.action.call', Point(37.978, 55.845),
              reqid='1561743676538210-7862597621-vla1-6002-vla-addrs-advert-12853',
              log_id='{"advertiserId": "3779", "campaignId": "9949", "product": "pin_on_route", "permalink": "15236040162"}')

    add_event(evs, 'billboard.action.search', Point(37.864, 55.202), place_id='point:2',
              reqid='1561743676538210-7862597621-vla1-6002-vla-addrs-advert-12853',
              log_id='{"advertiserId": "3780", "campaignId": "9950", "product": "billboard"}')

    add_event(evs, 'billboard.action.open_site', Point(37.770, 55.990),
              reqid='1561743676538210-7862597621-vla1-6002-vla-addrs-advert-12853',
              log_id='{"advertiserId": "3781", "campaignId": "9951", "product": "pin_on_route", "permalink": "15236040262"}')

    add_event(evs, 'billboard.action.open_app', Point(37.771, 55.757),
              reqid='1561743676538210-7862597621-vla1-6002-vla-addrs-advert-12853',
              log_id='{"advertiserId": "3782", "campaignId": "9952", "product": "zero_speed_banner"}')

    add_event(evs, 'billboard.action.save_offer', Point(37.540, 55.108),
              reqid='1561743676538210-7862597621-vla1-6002-vla-addrs-advert-12853',
              log_id='{"advertiserId": "3783", "campaignId": "9953", "product": "pin_on_route", "permalink": "15236040362"}')

    # navigation
    add_event(evs, 'billboard.navigation.go', Point(37.701, 55.547),
              reqid='1561741676538210-3431345608-vla1-6002-vla-addrs-advert-12853',
              log_id='{"advertiserId": "3784", "campaignId": "9959", "product": "pin_on_route", "permalink": "15236040062"}')

    add_event(evs, 'billboard.navigation.via', Point(37.201, 55.548),
              reqid='1561741676538210-3431345608-vla1-6002-vla-addrs-advert-12853',
              log_id='{"advertiserId": "3784", "campaignId": "9959", "product": "pin_on_route", "permalink": "15236040062"}')

    Validator('billboard').validate_example(evs, 'logs')
