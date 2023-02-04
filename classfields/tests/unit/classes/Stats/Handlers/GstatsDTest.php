<?php
/**
 * Created by PhpStorm.
 * User: molodtsov
 * Date: 05.04.16
 * Time: 10:13
 */

namespace lib5\tests\unit\classes\Stats\Handlers;


use lib5\classes\Stats\Connection\InMemory;
use lib5\classes\Stats\Counter;
use lib5\classes\Stats\Gauge;
use lib5\classes\Stats\Handlers\CounterInterface;
use lib5\classes\Stats\Handlers\GaugeInterface;
use lib5\classes\Stats\Handlers\RaterInterface;
use lib5\classes\Stats\Handlers\TimerInterface;
use lib5\classes\Stats\Rater;
use lib5\classes\Stats\Timer;

class GstatsDTest extends \PHPUnit_Framework_TestCase
{
    use \Xpmock\TestCaseTrait;

    /**
     * @var \lib5\classes\Stats\Handlers\GstatsD
     */
    protected $object;

    protected $connection;

    /**
     * Sets up the fixture, for example, opens a network connection.
     * This method is called before a test is executed.
     */
    protected function setUp()
    {
        $this->object = $this->mock('\lib5\classes\Stats\Handlers\GstatsD')->new();
        $this->connection =  new InMemory();
        $this->object->this()->connection = $this->connection;
    }

    public static function providerPrepareData()
    {
        return [
            ['key', 1, TimerInterface::TIMER_TYPE, 'prefix',     't prefix.key.'.TimerInterface::TIMER_TYPE.' 1000000' . PHP_EOL],
            ['key', 1.01, TimerInterface::TIMER_TYPE, 'prefix',  't prefix.key.'.TimerInterface::TIMER_TYPE.' 1010000' . PHP_EOL],
            ['key', 1, TimerInterface::MEMORY_TYPE, 'prefix',    't prefix.key.'.TimerInterface::MEMORY_TYPE.' 1' . PHP_EOL],
            ['key', 1, CounterInterface::COUNTER_TYPE, 'prefix', 'c prefix.key.'.CounterInterface::COUNTER_TYPE.' 1' . PHP_EOL],
            ['key', 1, RaterInterface::RATER_TYPE, 'prefix',     'm prefix.key.'.RaterInterface::RATER_TYPE.' 1' . PHP_EOL],
            ['key', 1, GaugeInterface::GAUGE_TYPE, 'prefix',     'g prefix.key.'.GaugeInterface::GAUGE_TYPE.' 1' . PHP_EOL],
            [1, 1, RaterInterface::RATER_TYPE, 'prefix',         'm prefix.1.'.RaterInterface::RATER_TYPE.' 1' . PHP_EOL],
            [1, 1, 'trololo', 'prefix',        false]
        ];
    }

    /**
     * @dataProvider providerPrepareData
     */
    public function testPrepareData($key, $value, $type, $prefix, $result)
    {
        $string = $this->object->this()->prepareData($key, $value, $type, $prefix);
        if ($result) {
            self::assertNotEmpty($string, __LINE__);
        }
        self::assertEquals($result, $string, __LINE__);
    }

    public function testRecordTimer()
    {
        // выглядит странно, но это работает и работает хорошо
        self::assertEmpty($this->connection->getMessages(), __LINE__);
        $object = new Timer('key', [$this->object], 'prefix');
        $this->reflect($object)->timeTotal = 1;
        $this->object->recordTimer($object);
        self::assertNotEmpty($this->connection->getMessages(), __LINE__);
        self::assertCount(1, $this->connection->getMessages(), __LINE__);
        self::assertEquals('t prefix.key.'.TimerInterface::TIMER_TYPE.' 1000000' . PHP_EOL, $this->connection->getMessages()[0], __LINE__);
    }

    public function testRecordMemory()
    {
        // выглядит странно, но это работает и работает хорошо
        self::assertEmpty($this->connection->getMessages(), __LINE__);
        $object = new Timer('key', [$this->object], 'prefix');
        $this->reflect($object)->memoryTotal = 1;
        $this->object->recordMemory($object);
        self::assertNotEmpty($this->connection->getMessages(), __LINE__);
        self::assertCount(1, $this->connection->getMessages(), __LINE__);
        self::assertEquals('t prefix.key.'.TimerInterface::MEMORY_TYPE.' 1' . PHP_EOL, $this->connection->getMessages()[0], __LINE__);
    }

    public function testRecordCounter()
    {
        // выглядит странно, но это работает и работает хорошо
        self::assertEmpty($this->connection->getMessages(), __LINE__);
        $object = new Counter('key', [$this->object], 'prefix');
        $this->reflect($object)->value = 1;
        $this->object->recordCounter($object);
        self::assertNotEmpty($this->connection->getMessages(), __LINE__);
        self::assertCount(1, $this->connection->getMessages(), __LINE__);
        self::assertEquals('c prefix.key.'.CounterInterface::COUNTER_TYPE.' 1' . PHP_EOL, $this->connection->getMessages()[0], __LINE__);
    }

    public function testRecordRater()
    {
        // выглядит странно, но это работает и работает хорошо
        self::assertEmpty($this->connection->getMessages(), __LINE__);
        $object = new Rater('key', [$this->object], 'prefix');
        $this->reflect($object)->value = 1;

        $this->object->recordRater($object);
        self::assertNotEmpty($this->connection->getMessages(), __LINE__);
        self::assertCount(1, $this->connection->getMessages(), __LINE__);
        self::assertEquals('m prefix.key.'.RaterInterface::RATER_TYPE.' 1' . PHP_EOL, $this->connection->getMessages()[0], __LINE__);
    }

    public function testRecordGauge()
    {
        // выглядит странно, но это работает и работает хорошо
        self::assertEmpty($this->connection->getMessages(), __LINE__);
        $object = new Gauge('key', [$this->object], 'prefix');
        $this->reflect($object)->value = 1;

        $this->object->recordGauge($object);
        self::assertNotEmpty($this->connection->getMessages(), __LINE__);
        self::assertCount(1, $this->connection->getMessages(), __LINE__);
        self::assertEquals('g prefix.key.'.GaugeInterface::GAUGE_TYPE.' 1' . PHP_EOL, $this->connection->getMessages()[0], __LINE__);
    }
}
