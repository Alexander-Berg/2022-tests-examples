<?php
if (!defined('ROOT_PATH')) {
    define('ROOT_PATH', realpath(dirname(__FILE__) . '/../') . '/' . 'vendor/autoru/');
}
$loader = require __DIR__ . '/../vendor/autoload.php';
$loader->setPsr4('octopus\\', __DIR__ . '/../src/');
$loader->setPsr4('tests\\', __DIR__ . '/../tests/');

require_once (__DIR__ . '/../vendor/fzaninotto/faker/src/autoload.php');

require_once (__DIR__ . '/../src/functions.php');
require_once (__DIR__ . '/../vendor/autoru/lib5/functions.php');
require_once (__DIR__ .  '/../vendor/autoru/lib5/classes/Core.php');
$core = \Core::getInstance();
$core->addPath(__DIR__ . '/../vendor/autoru/lib5/interfaces/');
$core->addPath(__DIR__ . '/../vendor/autoru/lib5/classes/');
date_default_timezone_set('Europe/Moscow');
define('SERVER_TYPE',   'dev');
define('RUN_TYPE',      'shell');
define('RSYNC_PATH',    '/usr/bin/rsync');
ini_set('memory_limit', '5120M');
Storage::set('SERVER_TYPE',     'dev');
Storage::set('RUN_TYPE',        'shell');
Storage::set('CURRENT_PROJECT', 'lib5');
if (empty($_SERVER['HOSTNAME'])) {
    $host = '';
    if (!empty($_SERVER['SERVER_ADDR'])) {
        $host = $_SERVER['SERVER_ADDR'];
    } else if (!empty($_SERVER['SERVER_NAME'])) {
        $host = $_SERVER['SERVER_NAME'];
    } else if (!empty($_SERVER['HTTP_HOST'])) {
        $host = $_SERVER['HTTP_HOST'];
    }
    $_SERVER['HOSTNAME'] = str_replace('.', '', $host);
}
$configs_type = \lib5\classes\App::isStaging()
    ? 'testing'
    : 'development';
$_SERVER['AUTORU_CONFIG_PHP_LIB5'] = __DIR__ . '/../configs/' . $configs_type . '/lib5.php';
/* Подгружаем конфиг из либов */
Config::loadLibConfig();
Config::set('event_log', false);
Config::includeProjectConfig('test@lib5', 'lib5');
Storage::set('Db', new Db);
$app = new \lib5\classes\App();
$app->init();
