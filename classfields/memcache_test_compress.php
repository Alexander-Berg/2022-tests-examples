<?php
$shell_script = __FILE__;
require_once(dirname(__FILE__).'/../../lib5/common.php');
$_GET['test_debug'] = 0;


echo "Testing SET without compress...\n";
testMemcacheSet(100000, 10000, 0);
echo "Testing SET with compress...\n";
testMemcacheSet(100000, 10000, MEMCACHE_COMPRESSED);

echo "Testing GET without compress...\n";
testMemcacheGet(100000, 10000, 0);
echo "Testing GET with compress...\n";
testMemcacheGet(100000, 10000, MEMCACHE_COMPRESSED);

function testMemcacheSet($stringLength, $count, $flag){
    $str = genRandomString($stringLength);
    echo "Exectuting memcache:set $count times with flag = $flag ...\n";
    $startTime = microtime(true);
    $Cache = Cache::factory(Config::get('cacheEngineData'));
    for ($i=0; $i < $count; $i++) {
        $status = $Cache->set('test', $str, 60, $flag);
        $statusStr = ($status) ? 'ok' : 'fail' ;
        //echo "Set is ".$statusStr."\n";
    }
    $time = microtime(true) - $startTime;
    echo "$count sets at $time \n";
}

function testMemcacheGet($stringLength, $count, $flag){
    $str = genRandomString($stringLength);
    $Cache = Cache::factory(Config::get('cacheEngineData'));
    $Cache->delete('test');
    $Cache->set('test', $str, 60, $flag);
    echo "Exectuting memcache:get $count times with flag = $flag ...\n";
    $startTime = microtime(true);
    for ($i=0; $i < $count; $i++) {
        $status = $Cache->get('test');
        $statusStr = ($status) ? 'ok' : 'fail' ;
        //echo "Set is ".$statusStr."\n";
    }
    $time = microtime(true) - $startTime;
    echo "$count gets at $time \n";
}



function genRandomString($length = 1000) {
    $characters = '0123456789abcdefghijklmnopqrstuvwxyz';
    $string = "";    

    for ($p = 0; $p < $length; $p++) {
        $string .= $characters[mt_rand(0, strlen($characters)-1)];
    }

    return $string;
}