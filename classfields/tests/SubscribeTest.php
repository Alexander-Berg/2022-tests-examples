<?php

//define('PROJECT', 'clients5');

require_once 'PHPUnit/Framework.php';
require_once dirname(__FILE__).'/../../lib5/common.php';
$_GET['test_debug'] = 1;

class SubscribeTest extends UnitTestCase
{
    protected $client_arr = false;

    protected $client_model = false;
    protected $user_model = false;
    
    protected $tariff_expire_date = '2010-05-05 12:00';
    protected $period_start = '2010-05-05';
    protected $period_end   = '2010-05-06';    
    
    private $do_insert = false;
    private $do_delete = false;
    
    public function __construct()
    {
        $this->client_model = Model::factory('clients@clients5');
        $this->user_model = Model::factory('client_users@clients5');
    }
    
	public function testSubscribe()
	{	
		$subscribe_model = Model::factory('subscribe@clients5');
   	
    	// Юр лица
		$recepients = $subscribe_model->getClientRecepients($this->period_start, $this->period_end, 'juridical', 'auto', false);		
		
		$result = ($recepients[0]['to'] == 'elovi3@mail.ru') && ($recepients[0]['template_params']['origin'] == 'sbs0002');
		$this->assertTrue($result, 'Неверный список клиентов в рассылке (вариант: юр лицо, авт продление, недостаточно средств)');
		
		$recepients = $subscribe_model->getClientRecepients($this->period_start, $this->period_end, 'juridical', 'manual', false);		
		$result = true;
		foreach ($recepients as $v) {
			if (($v['to'] != 'elovi1@mail.ru') && ($v['to'] != 'elovi2@mail.ru') || ($v['template_params']['origin'] != 'sbs0001')) {
				$result = false;
			}
		}	
		$this->assertTrue($result, 'Неверный список клиентов в рассылке (вариант: юр лицо, ручн продление)');	
				
		$recepients = $subscribe_model->getClientRecepients($this->period_start, $this->period_end, 'juridical', 'auto', true);
		$result = true;
		foreach ($recepients as $v) {
			if (($v['to'] != 'elovi1@mail.ru') && ($v['to'] != 'elovi2@mail.ru') || ($v['template_params']['origin'] != 'sbs0001')) {
				$result = false;
			}
		}
		$this->assertTrue($result, 'Неверный список клиентов в рассылке (вариант: юр лицо, авт продление, достаточно средств)');
		
		
		// Физ лица
		$recepients = $subscribe_model->getClientRecepients($this->period_start, $this->period_end, 'private', 'auto', false);
		$result = ($recepients[0]['to'] == 'elovi5@mail.ru') && ($recepients[0]['template_params']['origin'] == 'sbs0004');
		$this->assertTrue($result, 'Неверный список клиентов в рассылке (вариант: физ лицо, авт продление, недостаточно средств)');
		
		$recepients = $subscribe_model->getClientRecepients($this->period_start, $this->period_end, 'private', 'manual', false);
		$result = ($recepients[0]['to'] == 'elovi4@mail.ru') && ($recepients[0]['template_params']['origin'] == 'sbs0003');
		$this->assertTrue($result, 'Неверный список клиентов в рассылке (вариант: физ лицо, ручн продление)');
		
		$recepients = $subscribe_model->getClientRecepients($this->period_start, $this->period_end, 'private', 'auto', true);			
		$result = ($recepients[0]['to'] == 'elovi4@mail.ru') && ($recepients[0]['template_params']['origin'] == 'sbs0003');
		$this->assertTrue($result, 'Неверный список клиентов в рассылке (вариант: физ лицо, ручн продление)');
	}
	
    public function setUp()
    {
    	parent::setUp();
    	
    	$tariff_row = $this->getTariff();
    	
    	if ($this->do_insert) {    		
        	$client_params = array();
        	$client_params['group_id'] = 1;
        	
        	$client_params['name'] = 'Клиент 1 (юр лицо, денег достаточно)';
        	$client_params['origin'] = 'sbs0001';
        	$client_params['type'] = 'juridical';
        	
        	$client_id = $this->createClient($client_params);
        	$account_id = $this->createBillingAccount($client_id, 500000);        	
        	$this->addTariff($client_id, $tariff_row['id'], 'auto', $this->tariff_expire_date);        
        	$this->addTariff($client_id, $tariff_row['id'], 'manual', $this->tariff_expire_date);   
        	$this->createUserAccess($client_id, 'elovi1@mail.ru');   
        	$this->createUserAccess($client_id, 'elovi2@mail.ru');
        	
        	$this->client_arr['jb'] = array('client_id' => $client_id, 'account_id' => $account_id); 
        	
        	$client_params['name'] = 'Клиент 2 (юр лицо, денег недостаточно)';
        	$client_params['origin'] = 'sbs0002';
        	$client_params['type'] = 'juridical';
			
        	$client_id = $this->createClient($client_params);
        	$this->addTariff($client_id, $tariff_row['id'], 'auto', $this->tariff_expire_date);  
        	$this->createUserAccess($client_id, 'elovi3@mail.ru');
			
			$this->client_arr['j'] = array('client_id' => $client_id, 'account_id' => 0); 
			
        	$client_params['name'] = 'Клиент 3 (физ лицо, денег достаточно)';
        	$client_params['origin'] = 'sbs0003';
        	$client_params['type'] = 'private';
			
        	$client_id = $this->createClient($client_params);
        	$account_id = $this->createBillingAccount($client_id, 500000);   
        	$this->addTariff($client_id, $tariff_row['id'], 'auto', $this->tariff_expire_date);
        	$this->addTariff($client_id, $tariff_row['id'], 'manual', $this->tariff_expire_date);
        	$this->createUserAccess($client_id, 'elovi4@mail.ru');     
        	$this->createPassportData($client_id, 'Петров', 'Петр', 'Петрович');
        	
        	$this->client_arr['pb'] = array('client_id' => $client_id, 'account_id' => $account_id); 
        	
        	$client_params['name'] = 'Клиент 4 (физ лицо, денег недостаточно)';
        	$client_params['origin'] = 'sbs0004';
        	$client_params['type'] = 'private';
			
        	$client_id = $this->createClient($client_params);  
        	$this->addTariff($client_id, $tariff_row['id'], 'auto', $this->tariff_expire_date);      	
        	$this->createUserAccess($client_id, 'elovi5@mail.ru');    
        	$this->createPassportData($client_id, 'Васечкин', 'Василий', 'Васильевич');
        	
        	$this->client_arr['p'] = array('client_id' => $client_id, 'account_id' => 0); 
        } else {
        	$q = Db::q("SELECT clients.id client_id, clients.type, accounts.id account_id FROM clients5.clients LEFT JOIN billing.accounts ON accounts.client_id=clients.id WHERE clients.origin LIKE 'sbs%'");
        	while ($row = Db::fetchAssoc($q)) {
        		if ($row['type'] == 'juridical') {
        			$key = !empty($row['account_id']) ? 'jb' : 'j';
        		} else {
        			$key = !empty($row['account_id']) ? 'pb' : 'p';
        		}
        		$this->client_arr[$key] = array(
        			'client_id'  => $row['client_id'],
        			'account_id' => $row['account_id'],
				);
        	}
        }       
    }
    
    public function createClient($params)
    {
    	Db::q("INSERT INTO clients5.clients SET group_id=#group_id, name=#name, origin=#origin, type=#type", $params);
    	return Db::$insert_id;
    }
    
    public function createUserAccess($client_id, $email)
    {
    	$user_row = Db::getRow("SELECT user.id FROM users.user WHERE email=#email", array('email' => $email));
    	if (empty($user_row['id'])) {
    		Db::q("INSERT INTO users.user SET email=#email, password='111', active=1", array('email' => $email));
    		$user_id = Db::$insert_id;
    	} else {
    		$user_id = $user_row['id'];
    	}
    	
    	Db::q("INSERT INTO clients5.client_users SET client_id=#client_id, user_id=#user_id", array('client_id' => $client_id, 'user_id' => $user_id));
    }
    
    public function createBillingAccount($client_id, $balance)
    {
        $id = ($client_id != 0 ? 2 : 1).sprintf('%09d', mt_rand(0, 999999999));
        $account_id = $id.(string)array_sum(str_split($id));
        
    	Db::q("INSERT INTO billing.accounts SET id=#id, status=1,client_id=#client_id,balance=500000", array('id' => $account_id, 'client_id' => $client_id, 'balance' => $balance));
    	return $account_id;
    }
    
    public function createPassportData($client_id, $lname, $fname, $mname)
    {
    	Db::q("INSERT INTO clients5.properties_private SET client_id=#client_id, lname=#lname, mname=#mname, fname=#fname", array('client_id' => $client_id, 'lname' => $lname, 'mname' => $mname, 'fname' => $fname));
    }
    
    public function getTariff()
    {
    	return Db::getRow("
    		SELECT t.id 
    		FROM clients5.tariffs t
    		INNER JOIN clients5.services s ON s.id=t.service_id 
    		INNER JOIN billing.pay_services ps ON s.payservice_id=ps.id
    		WHERE t.status='active' AND t.count=10 AND t.price>0 AND ps.server_id=43
    		LIMIT 1
    	");
    }
    
    public function addTariff($client_id, $tariff_id, $prolongation, $expire_date)
    {
    	$params = array('client_id' => $client_id, 'tariff_id' => $tariff_id, 'prolongation' => $prolongation, 'expire_date' => $expire_date);
    	return Db::q("INSERT INTO clients5.client_tariffs SET client_id=#client_id, tariff_id=#tariff_id, prolongation=#prolongation, status='active', expire_date=#expire_date", $params);
    }
    
    public function tearDown()
    {
    	if ($this->do_delete) {
    		foreach ($this->client_arr as $v) {
    			Db::q("DELETE FROM clients5.client_tariffs WHERE client_id=#client_id", array('client_id' => $v['client_id']));
    			if ($v['account_id']) {
	    			Db::q("DELETE FROM billing.accounts WHERE id=#account_id", array('account_id' => $v['account_id']));
	    		}
	    		
	    		//Db::q("DELETE FROM users.user WHERE id IN (SELECT user_id FROM clients5.client_users WHERE client_id=#client_id)", array('client_id' => $v['client_id']));
	    		Db::q("DELETE FROM clients5.clients WHERE id=#client_id", array('client_id' => $v['client_id']));
    		}        
    	}
    }
    
}