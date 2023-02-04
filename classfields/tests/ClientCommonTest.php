<?php
require_once 'PHPUnit/Framework.php';
require_once dirname(__FILE__).'/../../lib5/common.php';
$_GET['debug'] = 0;
 
class ClientCommonTest extends UnitTestCase
{
    public function testClientAdd()
    {
        $client_model = Model::factory('clients@clients5');
        $this->assertTrue(!empty($client_model), 'Ошибка создания Clients Model');
        
        $client_params = array();
        $client_params['name'] = 'Planet Express';
        $id = $client_model->save(false, $client_params, 'new');        
        $this->assertFalse($id, 'Добавлен клиент, но при этом не указана его группа');
        
        $client_params['group_id'] = 'Some text';
        $id = $client_model->save(false, $client_params, 'new');        
        $this->assertFalse($id, 'Добавлен клиент с неправильно указанной группой');
        
        $client_params['group_id'] = 1; // dealer
        $id = $client_model->save(false, $client_params, 'new');        
        $this->assertTrue($id > 0, 'Не добавляется автодилер');
        $client_model->real_delete($id);
        
        $client_params['group_id'] = 2; // bank
        $id = $client_model->save(false, $client_params, 'new');        
        $this->assertTrue($id > 0, 'Не добавляется банк');
        $client_model->real_delete($id);
        
        $client_params['group_id'] = 3; // reklama
        $id = $client_model->save(false, $client_params, 'new');        
        $this->assertTrue($id > 0, 'Не добавляется агентство');
        $client_model->real_delete($id);

    }
    
    /**
    * Тестирует изменение данных клиента.
    * 
    * @depends testClientAdd
    */
    public function testClientChange()
    {
        // Тестовый клиент
        $client_model = Model::factory('clients@clients5');
        $client_params['name'] = 'Planet Express';
        $client_params['group_id'] = 1; // dealer
        $id = $client_model->save(false, $client_params, 'new');
        
        $saved_id = $client_model->save($id, $client_params, 'edit');
        $this->assertEquals($id, $saved_id, 'Сохранение возвращает неправильный id');
        
        $new_name = "Mom's Corp.";
        $client_params = array('name' => $new_name);
        $client_model->save($id, $client_params, 'edit');
        $row = $client_model->getClient($id);
        $this->assertEquals($row['name'], $new_name, 'Имя не сохраняется');
        
        $this->assertEquals('new', $row['status'], 'Не прописывается статус для нового клиента');
        
        $client_params = array('name' => $new_name, 'type' => 'juridical');
        $client_model->save($id, $client_params, 'edit');
        $row = $client_model->getClient($id);
        $this->assertEquals($row['type'], 'juridical', 'Не меняется тип клиента');
        
        $client_params = array('status' => 'active');
        $client_model->save($id, $client_params, 'edit');
        $row = $client_model->getClient($id);
        $this->assertEquals($row['status'], 'active', 'Не меняется статус');

        $row = $client_model->getClient($id);
        $old_status = $row['status'];
        $client_params = array('status' => 'some_really_wrong_status');
        $client_model->save($id, $client_params, 'edit');
        $row = $client_model->getClient($id);
        $this->assertEquals($row['status'], $old_status, 'Cтатус меняется на неправильный');

        $url = 'auto.ru';
        $client_params = array('url' => $url);
        $client_model->save($id, $client_params, 'edit');
        $row = $client_model->getClient($id);
        $this->assertEquals($row['url'], 'http://'.$url, 'Не добавляется http:// к урлу.');
        
        $url = 'http://i.ya.ru';
        $client_params = array('url' => $url);
        $client_model->save($id, $client_params, 'edit');
        $row = $client_model->getClient($id);
        $this->assertEquals($row['url'], $url, 'http:// урл не обрабатывается.');
        
        // Удаление тестового клиента
        $client_model->real_delete($id);
    }
    
    /**
    * 
    * @depends  testClientAdd
    **/
    public function testManyClients()
    {
        $client_model = Model::factory('clients@clients5');
        $client_params_1['name'] = 'Planet Express';
        $client_params_1['group_id'] = 1; // dealer
        $client_params_1['contact_name'] = 'Bender'; 
        $first  = $client_model->save(false, $client_params_1, 'new');
        
        $client_params_2['name'] = "Mom's corp.";
        $client_params_2['group_id'] = 2; // bank
        $client_params_2['description'] = ";'/*%!"; 
        $second = $client_model->save(false, $client_params_2, 'new');
        
        $row = $client_model->getClient(array($first, $second));
        
        $this->assertEquals($row[$first]['contact_name'], $client_params_1['contact_name']);
        $this->assertEquals($row[$second]['description'], $client_params_2['description']);
        
        $client_model->real_delete($first);
        $client_model->real_delete($second);
    }
}
?>