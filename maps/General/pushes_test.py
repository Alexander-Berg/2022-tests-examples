import json

from maps.poi.notification.lib.sup import (
    make_push_json_string,
    PushSettings,
)


def _check_push_json(row, push_settings, json_push):
    push_json_string = make_push_json_string(row, push_settings)
    assert json.loads(push_json_string) == json_push


def test_push_json_string():
    _check_push_json(
        row={
            'application': 'ru.yandex.traffic',
            'device_id': '00003BE7-EF1B-413F-9609-3DED13AFFC53',
            'uuid': 'c3b2820a6bfea5b3bfe6895a382cc5fe',
            'organizations': [1127247543]
        },
        push_settings=PushSettings(
            push_type='geoplatform_org_status',
            force_push=False,
            receiver='uuid',
            texts_seed=0,
            target='feedback_form',
        ),
        json_push={
            'receiver': 'tag:iid==c3b2820a6bfea5b3bfe6895a382cc5fe AND '
                        'theme NOT IN (\'maps_org_feedback_off\', \'maps_org_feedback\', \'org_feedback\')',
            'ttl': 7200,
            'schedule': 'now',
            'adjust_time_zone': False,
            'android_features': {
                'vibrationOn': False,
                'priority': 1,
                'silent': False,
                'ledType': 1,
                'soundType': 1
            },
            'ios_features': {
                'content_available': False,
                'mutable_content': False,
                'soundType': 1
            },
            'is_data_only': False,
            'project': 'mobmaps',
            'throttle_policies': {
                'device_id': 'maps_daily1_weekly2',
                'install_id': 'maps_daily1_weekly2'
            },
            'data': {
                'push_action': 'uri',
                'push_id': 'geoplatform_org_status',
                'push_uri': 'https://yandex.ru/web-maps/feedback/?type=organization%2Fedit-status&'
                            'context=organization.push_notifications&'
                            'uri=ymapsbm1%3A%2F%2Forg%3Foid%3D1127247543&'
                            'client_id=ru.yandex.traffic&'
                            'device_id=00003BE7-EF1B-413F-9609-3DED13AFFC53&'
                            'uuid=c3b2820a6bfea5b3bfe6895a382cc5fe',
                'topic_push': 'maps_org_feedback'
            },
            'notification': {
                'body': 'Проверьте, работают ли эти организации',
                'iconId': 'notifications_yandex_map_logo',
                'title': 'Улучшим карты вместе ✨'
            }
        },
    )

    _check_push_json(
        row={
            'application': 'ru.yandex.traffic',
            'puid': '123456',
            'organizations': [111, 222]
        },
        push_settings=PushSettings(
            push_type='geoplatform_org_status',
            force_push=False,
            receiver='puid',
            texts_seed=0,
            target='feedback_form',
        ),
        json_push={
            'receiver': 'tag:uid==123456 AND app_id==\'ru.yandex.traffic\' AND '
                        'theme NOT IN (\'maps_org_feedback_off\', \'maps_org_feedback\', \'org_feedback\')',
            'ttl': 7200,
            'schedule': 'now',
            'adjust_time_zone': False,
            'android_features': {
                'vibrationOn': False,
                'priority': 1,
                'silent': False,
                'ledType': 1,
                'soundType': 1
            },
            'ios_features': {
                'content_available': False,
                'mutable_content': False,
                'soundType': 1
            },
            'is_data_only': False,
            'project': 'mobmaps',
            'throttle_policies': {
                'device_id': 'maps_daily1_weekly2',
                'install_id': 'maps_daily1_weekly2'
            },
            'data': {
                'push_action': 'uri',
                'push_id': 'geoplatform_org_status',
                'push_uri': 'https://yandex.ru/web-maps/feedback/?type=organization%2Fedit-status&'
                            'context=organization.push_notifications&'
                            'uri=ymapsbm1%3A%2F%2Forg%3Foid%3D111&'
                            'uri=ymapsbm1%3A%2F%2Forg%3Foid%3D222&'
                            'client_id=ru.yandex.traffic',
                'topic_push': 'maps_org_feedback'
            },
            'notification': {
                'body': 'Проверьте, работают ли эти организации',
                'iconId': 'notifications_yandex_map_logo',
                'title': 'Улучшим карты вместе ✨'
            },
            'filters': {
                'passportUid': '123456'
            }
        },
    )

    _check_push_json(
        row={
            'application': 'ru.yandex.yandexnavi',
            'device_id': 'device_address',
            'uuid': 'uuid_address',
            'uid': 'uid_address',
            'bld_lon': 37.37,
            'bld_lat': 55.55,
            'ghash9': 'ghash9',
        },
        push_settings=PushSettings(
            push_type='geoplatform_address_request',
            force_push=False,
            receiver='puid',
            texts_seed=0,
            target='ugc_account',
        ),
        json_push={
            'receiver': 'tag:uid==uid_address AND app_id==\'ru.yandex.yandexnavi\' AND '
                        'theme NOT IN (\'maps_feedback_off\', \'maps_feedback\', \'feedback\')',
            'ttl': 7200,
            'schedule': 'now',
            'adjust_time_zone': False,
            'android_features': {
                'vibrationOn': False,
                'priority': 1,
                'silent': False,
                'ledType': 1,
                'soundType': 1
            },
            'ios_features': {
                'content_available': False,
                'mutable_content': False,
                'soundType': 1
            },
            'is_data_only': False,
            'project': 'navi',
            'throttle_policies': {
                'device_id': 'navi_daily1_weekly2',
                'install_id': 'navi_daily1_weekly2'
            },
            'data': {
                'push_action': 'uri',
                'push_id': 'geoplatform_address_request',
                'push_uri': 'yandexnavi://show_web_view?link='
                            'https%3A%2F%2Fyandex.ru%2Fmaps%2Fprofile%2Fugc%2Fassignments%2F'
                            '%3Fz%3D17%26ll%3D37.37%252C55.55%26assignment_id%3Daddress_add'
                            '%253Aghash9%26context%3Dugc.address_add.push_notifications'
                            '%26client_id%3Dru.yandex.yandexnavi&authenticate=use_auth',
                'topic_push': 'maps_address_feedback'
            },
            'notification': {
                'title': 'Добавьте адрес здания на карту',
                'body': 'Чтобы доставке или такси было проще приехать, если они понадобятся',
                'icon': 'https://avatars.mds.yandex.net/get-bunker/'
                        '118781/474c067c398d7d1e80ce1c5f987cc23a184e4e2b/orig',
            },
            'filters': {
                'passportUid': 'uid_address'
            }
        },
    )

    _check_push_json(
        row={
            'application': 'ru.yandex.traffic',
            'puid': '123456',
            'assignment_id': 'os:111'
        },
        push_settings=PushSettings(
            push_type='geoplatform_org_status',
            force_push=False,
            receiver='puid',
            texts_seed=0,
            target='ugc_account',
        ),
        json_push={
            'receiver': 'tag:uid==123456 AND app_id==\'ru.yandex.traffic\' AND '
                        'theme NOT IN (\'maps_org_feedback_off\', \'maps_org_feedback\', \'org_feedback\')',
            'ttl': 7200,
            'schedule': 'now',
            'adjust_time_zone': False,
            'android_features': {
                'vibrationOn': False,
                'priority': 1,
                'silent': False,
                'ledType': 1,
                'soundType': 1
            },
            'ios_features': {
                'content_available': False,
                'mutable_content': False,
                'soundType': 1
            },
            'is_data_only': False,
            'project': 'mobmaps',
            'throttle_policies': {
                'device_id': 'maps_daily1_weekly2',
                'install_id': 'maps_daily1_weekly2'
            },
            'data': {
                'push_action': 'uri',
                'push_id': 'geoplatform_org_status',
                'push_uri': 'yandexmaps://open_url?url='
                            'https%3A%2F%2Fyandex.ru%2Fweb-maps%2Fprofile%2Fugc%2Fassignments%2F%3F'
                            'context%3Dugc.org_status.push_notifications%26'
                            'assignment_id%3Dos%253A111%26'
                            'client_id%3Dru.yandex.traffic',
                'topic_push': 'maps_org_feedback'
            },
            'notification': {
                'body': 'Проверьте, работают ли эти организации',
                'iconId': 'notifications_yandex_map_logo',
                'title': 'Улучшим карты вместе ✨'
            },
            'filters': {
                'passportUid': '123456'
            }
        },
    )

    _check_push_json(
        row={
            'application': 'ru.yandex.yandexnavi',
            'puid': '123456',
            'assignment_id': 'os:111'
        },
        push_settings=PushSettings(
            push_type='geoplatform_org_status',
            force_push=False,
            receiver='puid',
            texts_seed=0,
            target='ugc_account',
        ),
        json_push={
            'receiver': 'tag:uid==123456 AND app_id==\'ru.yandex.yandexnavi\' AND '
                        'theme NOT IN (\'maps_org_feedback_off\', \'maps_org_feedback\', \'org_feedback\')',
            'ttl': 7200,
            'schedule': 'now',
            'adjust_time_zone': False,
            'android_features': {
                'vibrationOn': False,
                'priority': 1,
                'silent': False,
                'ledType': 1,
                'soundType': 1
            },
            'ios_features': {
                'content_available': False,
                'mutable_content': False,
                'soundType': 1
            },
            'is_data_only': False,
            'project': 'navi',
            'throttle_policies': {
                'device_id': 'navi_daily1_weekly2',
                'install_id': 'navi_daily1_weekly2'
            },
            'data': {
                'push_action': 'uri',
                'push_id': 'geoplatform_org_status',
                'push_uri': 'yandexnavi://show_web_view?link='
                            'https%3A%2F%2Fyandex.ru%2Fmaps%2Fprofile%2Fugc%2Fassignments%2F%3F'
                            'context%3Dugc.org_status.push_notifications%26'
                            'assignment_id%3Dos%253A111%26'
                            'client_id%3Dru.yandex.yandexnavi'
                            '&authenticate=use_auth',
                'topic_push': 'maps_org_feedback'
            },
            'notification': {
                'body': 'Проверьте, работают ли эти организации',
                'icon': 'https://avatars.mds.yandex.net/get-bunker/118781/474c067c398d7d1e80ce1c5f987cc23a184e4e2b/orig',
                'title': 'Улучшим карты вместе ✨'
            },
            'filters': {
                'passportUid': '123456'
            }
        },
    )
