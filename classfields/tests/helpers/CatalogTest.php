<?php
namespace tests\helpers;

class CatalogTest extends \PHPUnit_Framework_TestCase
{
    /**
     * @param $code
     * @param $expected
     *
     * @dataProvider providerGetMarkByCode
     */
    public function testGetMarkByCode($code, $expected)
    {
        $mark = \octopus\helpers\Catalog::getMarkByCode($code);
        $this->assertEquals($expected, $mark);
    }

    public function providerGetMarkByCode()
    {
        return [
            [
                'ALFA_ROMEO',
                [
                    'id' => 7,
                    'group_alias' => 'foreign',
                    'country_id' => 205,
                ],
            ],
            [
                'HOLDEN',
                [
                    'id' => 103,
                    'group_alias' => null,
                    'country_id' => 211,
                ],
            ],
            [
                'VAZ',
                [
                    'id' => 288,
                    'group_alias' => 'domestic',
                    'country_id' => 225,
                ],
            ],
            [
                'SOME_MARK',
                null
            ]
        ];

    }
}
