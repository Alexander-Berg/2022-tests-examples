# coding: utf-8

CLIENT_NAME = u'ООО "Клиент"'
AGENCY_NAME = u'ООО "Агентство"'
FIXED_PH_PARAMS = {u'fname': u'Прасковья',
                   u'lname': u'Кактотамова',
                   u'mname': u'Дмитриевна',
                   u'account': u'40817810455000000131',
                   u'bik': u'044030653',
                   u'inn': u'375453119207',
                   u'pfr': u'578-139-802 38',
                   u'birthday': u'1979-11-19',
                   u'type': u'ph'}
FIXED_PH_AUTORU_PARAMS = {u'fname': u'Прасковья',
                          u'lname': u'Кактотамова',
                          u'mname': u'Дмитриевна',
                          u'phone': u'123123',
                          u'email': u'asd@asd.asd',
                          u'type': u'ph_autoru'}
FIXED_UR_PARAMS = {u'name': u'ООО "Плательщик"',
                   u'inn': u'7838298476',
                   u'account': u'40702810982208554168',
                   u'fname': u'',
                   u'lname': u'',
                   u'mname': u'',
                   u'type': u'ur'}
FIXED_UR_AUTORU_PARAMS = {u'name': u'ООО "Плательщик"',
                          u'inn': u'7838298476',
                          u'legaladdress': u'адрес',
                          u'phone': u'8 800 555 35 55',
                          u'email': u'asd@asd.asd',
                          u'type': u'ur_autoru'}
FIXED_UR_YTKZ_PARAMS = {u'name': u'EU nonres legal Payer',
                      u'inn': u'658500388',
                      u'account': u'96284558338824',
                      u'type': u'ur_ytkz'}
FIXED_US_YT_PARAMS = {u'name': u'USA nonres legal Payer',
                      u'account': u'LV90HABA0117329710888',
                      u'inn': u'9934728933042',
                      u'swift': u'SABRRUMM',
                      u'type': u'us_yt',
                      u'purchase_order': u'po12'}
FIXED_US_YTPH_PARAMS = {u'fname': u'Payer',
                        u'lname': u'USA',
                        u'type': u'us_ytph',
                        u'birthday': u'1979-11-19',
                      u'purchase_order': u'po12'}
FIXED_SW_UR_PARAMS = {u'name': u'Swiss legal Payer',
                      u'account': u'58605',
                      u'inn': u'535904',
                      u'type': u'sw_ur',
                      u'purchase_order': u'po12'}
FIXED_USU_PARAMS = {u'name': u'USA legal Payer',
                    u'type': u'usu',
                      u'purchase_order': u'po12'}
FIXED_USP_PARAMS = {u'lname': u'Smith',
                    u'fname': u'Alex',
                    u'type': u'usp',
                      u'purchase_order': u'po12'}
FIXED_BYU_PARAMS = {u'name': u'LTD, Belarus',
                    u'inn': u'272242202',
                    u'account': u'BY56ALFA56751218795427663075',
                    u'type': u'byu'}
FIXED_BYP_PARAMS = {u'fname': u'Анна',
                    u'lname': u'Тестерова',
                    u'mname': u'Тестеровна',
                    u'type': u'byp'}
FIXED_KZU_PARAMS = {u'name': u'KZ legal Payer',
                    u'kz-in': u'496227421585',
                    u'type': u'kzu'}
FIXED_KZP_PARAMS = {u'fname': u'Анна',
                    u'lname': u'Тестерова',
                    u'mname': u'Тестеровна',
                    u'kz-in': u'123456789012',
                    u'type': u'kzp'}
FIXED_SK_UR_PARAMS = {u'name': u'korean org',
                      u'inn': u'123-12-12345',
                      u'longname': u'korean longname org',
                      u'city': u'Seoul',
                      u'postaddress': u'post address',
                      u'postcode': u'123456',
                      u'phone': u'+82-02-312-3456',
                      u'type': u'sk_ur'}
FIXED_SW_PH_PARAMS = {u'lname': u'Swiss nonres',
                      u'fname': u'legal Payer',
                      u'phone': u'8 800 555 35 55',
                      u'email': u'asd@asd.asd',
                      u'type': u'sw_ph',
                      u'purchase_order': u'po12'}
FIXED_SW_YT_PARAMS = {u'name': u'Swiss nonres legal Payer',
                      u'account': u'LV90HABA0117329710888',
                      u'inn': u'9934728933042',
                      u'type': u'sw_yt',
                      u'purchase_order': u'po12'}
FIXED_SW_YTPH_PARAMS = {u'fname': u'Payer',
                        u'lname': u'Swiss',
                        u'type': u'sw_ytph',
                        u'birthday': u'1979-11-19',
                      u'purchase_order': u'po12'}
FIXED_BY_YTPH_PARAMS = {u'fname': u'Евстафий',
                        u'lname': u'Кактотамов',
                        u'type': u'by_ytph',
                        u'verified-docs': u'0',
                      u'purchase_order': u'po12'}
FIXED_IL_UR_PARAMS = {u'iban': u'IL470130620000060161516',
                      u'name': u'Israeli legal Payer',
                      u'inn': u'9934728933042',
                      u'type': u'il_ur'}
FIXED_EU_YT_PARAMS = {u'name': u'EU nonres legal Payer',
                      u'inn': u'658500388',
                      u'account': u'96284558338824',
                      u'type': u'eu_yt'}
FIXED_AM_UR_PARAMS = {u'name': u'AM legal Payer',
                      u'inn': u'658500388',
                      u'account': u'96284558338824',
                      u'type': u'am_ur'}
FIXED_AZ_UR_PARAMS = {u'name': u'AZ legal Payer',
                      u'iban': u'IL470130620000060161516',
                      u'ben-bank-code': '123456',
                      u'type': u'az_ur',
                      u'inn':'123456789'}
FIXED_YT_PARAMS = {u'name': u'YT legal Payer',
                   u'inn': u'584759476',
                   u'account': u'100548',
                   u'type': u'yt'}

FIXED_YTPH_PARAMS = {u'fname': u'Вейдер',
                     u'lname': u'Дарт',
                     u'phone': u'8 800 555 35 55',
                     u'email': u'dart@vaider.yandex',
                     u'city': u'Екатеринбург',
                     u'postaddress': u'ул. Пушкина, д. Колотушкина',
                     u'type': u'ytph',
                     u'birthday': u'1979-11-19'}

FIXED_FR_UR_PARAMS = {u'name': u'FR legal Payer',
                      u'tva-number': u'FRXX999999999',
                      u'inn': u'123123123',
                      u'type': u'fr_ur'}

FIXED_GB_UR_PARAMS = {u'name': u'GB legal Payer',
                      u'vat-number': u'UKXX999999999',
                      u'inn': u'123123123',
                      u'type': u'gb_ur'}

FIXED_ENDBUYER_UR_PARAMS = {u'name': u'ООО "Конечный покупатель"',
                            u'inn': u'4192690017',
                            u'postaddress': u"а/я Кладр 0"}

FIXED_YT_KZP_PARAMS = {u'fname': u'Имя',
                       u'lname': u'Фамилия',
                       u'phone': u'+7 727 123-45-78',
                       u'email': u'dart-vaider@yandex.kz',
                       u'city': u'Алматы',
                       u'postaddress': u'ул. Назарбаева, д. 1',
                       u'type': u'yt_kzp',
                       u'kz-in': u'012345678912',
                       u'verified-docs': u'0'}

FIXED_YT_KZU_PARAMS = {u'name': u'Название компании',
                       u'phone': u'+7 727 123 45 67',
                       u'email': u'e@ma.il',
                       u'postcode': u'888222',
                       u'city': u'Нурсултан',
                       u'postaddress': u'ул. Назарбаева, д. 1',
                       u'legaladdress': u'юр\nад\nрес',
                       u'kz-in': u'123456789012',
                       u'bik': u'KZBICBIC',
                       u'iik': u'KZ111222333444555666'}

FIXED_HK_YTPH_PARAMS = {
    u'fname': u"Нерезидентушка",
    u'lname': u"Гонконгов",
    u'birthday': u'1979-11-19',}

FIXED_HK_YTPH_PARAMS_WITH_YAMONEY = {
    u'fname': u"Нерезидентушка",
    u'lname': u"Гонконгов",
    u'birthday': u'1979-11-19',
    "bank_type": '3',
    'yamoney_wallet': '123123123',
    "iban": "",
    "swift": ""}

FIXED_AM_JP_PARAMS = {u'name': u'AM legal Payer',
                      u'inn': u'65850038',
                      u'account': u'96284558338824',
                      u'type': u'am_ur'}

FIXED_HK_YT_PARAMS = {u'name': u'HK Payer',
                      u'account': u'58605',
                      u'inn': u'535904',
                      u'type': u'hk_yt'}
FIXED_RO_UR_PARAMS = {u'name': u'RO legal Payer',
                      u'tva-number': u'ROXX999999999',
                      u'inn': u'123123123',
                      u'type': u'ro_ur',
                      u'country_id': u'10077',
                      "account": "16022026234101"}

FIXED_DE_UR_PARAMS = {u'name': u'DE legal Payer',
                      u'longname': u'ROXX999999999',
                      u'inn': u'123123123',
                      u'phone': u'54321',
                      u'email': u'aa@bb.cc',
                      u'country_id': u'10077',
                      u'city': u'Gorod',
                      u'legaladdress': u'123123123',
                      u'postaddress': u'123123123',
                      u'postcode': u'123123123',
                      u'type': u'de_ur',
                      u'account': u'96284558338824',
                      u'swift': u'SABRRUMM'}

FIXED_DE_UR_PARAMS = {u'name': u'DE Payer',
                      u'longname': u'ROXX999999999',
                      u'inn': u'123123123',
                      u'phone': u'54321',
                      u'email': u'aa@bb.cc',
                      u'country_id': u'10077',
                      u'city': u'Gorod',
                      u'legaladdress': u'123123123',
                      u'postaddress': u'123123123',
                      u'postcode': u'123123123',
                      u'type': u'de_ur',
                      u'account': u'96284558338824',
                      u'swift': u'SABRRUMM'}

FIXED_DE_PH_PARAMS = {u'fname': u'DE Payer',
                      u'lname': u'Lastname',
                      u'phone': u'54321',
                      u'fax': u'12345',
                      u'email': u'aa@bb.cc',
                      u'country_id': u'10077',
                      u'city': u'Gorod',
                      u'postaddress': u'aaa 123123123'}

FIXED_DE_YT_PARAMS = {u'name': u'Deutschland nonres legal Payer',
                      u'longname': u'ROXX999999999',
                      u'country_id': u'10077',
                      u'legaladdress': u'123123123',
                      u'postaddress': u'123123123',
                      u'account': u'LV90HABA0117329710888',
                      u'inn': u'9934728933042',
                      u'type': u'de_yt',
                      u'swift': u'SABRRUMM'}

FIXED_DE_YTPH_PARAMS = {u'fname': u'Payer',
                        u'lname': u'Deutschland!!!',
                        u'phone': u'18181818',
                        u'email': u'oo@aa.bb',
                        u'country_id': u'10077',
                        u'type': u'de_ytph',
                        u'birthday': u'1979-11-19'}