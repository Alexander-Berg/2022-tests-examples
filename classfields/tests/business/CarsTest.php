<?php
namespace tests\business;

class CarsTest extends \PHPUnit_Framework_TestCase
{
    /**
     * @param $sale_data
     * @param $collectionModifications
     * @param $methods_data
     * @param $this_mark
     * @param $this_seller
     * @param $expected
     *
     * @dataProvider providerGetSimilar
     */
    public function testGetSimilar($sale_data, $collectionModifications, $methods_data, $this_mark, $this_seller, $expected)
    {
        // Sale
        $saleMock = $this->getMockBuilder('\octopus\entities\Cars')
                         ->setMethods(['getRaw', 'getCurrency', 'getClientId'])
                         ->getMock();
        $returnSaleRawMap = [
            [\octopus\entities\Cars::FIELD_MARK_ID, $sale_data['mark_id']],
            [\octopus\entities\Cars::FIELD_NEW_CLIENT_ID, $sale_data['new_client_id']],
        ];
        $saleMock->expects($this->any())->method('getRaw')->will($this->returnValueMap($returnSaleRawMap));
        $saleMock->expects($this->any())->method('getClientId')->will($this->returnValue($sale_data['new_client_id']));
        $saleMock->expects($this->any())->method('getCurrency')->will($this->returnValue($sale_data['currency']));

        $collectionSales = $this->getMockBuilder('\octopus\classes\Collection')
                                ->setMethods(['getItems'])
                                ->getMock();
        $collectionSales->expects($this->any())->method('getItems')->will($this->returnValue([$saleMock]));


        // $storageSales
        $storageSales = $this->getMockBuilder('\octopus\storages\Sphinx\Cars')
                             ->setMethods(['getSimilarSalesFacets', 'findSimilar'])
                             ->getMock();
        $storageSales->expects($this->once())->method('getSimilarSalesFacets')->will($this->returnValue($methods_data['getSimilarSalesFacets']));
        $storageSales->expects($this->once())->method('findSimilar')->will($this->returnValue($methods_data['findSimilar']));

        // self
        $businessCars = $this->getMockBuilder('\octopus\business\Cars')
            ->setMethods(['formNonFacetsParams', 'prepareGeoFacets', 'prepareOptionFacets', 'parseGeoFacetsResult', 'parseOptionFacetsResult', 'formSimilarResult'])
            ->getMock();

        $businessCars->expects($this->once())->method('formNonFacetsParams')->will($this->returnValue($methods_data['formNonFacetsParams']));
        $businessCars->expects($this->once())->method('prepareGeoFacets')->will($this->returnValue($methods_data['prepareGeoFacets']));
        $businessCars->expects($this->once())->method('prepareOptionFacets')->will($this->returnValue($methods_data['prepareOptionFacets']));
        $businessCars->expects($this->any())->method('parseGeoFacetsResult')->will($this->returnValue($methods_data['parseGeoFacetsResult']));
        $businessCars->expects($this->any())->method('parseOptionFacetsResult')->will($this->returnValue($methods_data['parseOptionFacetsResult']));
        $businessCars->expects($this->once())->method('formSimilarResult')->will($this->returnValue($methods_data['formSimilarResult']));

        $businessCars->setStorage('Sphinx\Cars', $storageSales);

        $result = $businessCars->getSimilar($collectionSales, $collectionModifications, [], $this_mark, $this_seller);

        $this->assertEquals($expected, $result);
    }

    public function providerGetSimilar()
    {
        // Sale modification

        $collectionModifications_0 = $this->getMockBuilder('\octopus\classes\Collection')
                                        ->setMethods(['getTotal'])
                                        ->getMock();
        $collectionModifications_0->expects($this->any())->method('getTotal')->will($this->returnValue(0));

        $entityModification = new \octopus\entities\Modifications();
        $collectionModifications_1 = $this->getMockBuilder('\octopus\classes\Collection')
                                          ->setMethods(['getTotal', 'getItems'])
                                          ->getMock();
        $collectionModifications_1->expects($this->any())->method('getTotal')->will($this->returnValue(1));
        $collectionModifications_1->expects($this->any())->method('getItems')->will($this->returnValue([$entityModification]));

        return [
            // #0
            [
                [
                    'mark_id' => '14',
                    'currency' => 'RUR',
                    'new_client_id' => null,
                ],
                $collectionModifications_0,
                [
                    'formNonFacetsParams' => [
                        'year' => '2008'
                    ],
                    'prepareGeoFacets' => [],
                    'prepareOptionFacets' => [],
                    'getSimilarSalesFacets' => [],
                    'parseGeoFacetsResult' => [],
                    'parseOptionFacetsResult' => [
                        'this_mark' => 20,
                        'this_seller' => 3,
                        'this_mark_seller' => 0,
                    ],
                    'findSimilar' => [],
                    'formSimilarResult' => new \octopus\classes\Collection(new \octopus\entities\Cars())
                ],
                true,
                false,
                [
                    'collectionCars' => new \octopus\classes\Collection(new \octopus\entities\Cars()),
                    'current' => 'this_mark',
                    'facets' => [
                        'this_mark' => 20,
                        'this_seller' => 3,
                        'this_mark_seller' => 0,
                    ],
                ]
            ],
            // #1
            [
                [
                    'mark_id' => '14',
                    'currency' => 'RUR',
                    'new_client_id' => null,
                ],
                $collectionModifications_0,
                [
                    'formNonFacetsParams' => [
                        'year' => '2008'
                    ],
                    'prepareGeoFacets' => [],
                    'prepareOptionFacets' => [],
                    'getSimilarSalesFacets' => [],
                    'parseGeoFacetsResult' => [],
                    'parseOptionFacetsResult' => [
                        'this_mark' => 0,
                        'this_seller' => 10,
                        'this_mark_seller' => 0,
                    ],
                    'findSimilar' => [],
                    'formSimilarResult' => new \octopus\classes\Collection(new \octopus\entities\Cars())
                ],
                true,
                false,
                [
                    'collectionCars' => new \octopus\classes\Collection(new \octopus\entities\Cars()),
                    'current' => 'full',
                    'facets' => [
                        'this_mark' => 0,
                        'this_seller' => 10,
                        'this_mark_seller' => 0,
                    ],
                ]
            ],
            // #2
            [
                [
                    'mark_id' => '14',
                    'currency' => 'RUR',
                    'new_client_id' => null,
                ],
                $collectionModifications_0,
                [
                    'formNonFacetsParams' => [
                        'year' => '2008'
                    ],
                    'prepareGeoFacets' => [],
                    'prepareOptionFacets' => [],
                    'getSimilarSalesFacets' => [],
                    'parseGeoFacetsResult' => [],
                    'parseOptionFacetsResult' => [
                        'this_mark' => 20,
                        'this_seller' => 12,
                        'this_mark_seller' => 0,
                    ],
                    'findSimilar' => [],
                    'formSimilarResult' => new \octopus\classes\Collection(new \octopus\entities\Cars())
                ],
                false,
                true,
                [
                    'collectionCars' => new \octopus\classes\Collection(new \octopus\entities\Cars()),
                    'current' => 'full',
                    'facets' => [
                        'this_mark' => 20,
                        'this_seller' => 12,
                        'this_mark_seller' => 0,
                    ],
                ]
            ],
            // #3
            [
                [
                    'mark_id' => '14',
                    'currency' => 'RUR',
                    'new_client_id' => '24875',
                ],
                $collectionModifications_0,
                [
                    'formNonFacetsParams' => [
                        'year' => '2008'
                    ],
                    'prepareGeoFacets' => [],
                    'prepareOptionFacets' => [],
                    'getSimilarSalesFacets' => [],
                    'parseGeoFacetsResult' => [],
                    'parseOptionFacetsResult' => [
                        'this_mark' => 20,
                        'this_seller' => 12,
                        'this_mark_seller' => 0,
                    ],
                    'findSimilar' => [],
                    'formSimilarResult' => new \octopus\classes\Collection(new \octopus\entities\Cars())
                ],
                false,
                true,
                [
                    'collectionCars' => new \octopus\classes\Collection(new \octopus\entities\Cars()),
                    'current' => 'this_seller',
                    'facets' => [
                        'this_mark' => 20,
                        'this_seller' => 12,
                        'this_mark_seller' => 0,
                    ],
                ]
            ],
            // #4
            [
                [
                    'mark_id' => '14',
                    'currency' => 'RUR',
                    'new_client_id' => null,
                ],
                $collectionModifications_0,
                [
                    'formNonFacetsParams' => [
                        'year' => '2008'
                    ],
                    'prepareGeoFacets' => [],
                    'prepareOptionFacets' => [],
                    'getSimilarSalesFacets' => [],
                    'parseGeoFacetsResult' => [],
                    'parseOptionFacetsResult' => [
                        'this_mark' => 20,
                        'this_seller' => 0,
                        'this_mark_seller' => 0,
                    ],
                    'findSimilar' => [],
                    'formSimilarResult' => new \octopus\classes\Collection(new \octopus\entities\Cars())
                ],
                false,
                true,
                [
                    'collectionCars' => new \octopus\classes\Collection(new \octopus\entities\Cars()),
                    'current' => 'full',
                    'facets' => [
                        'this_mark' => 20,
                        'this_seller' => 0,
                        'this_mark_seller' => 0,
                    ],
                ]
            ],
            // #5
            [
                [
                    'mark_id' => '14',
                    'currency' => 'RUR',
                    'new_client_id' => '1234',
                ],
                $collectionModifications_1,
                [
                    'formNonFacetsParams' => [
                        'year' => '2008'
                    ],
                    'prepareGeoFacets' => [
                        'ya_city_id' => 213,
                        'ya_region_id' => 1,
                        'ya_country_id' => 225
                    ],
                    'prepareOptionFacets' => [],
                    'getSimilarSalesFacets' => [
                        [
                            ['count(*)' => '151']
                        ],
                        [
                            [
                                'mark_id' => '151',
                                'count(*)' => '1'
                            ],
                            [
                                'mark_id' => '190',
                                'count(*)' => '3'
                            ]
                        ],
                    ],
                    'parseGeoFacetsResult' => [
                        'param_name' => 'ya_city_id',
                        'param_value' => 213,
                        'geo_id' => 213
                    ],
                    'parseOptionFacetsResult' => [
                        'this_mark' => 12,
                        'this_seller' => 11,
                        'this_mark_seller' => 3,
                    ],
                    'findSimilar' => [],
                    'formSimilarResult' => new \octopus\classes\Collection(new \octopus\entities\Cars())
                ],
                true,
                true,
                [
                    'collectionCars' => new \octopus\classes\Collection(new \octopus\entities\Cars()),
                    'current' => 'this_mark_seller',
                    'facets' => [
                        'this_mark' => 12,
                        'this_seller' => 11,
                        'this_mark_seller' => 3,
                    ],
                    'geo_id' => 213
                ]
            ],
        ];
    }

    /**
     * @param $similar_sales
     * @param $currency
     * @param $video
     * @param $expected
     *
     * @dataProvider providerFormSimilarResult
     */
    public function testFormSimilarResult($similar_sales, $currency, $video, $expected)
    {
        // $storageVideos
        $storageVideos = $this->getMockBuilder('\octopus\storages\Db\Cars\Videos')
            ->setMethods(['findAllByParams'])
            ->getMock();
        $storageVideos->expects($this->any())->method('findAllByParams')->will($this->returnValue($video));

        $businessCars = new \octopus\business\Cars();
        $businessCars->setStorage('Db\Cars\Videos', $storageVideos);

        $result = $businessCars->formSimilarResult($similar_sales, $currency);

        $this->assertEquals($expected, $result);

    }

    public function providerFormSimilarResult()
    {
        $data = [
            [
                'similar_data' => [
                    'data' => [],
                    'count' => 0
                ],
                'currency' => 'RUR',
                'video' => []
            ],
            [
                'similar_data' => [
                    'data' => [
                        [
                            'id' => '89543412',
                            'hash' => 'dsf43d',
                            'section_id' => '1',
                            'year' => '2008',
                            'mark_id' => '3225',
                            'folder_id' => '51214',
                            'modification_id' => '131',
                            'images' => '63052-d9c81ef0408f0462caafc06460a77508',
                            'source' => 'oldsale',
                            'state' => '1',
                            'price' => '910000.000000',
                            'geo_id' => '213',
                            'rank_expr' => '2'
                        ],
                        [
                            'id' => '1255236',
                            'hash' => 'sdf321',
                            'section_id' => '1',
                            'year' => '2010',
                            'mark_id' => '3225',
                            'folder_id' => '51214',
                            'modification_id' => '1241',
                            'images' => '63200-79f1a39ff2c87f451d27509ddc54fd67',
                            'source' => 'oldsale',
                            'state' => '1',
                            'price' => '850000.000000',
                            'geo_id' => '213',
                            'rank_expr' => '2'
                        ],
                        [
                            'id' => '1012608009',
                            'hash' => '243tr4',
                            'section_id' => '1',
                            'year' => '2011',
                            'mark_id' => '3225',
                            'folder_id' => '3463',
                            'modification_id' => '12121',
                            'images' => '39173-b325a40ad12aed6356af0309e99a6917',
                            'source' => 'oldsale',
                            'state' => '1',
                            'price' => '830000.000000',
                            'geo_id' => '213',
                            'rank_expr' => '2'
                        ],
                    ],
                    'count' => 3
                ],
                'currency' => 'USD',
                'video' => [
                    '1012608009' => [
                        'sale_id' => '1012608009',
                        'provider_alias' => 'Youtube',
                        'parse_value' => 'q_nBlpFBSa8'
                    ]
                ]
            ]
        ];

        foreach ($data as $i => $item) {
            $collectionCars = new \octopus\classes\Collection(new \octopus\entities\Cars());
            $similar_data = $item['similar_data'];
            $data_to_fill = $similar_data['data'];
            if (!empty($similar_data['data'])) {
                foreach ($similar_data['data'] as $j => $sale) {

                    $data_to_fill[$j]['currency'] = $item['currency'];

                    $data_to_fill[$j]['images'] = [
                        [
                            'name' => $sale['images'],
                            'source' => $sale['source']
                        ]
                    ];

                    $data_to_fill[$j]['video'] = \Helpers_Array::get($item['video'], $sale['id'], []);

                }
                $collectionCars->fill($data_to_fill);
            }
            $collectionCars->setTotalAll($similar_data['count']);
            $data[$i]['result'] = $collectionCars;
        }

        return [
            // #0
            [
                $data[0]['similar_data'],
                $data[0]['currency'],
                $data[0]['video'],
                $data[0]['result'],
            ],
            // #1
            [
                $data[1]['similar_data'],
                $data[1]['currency'],
                $data[1]['video'],
                $data[1]['result'],
            ],
        ];

    }

    /**
     * @param $sale_data
     * @param $modification_data
     * @param $expected
     * @param $offset
     * @param $limit
     *
     * @dataProvider providerFormNonFacetsParams
     */
    public function testFormNonFacetsParams($sale_data, $modification_data, $expected, $offset = null, $limit = null)
    {
        // Sale
        $saleMock = $this->getMockBuilder('\octopus\entities\Cars')
                         ->setMethods(['getId', 'getRaw', 'getCurrency', 'getPrice', 'getState'])
                         ->getMock();
        $saleMock->expects($this->any())->method('getId')->will($this->returnValue($sale_data['id']));
        $returnSaleRawMap = [
            [\octopus\entities\Cars::FIELD_CUSTOM, $sale_data['custom']],
            [\octopus\entities\Cars::FIELD_YEAR, $sale_data['year']],
            [\octopus\entities\Cars::FIELD_CATEGORY_ID, $sale_data['category_id']],
            [\octopus\entities\Cars::FIELD_SECTION_ID, $sale_data['section_id']],
            [\octopus\entities\Cars::FIELD_FOLDER_ID, $sale_data['folder_id']],
            [\octopus\entities\Cars::FIELD_MARK_ID, $sale_data['mark_id']],
            [\octopus\entities\Cars::FIELD_NEW_CLIENT_ID, $sale_data['new_client_id']],
        ];
        $saleMock->expects($this->any())->method('getRaw')->will($this->returnValueMap($returnSaleRawMap));
        $saleMock->expects($this->any())->method('getCurrency')->will($this->returnValue($sale_data['currency']));
        $saleMock->expects($this->any())->method('getPrice')->will($this->returnValue($sale_data['price']));
        $saleMock->expects($this->any())->method('getState')->will($this->returnValue($sale_data['state']));

        // Modification
        if (empty($modification_data)) {
            $modificationMock = null;
        } else {
            $modificationMock = $this->getMockBuilder('\octopus\entities\Modifications')
                                     ->setMethods(['isTechMod', 'getId', 'getRaw', 'getEngineType'])
                                     ->getMock();
            $modificationMock->expects($this->any())->method('isTechMod')->will($this->returnValue($modification_data['tech_mode']));
            if (isset($modification_data['id'])) {
                $modificationMock->expects($this->any())->method('getId')->will($this->returnValue($modification_data['id']));
            }
            $modificationMock->expects($this->any())->method('getEngineType')->will($this->returnValue($modification_data['engine_type_obj']));

            $returnModificationRawMap = [
                [\octopus\entities\Modifications::FIELD_ENGINE_TYPE, $modification_data['engine_type']],
                [\octopus\entities\Modifications::FIELD_BODY_TYPE, $modification_data['body_type']],
                [\octopus\entities\Modifications::FIELD_ENGINE_VOLUME, $modification_data['engine_volume']],
                [\octopus\entities\Modifications::FIELD_ENGINE_POWER, $modification_data['engine_power']],
            ];
            $modificationMock->expects($this->any())->method('getRaw')->will($this->returnValueMap($returnModificationRawMap));
        }

        $business = new \octopus\business\Cars();
        $result = $business->formNonFacetsParams($saleMock, $modificationMock, $offset, $limit);

        $this->assertEquals($expected, $result);
    }

    public function providerFormNonFacetsParams()
    {
        return [
            // #0
            [
                [
                    'id' => '4',
                    'custom' => '1',
                    'year' => '2012',
                    'category_id' => '15',
                    'folder_id' => '44',
                    'mark_id' => '14',
                    'section_id' => '2',
                    'new_client_id' => '324',
                    'currency' => 'USD',
                    'price' => (object) [
                        'RUR' => 560000.00,
                        'USD' => 870000.70,
                        'EUR' => 990000.43,
                    ],
                    'state' => (object) [
                        'alias' => 'onrun'
                    ],
                ],
                [
                    'tech_mod' => true,
                    'engine_power' => '15',
                    'engine_volume' => '90',
                    'body_type' => null,
                    'engine_type' => '1260',
                    'engine_type_obj' => (object) [
                        'alias' => 'hybrid'
                    ],

                ],
                [
                    'id' => '4',
                    'custom' => '1',
                    'year' => [
                        'min' => 2011,
                        'max' => 2013,
                    ],
                    'engine_type' => '1260',
                    'price' => [
                        'min' => 783001.0,
                        'max' => 957001.0,
                    ],
                    'state' => 4,
                    'category_id' => '15',
                    'section_id' => '2',
                    'photo' => 1,
                    'folder_id' => '44',
                    'modification_default' => 0,
                    'status' => [0,8,9],
                    'expire_date' => strtotime("today 00:00:00"),
                    'modification_id' => 0,
                    'limit' => 10,
                    'offset' => 0
                ]
            ],
            // #1
            [
                [
                    'id' => '4',
                    'custom' => '1',
                    'year' => '2015',
                    'category_id' => '15',
                    'folder_id' => '44',
                    'mark_id' => '14',
                    'section_id' => '1',
                    'new_client_id' => 6543,
                    'currency' => 'EUR',
                    'price' => (object) [
                        'RUR' => 560000.00,
                        'USD' => 870000.70,
                        'EUR' => 990000.43,
                    ],
                    'state' => (object) [
                        'alias' => 'beaten'
                    ],
                ],
                [
                    'tech_mod' => false,
                    'id' => 12355,
                    'engine_power' => '110',
                    'engine_volume' => '2200',
                    'body_type' => '1358',
                    'engine_type' => '1240',
                    'engine_type_obj' => (object) [
                        'alias' => 'gasoline'
                    ],
                ],
                [
                    'id' => '4',
                    'custom' => '1',
                    'year' => [
                        'min' => 2014,
                        'max' => 2016,
                    ],
                    'engine_type' => [-1, -2],
                    'engine_power' => [
                        'min' => 93,
                        'max' => 126
                    ],
                    'engine_volume' => [
                        'min' => 2100,
                        'max' => 2300
                    ],
                    'body_type' => '1358',
                    'price' => [
                        'min' => 891000.0,
                        'max' => 1089000.0,
                    ],
                    'state' => 4,
                    'category_id' => '15',
                    'section_id' => '1',
                    'photo' => 1,
                    'folder_id' => '44',
                    'modification_default' => 0,
                    'status' => [0,8,9],
                    'expire_date' => strtotime("today 00:00:00"),
                    'modification_id' => 12355,
                    'limit' => 15,
                    'offset' => 10
                ],
                10,
                15
            ],
        ];
    }

    /**
     * @param array $params   Параметры
     * @param array $expected Ожидаемый результат
     *
     * @dataProvider providerPrepareOptionFacets
     */
    public function testPrepareOptionFacets($params, $expected)
    {
        $mockSale = $this->getMockBuilder('\octopus\entities\Cars')
                         ->setMethods(['getClientId', 'getRaw'])
                         ->getMock();
        $mockSale->expects($this->once())->method('getClientId')->will($this->returnValue($params['new_client_id']));
        $returnRawMap = [
            [\octopus\entities\Cars::FIELD_MARK_ID, $params['mark_id']],
            [\octopus\entities\Cars::FIELD_NEW_CLIENT_ID, $params['new_client_id']],
        ];
        $mockSale->expects($this->any())->method('getRaw')->will($this->returnValueMap($returnRawMap));

        $business = new \octopus\business\Cars();
        $result = $business->prepareOptionFacets($mockSale);

        $this->assertEquals($expected, $result);
        $this->assertInternalType('int', $result['mark_id']);
        if (isset($result['new_client_id'])) {
            $this->assertInternalType('int', $result['new_client_id']);
        }

    }

    public function providerPrepareOptionFacets()
    {
        return [
            [
                [
                    'mark_id' => 12,
                    'new_client_id' => null,
                ],
                [
                    'mark_id' => 12
                ]
            ],
            [
                [
                    'mark_id' => 345,
                    'new_client_id' => 12657,
                ],
                [
                    'mark_id' => 345,
                    'new_client_id' => 12657,
                ]
            ],
            [
                [
                    'mark_id' => '345',
                    'new_client_id' => '12657',
                ],
                [
                    'mark_id' => 345,
                    'new_client_id' => 12657,
                ]
            ]
        ];
    }

    /**
     * @param array $geo_ids  Входные параметры
     * @param array $expected Ожидаемый результат
     *
     * @dataProvider providerPrepareGeoFacets
     */
    public function testPrepareGeoFacets($geo_ids, $expected)
    {
        $helperGeo = $this->getMockBuilder('\octopus\helpers\Geo')
            ->setMethods(['getExtendedRegions'])
            ->getMock();
        $helperGeo->expects($this->any())->method('getExtendedRegions')->will($this->returnValue($expected));

        $business = new \octopus\business\Cars();
        $business->setResource('\octopus\helpers\Geo', $helperGeo);
        $result = $business->prepareGeoFacets($geo_ids);

        $this->assertEquals($expected, $result);
    }

    public function providerPrepareGeoFacets()
    {
        return [
            [
                0,
                []
            ],
            [
                [],
                []
            ],
            [
                [
                    213
                ],
                [
                    'ya_city_id' => 213,
                    'ya_region_id' => 1,
                    'ya_country_id' => 225
                ]
            ],
            [
                [
                    213, 2
                ],
                [
                    'ya_country_id' => 225
                ]
            ],
        ];
    }

    /**
     * @param array $geo_facets      Гео объекты
     * @param array $facets_result   Результат фасетного запроса
     * @param int   $sales_min_count Минимальное количество объявлений
     * @param array $expected        Ожидаемый результат
     *
     * @dataProvider providerParseGeoFacetsResult
     */
    public function testParseGeoFacetsResult($geo_facets, $facets_result, $sales_min_count, $expected)
    {
        $business = new \octopus\business\Cars();
        $result = $business->parseGeoFacetsResult($geo_facets, $facets_result, $sales_min_count);

        $this->assertEquals($expected, $result);
    }

    public function providerParseGeoFacetsResult()
    {
        return [
            [
                [
                    'ya_country_id' => 225,
                ],
                [
                    [
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 1268,
                        'count(*)' => 6
                    ],
                    [
                        'ya_country_id' => 159,
                        'mark_id' => 109,
                        'new_client_id' => 1268,
                        'count(*)' => 6
                    ],
                ],
                8,
                []
            ],
            [
                [
                    'ya_region_id' => 1,
                    'ya_country_id' => 225,
                ],
                [
                    [
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 1268,
                        'count(*)' => 3
                    ],
                    [
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 89,
                        'new_client_id' => 736,
                        'count(*)' => 1
                    ],
                    [
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 75,
                        'count(*)' => 6
                    ],
                    [
                        'ya_region_id' => 12,
                        'ya_country_id' => 159,
                        'mark_id' => 112,
                        'new_client_id' => 7345,
                        'count(*)' => 15
                    ],
                ],
                6,
                [
                    'param_name' => 'ya_country_id',
                    'param_value' => 225,
                    'geo_id' => 225
                ]
            ],
            [
                [
                    'ya_city_id' => 213,
                    'ya_region_id' => 1,
                    'ya_country_id' => 225,
                ],
                [
                    [
                        'ya_city_id' => 213,
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 1268,
                        'count(*)' => 12
                    ],
                    [
                        'ya_city_id' => 165,
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 89,
                        'new_client_id' => 736,
                        'count(*)' => 1
                    ],
                ],
                10,
                [
                    'param_name' => 'ya_city_id',
                    'param_value' => 213,
                    'geo_id' => 213
                ]
            ],
            [
                [],
                [
                    [
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 1268,
                        'count(*)' => 6
                    ],
                    [
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 75,
                        'count(*)' => 6
                    ],
                ],
                6,
                []
            ]
        ];
    }

    /**
     * @param array $option_facets
     * @param array $facets_result
     * @param array $geo_param
     * @param array $expected
     *
     * @dataProvider providerParseOptionFacetsResult
     */
    public function testParseOptionFacetsResult($option_facets, $facets_result, $geo_param, $expected)
    {
        $business = new \octopus\business\Cars();
        $result = $business->parseOptionFacetsResult($option_facets, $facets_result, $geo_param);

        $this->assertEquals($expected, $result);

    }

    public function providerParseOptionFacetsResult()
    {
        return [
            // #0
            [
                [
                    'mark_id' => 109,
                ],
                [
                    [
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 1268,
                        'count(*)' => 6
                    ],
                    [
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 75,
                        'count(*)' => 13
                    ],
                    [
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 85,
                        'new_client_id' => 1268,
                        'count(*)' => 6
                    ],
                    [
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 75,
                        'count(*)' => 1
                    ],
                ],
                [],
                [
                    'this_mark' => 20,
                    'this_seller' => 0,
                    'this_mark_seller' => 0,
                ]
            ],
            // #1
            [
                [
                    'mark_id' => 96,
                    'new_client_id' => 358,
                ],
                [
                    [
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 96,
                        'new_client_id' => 1268,
                        'count(*)' => 26
                    ],
                    [
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 358,
                        'count(*)' => 7
                    ],
                    [
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 96,
                        'new_client_id' => 358,
                        'count(*)' => 11
                    ],
                    [
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 75,
                        'count(*)' => 6
                    ],
                    [
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 12,
                        'new_client_id' => 358,
                        'count(*)' => 2
                    ],
                    [
                        'ya_region_id' => 13,
                        'ya_country_id' => 225,
                        'mark_id' => 96,
                        'new_client_id' => 358,
                        'count(*)' => 8
                    ],
                ],
                [],
                [
                    'this_mark' => 45,
                    'this_seller' => 28,
                    'this_mark_seller' => 19,
                ]
            ],
            // #2
            [
                [
                    'mark_id' => 115,
                ],
                [
                    [
                        'ya_city_id' => 13,
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 96,
                        'new_client_id' => 1268,
                        'count(*)' => 26
                    ],
                    [
                        'ya_city_id' => 213,
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 75,
                        'count(*)' => 18
                    ],
                    [
                        'ya_city_id' => 213,
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 115,
                        'new_client_id' => 1268,
                        'count(*)' => 5
                    ],
                    [
                        'ya_city_id' => 2,
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 115,
                        'new_client_id' => 75,
                        'count(*)' => 21
                    ],
                    [
                        'ya_city_id' => 213,
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 115,
                        'new_client_id' => 321,
                        'count(*)' => 12
                    ],
                ],
                [
                    'ya_city_id' => 213
                ],
                [
                    'this_mark' => 17,
                    'this_seller' => 0,
                    'this_mark_seller' => 0,
                ]
            ],
            // #3
            [
                [
                    'mark_id' => 80,
                    'new_client_id' => 10023,
                ],
                [
                    [
                        'ya_city_id' => 13,
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 80,
                        'new_client_id' => 10023,
                        'count(*)' => 28
                    ],
                    [
                        'ya_city_id' => 213,
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 109,
                        'new_client_id' => 10023,
                        'count(*)' => 18
                    ],
                    [
                        'ya_city_id' => 213,
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 80,
                        'new_client_id' => 1268,
                        'count(*)' => 5
                    ],
                    [
                        'ya_city_id' => 12,
                        'ya_region_id' => 2,
                        'ya_country_id' => 225,
                        'mark_id' => 80,
                        'new_client_id' => 10023,
                        'count(*)' => 21
                    ],
                    [
                        'ya_city_id' => 213,
                        'ya_region_id' => 1,
                        'ya_country_id' => 225,
                        'mark_id' => 115,
                        'new_client_id' => 10023,
                        'count(*)' => 12
                    ],
                ],
                [
                    'ya_region_id' => 2
                ],
                [
                    'this_mark' => 26,
                    'this_seller' => 39,
                    'this_mark_seller' => 21,
                ]
            ],
            // #4
            [
                [
                    'mark_id' => 109,
                ],
                [],
                [
                    'ya_city_id' => 213
                ],
                [
                    'this_mark' => 0,
                    'this_seller' => 0,
                    'this_mark_seller' => 0,
                ]
            ],


        ];

    }

}
