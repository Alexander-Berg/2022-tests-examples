<?php
/**
 * Created by PhpStorm.
 * User: molodtsov
 * Date: 04.04.
 * Time: 19:17
 */

namespace lib5\tests\unit\classes;

use lib5\classes\Stats;

/**
 * Class StatsTest
 * @package lib5\tests\unit\classes
 * @coversDefaultClass \lib5\classes\Stats
 */
class StatsTest extends \PHPUnit_Framework_TestCase
{
    use \Xpmock\TestCaseTrait;

    /**
     * @var Stats
     */
    protected $object;

    public function testGetInstance()
    {
        self::assertEmpty($this->reflect($this->object)->instances, __LINE__);

        $ins = $this->object->getInstance();

        self::assertInstanceOf('\lib5\classes\Stats', $ins, __LINE__);

        self::assertEquals(Stats::DEFAULT_NAMESPACE, $this->reflect($ins)->namespace, __LINE__);

        self::assertNotEmpty($this->reflect($this->object)->instances, __LINE__);
        self::assertCount(1, $this->reflect($this->object)->instances, __LINE__);

        self::assertArrayHasKey(Stats::DEFAULT_NAMESPACE, $this->reflect($this->object)->instances, __LINE__);

        $key = 'test';
        $ins = $this->object->getInstance($key);

        self::assertInstanceOf('\lib5\classes\Stats', $ins, __LINE__);

        self::assertEquals($key, $this->reflect($ins)->namespace, __LINE__);

        self::assertNotEmpty($this->reflect($this->object)->instances, __LINE__);
        self::assertCount(2, $this->reflect($this->object)->instances, __LINE__);

        self::assertArrayHasKey($key, $this->reflect($this->object)->instances, __LINE__);
    }

    public function testSetNamespace()
    {
        $ins = $this->object->getInstance();
        self::assertEquals(Stats::DEFAULT_NAMESPACE, $this->reflect($ins)->namespace, __LINE__);

        $handler = new Stats\Handlers\GstatsD(new Stats\Connection\InMemory());

        $ins->pushHandler($handler);

        self::assertEquals(\lib5\classes\Stats::DEFAULT_NAMESPACE, $handler->getNamespace(), __LINE__);

        $key = 'test';
        $ins->setNamespace($key);

        self::assertEquals($key, $handler->getNamespace(), __LINE__);

        self::assertEquals($key, $this->reflect($ins)->namespace, __LINE__);
    }

    public function testGetTimer()
    {
        $ins = $this->object->getInstance();
        $handlers = ['trololo'];
        $this->reflect($ins)->handlers = $handlers;
        $key = 'test';
        $timer = $ins->getTimer($key);
        self::assertInstanceOf('\lib5\classes\Stats\Timer', $timer, __LINE__);
        self::assertEquals($handlers, $this->reflect($timer)->handlers, __LINE__);
        self::assertEquals($key, $timer->getName(), __LINE__);
        self::assertEquals(\lib5\classes\Stats::DEFAULT_NAMESPACE, $timer->getNamespace(), __LINE__);
    }

    public function testGetCounter()
    {
        $ins = $this->object->getInstance();
        $handlers = ['trololo'];
        $this->reflect($ins)->handlers = $handlers;
        $key = 'test';
        $counter = $ins->getCounter($key);
        self::assertInstanceOf('\lib5\classes\Stats\Counter', $counter, __LINE__);
        self::assertEquals($handlers, $this->reflect($counter)->handlers, __LINE__);
        self::assertEquals($key, $counter->getName(), __LINE__);
        self::assertEquals(\lib5\classes\Stats::DEFAULT_NAMESPACE, $counter->getNamespace(), __LINE__);
    }

    public function testGetGauge()
    {
        $ins = $this->object->getInstance();
        $handlers = ['trololo'];
        $this->reflect($ins)->handlers = $handlers;
        $key = 'test';
        $object = $ins->getGauge($key);
        self::assertInstanceOf('\lib5\classes\Stats\Gauge', $object, __LINE__);
        self::assertEquals($handlers, $this->reflect($object)->handlers, __LINE__);
        self::assertEquals($key, $object->getName(), __LINE__);
        self::assertEquals(\lib5\classes\Stats::DEFAULT_NAMESPACE, $object->getNamespace(), __LINE__);
    }

    public function testGetRater()
    {
        $ins = $this->object->getInstance();
        $handlers = ['trololo'];
        $this->reflect($ins)->handlers = $handlers;
        $key = 'test';
        $object = $ins->getRater($key);
        self::assertInstanceOf('\lib5\classes\Stats\Rater', $object, __LINE__);
        self::assertEquals($handlers, $this->reflect($object)->handlers, __LINE__);
        self::assertEquals($key, $object->getName(), __LINE__);
        self::assertEquals(\lib5\classes\Stats::DEFAULT_NAMESPACE, $object->getNamespace(), __LINE__);
    }

    public function testPushGetHandler()
    {
        $ins = $this->object->getInstance();

       self::assertEmpty($this->reflect($ins)->handlers, __LINE__);

        $ins->pushHandler(new Stats\Handlers\GstatsD(new Stats\Connection\InMemory()));

       self::assertNotEmpty($this->reflect($ins)->handlers, __LINE__);

       self::assertInstanceOf('lib5\classes\Stats\Handlers\GstatsD', reset($this->reflect($ins)->handlers), __LINE__);
    }

    public function testCloneWithName()
    {
        self::assertEmpty($this->reflect($this->object)->instances, __LINE__);

        $ins = $this->object->getInstance();

        self::assertInstanceOf('\lib5\classes\Stats', $ins, __LINE__);

        self::assertEquals(Stats::DEFAULT_NAMESPACE, $this->reflect($ins)->namespace, __LINE__);

        self::assertNotEmpty($this->reflect($this->object)->instances, __LINE__);
        self::assertCount(1, $this->reflect($this->object)->instances, __LINE__);

        self::assertArrayHasKey(Stats::DEFAULT_NAMESPACE, $this->reflect($this->object)->instances, __LINE__);

        $key = 'test';
        $ins2 = $this->object->cloneWithName($key);

        self::assertInstanceOf('\lib5\classes\Stats', $ins2, __LINE__);

        self::assertEquals($key, $this->reflect($ins2)->namespace, __LINE__);

        self::assertNotEmpty($this->reflect($this->object)->instances, __LINE__);
        self::assertCount(2, $this->reflect($this->object)->instances, __LINE__);

        self::assertArrayHasKey($key, $this->reflect($this->object)->instances, __LINE__);

        $key = 'test2';
        $ins3 = $this->object->cloneWithName($key);  // клонируем со новым ключом

        self::assertEquals($key, $this->reflect($ins3)->namespace, __LINE__);
        self::assertCount(3, $this->reflect($this->object)->instances, __LINE__);
        self::assertArrayHasKey($key, $this->reflect($this->object)->instances, __LINE__);


        $key = 'test';
        $ins4 = $this->object->cloneWithName($key); // клонируем со старым ключом, вернёт уже готовый инстанс

        self::assertInstanceOf('\lib5\classes\Stats', $ins4, __LINE__);
        self::assertEquals($key, $this->reflect($ins4)->namespace, __LINE__);
        self::assertCount(3, $this->reflect($this->object)->instances, __LINE__);
        self::assertSame($ins2, $ins4, __LINE__);

        $ins5 = $this->object->cloneWithName(); // клонируем со старым ключом, вернёт уже готовый инстанс

        self::assertInstanceOf('\lib5\classes\Stats', $ins5, __LINE__);
        self::assertEquals(Stats::DEFAULT_NAMESPACE, $this->reflect($ins5)->namespace, __LINE__);
        self::assertCount(3, $this->reflect($this->object)->instances, __LINE__);
        self::assertSame($ins, $ins5, __LINE__);
    }

    /**
     * Sets up the fixture, for example, opens a network connection.
     * This method is called before a test is executed.
     */
    protected function setUp()
    {
//        $this->reflect()
        $this->object = $this->mock('\lib5\classes\Stats')->new();
    }

    /**
     * Tears down the fixture, for example, closes a network connection.
     * This method is called after a test is executed.
     */
    protected function tearDown()
    {
        $this->reflect($this->object)->instances = [];
    }
}
