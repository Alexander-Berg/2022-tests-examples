from flask import Flask
import json
from ya_courier_backend.resources.current_user import get_passport_user

MULTI_RESPONSE = """
{
    "status": {
        "value": "VALID",
        "id": 0
    },
    "error": "OK",
    "age": 84313,
    "expires_in": 7691687,
    "ttl": "5",
    "default_uid": "611320583",
    "users": [
        {
            "id": "445339711",
            "status": {
                "value": "VALID",
                "id": 0
            }
        },
        {
            "id": "445339734",
            "status": {
                "value": "VALID",
                "id": 0
            },
            "uid": {
                "value": "445339734",
                "lite": false,
                "hosted": false
            },
            "login": "",
            "have_password": false,
            "have_hint": false,
            "karma": {
                "value": 0
            },
            "karma_status": {
                "value": 0
            },
            "regname": "uid-7fhlmcgk",
            "display_name": {
                "name": "Vasya Pupkin",
                "avatar": {
                    "default": "30431/enc-fd3d1f574285def7034a36c2a178138b6eff2c6524b2abe93538f5858f15ce74",
                    "empty": false
                },
                "social": {
                    "profile_id": "78429589",
                    "provider": "gg",
                    "redirect_target": "1586203838.71238.78429589.7cdcd6756f64b30e1351686a48616002"
                }
            },
            "attributes": {
                "1007": "Pupkin Vasily",
                "29": "m",
                "31": "ru",
                "34": "ru",
                "98": "30431/enc-fd3d1f574285def7034a36c2a178138b6eff2c6524b2abe93538f5858f15ce74"
            },
            "address-list": [
                {
                    "address": "uid-7fhlmcgk@fake1.yandex.com",
                    "validated": true,
                    "default": true,
                    "rpop": false,
                    "silent": false,
                    "unsafe": false,
                    "native": false,
                    "born-date": "2016-12-15 13:23:37"
                }
            ],
            "phones": [
                {
                    "id": "370639960",
                    "attributes": {
                        "102": "+4000000000091"
                    }
                }
            ],
            "auth": {
                "password_verification_age": -1,
                "have_password": false,
                "secure": true,
                "partner_pdd_token": false,
                "social": {
                    "profile_id": "78429589"
                }
            }
        },
        {
            "id": "611320583",
            "status": {
                "value": "VALID",
                "id": 0
            },
            "uid": {
                "value": "611320583",
                "lite": false,
                "hosted": false
            },
            "login": "pupkin-dev-login",
            "have_password": true,
            "have_hint": true,
            "karma": {
                "value": 0
            },
            "karma_status": {
                "value": 0
            },
            "regname": "pupkin-dev",
            "display_name": {
                "name": "pupkin-dev",
                "avatar": {
                    "default": "0/0-0",
                    "empty": true
                }
            },
            "attributes": {
                "1007": "Pupkin Vasily",
                "31": "ru",
                "34": "ru"
            },
            "address-list": [
                {
                    "address": "pupkin-dev@yandex.ru",
                    "validated": true,
                    "default": true,
                    "rpop": false,
                    "silent": false,
                    "unsafe": false,
                    "native": true,
                    "born-date": "2018-03-23 16:02:41"
                },
                {
                    "address": "pupkin-dev@yandex.by",
                    "validated": true,
                    "default": false,
                    "rpop": false,
                    "silent": false,
                    "unsafe": false,
                    "native": true,
                    "born-date": "2018-03-23 16:02:41"
                }
            ],
            "phones": [],
            "auth": {
                "password_verification_age": 16367722,
                "have_password": true,
                "secure": true,
                "partner_pdd_token": false
            }
        }
    ],
    "allow_more_users": true,
    "connection_id": "s:1542006359747:zefwQBRn3lkAfxBJuBYBIA:58"
}
"""


SINGLE_RESPONSE = """
{
    "age": 86614,
    "expires_in": 7689386,
    "ttl": "5",
    "error": "OK",
    "status": {
        "value": "VALID",
        "id": 0
    },
    "uid": {
        "value": "611320583",
        "lite": false,
        "hosted": false
    },
    "login": "pupkin-dev-login",
    "have_password": true,
    "have_hint": true,
    "karma": {
        "value": 0
    },
    "karma_status": {
        "value": 0
    },
    "regname": "pupkin-dev",
    "display_name": {
        "name": "pupkin-dev",
        "avatar": {
            "default": "0/0-0",
            "empty": true
        }
    },
    "attributes": {
        "1007": "Pupkin Vasily",
        "31": "ru",
        "34": "ru"
    },
    "address-list": [
        {
            "address": "pupkin-dev@ya.ru",
            "validated": true,
            "default": false,
            "rpop": false,
            "silent": false,
            "unsafe": false,
            "native": true,
            "born-date": "2018-03-23 16:02:41"
        },
        {
            "address": "pupkin-dev@yandex.by",
            "validated": true,
            "default": false,
            "rpop": false,
            "silent": false,
            "unsafe": false,
            "native": true,
            "born-date": "2018-03-23 16:02:41"
        }
    ],
    "phones": [],
    "auth": {
        "password_verification_age": 16370023,
        "have_password": true,
        "secure": true,
        "partner_pdd_token": false
    },
    "connection_id": "s:1542006359747:zefwQBRn3lkAfxBJuBYBIA:58"
}
"""


def test_passport_user_multi():
    app = Flask(__name__)
    with app.app_context():
        result = get_passport_user(json.loads(MULTI_RESPONSE))
        assert result == {
            "passportUser": {
                "login": "pupkin-dev-login",
                "displayName": "pupkin-dev",
                "avatarId": "0/0-0",
                "uid": "611320583",
                "_needUpdateSession": False,
                "accounts": [
                    {
                        "login": "",
                        "displayName": "Vasya Pupkin",
                        "avatarId": "30431/enc-fd3d1f574285def7034a36c2a178138b6eff2c6524b2abe93538f5858f15ce74",
                        "uid": "445339734",
                        "fio": "Pupkin Vasily",
                        "phone": "+4000000000091",
                        "email": "uid-7fhlmcgk@fake1.yandex.com"
                    },
                    {
                        "login": "pupkin-dev-login",
                        "displayName": "pupkin-dev",
                        "avatarId": "0/0-0",
                        "uid": "611320583",
                        "fio": "Pupkin Vasily",
                        "phone": "",
                        "email": "pupkin-dev@yandex.ru"
                    }
                ],
                "csrfToken": result['passportUser']['csrfToken'],
                "fio": "Pupkin Vasily",
                "email": "pupkin-dev@yandex.ru",
                "phone": "",
                "lang": {
                    "id": "ru",
                    "name": "ru"
                }
            }
        }


def test_passport_user_single():
    app = Flask(__name__)
    with app.app_context():
        result = get_passport_user(json.loads(SINGLE_RESPONSE))
    assert result == {
        "passportUser": {
            "login": "pupkin-dev-login",
            "displayName": "pupkin-dev",
            "avatarId": "0/0-0",
            "uid": "611320583",
            "fio": "Pupkin Vasily",
            "phone": "",
            "email": "pupkin-dev@ya.ru",
            "_needUpdateSession": False,
            "accounts": [
                {
                    "login": "pupkin-dev-login",
                    "displayName": "pupkin-dev",
                    "avatarId": "0/0-0",
                    "uid": "611320583",
                    "fio": "Pupkin Vasily",
                    "phone": "",
                    "email": "pupkin-dev@ya.ru"
                }
            ],
            "csrfToken": result['passportUser']['csrfToken'],
            "lang": {
                "id": "ru",
                "name": "ru"
            }
        }
    }
