#!/usr/bin/php -q
<?php

use \api2\interfaces\iApiMethods;

$_GET['event_log'] = 0;
$_GET['test_debug'] = 0;
$shell_script = __FILE__;
require_once (dirname(__FILE__) . '/../../lib5/common.php');

/**
 * Arguments
 */
if ($key = array_search('--host', $argv)) {
    $host = $argv[$key + 1];
} else {
    echo 'Не задан host. (--host api.auto.ru)';
    die;
}

$scenario = null;
$scenario_key = null;
if ($scenario_key = array_search('--scenario', $argv)) {
    $scenario = $argv[$scenario_key + 1];
} else {
    echo 'Не задан scenario. (--scenario ConfirmPhone [param1, param2, ...])' . PHP_EOL;
}

$uuid = null;
if ($key = array_search('--uuid', $argv)) {
    $uuid = $argv[$key + 1];
}

$sid = null;
if ($key = array_search('--sid', $argv)) {
    $sid = $argv[$key + 1];
}


/**
 * Main
 */
$client = new \api2\classes\ApiClient($host);

if ($client->init($uuid, $sid)) {
    echo sprintf('Uuid: "%s", Sid: "%s".' . PHP_EOL, $client->getParam('uuid'), $client->getParam('sid'));
    if ($scenario) {
        if (function_exists('scenario' . $scenario)) {
            call_user_func('scenario' . $scenario, array_slice($argv, $scenario_key + 2));
        } else {
            echo 'Сценарий "' . $scenario . '" не найден.' . PHP_EOL;
        }
    }
} else {
    echo implode(PHP_EOL, $client->getErrors()) . PHP_EOL;
}


/**
 * Scenarios
 */

/**
 * Регистрация
 *
 * @param array $params параметры
 *
 * @return void
 */
function scenarioRegister($params)
{
    global $client;

    $login = array_shift($params);
    $password = array_shift($params);
    $response = $client->callRest(iApiMethods::METHOD_USERS_AUTH_REGISTER, ['login' => $login, 'password' => $password]);
    print_r($response);
}

/**
 * Добавление телефона
 *
 * @param array $params параметры
 *
 * @return void
 */
function scenarioAddPhoneNotAuth($params)
{
    global $client;

    $phone = array_shift($params);
    $response = $client->callRest(iApiMethods::METHOD_USERS_PROFILE_ADD_PHONE, ['phone' => $phone]);
    print_r($response);
}

/**
 * Подтверждение телефона
 *
 * @param array $params параметры
 *
 * @return void
 */
function scenarioConfirmPhone($params)
{
    global $client;

    $code = array_shift($params);
    $response = $client->callRest(iApiMethods::METHOD_USERS_PROFILE_CONFIRM_PHONE, ['code' => $code]);
    print_r($response);
}

/**
 * Залогиниваемся
 *
 * @param array $params параметры
 *
 * @return void
 */
function scenarioLogIn($params)
{
    global $client;

    $login = array_shift($params);
    $pass = array_shift($params);

    $response = $client->callRest(iApiMethods::METHOD_USERS_AUTH_LOGIN, ['login' => $login, 'pass' => $pass]);
    print_r($response);
}

/**
 * Разлогиниваемся
 *
 * @param array $params параметры
 *
 * @return void
 */
function scenarioLogOut($params)
{
    global $client;

    $response = $client->callRest(iApiMethods::METHOD_USERS_AUTH_LOGOUT);
    print_r($response);
}