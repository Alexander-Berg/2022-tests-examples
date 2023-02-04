#!/usr/bin/php -q
<?php
/**
 * AUTO.RU Framework
 *
 * @category   lib5
 * @version $Id: $
 */

/**
 * Пример файла исполняющегося из шела или крона
 */
/**
     * page_id
     * region_id
     * date_from
     * date_to
     * time
     * words
     * */
$shell_script = __FILE__;
require_once(dirname(__FILE__).'/../../lib5/common.php');
error_reporting(E_ALL);
ini_set('display_errors', 1);

$res = FileStorage::delete('/test/users/images/09/');

echo "res = \n";
var_dump($res);