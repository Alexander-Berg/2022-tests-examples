#!/usr/bin/php
<?php
set_time_limit(0);
$_GET['test_debug'] = 1;
$_GET['event_log'] = 1;
$shell_script = __FILE__;
require_once(dirname(__FILE__).'/../../lib5/common.php');
Core::getInstance()->addPath(ROOT_PATH . 'lib5/libs/Api_Client/');

$config = Config::get('api_client_config@api');
$config['format'] = 'serialize';
$config['api_url'] .= $config['interface'] . '/';
$api = new Api_Client($config);

if (!empty($argv[1])) {
    list($project, $controller, $method) = explode('.', $argv[1]);
}
$params = array();
if (!empty($argv[2])) {
    $params_string = $argv[2];
    $params_raw = preg_split('/&+/', $params_string);
    if (!empty($params_raw)) {
        foreach ($params_raw as $param) {
            if (strpos($param, '=') !== false) {
                list($key, $value) = explode('=', $param);
                $params[$key] = $value;
            }
        }
    }
}
$result = $api->$project->$controller->$method($params);
print_r($result);
