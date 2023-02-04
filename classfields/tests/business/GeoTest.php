<?php
/**
 * Created by PhpStorm.
 * User: ichered
 * Date: 16.10.15
 * Time: 13:04
 */

namespace tests\business;


class GeoTest extends \PHPUnit_Framework_TestCase
{
    /**
     * Тестируем метод extendRegionForSalesWithService
     *
     * @param $return_values
     *
     * @dataProvider providerTestЕxtendRegionForSalesWithService
     *
     * @return void
     */
    public function testЕxtendRegionForSalesWithService($params, $return_values, $expected)
    {
        $business = new \octopus\business\Geo();
        if (isset($return_values['resources']['octopus\helpers\Geo'])) {
            $helperGeo = $this->getMockBuilder('octopus\helpers\Geo')->setMethods(['getExtendedRegions'])->getMock();
            $helperGeo->expects($this->any())->method('getExtendedRegions')
                ->will($this->returnValue($return_values['resources']['octopus\helpers\Geo']['getExtendedRegions']));
            $business->setResource('octopus\helpers\Geo', $helperGeo);
        }

        $storageSales = $this->getMockBuilder('octopus\storages\Sphinx\Cars')->setMethods(['cache', 'getSalesWithServiceFacets'])->getMock();
        $storageSales->expects($this->any())->method('cache')->will($this->returnValue($storageSales));
        if (isset($return_values['storages']['Sphinx\Cars'])) {
            $storageSales->expects($this->any())->method('getSalesWithServiceFacets')
                ->will($this->returnValue($return_values['storages']['Sphinx\Cars']['getSalesWithServiceFacets']));
        }
        $business->setStorage('Sphinx\Cars', $storageSales);

        $params['geo_ids'] = isset($params['geo_ids']) ? $params['geo_ids'] : [];
        $params['search_params'] = isset($params['search_params']) ? $params['search_params'] : [];
        $params['sales_min_count'] = isset($params['sales_min_count']) ? $params['sales_min_count'] : [];
        $params['service'] = isset($params['service']) ? $params['service'] : [];

        $this->assertEquals(
            $expected,
            $business->extendRegionForSalesWithService(
                $params['geo_ids'],
                $params['search_params'],
                $params['sales_min_count'],
                $params['service']
            )
        );
    }

    public function providerTestЕxtendRegionForSalesWithService()
    {
        return [
            [
                [],
                [
                    'storages' => [
                        'Sphinx\Cars' => [
                            'getSalesWithServiceFacets' => [
                                0 => [
                                    'full' => true,
                                    'facet_result' => 0
                                ]
                            ]
                        ]
                    ]
                ],
                [
                    'geo_id' => 0
                ]
            ],
            [
                [
                    'search_params' => [
                        'section_id' => 1,
                    ],
                    'geo_ids' => [213],
                    'sales_min_count' => 6,
                    'service' => 'all_sale_special',
                ],
                [
                    'resources' => [
                        'octopus\helpers\Geo' => [
                            'getExtendedRegions' => [
                                'ya_city_id' => 213,
                                'ya_category_id' => 1,
                                'ya_country_id' => 225
                            ]
                        ],
                    ],
                    'storages' => [
                        'Sphinx\Cars' => [
                            'getSalesWithServiceFacets' => [
                                0 => [
                                    'ya_city_id' => 213,
                                    'facet_result' => 100,
                                ],
                                1 => [
                                    'ya_region_id' => 1,
                                    'facet_result' => 100
                                ],
                                2 => [
                                    'ya_country_id' => 225,
                                    'facet_result' => 100,
                                ],
                                3 => [
                                    'full' => true,
                                    'facet_result' => 100
                                ]
                            ],
                        ]
                    ]

                ],
                [
                    'param_name' => 'ya_city_id',
                    'param_value' => 213,
                    'geo_id' => 213,
                ]
            ],
            [
                [
                    'search_params' => [
                        'section_id' => 1,
                        'photo' => 1,
                        'mark' => 100,
                        'salon' => 100
                    ],
                    'geo_ids' => [213,312],
                    'sales_min_count' => 1,
                    'service' => 'all_sale_special',
                ],
                [
                    'resources' => [
                        'octopus\helpers\Geo' => [
                            'getExtendedRegions' => [
                                'ya_city_id' => 213,
                                'ya_category_id' => 1,
                                'ya_country_id' => 225
                            ]
                        ],
                    ]
                ],
                [
                    'geo_id' => 0
                ]
            ],
        ];
    }

    /**
     * @param $compare
     * @param $facets_result
     * @param $sales_min_count
     *
     * @dataProvider providerTestParseFacetsResult
     */
    public function testParseFacetsResult($compare, $facets_result, $sales_min_count)
    {
        $business = new \octopus\business\Geo();
        $result = $business->parseFacetsResult($facets_result, $sales_min_count);
        $this->assertEquals($compare, $result);
    }

    public function providerTestParseFacetsResult()
    {
        return [
            [
                [
                    'geo_id' => 0
                ],
                [
                    'run' => [
                        'facets_result' => 100,
                    ],
                    'year' => [
                        'facets_result' => 1000
                    ],
                ],
                10
            ],
            [
                [
                    'geo_id' => 0
                ],
                [
                    'run' => 'dasdas',
                    'year' => 'fkjsdhfkjsfd1313'
                ],
                10
            ]
        ];
    }
}