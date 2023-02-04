<?php
/**
 * Created by PhpStorm.
 * User: molodtsov
 * Date: 22.04.16
 * Time: 19:34
 */

namespace lib5\tests\unit\classes\Stats;

use lib5\classes\Stats\Connection\InMemory;
use lib5\classes\Stats\Handlers\JsonString;

class CounterTest extends \PHPUnit_Framework_TestCase
{
    use \Xpmock\TestCaseTrait;

    /**
     * @var \lib5\classes\Stats\Counter
     */
    protected $object;

    /**
     * @var InMemory
     */
    protected $connection;

    /**
     * @var JsonString
     */
    protected $handler;

    /**
     * Sets up the fixture, for example, opens a network connection.
     * This method is called before a test is executed.
     */
    protected function setUp()
    {
        /** @var \lib5\classes\Stats\Counter object */
        $this->object = $this->mock('\lib5\classes\Stats\Counter')->new();
        $this->connection = new InMemory();
        $this->handler    = new JsonString($this->connection);
        $this->object->setHandlers([$this->handler]);
    }


    public function testIncrement()
    {
        $this->object->increment();
        $this->object->increment(10);
        $messages = $this->connection->getMessages();
        self::assertCount(2, $messages, __LINE__);

        self::assertEquals(1,  json_decode($messages[0])->value, __LINE__);
        self::assertEquals(10, json_decode($messages[1])->value, __LINE__);
    }


    public function testDecrement()
    {
        $this->object->decrement();
        $this->object->decrement(10);
        $messages = $this->connection->getMessages();
        self::assertCount(2, $messages, __LINE__);

        self::assertEquals(-1,  json_decode($messages[0])->value, __LINE__);
        self::assertEquals(-10, json_decode($messages[1])->value, __LINE__);
    }


    public function testSetValue()
    {
        $this->object->setValue(1);
        $this->object->setValue(-1);
        $messages = $this->connection->getMessages();
        self::assertCount(2, $messages, __LINE__);

        self::assertEquals(1,  json_decode($messages[0])->value, __LINE__);
        self::assertEquals(-1, json_decode($messages[1])->value, __LINE__);
    }

    public function testGetValue()
    {
        self::assertEquals(0, $this->object->getValue(), __LINE__);

        $this->object->setValue(10);

        self::assertEquals(0, $this->object->getValue(), __LINE__);

        $this->object->this()->value = 10;

        self::assertEquals(10, $this->object->getValue(), __LINE__);
    }
}
