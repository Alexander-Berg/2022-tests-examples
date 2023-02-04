<?php
/**
 * Created by PhpStorm.
 * User: molodtsov
 * Date: 22.03.16
 * Time: 15:04
 */

require         __DIR__ . '/../vendor/autoload.php';
require_once    __DIR__ . '/../functions.php';
require_once    __DIR__ . '/../classes/Core.php';

$_SERVER['AUTORU_CONFIG_PHP_LIB5'] = __DIR__ .'/../config/systemTemplates/dev_yandex.j2';
$_SERVER['AUTORU_CONFIG_PHP_LIB2'] = __DIR__ .'/../config/systemTemplates/dev_yandex.j2';

$core = \Core::getInstance();

$App = new \lib5\classes\AppLight();
$App->init();
$App->initCommon();
