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


$_GET['test_debug'] =1;

$shell_script = __FILE__;
require_once(dirname(__FILE__).'/../../lib5/common.php');


$clients = array(0 => array('id' => 1, 'name' => 'test'));

// Создаем временную таблицу для сортировки по имени клиента
/*
Db::q('CREATE TEMPORARY TABLE neova.tmp_clients (`id` int(11) NOT NULL AUTO_INCREMENT,`client_id` int(11) NOT NULL,
       `client_name` varchar(255) NOT NULL, PRIMARY KEY (`id`)) DEFAULT CHARSET=cp1251', false, 'neova');


Db::begin('neova');
foreach ($clients as $client) {
    Db::q('INSERT INTO neova.tmp_clients SET client_id=#id, client_name=#name', $client, 'neova');
}
Db::commit('neova');
*/

print_r(Model::getData('Advertisers:find@reclama5', array('vals' => Helpers_Locale::iconvArr( 'utf-8', 'windows-1251', $_GET))));

