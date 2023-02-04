#!/usr/bin/php -q
<?php
$shell_script = __FILE__;
require_once(dirname(__FILE__).'/../../lib5/common.php');

$_GET = array();
$_GET['event_log'] = 0;

ignore_user_abort(true);
set_time_limit(0);
header ('Content-type: text/html; charset=utf-8');


$clientObj = getClient('all');

/* опции по умолчанию. Считаем кол-во совпавших триграм  */
//$clientObj->SetMatchMode ( SPH_MATCH_EXTENDED2 );
//$clientObj->SetRankingMode ( SPH_RANK_WORDCOUNT );
$clientObj->SetLimits(0, 10, 10);
$clientObj->SetArrayResult ( true );
//$clientObj->ResetFilters();
//$clientObj->SetFilter ( "client_id", array(2252));
if(count($argv) == 4) {
    $clientObj->SetFilterRange ( $argv[1], $argv[2], $argv[3]);
} else {
    $clientObj->SetFilter ( $argv[1], array($argv[2]));
}
//$clientObj->SetSortMode ( SPH_SORT_EXPR,  "@weight + category_weight + 2 - abs(len - $len)");

print_r($clientObj->Query('', "all"));



function getClient($client)
{
    $cl = new SphinxClient();
    $confs = Config::get('sphinxClients@sphinx');
    if (!isset($confs[$client])) {
        throw new SphinxSearchException("Client name '{$client}' is not defined", 1);
    }
    $conf = $confs[$client];

    $cl->setServer($conf['host'], $conf['port']);

    $cl->setConnectTimeout(30);
    $cl->setMaxQueryTime(30000);

    return $cl;
}
?>