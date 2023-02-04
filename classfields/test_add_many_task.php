#!/usr/bin/php -q
<?php
$_GET = array();
$_GET['test_debug']  = 0;
$_GET['debug_level'] = 0;

ini_set('memory_limit', '1G');
ignore_user_abort(true);
set_time_limit(0);

require_once(dirname(__FILE__) . '/../../lib5/common.php');

$hConsole = \lib5\classes\Helpers\Console::getInstance();
$hConsole->setParams($argc, $argv);

$params = array();
$hConsole->log('start');

//Подготовка данных для API

//Токен
$api_token = $hConsole->getArgument("token");
if (empty($api_token)) {
    throw new \Exception("requred api token");
}

//ID проекта. Можно глянуть в админке толоки https://sandbox.toloka.yandex-team.ru/requester
$project_id = 818;

$task_values = [
    [
        'name' => 'sale_id',
        'value' => [
            '88887',
            '57111'
        ]
    ],
    [
        'name' => 'img_1',
        'value' => [
            'http://is16.dev.autoru.yandex.net/all/images/b7/29/900x675/b729c34050d190ddbfd1492d8d15eda9.jpg',
            'http://is13.autoi.ru/all/images/94/d8/900x675/94d8c033377184f143b921be387bf26d.jpg'
        ]
    ],
    [
        'name' => 'img_2',
        'value' => [
            'http://is7.dev.autoru.yandex.net/all/images/46/db/900x675/46dbbea2224d3383fb1d23145d141af6.jpg',
            'http://is8.autoi.ru/all/images/66/4b/900x675/664b6d5c9164212bc42a29cefab7927d.jpg'
        ]
    ],
    [
        'name' => 'img_3',
        'value' => [
            'http://is17.dev.autoru.yandex.net/all/images/32/c9/900x675/32c9dbefad8704f69dc85722a5b1d832.jpg',
            'http://is8.autoi.ru/all/images/cb/a9/900x675/cba9ddb7a83637c6eafa558e4fff464e.jpg'
        ]
    ]
];

$toloka = new \yatoloka\classes\Toloka($api_token);

$hConsole->log('Token: ' . $toloka->getToken());
$hConsole->log('Host: ' . $toloka->getHost());

//Заряжаем данные для нового пула
$pool_request = new \yatoloka\classes\Toloka\Pool();

$pool_request->adultContent = false;
$pool_request->assignmentTimeoutSeconds = 300;
$pool_request->captchaLevel = \yatoloka\classes\Toloka\Pool::CAPTCHA_LEVEL_NONE;
$pool_request->expireDate = date('Y-m-d\TH:i', strtotime('+1 day'));
$pool_request->maxAssignmentsCount = 3;
$pool_request->name = 'api_test_' . date('Y-m-d\TH:i');
$pool_request->price = 0;
$pool_request->scope = \yatoloka\classes\Toloka\Pool::SCOPE_PUBLIC;

//Создаем новы пул
$pool_response = $toloka->createPool($project_id, $pool_request);

$pool_id = $pool_response->id;
$hConsole->log('Create Pool Name: ' . $pool_response->name . ' ID: ' . $pool_id);

//Заряжаем новую задачу
$task_request = new \yatoloka\classes\Toloka\Task();
$task_request->values = $task_values;

//Создаем задачу содержащая микро таски
$task_response = $toloka->createMicroTask($pool_id, $task_request);

$hConsole->log('Create Task ID: ' . $task_response['id']);
$hConsole->log('Micro tasks count: ' . $task_response['microtasksCount']);
$hConsole->log('Golden tasks count: ' . $task_response['goldenMicrotasksCount']);
$hConsole->log('Training tasks count: ' . $task_response['trainingMicrotasksCount']);

//Запускаем пул
$pool_response = $toloka->startPool($pool_id);

if ($pool_response instanceof \yatoloka\classes\Toloka\Pool) {
    $hConsole->log('Start Pool: ' . $pool_response->startDate);
} else {
    $hConsole->log('Start Pool: Error');

    print_r($toloka->getCurlResponse());
    print_r($toloka->getApiResponse());
}

$hConsole->log('finish');
