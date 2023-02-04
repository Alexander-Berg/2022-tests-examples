<?php
namespace tests\helpers;

class GeoTest extends \PHPUnit_Framework_TestCase
{
    /**
     * @param $ids
     * @param $expected
     *
     * @dataProvider providerGetFinalGeoIds
     */
    public function testGetFinalGeoIds($ids, $expected)
    {
        $helperGeo = new \octopus\helpers\Geo();
        $result = $helperGeo->getFinalGeoIds($ids);
        $this->assertEquals($expected, $result);
    }

    public function providerGetFinalGeoIds()
    {
        return [
            [
                [],
                []
            ],
            [
                [
                    7567348
                ],
                []
            ],
            [
                [
                    213,
                    115669
                ],
                [
                    213,
                    20934,
                    112520,
                    112543,
                    112587,
                    112610,
                    115670,
                    115671,
                    115672,
                ]
            ],
        ];
    }

    /**
     * @param $ids
     * @param $expected
     *
     * @dataProvider providerGetParentAndTypeById
     */
    public function testGetParentAndTypeById($ids, $expected)
    {
        $helperGeo = new \octopus\helpers\Geo();
        $result = $helperGeo->getParentAndTypeById($ids);
        $this->assertEquals($expected, $result);
    }

    public function providerGetParentAndTypeById()
    {
        return [
            [
                213, // Москва
                [
                    213 => ['parent_id' => 1, 'type' => 6, 'weight' => 100],
                ]
            ],
            [
                216, // Зеленоград
                [
                    216 => ['parent_id' => 1, 'type' => 6, 'weight' => 50],
                ]
            ],
            [
                [
                    225, // Россия
                    2, // СПб
                    159 // Казахстан
                ],
                [
                    225 => ['parent_id' => 225, 'type' => 3, 'weight' => 50],
                    2 => ['parent_id' => 10174, 'type' => 6, 'weight' => 100],
                    159 => ['parent_id' => 159, 'type' => 3, 'weight' => 30]
                ]
            ]
        ];
    }

    /**
     * @param $geo_ids
     * @param $parents
     * @param $expected
     *
     * @dataProvider providerGetCommonParent
     */
    public function testGetCommonParent($geo_ids, $parents, $expected)
    {
        $helper = $this->getMockBuilder('\octopus\helpers\Geo')
            ->setMethods(['getParentAndTypeById'])
            ->getMock();
        $helper->expects($this->any())->method('getParentAndTypeById')->will($this->returnValue($parents));

        $result = $helper->getCommonParent($geo_ids);
        $this->assertEquals($expected, $result);
    }

    public function providerGetCommonParent()
    {
        return [
            [
                [
                    2 // СПб
                ],
                [
                    2 => ['parent_id' => 10174, 'type' => 6, 'weight' => 100],
                ],
                10174
            ],
            [
                [
                    216, // Зеленоград
                    213, // Москва
                    10747, // Подольск
                ],
                [
                    216 => ['parent_id' => 1, 'type' => 6, 'weight' => 50],
                    213 => ['parent_id' => 1, 'type' => 6, 'weight' => 50],
                    10747 => ['parent_id' => 1, 'type' => 6, 'weight' => 50],
                ],
                1
            ],
            [
                [
                    10747, // Подольск
                    10869 // Каменногорск (Лен обл)
                ],
                [
                    10869 => ['parent_id' => 10174, 'type' => 6, 'weight' => 50],
                    10747 => ['parent_id' => 1, 'type' => 6, 'weight' => 50],
                ],
                225 // Россия
            ],
            [
                [
                    10747, // Подольск
                    159 // Казахстан
                ],
                [
                    159 => ['parent_id' => 159, 'type' => 3, 'weight' => 30],
                    10747 => ['parent_id' => 1, 'type' => 6, 'weight' => 50],
                ],
                false // Россия
            ],
            [
                [
                    10747, // Подольск
                    20273 // Актобе (Казахстан)
                ],
                [
                    20273 => ['parent_id' => 29404, 'type' => 5, 'weight' => 30],
                    10747 => ['parent_id' => 1, 'type' => 6, 'weight' => 50],
                ],
                false // Россия
            ],
        ];
    }

    /**
     * @param $geo_ids
     * @param $methods_data
     * @param $expected
     *
     * @dataProvider providerGetExtendedRegions
     */
    public function testGetExtendedRegions($geo_ids, $methods_data, $expected)
    {
        $helper = $this->getMockBuilder('\octopus\helpers\Geo')
                       ->setMethods(['getCommonParent', 'getParentAndTypeById', 'getFieldNameByType'])
                       ->getMock();
        $helper->expects($this->any())->method('getCommonParent')->will($this->returnValue($methods_data['getCommonParent']));
        $helper->expects($this->any())->method('getParentAndTypeById')->will($this->returnValueMap($methods_data['getParentAndTypeById']));
        $valuesMap = [
            [6, "ya_city_id"],
            [5, "ya_region_id"],
            [3, "ya_country_id"],
        ];
        $helper->expects($this->any())->method('getFieldNameByType')->will($this->returnValueMap($valuesMap));

        $result = $helper->getExtendedRegions($geo_ids);

        $this->assertEquals($expected, $result);
    }

    public function providerGetExtendedRegions()
    {
        return [
            [
                [],
                [],
                []
            ],
            [
                [
                    225
                ],
                [
                    'getCommonParent' => 225,
                    'getParentAndTypeById' => [
                        [225, [ 225 => ['parent_id' => 225, 'type' => 3, 'weight' => 50]],],
                    ],
                ],
                [
                    'ya_country_id' => 225
                ]
            ],
            [
                [
                    10747, // Подольск
                    10869 // Каменногорск (Лен обл)
                ],
                [
                    'getCommonParent' => 225,
                    'getParentAndTypeById' => [
                        [225, [ 225 => ['parent_id' => 225, 'type' => 3, 'weight' => 50]],],
                    ],
                ],
                [
                    'ya_country_id' => 225
                ]
            ],
            [
                [
                    10747, // Подольск
                    216, // Зеленоград
                ],
                [
                    'getCommonParent' => 1,
                    'getParentAndTypeById' => [
                        [1, [ 1 => ['parent_id' => 225, 'type' => 5, 'weight' => 100]],],
                        [225, [ 225 => ['parent_id' => 225, 'type' => 3, 'weight' => 50]],],
                    ],
                ],
                [
                    'ya_region_id' => 1,
                    'ya_country_id' => 225,
                ]
            ],
            [
                [
                    216, // Зеленоград
                ],
                [
                    'getCommonParent' => 216,
                    'getParentAndTypeById' => [
                        [216, [ 216 => ['parent_id' => 1, 'type' => 6, 'weight' => 50]],],
                        [1, [1 => ['parent_id' => 225, 'type' => 5, 'weight' => 100]],],
                        [225, [ 225 => ['parent_id' => 225, 'type' => 3, 'weight' => 50]],],
                    ],
                ],
                [
                    'ya_city_id' => 216,
                    'ya_region_id' => 1,
                    'ya_country_id' => 225,
                ]
            ]
        ];
    }

    /**
     * @param $type
     * @param $expected
     *
     * @dataProvider providerGetFieldNameByType
     */
    public function testGetFieldNameByType($type, $expected)
    {
        $helperGeo = new \octopus\helpers\Geo();
        $result = $helperGeo->getFieldNameByType($type);
        $this->assertEquals($expected, $result);
    }

    public function providerGetFieldNameByType()
    {
        return [
            [
                12,
                false
            ],
            [
                'wrt',
                false
            ],
            [
                3,
                'ya_country_id'
            ],
            [
                5,
                'ya_region_id'
            ],
            [
                6,
                'ya_city_id'
            ]
        ];
    }

    /**
     * @param $geo_ids
     * @param $methods_data
     * @param $expected
     *
     * @dataProvider providerGetSearchGeoParams
     */
    public function testGetSearchGeoParams($geo_ids, $methods_data, $expected)
    {
        $helper = $this->getMockBuilder('\octopus\helpers\Geo')
                       ->setMethods(['getParentAndTypeById', 'getFieldNameByType', 'getFinalGeoIds'])
                       ->getMock();
        $helper->expects($this->any())->method('getParentAndTypeById')->will($this->returnValue($methods_data['getParentAndTypeById']));
        $valuesMap = [
            [6, "ya_city_id"],
            [5, "ya_region_id"],
            [3, "ya_country_id"],
        ];
        $helper->expects($this->any())->method('getFieldNameByType')->will($this->returnValueMap($valuesMap));
        $helper->expects($this->any())->method('getFinalGeoIds')->will($this->returnValue($methods_data['getFinalGeoIds']));

        $result = $helper->getSearchGeoParams($geo_ids);

        $this->assertEquals($expected, $result);
    }

    public function providerGetSearchGeoParams()
    {
        return [
            [
                [],
                [
                    'getParentAndTypeById' => [],
                    'getFinalGeoIds' => [],
                ],
                [
                    'geo_id' => []
                ]
            ],
            [
                [
                    216, // Зеленоград
                    213, // Москва
                    2, // СПб
                ],
                [
                    'getParentAndTypeById' => [
                        216 => ['parent_id' => 1, 'type' => 6, 'weight' => 50],
                        213 => ['parent_id' => 1, 'type' => 6, 'weight' => 100],
                        2 => ['parent_id' => 10174, 'type' => 6, 'weight' => 50],
                    ],
                    'getFinalGeoIds' => [],
                ],
                [
                    'ya_city_id' => [216, 213, 2]
                ]
            ],
            [
                [
                    1, // Москва и обл
                    10174, // Лен обл
                ],
                [
                    'getParentAndTypeById' => [
                        1 => ['parent_id' => 225, 'type' => 5, 'weight' => 100],
                        10174 => ['parent_id' => 225, 'type' => 5, 'weight' => 50],
                    ],
                    'getFinalGeoIds' => [],
                ],
                [
                    'ya_region_id' => [1, 10174]
                ]
            ],
            [
                [
                    213, // Москва
                    10174, // Лен обл
                ],
                [
                    'getParentAndTypeById' => [
                        213 => ['parent_id' => 1, 'type' => 6, 'weight' => 100],
                        10174 => ['parent_id' => 225, 'type' => 5, 'weight' => 50],
                    ],
                    'getFinalGeoIds' => [12414, 24235, 1241, 214],
                ],
                [
                    'geo_id' => [12414, 24235, 1241, 214]
                ]
            ]
        ];
    }
}
