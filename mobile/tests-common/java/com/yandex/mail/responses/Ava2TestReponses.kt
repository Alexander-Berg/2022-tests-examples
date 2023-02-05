@file:Suppress("StringLiteralDuplication")

package com.yandex.mail.responses

val AVA2_RESPONSE_JSON = """
{
    "catdog":{
        "\"Misha\" <misha@mail.ru>":[
            {
                "domain":"mail.ru",
                "mono":"MI",
                "ava":{
                    "url":"https://avatars-fast.yandex.net/get-profile-avatar/people-d45b7f843996040236a559f9ad0b9833/large",
                    "type":"avatar",
                    "url_mobile":"https://avatars-fast.yandex.net/get-profile-avatar/people-d45b7f843996040236a559f9ad0b9833/large",
                    "url_small":"https://avatars-fast.yandex.net/get-profile-avatar/people-d45b7f843996040236a559f9ad0b9833/large"
                },
                "display_name":"Misha",
                "email":"Misha <misha@mail.ru>",
                "color":"#3d88f2",
                "local":"misha",
                "valid":true
            }
        ],
        "noreply@kopyta.org":[
            {
                "domain":"kopyta.org",
                "mono":"K",
                "ava":null,
                "display_name":"",
                "email":"noreply@kopyta.org",
                "color":"#ffffff",
                "local":"noreply",
                "valid":true
            }
        ],
        "not a valid email":[
            {
                "valid":false,
                "email":"not a valid email"
            }
        ],
        "jkennedy@yandex-team.ru":[
            {
                "domain":"yandex-team.ru",
                "mono":"JK",
                "ava":{
                    "url_mobile":"https://yastatic.net/mail/socialavatars/socialavatars/v4/github.132.png",
                    "type":"icon",
                    "name":"github"
                },
                "display_name":"",
                "email":"jkennedy@yandex-team.ru",
                "color":"#56bf68",
                "local":"jkennedy",
                "valid":true
            }
        ],
        "not valid with unknown ava type":[
            {
                "valid":false,
                "email":"not valid with unknown ava type",
                "ava":{
                    "url_mobile":"https://yastatic.net/mail/socialavatars/socialavatars/v4/github.132.png",
                    "type":"unexpected_ava_type",
                    "name":"github"
                }
            }
        ]
    },
    "status":{
        "status":1
    }
}
"""
