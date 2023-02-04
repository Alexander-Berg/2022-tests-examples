#!/usr/bin/php
<?php
$_GET['test_debug'] = 0;
$_GET['debug_level'] = 0;
$_GET['clear_cache'] = 0;
$_GET['event_log'] = 0;
ini_set('memory_limit', -1);
set_time_limit(0);

$shell_script = __FILE__;
require_once (dirname(__FILE__) . '/../../lib5/common.php');

$action = new \moderation\classes\Console\cvTest(array_splice($argv, 1));
$action->execute();
