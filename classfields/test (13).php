#!/usr/bin/php
<?php
/**
 * Меняет пароли у всех админов, отправляет уведомления.
 *
 * @package users5
 */
set_time_limit(0);
ini_set('memory_limit', 2548576000);

$_GET['event_log'] = 0;
$_GET['test_debug'] = 0;

$shell_script = __FILE__;
require_once(dirname(__FILE__) . '/../../../lib5/common.php');
define('PER_ATOM', 1000000);
/**
 * @var $mConvert \users8\models\MessagesConvert
 */
$mConvert = new \users8\models\MessagesConvert();
var_dump($mConvert->test([3337428]));