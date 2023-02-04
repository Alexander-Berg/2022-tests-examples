#!/usr/bin/php
<?php
set_time_limit(3600);
ini_set("memory_limit", "2G");
$_GET['test_debug'] = 1;
$_GET['debug_level'] = 2;
$_GET['debug_display'] = 1;

require realpath(__DIR__ . '/../../common.php');

if (empty($argv[1])) {
    echo 'Usage: ./test_task.php CLASS' . PHP_EOL . 'Example: ./test_task.php MessageAdd@users8' . PHP_EOL . PHP_EOL;
} else {
    $oTaskManager = new \lib5\classes\Tasks\Manager('Gearman');
    $oTaskManager->consume($argv[1]);
}
