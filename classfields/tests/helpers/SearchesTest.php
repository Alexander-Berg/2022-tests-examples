<?php
namespace tests\helpers;

class SearchesTest extends \PHPUnit_Framework_TestCase
{
    public function testGetContainerKey()
    {
        $result = \octopus\helpers\Searches::getContainerKey();
        $this->assertEquals('CONTAINER_SEARCH', $result);
    }
}
