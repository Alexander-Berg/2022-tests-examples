/* jshint maxlen :  1000 */
module.exports = {
    // Отправил HEAD-запрос curl'ом
    'curl' : {
        method : 'HEAD',
        cookies : {}
    },
    // Отключил поддержку кук, но его пропалили
    'noob-cookieless' : {
        url : 'http://yandex.ru/?nocookiesupport=yes'
    },
    // Отключил поддержку кук, но Украина тоже не дремлет
    'noob-cookieless-ua' : {
        url : 'http://yandex.ua/?nocookiesupport=yes'
    },
    // Потёр все куки, зашёл на сервис
    'noob' : {
        cookies : {}
    },
    // 44 блой куки my -- 1 (показывать десктопную версию на мобильных)
    'noob-always-desktop' : {
        cookies : {
            my : 'YycCAAEoAQIsAQE2AQEA'
        }
    },
    // Морда Яндекса выставила куку mda=0
    'noob-no-mda' : {
        cookies : {
            mda : '0'
        }
    },
    // Сходил на морду Яндекса, поставил Киев и украинский, вернулся обратно
    'noob-becomes-saloed' : {
        cookies : {
            fuid01 : '51f7b42e718b013e.G9ETv9imbhTxX51mdr1r7ttsmEQ_-zvdejsgU5APV7090SVd_ftXinLEaogQ7x3TcjWihlkVP0HPX2uB_C9H4aNSJCsJ9hFMej8cmv2HjmeqXsnBXwcZlyzkpb0_Q9EZ',
            yandex_gid : '143',
            yandexuid : '6396814091384446160',
            my : 'YycCAAIA'
        }
    },
    // Пользователь со сломаной кукой my
    'noob-broken-cookie-my' : {
        cookies : {
            fuid01 : '51f7b42e718b013e.G9ETv9imbhTxX51mdr1r7ttsmEQ_-zvdejsgU5APV7090SVd_ftXinLEaogQ7x3TcjWihlkVP0HPX2uB_C9H4aNSJCsJ9hFMej8cmv2HjmeqXsnBXwcZlyzkpb0_Q9EZ',
            yandex_gid : '143',
            yandexuid : '6396814091384446160',
            my : 'eval(compile(\'for%20x%20in%20range(1)%3a\n%20import%20time\n%20time.sleep(20)\'%2c\'a\'%2c\'single\'))'
        }
    },
    // Авторизовался через Твиттер
    'noob-goes-social' : {
        cookies : {
            'yandexuid' : '8418475321384449125',
            'yandex_login' : 'uid-3zosp7fx',
            my : 'YwA=',
            L : 'XmNYE34BWQldCUMGQ2YHWV1JYFB0Y0wIJT9CODkiWUYOLicHeiRJMTMTHSV/CUFBG0QxJxk8fkBWAS8aDDwhDA==.1384449146.10080.282893.3dbbbd40b2457cc989ce100cb0656169',
            'Session_id' : '2 : 1384449146.0.5.234774848.8 : 1384449146989 : 1297613535 : 80.58 : 20709390.0.1.0.-1.101554.4344.3c69338a863b7a7f70a5fd27b2c1a337'
        },
        passportData : {
            'status' : {
                'value' : 'VALID',
                'id' : 0
            },
            'error' : 'OK',
            'age' : 13,
            'ttl' : '5',
            'auth' : {
                'password_verification_age' : -1,
                'have_password' : false,
                'secure' : false,
                'allow_plain_text' : true,
                'social' : {
                    'profile_id' : '20709390'
                }
            },
            'uid' : {
                'value' : '234774848',
                'lite' : false,
                'hosted' : false
            },
            'login' : '',
            'have_password' : false,
            'have_hint' : false,
            'karma' : {
                'value' : 0
            },
            'karma_status' : {
                'value' : 0
            },
            'regname' : 'uid-3zosp7fx',
            'display_name' : {
                'name' : 'Vasya Pupkin',
                'social' : {
                    'profile_id' : '20709390',
                    'provider' : 'tw',
                    'redirect_target' : '1384449159.82382.20709390.b266d9a1b6a58450c343b5c2eb17a4d3'
                }
            },
            'dbfields' : {
                'subscription.login.668' : '',
                'subscription.login.669' : '',
                'userinfo.lang.uid' : 'ru'
            }
        }
    },
    // Всё-таки зарегился на Яндексе!
    'noob-becomes-user' : {
        cookies : {
            uid : 'X2zfD1KFBIhUrQn6AyKGAg==',
            yandexuid : '8418475321384449125',
            yandex_login : 'fakeflack',
            my : 'YwA=',
            L : 'X2MPSX0AUwNdAUUHRGUHWFxHZ1B2YkEIIThBJj8rHkZEeD9FNCRQPyEQBDQtGAkNS0Q8Pg07eUMDQXcfGjU2TA==.1384449592.10080.224583.c2f68c812fdfdee32bd94fbab1f930da',
            'Session_id' : '2 : 1384449592.0.5.234776563.8 : 1384449592468 : 1297613535 : 7.0.1.1.0.101554.298.cfa8da3ca4af6332fc37135ee734c4cf'
        },
        passportData : {
            'status' : {
                'value' : 'VALID',
                'id' : 0
            },
            'error' : 'OK',
            'age' : 7,
            'ttl' : '5',
            'auth' : {
                'password_verification_age' : 7,
                'have_password' : true,
                'secure' : false,
                'allow_plain_text' : true
            },
            'uid' : {
                'value' : '234776563',
                'lite' : false,
                'hosted' : false
            },
            'login' : 'fakeflack',
            'have_password' : true,
            'have_hint' : true,
            'karma' : {
                'value' : 0
            },
            'karma_status' : {
                'value' : 0
            },
            'regname' : 'fakeflack',
            'display_name' : {
                'name' : 'fakeflack'
            },
            'dbfields' : {
                'subscription.login.668' : '',
                'subscription.login.669' : '',
                'userinfo.lang.uid' : 'ru'
            },
            'address-list' : [
                [
                    { 'address' : 'ejtoxic@yandex.ru', 'validated' : true, 'default' : false, 'prohibit-restore' : false, 'rpop' : false, 'unsafe' : false, 'native' : true, 'born-date' : '2014-05-14 18:40:42' },
                    { 'address' : 'ejtoxic@yandex.ua', 'validated' : true, 'default' : false, 'prohibit-restore' : false, 'rpop' : false, 'unsafe' : false, 'native' : true, 'born-date' : '2014-05-14 18:40:42' }
                ]
            ]
        }
    },
    /**
     * Как воспроизвести:
     * 1). получить уведомление из МК
     * 2). скопировать ссылку
     * 3). разлогиниться
     * 4). перейти по скопированной ссылке
     * 5). зайти на свою рабочую копию, где снимается дамп
     */
    'lite-auth' : {
        cookies : {
            // Я уж не буду палить все свои куки :)
            uid : '...',
            yandexuid : '...',
            'Session_id' : '2 : 1384449146.0.5.234774848.8 : 1384449146989 : 1297613535 : 80.58 : 20709390.0.1.0.-1.101554.4344.3c69338a863b7a7f70a5fd27b2c1a337',
            L : '...',
            yandex_login : 'alexrybakov',
            my : 'YycCAAEoAQIsAQA2AQEA'
        },
        passportData : {
            'status' : {
                'value' : 'VALID',
                'id' : 0
            },
            'error' : 'OK',
            'age' : 10,
            'ttl' : '0',
            'auth' : {
                'password_verification_age' : -1,
                'have_password' : true,
                'secure' : false,
                'allow_plain_text' : true
            },
            'uid' : {
                'value' : '20606950',
                // АХА!
                'lite' : true,
                'hosted' : false
            },
            'login' : 'alexrybakov',
            'have_password' : true,
            'have_hint' : true,
            'karma' : {
                'value' : 0
            },
            'karma_status' : {
                'value' : 0
            },
            'regname' : 'alexrybakov',
            'display_name' : {
                'name' : 'alexrybakov'
            },
            'dbfields' : {
                'subscription.login.668' : 'alexrybakov',
                'subscription.login.669' : 'flack',
                'userinfo.lang.uid' : 'ru'
            }
        }
    },
    // Пользователь ПДД
    'pdd-auth' : {
        cookies : {
            yandexuid : '134840321405022899',
            Session_id : '3:1411122532.5.0.1411122532000:cM900DMJ5OABAQAAuAYCKg:54.0|1130000011801615.0.2|116777.942586.Jq40hDEZsxD7y8Iq_Mw3YgWvzEY',
            L : 'Uy1DfQJIBmZyQGpda3puAXNsWW1vAWJzMVlaDjsuLQ07RntcY01lXllETloOCVQGFFAKM0k9AA==.1411122532.11199.384157.9a3b48d7ed59fa59f236d3d9e2dfcfec',
            yandex_login : 'robbitter-8239341377@mellior.ru',
            my : 'Yx8DAQECJgEBJwIAASgEgNXgAFOOqMWnmywBATYBAQA='
        },
        passportData : {
            'status' : {
                'value' : 'VALID',
                'id' : 0
            },
            'error' : 'OK',
            'age' : 75,
            'ttl' : '5',
            'auth' :
            {
                'password_verification_age' : 75,
                'have_password' : true,
                'secure' : false,
                'allow_plain_text' : true,
                'partner_pdd_token' : false
            },
            'uid' :
            {
                'value' : '1130000011801615',
                'lite' : false,
                'hosted' : true,
                'domid' : '1024',
                'domain' : 'mellior.ru',
                'mx' : '1',
                'domain_ena' : '1',
                'catch_all' : false
            },
            'login' : 'robbitter-8239341377@mellior.ru',
            'have_password' : true,
            'have_hint' : true,
            'karma' : {
                'value' : 0
            },
            'karma_status' : {
                'value' : 0
            },
            'regname' : 'robbitter-8239341377@mellior.ru',
            'display_name' : {
                'name' : 'robbitter-8239341377@mellior.ru'
            },
            'dbfields' :
            {
                'subscription.login.668' : '',
                'subscription.login.669' : '',
                'userinfo.lang.uid' : 'ru'
            }
        }
    },
    // Перестал страдать херней и залогинился своим аккаунтом
    'yandexoid' : {
        cookies : {
            // Я уж не буду палить все свои куки :)
            uid : '...',
            yandexuid : '...',
            'Session_id' : '2 : 1384449146.0.5.234774848.8 : 1384449146989 : 1297613535 : 80.58 : 20709390.0.1.0.-1.101554.4344.3c69338a863b7a7f70a5fd27b2c1a337',
            L : '...',
            yandex_login : 'alexrybakov',
            // Регион в tune - Санкт-Петербург
            yandex_gid : '2',
            // 44й блок - 0 (мобильная версия на мобильном)
            // 39й блок - 1 (язык интерфейса ru)
            my : 'YycCAAEoAQIsAQA2AQEA'
        },
        // обновление сессии не требуется
        passportData : {
            'status' : {
                'value' : 'VALID',
                'id' : 0
            },
            'error' : 'OK',
            'age' : 8945,
            'ttl' : '5',
            'auth' : {
                'password_verification_age' : 265225,
                'have_password' : true,
                'secure' : false,
                'allow_plain_text' : true
            },
            // Честная авторизация
            // не соц. или lite
            'uid' : {
                'value' : '20606950',
                'lite' : false,
                'hosted' : false
            },
            // Залогин
            'login' : 'alexrybakov',
            'have_password' : true,
            'have_hint' : true,
            'karma' : {
                'value' : 0
            },
            'karma_status' : {
                'value' : 0
            },
            'regname' : 'alexrybakov',
            'display_name' : {
                'name' : 'alexrybakov'
            },
            dbfields : {
                // Бета-тестер
                'subscription.login.668' : 'alexrybakov',
                // Сотрудник Яндекса
                'subscription.login.669' : 'flack',
                // Язык интерфейса на tune
                'userinfo.lang.uid' : 'ru'
            }
        }
    },
    'user-noauth-stale' : {
        cookies : {
            uid : '...',
            yandexuid : '...',
            'Session_id' : 'noauth:1433427120',
            L : '...',
            yandex_login : 'dmitry-sorin',
            my : 'YycCAAEoAQIsAQA2AQEA'
        },
        passportData : {
            status : {
                value : 'NOAUTH',
                id : 3
            },
            error : 'OK',
            age : 100934,
            session_fraud : 0
        }
    },
    'user-noauth-fresh' : {
        cookies : {
            uid : '...',
            yandexuid : '...',
            'Session_id' : 'noauth:1433427120',
            L : '...',
            yandex_login : 'dmitry-sorin',
            my : 'YycCAAEoAQIsAQA2AQEA'
        },
        passportData : {
            status : {
                value : 'NOAUTH',
                id : 3
            },
            error : 'OK',
            age : 10,
            session_fraud : 0
        }
    },
    // Авторизован, но авторизация протухла
    'user-need-reset' : {
        cookies : {
            uid : 'X2zfD1KFBIhUrQn6AyKGAg==',
            yandexuid : '8418475321384449125',
            yandex_login : 'fakeflack',
            my : 'YwA=',
            L : 'X2MPSX0AUwNdAUUHRGUHWFxHZ1B2YkEIIThBJj8rHkZEeD9FNCRQPyEQBDQtGAkNS0Q8Pg07eUMDQXcfGjU2TA==.1384449592.10080.224583.c2f68c812fdfdee32bd94fbab1f930da',
            'Session_id' : '2 : 1384449592.0.5.234776563.8 : 1384449592468 : 1297613535 : 7.0.1.1.0.101554.298.cfa8da3ca4af6332fc37135ee734c4cf'
        },
        passportData : {
            'status' : {
                'value' : 'NEED_RESET',
                'id' : 1
            },
            'error' : 'OK',
            'age' : 3629955,
            'ttl' : '5',
            'auth' : {
                'password_verification_age' : 3629955,
                'have_password' : true,
                'secure' : false,
                'allow_plain_text' : true
            },
            'uid' : {
                'value' : '234776563',
                'lite' : false,
                'hosted' : false
            },
            'login' : 'fakeflack',
            'have_password' : true,
            'have_hint' : true,
            'karma' : {
                'value' : 0
            },
            'karma_status' : {
                'value' : 0
            },
            'regname' : 'fakeflack',
            'display_name' : {
                'name' : 'fakeflack'
            },
            'dbfields' : {
                'subscription.login.668' : '',
                'subscription.login.669' : '',
                'userinfo.lang.uid' : 'ru'
            },
            'new-session' : {
                'value' : '2:1388080438.0.5.234776563.8:1384449592468:1297613535:7.0.1.1.0.103568.3547.9c2e7e116f47eaa7f5c05fb6f83a3b4b',
                'domain' : '.yandex.ru',
                'expires' : 2147483647,
                'http-only' : true
            }
        }
    },
    // Залогинился и пошел в/на Украину или на не-яндексовый домен
    'user-goes-from-yandexru' : {
        cookies : {
            'Session_id' : '2 : 1386354685.-114.5.224922411.8 : 1383673695815 : 1422501285 : 34.0.1.1.0.102610.4699.1e437b3692f0ab44526cb889896a5a0e'
        },
        passportData : {
            'status' : {
                'value' : 'INVALID',
                'id' : 5
            },
            'error' : 'hostname specified doesn\'t belong to cookie domain'
        }
    },
    // Блекбокс упал
    'blackbox-down' : {
        cookies : {
            'Session_id' : '2 : 1386354685.-114.5.224922411.8 : 1383673695815 : 1422501285 : 34.0.1.1.0.102610.4699.1e437b3692f0ab44526cb889896a5a0e'
        },
        passportData : null
    },
    'request-tvm' : {
        cookies : {
            'Session_id' : '2 : 1386354685.-114.5.224922411.8 : 1383673695815 : 1422501285 : 34.0.1.1.0.102610.4699.1e437b3692f0ab44526cb889896a5a0e'
        },
        passportData : {
            age : 156136,
            expires_in : 7619864,
            ttl : '5',
            session_fraud : 0,
            error : 'OK',
            status : { value : 'VALID', id : 0 },
            uid : { value : '11111012', lite : false, hosted : false },
            login : 'paul.zoewatel',
            have_password : true,
            have_hint : true,
            karma : { value : 0 },
            karma_status : { value : 6000 },
            auth : {
                password_verification_age : 13821810,
                have_password : true,
                secure : false,
                allow_plain_text : true,
                partner_pdd_token : false
            },
            connection_id : 's:1446212368654:bfzGJUiAfuUJIwAAuAYCKg:6',
            ticket : '2:11:1460034478:7D:EKaW6RM:UShQ0E-FwgSuyutN6qYwmh9E4ZjqISscvaf4zw68ogCqyLGyNPLEt2nkPIlT_fBzycoBu7JOoM62402m0Uar8c_8ixDXCccaEbEr1Af9Ak9Ch8KSkEaWkO-oPmZhcJxkbjjZGNRSRmcpNJNhasntT0PjE_Unb-JWaso5cZa65mw'
        }
    }
};
