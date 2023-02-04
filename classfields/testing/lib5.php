<?php
// ВНИМАНИЕ! Конфиги больше не используются и будут в скором времени удалены.
// Все переменные теперь заводятся только в конфиге проекта и в конфиге lib5
// Как в старые добрые времена
// Хосты к базам данных заводятся через /etc/autoru/resources.json через админов
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################
####################################################################################################################

// region Константы

$domain = 'test.autoru.yandex.net';  // главные тестовый домен
$mainHost = '.' . $domain;
$fqdnHost = $mainHost;              // хост для баз данных
$developer = '';  // имя девелопера

// region Базы

$memcache = [
    'memcache' => [
        'memcache1' => ['host' => 'memcache1' . $fqdnHost, 'port' => 11211, 'reconnect_limit'=> 2],
        'memcache2' => ['host' => 'memcache2' . $fqdnHost, 'port' => 11211, 'reconnect_limit'=> 2],
        'memcache3' => ['host' => 'memcache3' . $fqdnHost, 'port' => 11211, 'reconnect_limit'=> 2],
        'memcache7' => ['host' => 'memcache7' . $fqdnHost, 'port' => 11211, 'reconnect_limit'=> 2],
        'memcache4' => ['host' => 'memcache4' . $fqdnHost, 'port' => 11211, 'reconnect_limit'=> 2],
        'memcache5' => ['host' => 'memcache5' . $fqdnHost, 'port' => 11211, 'reconnect_limit'=> 2],
        'memcache6' => ['host' => 'memcache6' . $fqdnHost, 'port' => 11211, 'reconnect_limit'=> 2]
    ]
];

$membase = [
    'membase' => [
        ['host' => 'membase-auth' . $fqdnHost, 'port' => 11211, 'reconnect_limit'=> 3]
    ]
];
$membaseBoards = [
    'membase' => [
        ['host' => 'membase-auth' . $fqdnHost, 'port' => 11311, 'reconnect_limit'=> 3]
    ]
];

// endregion

// region Урлы

$imgHosts = [
    'is'        => 'is' . $mainHost,
    'is_prod'   => 'is.autoi.ru',
    'i'         => 'i' . $mainHost,
    'i_dev'     => 'i' .  $mainHost,
    'flexi'     => '%s' . $mainHost,
    'host'      => 'img.' . $developer . $mainHost,
    'static_host' => $domain
];

// endregion

// endregion

// region Настройки

$adminConfig = [

    // region Константы

    'env'                => 'staging',
    'DEVELOPER_NAME'     => $developer,
    'log_dir'            => '/var/log/php-fpm/autoru-backend-cabinet/app/',
    'general_domain'     => $domain,
    'cookie_domain'      => $mainHost,
    'root_host'          => $developer . $mainHost,
    'tmp_dir'            => '/tmp/',

    'office_ip'          => [
        '91.188.176.110/30',
        '78.107.106.128/28',
        '127.0.0.0/8',      // тут понятно
        '192.168.96.1/21',  // dev
        '10.20.0.0/24'      // VPN адреса
    ],

    // endregion

    // region Урлы

    'img'                => [
        'avatar_mds'   => [ // https://wiki.yandex-team.ru/mds/avatars/#xosty
            'read'        => ['host' => 'avatars.mdst.yandex.net', 'port' => 80],
            'read_secure' => ['host' => 'avatars.mdst.yandex.net', 'port' => 80],
            'read_int'      => ['host' => 'avatars-int.mdst.yandex.net', 'port' => 13000],
            'write'      => ['host' => 'avatars-int.mdst.yandex.net', 'port' => 13000],
        ],

        'storage_mds'   => [ // https://beta.wiki.yandex-team.ru/mds/#Интерфейс доступа к данным хранилища
            'write'             => ['host' => 'storage-int.mdst.yandex.net', 'port' => 1111],
            'read_int'          => ['host' => 'storage-int.mdst.yandex.net', 'port' => 80],
            'read_int_secure'   => ['host' => 'storage-int.mdst.yandex.net', 'port' => 443],
            'read'              => ['host' => 'storage.mdst.yandex.net',     'port' => 80],
            'read_secure'       => ['host' => 'storage.mdst.yandex.net',     'port' => 443],
        ],
        'isilon_url'  => 'http://' . $imgHosts['is'] . '/',
        'url'         => 'http://' . $imgHosts['i_dev'] . '/',                      // Повсмеместно используемая (View) ссылка на i.auto.ru
        'cookieless_url'=> 'http://i.test.autoru.yandex.net/',    // Ссылка на домен без кук. autoi.ru, например
//        'host'        => 'img.' . $developer . $mainHost,                           // Основная часть генерируемого доменного имени хоста картинок
	'host'        => 'img.test.autoru.yandex.net',
        'root'        => '/home/' . $developer . '/work/img/WWW',                   // Depricated
        'tmp_dir'     => '/home/' . $developer . '/work/img/WWW',                   // Временное хранилище для файлов, ещё не отправленных Rsync
        'i_url'       => '/i/',
        'i_local_url' => '/' . $imgHosts['i'] . '/' . $developer . '/',
        'domain'      => $imgHosts['is_prod'],
        'is_host'      => $imgHosts['is'],
        'static_host' => $imgHosts['static_host']
    ],

    'gnat_top_url'       => 'http://all7.' . $developer . '.auto:9999/gnat/get',
    'billing_url'        => 'http://billing5.' . $developer . $mainHost . '/',
    'idea_prod_domain'   => 'idea.auto.ru',
    'm_api_url'          => 'http://api.' . $developer . $mainHost . '/',
    'api_url'            => 'http://api.auto.ru/rest/api',
    'media5_calc_url'    => 'http://clients.' . $developer . $mainHost . '/',
    'media5_calc_host'   => 'clients.' . $developer . $mainHost,
    'media7_calc_url'    => 'http://clients.' . $developer . $mainHost . '/',
    'media7_calc_host'   => 'clients.' . $developer . $mainHost,
    'media7_temp_upload' => '/isilon/temp/media_upload',
    'my5_domain'         => 'http:/my.auto.ru/',

    'all7'               => [
        'prod_domain' => 'auto.ru',
        'database_name' => [
            'prices'       => 'all7_prices',
            'sales_search' => 'sales_search'
        ]
    ],

    'image3'             => [
        'Flexihash' => [
            'url_host' => $imgHosts['flexi']
        ],
        'Isilon2Avatar' => [
            'url_host' => $imgHosts['flexi']
        ]
    ],

    'files'              => [
        'storage' => [
            'Local' => [
                'dir_root' => '/servers/isilon-test/test',
                'url_host' => $imgHosts['flexi']
            ],
            'Http'  => [
                'dir_root' => '/servers/isilon-test/test',
                'url_host' => $imgHosts['flexi']
            ],
            'Ftp'   => [
                'dir_root' => '/servers/isilon-test/test',
                'url_host' => $imgHosts['flexi']
            ],
            'Avatar'=> [

            ]
        ]
    ],

    'file_storage'       => [
        'isilon_nfs'    => [
            'type'   => 'nfs',
            'params' => [
                'root'       => '/servers/isilon-test/test',
                'url_host'   => $imgHosts['is'],
                'url_prefix' => ''
            ]
        ],
        'isilon_webdav' => []
    ],

    'autorutv'           => [
        'xml_tvgrid_path' => '/servers/wowza-content/tvgrid.xml',
        'cookie_domain'   => $mainHost
    ],

    'captcha7'           => [
        'domain_s'          => 'https://captcha.auto.ru/',
        'domain'            => 'http://captcha.auto.ru/',
        'form_url'          => 'http://captcha.auto.ru/',
        'default_back_url' => 'http://auto.ru/'
    ],

    'catalog5'           => [
        'image3'      => ['dir_root' => '/isilon/'],
        'images_path' => '/home/bris/work/catalog_v3/WWW/images/'
    ],

    'stop'               => [
        'images_path' => '/home/bris/work/catalog_v3/WWW/images/'
    ],

    'clubs'              => [
        'images_path' => '/home/chez/work/clubs/WWW/images/'
    ],

    'jira_api_config'    => [
        'wsdl' => 'http://jira' . $mainHost . '/rpc/soap/jirasoapservice-v2?wsdl',
        'user' => 'mix',
        'pass' => 'mix'
    ],

    'office5'            => [
        'banner_path'       => '/home/stereo/work/i/reclama5/banners/',
        'banner_url'        => 'http://' . $imgHosts['i'] . '/stereo/reclama5/banners/',
        'remote_banner_url' => 'http://neova.auto.ru/pictures/',
        'banner_dir'        => '/stereo/reclama5/banners/',
        'neova_host_local'  => 'http://' . $imgHosts['i'] . '/',
        'db_all_logs'       => ['host' => 'localhost']
    ],

    'partscatalog'       => [
        'img_url'       => 'http://.' . $developer . $mainHost . '/partscatalog/',
        'cookie_domain' => $mainHost
    ],

    'users5'             => [
        'captcha_url' => 'http://users.' . $developer . $mainHost . '/images/verification/',
        'captcha_src' => '/home/' . $developer . '/work/users/WWW/images/verification/'
    ],

    'users7'             => [
        'captcha_url' => 'http://users.' . $developer . $mainHost . '/images/verification/',
        'captcha_src' => '/home/' . $developer . '/work/users/WWW/images/verification/'
    ],

    'users8'             => [
        'captcha_url' => 'http://is.auto.ru/users/verification/',
        'captcha_src' => '/home/' . $developer . '/work/users/WWW/images/verification/',
        'fonts_src'   => '/home/' . $developer . '/work/users8/resources/captcha_fonts/'
    ],

    // endregion

    // region Базы

    'dns' => '37.9.74.161',

    'db_model1'          => [
        'master'           => ['host' => 'db-master' . $fqdnHost, 'port' => '3511'],        // 192.168.0.89
        'master-dub'       => ['host' => 'db-master' . $fqdnHost, 'port' => '3511'],
        'master-all'       => ['host' => 'db-all' . $fqdnHost, 'port' => '3510'],
        'master-archive'   => ['host' => 'db-archive' . $fqdnHost, 'port' => '3516'],
        'slave'            => ['host' => 'db-slave' . $fqdnHost, 'port' => '3511'],       // 192.168.0.100

        'dboard-master'    => ['host' => 'db-board' . $fqdnHost, 'port' => '3512'],         // 192.168.0.3 - dboard-master.ib
        'dboard-slave'     => ['host' => 'db-board' . $fqdnHost, 'port' => '3512'],         // 192.168.0.3 - dboard-slave.ib

        'messages'         => ['host' => 'db-misc' . $fqdnHost, 'port' => '3512'],
        'messages-slave'   => ['host' => 'db-misc' . $fqdnHost, 'port' => '3512'],
        'twin13'           => ['host' => 'twin13' . $fqdnHost],
        'reclama'          => ['host' => 'db-neova' . $fqdnHost, 'port' => '3513'],        // DB reclama
        'neova'            => ['host' => 'db-neova' . $fqdnHost, 'port' => '3513'],        // DB reclama
        'neova-slave'      => ['host' => 'db-neova' . $fqdnHost, 'port' => '3513'],
        'neova-stat'       => ['host' => 'db-neova' . $fqdnHost, 'port' => '3513'],
        'neova-stat-slave' => ['host' => 'db-neova' . $fqdnHost, 'port' => '3513'],
        'partscatalog'     => ['host' => 'twin13' . $fqdnHost],
        'parts5'           => ['host' => 'db-parts' . $fqdnHost],
        'poi-cache'        => ['host' => 'db-master' . $fqdnHost, 'port' => '3511'],
        'auth'             => ['username' => 'auto', 'password' => 'KiX1euph'],
        'subscribe'        => ['host' => 'db-misc' . $fqdnHost, 'port' => '3512'],
        //поиск по карзам
        'dbsearch'         => ['host' => 'db-slave' . $fqdnHost], //dbsearch' . $fqdnHost
        'search_sphinx'    => ['host' => 'db-slave' . $fqdnHost],

        'pinba'            => ['host' => 'db-pinba' . $fqdnHost],
        'all-logs'         => ['host' => 'db-logs' . $fqdnHost], //twin8' . $fqdnHost
        'api-logs'         => ['host' => 'db-logs-api' . $fqdnHost],

        'security'         => ['host' => 'db-security' . $fqdnHost]
    ],

    'zones'              => [
        'settings'  => [
            'db' => [
                'server'   => 'master', // имя сервера
                'type'     => 'mysql', // тип адаптера Db2
                'username' => 'auto',
                'password' => 'KiX1euph'
            ]
        ],
        'resources' => [

            'zone_news7'      => [
                'db'    => [
                    'master'   => [ // имя сервера
                                    'host' => 'db-master' . $fqdnHost,
				    'port' => 3511,
                                    'type' => 'mysql' // тип адаптера Db2
                    ],
                    'logs'     => [ // имя сервера
                                    'host' => 'db-logs' . $fqdnHost,
				    'port' => 3515,
                                    'type' => 'mysql' // тип адаптера Db2
                    ],
                    'slave'    => [
                        'host'      => 'db-slave' . $fqdnHost,
                        'slave-for' => 'master', // имя сервера, для которого этот сервер будет slave'ом
                        'slave-lag' => 5, // отставание slave'a от мастера в секундах (обязателен для слейва)
			'port'      => '3511'
                    ],
                    'archive'  => [ // имя сервера
                                    'host' => 'db-archive' . $fqdnHost,
				    'port' => 3516,
                                    'type' => 'mysql' // тип адаптера Db2
                    ],
                    'sphinxQL' => [
                        'host' => 'sphinx' . $fqdnHost,
                        'port' => 21570,
                        'type' => 'sphinxQL'
                    ]
                ],
                'cache' => $membase + $memcache
            ],
            'zone1'           => [
                'db'    => [
                    'master'  => [ // имя сервера
                                   'host' => 'db-master' . $fqdnHost,
				   'port' => 3511,
                                   'type' => 'mysql' // тип адаптера Db2
                    ],
                    'logs'    => [ // имя сервера
                                   'host' => 'db-logs' . $fqdnHost,
				   'port' => 3515,
                                   'type' => 'mysql' // тип адаптера Db2
                    ],
                    'slave'   => [
                        'host'      => 'db-slave' . $fqdnHost,
                        'slave-for' => 'master', // имя сервера, для которого этот сервер будет slave'ом
                        'slave-lag' => 5, // отставание slave'a от мастера в секундах (обязателен для слейва)
			'port'      => '3511'
                    ],
                    'archive' => [ // имя сервера
                                   'host' => 'db-archive' . $fqdnHost,
				   'port' => 3516,
                                   'type' => 'mysql' // тип адаптера Db2
                    ],
                    'archive_master' => [
                        'host' => 'db-archive' . $fqdnHost, 'type' => 'mysql'
                    ]
                ],
                'cache' => $membase + $memcache
            ],
            'messages'        => [
                'db'    => [
                    'master' => [ // имя сервера
                                  'host' => 'db-misc' . $fqdnHost,
                                  'type' => 'mysql', // тип адаптера Db2
                                  'port' => '3512'
                    ],
                    'slave'  => [
                        'host'      => 'db-misc' . $fqdnHost,
                        'slave-for' => 'master', // имя сервера, для которого этот сервер будет slave'ом
                        'slave-lag' => 5, // отставание slave'a от мастера в секундах (обязателен для слейва)
                        'port' => '3512'
                    ]
                ],
                'cache' => $memcache
            ],
            'logs'            => [
                'db'    => [
                    'master'   => [ // имя сервера
                                    'host' => 'db-logs' . $fqdnHost,
				    'port' => 3515,
                                    'type' => 'mysql' // тип адаптера Db2
                    ],
                    'slave'    => [
                        'host'      => 'db-logs' . $fqdnHost,
			'port'      => 3515,
                        'slave-for' => 'master', // имя сервера, для которого этот сервер будет slave'ом
                        'slave-lag' => 5 // отставание slave'a от мастера в секундах (обязателен для слейва)
                    ],
                    'api-logs' => [
                        'host' => 'db-logs-api' . $fqdnHost,
			'port' => 3515,
                        'type' => 'mysql'
                    ]
                ],
                'cache' => $memcache
            ],
            'sessions'        => [
                'cache' => $membase + $memcache
            ],
            'board'           => [
                'db'    => [
                    'master' => [ // имя сервера
                                  'host' => 'db-board' . $fqdnHost,
                                  'type' => 'mysql' // тип адаптера Db2
                    ],
                    'slave'  => [
                        'host'      => 'db-board' . $fqdnHost,
                        'slave-for' => 'master', // имя сервера, для которого этот сервер будет slave'ом
                        'slave-lag' => 5 // отставание slave'a от мастера в секундах (обязателен для слейва)
                    ]
                ],
                'cache' => $membaseBoards + $memcache
            ],
            'users'           => [
                'cache' => $membaseBoards + $memcache
            ],
            'zone_officeteam' => [
                'db'    => [
                    'master'         => [ // имя сервера
                                          'host' => 'db-master' . $fqdnHost,
					  'port' => 3511,
                                          'type' => 'mysql' // тип адаптера Db2
                    ],
                    'slave'          => [
                        'host'      => 'db-slave' . $fqdnHost,
                        'slave-for' => 'master', // имя сервера, для которого этот сервер будет slave'ом
                        'slave-lag' => 5, // отставание slave'a от мастера в секундах (обязателен для слейва)
			'port'      => '3511'
                    ],
                    'sphinx-office7' => [
                        'host' => 'sphinx' . $fqdnHost,
                        'port' => 3310,
                        'type' => 'sphinxQL' // тип адаптера Db2
                    ]
                ],
                'cache' => $memcache
            ],
            'zone_moderation' => [
                'db'     => [
                    'master' => [
                        'host' => 'db-moderation' . $fqdnHost,
			'port' => 3512,
                        'type' => 'mysql'
                    ],
                    'slave'  => [
                        'host'      => 'db-moderation' . $fqdnHost,
                        'slave-for' => 'master',
                        'slave-lag' => 5,
			'port' => 3512
                    ],
                    'sphinx' => [
                        'host' => 'sphinx' . $fqdnHost,
                        'port' => 3311,
                        'type' => 'sphinxQL' // тип адаптера Db2
                    ]
                ],
                'cache'  => $memcache,
                'sphinx' => [
                    'moderation' => [
                        'host'     =>' "sphinx"' . $fqdnHost,
                        'port'     => 21558,
                        'encoding' => 'utf-8'
                    ]
                ]
            ],

            'zone_mediateam'  => [
                'db'    => [
                    'master' => [
                        'host' => 'db-master' . $fqdnHost,
			'port' => 3511,
                        'type' => 'mysql'
                    ],
                    'slave'  => [
                        'host'      => 'db-slave' . $fqdnHost,
                        'slave-for' => 'master',
                        'slave-lag' => 5,
			'port'      => '3511'
                    ]
                ],
                'cache' => $membase + $memcache
            ],

            'zone_partsteam'  => [
                'db'    => [
                    'partscatalog' => [
                        'host' => 'twin13' . $fqdnHost,
                        'type' => 'mysql'
                    ],
                    'parts5'       => [
                        'host' => 'db-parts' . $fqdnHost,
                        'type' => 'mysql'
                    ],
                    'master'       => [
                        'host' => 'db-master' . $fqdnHost,
			'port' => 3511,
                        'type' => 'mysql'
                    ],
                    'slave'        => [
                        'host'      => 'db-slave' . $fqdnHost,
                        'slave-for' => 'master',
                        'slave-lag' => 5,
			'port'      => '3511'
                    ]
                ],
                'cache' => $membase + $memcache
            ],

            'zone_all'        => [
                'db'                => [
                    'master'         => [ // имя сервера
                        'host' => 'db-all' . $fqdnHost,
			'port' => 3510,
                        'type' => 'mysql' // тип адаптера Db2
                    ],
                    'slave'          => [
                        'host'      => 'db-all' . $fqdnHost,
			'port'      => 3510,
                        'slave-for' => 'master', // имя сервера, для которого этот сервер будет slave'ом
                        'slave-lag' => 5 // отставание slave'a от мастера в секундах (обязателен для слейва)
                    ],
                    'sphinx'         => [
                        'host' => 'sphinx' . $fqdnHost,
                        'port' => 3307,
                        'type' => 'sphinxQL' // тип адаптера Db2
                    ],
                    'sphinx-catalog' => [
                        'host' => 'sphinx' . $fqdnHost,
                        'port' => 3333,
                        'type' => 'sphinxQL' // тип адаптера Db2
                    ],
                    'sphinx-statistics' => [
                        'host' => 'sphinx-all' . $fqdnHost,
                        'port' => 3309,
                        'type' => 'sphinxQL' // тип адаптера Db2
                    ]
                ],
                'sphinx-statistics' => [
                    'host' => 'sphinx' . $fqdnHost,
                    'port' => 3309,
                    'type' => 'sphinxQL' // тип адаптера Db2
                ],
                'cache'             => $memcache,
                'sphinx'            => [
                    'all7' => [
                        'host' => 'sphinx' . $fqdnHost,
                        'port' => 21999
                    ]
                ]
            ],
            'zone_users'      => [
                'db'    => [
                    'master' => [
                        'host' => 'db-master' . $fqdnHost,
			'port' => 3511,
                        'type' => 'mysql'
                    ],
                    'slave'  => [
                        'host'      => 'db-slave' . $fqdnHost,
                        'slave-for' => 'master',
                        'slave-lag' => 5,
			'port'      => '3511'
                    ]
                ],
                'cache' => $memcache
            ],

            'zone_security'   => [
                'db' => [
                    'master' => [ // имя сервера
                                  'host' => 'db-security' . $fqdnHost,
                                  'type' => 'mysql' // тип адаптера Db2
                    ],
                    'slave'  => [
                        'host'      => 'db-security' . $fqdnHost,
                        'slave-for' => 'master',
                        'slave-lag' => 5
                    ]
                ]
            ]
        ]
    ],

    'task_managers'      => [
        'Gearman' => [
            'servers' => [
                ['host' => 'gearman' . $fqdnHost, 'port' => 4730]
            ]
        ]
    ],

    'reclama'            => [
        'farm'      => 'neova.auto.ru', // Имя фермы рекламных серверов
        'request'   => 'http://neova-request.test.autoru.yandex.net:80/',
        'http'      => 'http://neova.auto.ru:80/',
        'click'     => 'http://neova.auto.ru:80/',
        'http_demo' => 'http://neova.auto.ru:80/pictures/',
        'show'      => [
            'url'  => 'http://avtonet.ru/',
            'salt' => 'S3nTfJFNWRPTb5IWnWpAQIXRuhwgOv0q1PUekBymzqKQxwTRdJBKYymUYpd7OUof'
        ]
    ],



    'antifraud'          => [
        'hs_read'  => ['host' => 'db-security' . $fqdnHost, 'port' => '3514'],
        'hs_write' => ['host' => 'db-security' . $fqdnHost, 'port' => '3514']
    ],

    'api2'               => [
        'uuid_read'  => ['host' => 'db-security' . $fqdnHost, 'port' => '3514'],
        'uuid_write' => ['host' => 'db-security' . $fqdnHost, 'port' => '3514']
    ],

    'office7'            => [
        'prod_domain' => 'office7.auto.ru',
        'db_all_logs' => ['host' => 'localhost']
    ],

    'parts5'             => [
        'hs_read'  => ['host' => 'mediateam' . $fqdnHost, 'port' => '9998'],
        'hs_write' => ['host' => 'mediateam' . $fqdnHost, 'port' => '9999']
    ],

    'solr'               => [
        'projects_home' => '/data/solr/projects', // где хранятся индексы или конфиг
        'home'          => '/data/solr',
        'path'          => '/data/solr/solr',  // Исполняемые файлы сервера
        'lib'           => '/data/solr/solr-lib',
        'master_port'   => '8983',
        /////////
        'run_params'    => '-XX:+UseParallelGC -Xms4096M -Xmx8192M -Dhttp.maxConnections=5000 -Dhttp.keepAlive=true',
        'master_host'   => 'solr' . $fqdnHost,
        'search_hosts'  => [
            'http://solr' . $fqdnHost . ':8983/solr/'  => [
                'mode'   => 'master',
                'params' => '-XX:+UseParallelGC -Xms2048M -Xmx4096M -Dhttp.maxConnections=5000 -Dhttp.keepAlive=true'
            ]
        ],

        'balancer'      => 'http://solr' . $fqdnHost . ':8983/solr/',

        //Активные ядра солара в формате  название - путь к директории
        'active_cores'  => [
            'parts'       => 'parts5/solr/parts',
            'parts_sale'  => 'parts5/solr/sale',
            'parts_apart' => 'parts5/solr/apart',
            'parts_cross' => 'parts5/solr/cross'
        ],
        'db'            => [
            'host' => 'mediateam' . $fqdnHost
        ]
    ],

    'sphinx'             => [
        'clients' => [
            // конференции
            'boards'         => [
                'host'     => '127.0.0.1',
                'port'     => 9312,
                // кодировка в которой проиндексированы документы
                'encoding' => 'utf-8'
            ],
            'suggest'      => [
                'host'     => '127.0.0.1',
                'port'     => 21333,
                // кодировка в которой проиндексированы документы
                'encoding' => 'utf-8'
            ],
            'all'          => [
                'sale1' => [
                    'host'     => 'sphinx' . $fqdnHost,
                    'port'     => 21666,
                    'encoding' => 'utf-8',
                    'indexes'  => 'sale1'
                ],
                'sale2' => [
                    'host'     => 'sphinx' . $fqdnHost,
                    'port'     => 21666,
                    'encoding' => 'utf-8',
                    'indexes'  => 'sale2,sale3,sale4,sale5'
                ]
            ],
            'search_suggest' => [
                'host'     => '127.0.0.1',
                'port'     => 21333,
                'encoding' => 'utf-8'
            ],
            'tasks_search' => [
                'host'     => '127.0.0.1',
                'port'     => 21334,
                'encoding' => 'utf-8'
            ],
            'users_search' => [
                'host'     => '127.0.0.1',
                'port'     => 21335,
                'encoding' => 'utf-8'
            ],
            'moderation'   => [
                'host'     => 'sphinx' . $fqdnHost,
                'port'     => 21558,
                'data_dir' => '/data/sphinx/moderation/'
            ],
            'moderation_rt'  => [
                'host'     => 'sphinx.dev',
                'port'     => 3311,
                'data_dir' => ROOT_PATH . 'sphinx/data/moderation_rt/'
            ],
            'office7'      => [
                'host'     => '127.0.0.1',
                'port'     => 21338,
                'encoding' => 'utf-8'
            ],
            'allnews'        => [
                'host'     => '127.0.0.1',
                'port'     => 21559,
                'encoding' => 'utf-8',
                'indexes'  => 'news,testdrives,articles'
            ],
            'news7'          => [
                'host'     => '127.0.0.1',
                'port'     => 21561,
                'encoding' => 'utf-8',
                'indexes'  => 'news7'
            ],
            'koleso5'        => [ // Добавлено только в DEV, а в remote - нет
                'host'     => '127.0.0.1',
                'port'     => 21562,
                'encoding' => 'utf-8',
                'indexes'  => 'sales'
            ]
        ]
    ],

    'vin'                => [
        'db' => [
            'master'       => ['host' => 'localhost'],
            'slave'        => ['host' => 'localhost'],
            'dboard'       => ['host' => 'localhost'],
            'star25'       => ['host' => 'localhost'],
            'reclama'      => ['host' => 'localhost'],
            'neova'        => ['host' => 'localhost'],
            'auth'         => ['username' => 'auto', 'password' => 'KiX1euph'],
            'twin13'       => ['host' => '192.168.100.78', 'username' => 'auto', 'password' => 'KiX1euph'],
            'partscatalog' => ['host' => '192.168.100.78', 'username' => 'root', 'password' => '']
        ]
    ],

    'yandex_sms_gate' => [
        'url' =>'https://sms.passport.yandex.ru:443/sendsms',
        'check_url' =>'https://sms.passport.yandex.ru:443/checksms',
        'route' => 'autoru',
        'sender' => 'autoru',
    ]

    // endregion
];

// endregion
