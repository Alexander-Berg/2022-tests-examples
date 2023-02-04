<?php
namespace tests\entities;

class MessagesTest extends \PHPUnit_Framework_TestCase
{
    public function testGetBody()
    {
        $messages = new \octopus\entities\Messages();
        $this->assertNull($messages->getBody());
    }
}

