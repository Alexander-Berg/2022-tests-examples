<?php
$loader = require __DIR__ . '/../vendor/autoload.php';
$loader->setPsr4('cabinet\\', __DIR__ . '/../src/');

require_once (__DIR__ . '/../vendor/autoru/lib5/functions.php');
require_once (__DIR__ .  '/../vendor/autoru/lib5/classes/Core.php');

$core = \Core::getInstance();
