#!/usr/bin/php -q
<?php
/**
 * AUTO.RU Framework
 *
 * @category   lib5
 * @version $Id: $
 */

/**
 *
 */

$shell_script = __FILE__;
require_once(dirname(__FILE__).'/../../lib5/common.php');

$_GET['debug'] = 0;
$md5_key = md5($argv[1]);
/*
$memcache_conf = Config::get('memcache_servers');
print_r($memcache_conf);
*/

echo "Key before md5: '{$argv[1]}'\n";
echo "Key after md5: '{$md5_key}'\n";

var_dump(Cache::factory(Config::get('cacheEngineData'))->get($md5_key));
