<?php
require_once 'PHPUnit/Framework.php';
require_once dirname(__FILE__).'/../../lib5/common.php';
$_GET['debug'] = 0;

class BillingTest extends UnitTestCase
{
    
    /**
    * @var Billing
    */
    private $billing;
    
    /**
    * @var Clients
    */
    private $clients;
    
    /**
    * @var Invoice
    */
    private $invoice;
    /**
    * @var Gateways
    */
    private $gateways;
    
    public function setUp()
    {
        parent::setUp();
        
        $this->billing  = Model::factory('Billing@billing5');    
        $this->clients  = Model::factory('Clients@clients5');    
        $this->invoice  = Model::factory('Invoice@billing5');    
        $this->gateways = Model::factory('Gateways@billing5');    
    }
    
    
    public function testPay()
    {
        $sum = 543.6756;

        // Для первого попавшегося аккаунта и любого гейтвея проведем
        // транзакцию пополнения счета
        Db::begin('master');
        $account = Db::getRow('SELECT * FROM billing.accounts ORDER BY RAND() LIMIT 1', array(), 'master');
        $gateway = Db::getRow('SELECT * FROM billing.gateways ORDER BY RAND() LIMIT 1', array(), 'master');
        $this->billing->_payInAccountRaw($account['id'], $gateway['id'], $sum);

        $changed_account = DB::getRow('SELECT * FROM billing.accounts WHERE id=#id', array('id' => $account['id']), 'master');
        $transaction = Db::getRow('SELECT * FROM billing.transactions ORDER BY id DESC LIMIT 1', array(), 'master');

        $this->assertEquals(round($account['balance'] + $sum, 2), $changed_account['balance'], 'На счет зачислена неверная сумма');
        $this->assertEquals(round($sum, 2), $transaction['amount'], 'Неверная сумма в транзакции');
        Db::rollback('master');

        // Теперь попробуем списать деньги с аккаунта чела
        Db::begin('master');
        //$params = array('sum' => str_replace(',','.',$sum));
        //$account = Db::getRow('SELECT * FROM billing.accounts WHERE balance > #sum ORDER BY RAND() LIMIT 1', $params, 'master');
        Db::rollback('master');
    }
    
    public function testClientInvoice() 
    {
        try {
            
            $sum = 3214.56;
            
            $client_params['name'] = 'testClientInvoice Client';
            $client_params['group_id'] = 1; // dealer
            $client_params['type'] = 'private'; // dealer
            $client_id = $this->clients->save(false, $client_params, 'new');        
        
            $mail  = 'test'.substr(md5(rand()),-5).'@auto.ru';
            Db::q('INSERT INTO users.user SET user.email = #mail', array('mail' => $mail));            
            $mail_is_added = Model::factory('Client_users@clients5')->add($mail, $client_id);
            $this->assertTrue($mail_is_added,'Клиенту не добавляется емейл');
            
            $acc_id = $this->billing->getClientAccountInfo($client_id);
            $this->assertTrue((bool) $acc_id, 'Не создается эккаунт для клиента');

            $gw = array_rand($this->gateways->getList());           
            $inv_id = $this->invoice->createInvoice($acc_id['account_id'], $gw, $sum);
            $this->assertTrue((bool) $inv_id, 'Не создается инвойс ');
            
            $this->invoice->payInvoice($inv_id,$sum);
            $balance = $this->billing->getAccountBalance($acc_id['account_id']);
            $this->assertEquals($balance, $sum, 'Неправильно пополняется баланс');
            
            $this->clients->real_delete($client_id);
            
            
            
        } catch (DbQueryException $e) {
            $this->assertFalse(true, $e);
        }
    }

}
?>