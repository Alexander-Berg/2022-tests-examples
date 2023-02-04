#!/usr/bin/php -q
<?php
$shell_script = __FILE__;
require_once (dirname(__FILE__) . '/../../lib5/common.php');

$_GET = array();
//$_GET['event_log'] = 1;

ignore_user_abort(true);
set_time_limit(0);
header ('Content-type: text/html; charset=utf-8');


$cl = new SphinxClient();

$cl->setServer('localhost', '21999');

$cl->setConnectTimeout(30);
$cl->setMaxQueryTime(30000);

//$cl->SetMatchMode ( \lib5\libs\Sphinx\Client::SPH_MATCH_EXTENDED2 );
//$clientObj->SetRankingMode ( \lib5\libs\Sphinx\Client::SPH_RANK_WORDCOUNT );
$cl->SetLimits(0, 1, 1);
$cl->SetArrayResult ( true );
//$clientObj->ResetFilters();
$cl->SetSelect ( "*");
$cl->SetFilter ( "@id", array(1));

//$clientObj->SetSortMode ( \lib5\libs\Sphinx\Client::SPH_SORT_EXPR,  "@weight + category_weight + 2 - abs(len - $len)");

print_r($cl->Query("", "all, all_delta")['matches'][0]);


print_r($cl->GetLastError());


//$test = array('category_id' => 11, 'section_id' => 11);
//var_dump($cl->updateAttributes('all', array_keys($test), array(19 => array_values($test))));
//
//
//print_r($cl->GetLastError());

//print_r($cl->GetLastWarning());