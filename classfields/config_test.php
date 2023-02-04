<?php
/**
 * конфиг тестового окружения
 */
$config_arr = [

    'test' => [
        'passport_api' => [
            'api_host' => 'http://passport-api-server.vrts-slb.test.vertis.yandex.net',
            'api_url' => 'http://passport-api-server.vrts-slb.test.vertis.yandex.net/api/1.x/',
            'api_domain' => 'auto'
        ],

//        'public_api' => [
//            'api_url' => 'http://darl-01-sas.dev.vertis.yandex.net:2600',
//            'api_domain' => 'auto',
//            'auth_login' => 'Vertis',
//            'auth_token' => 'swagger-025b6a073d84564e709033f07438aa62'
//        ],

        'vin_decoder' => [
            'host' => 'vin-decoder-api-01-sas.test.vertis.yandex.net',
            'port' => 36314,
        ],
    ]
];
