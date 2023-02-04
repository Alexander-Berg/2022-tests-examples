<?php
namespace tests\helpers;

class SeoTest extends \PHPUnit_Framework_TestCase
{
    /**
     * @param $alias
     * @param $expected
     *
     * @dataProvider providerGetTargetingObjectTypeIdByAlias
     */
    public function testGetTargetingObjectTypeIdByAlias($alias, $expected)
    {
        $result = \octopus\helpers\Seo::getTargetingObjectTypeIdByAlias($alias);
        $this->assertEquals($expected, $result);
    }

    public function providerGetTargetingObjectTypeIdByAlias()
    {
        return [
            [
                'main_page_background',
                1
            ],
            [
                'preset',
                2
            ],
            [
                'something',
                false
            ],
        ];

    }
}
