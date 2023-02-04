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

$shell_script = __FILE__;
require_once(dirname(__FILE__).'/../../lib5/common.php');

//Model::getData('services:activate@clients5', array('service_id' => 10, 'client_id' => 1, 'object' => 'sale1', 'rows' => 977360, 'date' => array('expire_date' => '2009-07-05', 'prolongation' => 'auto')));

//Model::getData('services:activateOpened@clients5', array('service_id' => 10, 'client_id' => 1, 'object' => 'sale1', 'rows' => 977360, 'expire_date' => '2009-06-29'));


//Model::getData('services:deactivate@clients5', array('service_id' => 10, 'client_id' => 1, 'object' => 'sale1', 'rows' => 977360));

Model::getData('services:deactivateRow@clients5', array('object' => 'sale1', 'row_id' => 1153271));