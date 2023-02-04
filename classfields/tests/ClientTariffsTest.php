<?php
require_once 'PHPUnit/Framework.php';
require_once dirname(__FILE__).'/../../lib5/common.php';
$_GET['debug'] = 0;

class ClientTariffsTest extends UnitTestCase
{

    public function testTariffClose()
    {
        $client_tariffs_model = Model::factory('Client_tariffs@clients5');

        // Обычная ситуация, считаем количество дней между датами.
        $days = $client_tariffs_model->countTransactionDays('2010-04-01 12:00', '2010-05-01');
        $this->assertEquals($days, 30, 'С 1 апреля по 1 мая вообще-то 30 дней');

        // Эта ситуация немного абсурдна, потому что тарифы покупаются по месяцам, и время
        // не должно отличаться.
        $days = $client_tariffs_model->countTransactionDays('2010-04-01 12:00', '2010-05-01 15:00');
        $this->assertEquals($days, 30, 'Косяк? лишние 3 часа дали целый день?');

        // В последние выходные марта переводят часы, поэтому проверим это тоже.
        $days = $client_tariffs_model->countTransactionDays('2010-03-20 12:00', '2010-04-20 12:00');
        $this->assertEquals($days, 31, 'С 1 апреля по 1 мая вообще-то 31 день');

        // Проверка оставшихся дней транзакции.
        // Казалось бы лишние 22 часа дадут нам еще один день? неа!
        $close_date  = '2010-04-02 01:00';
        $expire_date = '2010-05-01 23:00';
        $days_left   = $client_tariffs_model->countTransactionDaysLeft($expire_date, $close_date);
        $this->assertEquals($days_left, 28, 'Неверный расчет по оставшимся дням 1');

        // Еще один контрольный тест
        $close_date  = '2010-04-02 11:42';
        $expire_date = '2010-05-02 08:30';
        $days_left   = $client_tariffs_model->countTransactionDaysLeft($expire_date, $close_date);
        $this->assertEquals($days_left, 29, 'Неверный расчет по оставшимся дням 2');

        // Наконец, проверим сколько же нам вернется денег
        $set_date    = '2010-04-02 12:00';
        $close_date  = '2010-04-02 16:00';
        $expire_date = '2010-05-02 12:00';
        $days        = $client_tariffs_model->countTransactionDays($set_date, $expire_date);
        $days_left   = $client_tariffs_model->countTransactionDaysLeft($expire_date, $close_date);

        $test_amount = 300; // рублей
        $return_sum  = $client_tariffs_model->calcTransactionReturnSum($test_amount, $days, $days_left);
        $this->assertEquals($return_sum, 290, 'Неверная сумма возврата');
        
        // Проверим 3 часа
        $check_one_day = $client_tariffs_model->checkTransactionOneDay('2010-04-01 12:00', '2010-04-01 15:00');
        $this->assertEquals($check_one_day, 1, 'Плохо считает 3 часа');
    }
    
    public function testTariffReport()
    {
    	$client_tariffs_model = Model::factory('Client_tariffs@clients5');
    	
    	$period_start = '2010-02-01';
    	$period_end = '2010-03-01';
    	
    	// Все дни в месяце
    	$trans = array(
    		array('set_date' => '2010-01-26 12:00', 'expire_date' => '2010-03-26 12:00', 'operation' => 'buy', 'amount' => -4720),
    	);
    	$info = $client_tariffs_model->getMonthPayInfoByTransactions($trans, $period_start, $period_end, '2010-01-26 12:00', '2010-03-26 12:00');
    	$this->assertEquals($info['duration'], 28, 'Тест 1: неправильно расчитан период, должен быть полный месяц 28 дней');
    	$this->assertEquals($info['price'], 2240, 'Тест 1: неправильно расчитана цена, за 28 дней 2240 (цена одного дня 80)');
    	
    	// Граничные значения месяца
    	$trans = array(
    		array('set_date' => '2010-02-01 12:00', 'expire_date' => '2010-03-01 12:00', 'operation' => 'buy', 'amount' => -2360),
    	);
    	$info = $client_tariffs_model->getMonthPayInfoByTransactions($trans, $period_start, $period_end, '2010-02-01 12:00', '2010-03-01 12:00');
    	$this->assertEquals($info['duration'], 28, 'Тест 2: неправильно расчитан период, должен быть полный месяц 28 дней');
    	$this->assertEquals($info['price'], 2360, 'Тест 2: неправильно расчитана цена, за 28 дней 2360');
    	
    	// Не попадает в месяц (левая граница)
    	$trans = array(
    		array('set_date' => '2010-01-01 12:00', 'expire_date' => '2010-02-01 12:00', 'operation' => 'buy', 'amount' => -2360),
    	);
    	$info = $client_tariffs_model->getMonthPayInfoByTransactions($trans, $period_start, $period_end, '2010-01-01 12:00', '2010-02-01 12:00');
    	$this->assertEquals($info['duration'], 0, 'Тест 3: период не должен считаться (тариф не попал в период)');
    	$this->assertEquals($info['price'], 0, 'Тест 3: цена не должна считаться (тариф не попал в период)');    
    	
    	// Не попадает в месяц (правая граница)
    	$trans = array(
    		array('set_date' => '2010-03-01 12:00', 'expire_date' => '2010-04-01 12:00', 'operation' => 'buy', 'amount' => -2360),
    	);
    	$info = $client_tariffs_model->getMonthPayInfoByTransactions($trans, $period_start, $period_end, '2010-03-01 12:00', '2010-04-01 12:00');
    	$this->assertEquals($info['duration'], 0, 'Тест 4: период не должен считаться (тариф не попал в период)');
    	$this->assertEquals($info['price'], 0, 'Тест 4: цена не должна считаться (тариф не попал в период)');  	
    	
    	// Куплен в середине месяца
    	$trans = array(
    		array('set_date' => '2010-02-05 12:00', 'expire_date' => '2010-03-05 12:00', 'operation' => 'buy', 'amount' => -2360),
    	);
    	$info = $client_tariffs_model->getMonthPayInfoByTransactions($trans, $period_start, $period_end, '2010-02-05 12:00', '2010-03-05 12:00');
    	$this->assertEquals($info['duration'], 24, 'Тест 5: неправильно расчитан период, должно быть 24 дня');
    	$this->assertEquals($info['price'], 2022.86, 'Тест 5: неправильно расчитана цена, за 24 дней 2022.86 (цена одного дня 84,29)');  	
   
    	// Истекает в середине месяца
    	$trans = array(
    		array('set_date' => '2010-01-26 12:00', 'expire_date' => '2010-02-26 12:00', 'operation' => 'buy', 'amount' => -2360),
    	);
    	$info = $client_tariffs_model->getMonthPayInfoByTransactions($trans, $period_start, $period_end, '2010-01-26 12:00', '2010-02-26 12:00');
    	$this->assertEquals($info['duration'], 25, 'Тест 6: неправильно расчитан период, должно быть 25 дней');
    	$this->assertEquals($info['price'], 1903.23, 'Тест 6: неправильно расчитана цена, за 25 дней 1903.23 (цена одного дня 76)'); 
    	
    	// Отказ в середине месяца
    	$trans = array(
    		array('set_date' => '2010-01-26 12:00', 'expire_date' => '2010-03-26 12:00', 'operation' => 'buy', 'amount' => -4720),
    		array('set_date' => '2010-03-26 12:00', 'expire_date' => '2010-02-15 17:00', 'operation' => 'close', 'amount' => 3120),
    	);
    	$info = $client_tariffs_model->getMonthPayInfoByTransactions($trans, $period_start, $period_end, '2010-01-26 12:00', '2010-02-15 17:00');
    	$this->assertEquals($info['duration'], 14, 'Тест 7: неправильно расчитан период, должно быть 14 дней');
    	$this->assertEquals($info['price'], 1120, 'Тест 7: неправильно расчитана цена, за 14 дней 1120 (цена одного дня 80)');  
    	
    	// Отказ в день покупки до 3х часов
    	$trans = array(
    		array('set_date' => '2010-02-05 12:00', 'expire_date' => '2010-03-05 12:00', 'operation' => 'buy', 'amount' => -2360),
    		array('set_date' => '2010-03-05 12:00', 'expire_date' => '2010-02-05 12:37', 'operation' => 'close', 'amount' => 2360),
    	);
    	$info = $client_tariffs_model->getMonthPayInfoByTransactions($trans, $period_start, $period_end, '2010-02-05 12:00', '2010-02-05 12:37');
    	$this->assertEquals($info['duration'], 0, 'Тест 8: неправильно расчитан период, должно быть 0 дней (< 3 часов)');
    	$this->assertEquals($info['price'], 0, 'Тест 8: неправильно расчитана цена, должно быть 0 (< 3 часов)'); 
    	
    	// Отказ в день покупки после 3х часов
    	$trans = array(
    		array('set_date' => '2010-02-05 12:00', 'expire_date' => '2010-03-05 12:00', 'operation' => 'buy', 'amount' => -2360),
    		array('set_date' => '2010-03-05 12:00', 'expire_date' => '2010-02-05 17:37', 'operation' => 'close', 'amount' => 2275.71),
    	);
    	$info = $client_tariffs_model->getMonthPayInfoByTransactions($trans, $period_start, $period_end, '2010-02-05 12:00', '2010-02-05 17:37');
    	$this->assertEquals($info['duration'], 1, 'Тест 9: неправильно расчитан период, должно быть 1 день (> 3 часов)');
    	$this->assertEquals($info['price'], 84.29, 'Тест 9: неправильно расчитана цена, за 1 день 84 (> 3 часов)');  
    }

}
?>