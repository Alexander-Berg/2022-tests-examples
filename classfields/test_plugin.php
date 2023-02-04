#!/usr/bin/php
<?php
$_GET['test_debug'] = 0;
$_GET['debug_level'] = 3;
$_GET['clear_cache'] = 1;
$_GET['event_log'] = 0;
ini_set('memory_limit', -1);
set_time_limit(0);

$shell_script = __FILE__;
require_once (dirname(__FILE__) . '/../../lib5/common.php');

$project_alias = false;
if (($key = array_search('-p', $argv)) || ($key = array_search('--project', $argv)) !== false) {
    $project_alias = trim($argv[$key + 1]);
}
if (!$project_alias) {
    help('Необходимо указать проект');
}

$plugin = false;
if (($key = array_search('-l', $argv)) || ($key = array_search('--plugin', $argv)) !== false) {
    $plugin = trim($argv[$key + 1]);
}
if (!$plugin) {
    help('Необходимо указать имя плагина');
}

$object_id = false;
if (($key = array_search('-i', $argv)) || ($key = array_search('--object_id', $argv)) !== false) {
    $object_id = trim($argv[$key + 1]);
}
if (!$object_id) {
    help('Необходимо указать ID объекта');
}


echo 'Запуск плагина ' . $plugin . ' для объекта ' . $object_id . ' в проекте ' . $project_alias  . PHP_EOL;

$cForeignObjectsCollection = new \moderation\classes\ForeignObjectCollection();
$cForeignObjectsCollection->setProject($project_alias);
$cForeignObjectsCollection->setObjects([$object_id]);
$cForeignObjectsCollection->testPlugin($plugin);

function help($msg = '')
{
    echo 'Параметры запуска:' . PHP_EOL;
    echo ' -p или --project  - Псевдоним проекта (all7)' . PHP_EOL;
    echo ' -l или --plugin   - Имя класса плагина (Price)' . PHP_EOL;
    echo ' -i или --id       - ID объекта' . PHP_EOL . PHP_EOL;
    echo $msg . PHP_EOL;
    exit;
}