<?php
namespace tests\modules\Front\v1\Sales;

class SimilarListTest extends \PHPUnit_Framework_TestCase
{
    /**
     * @param array $params Входные параметры
     *
     * @dataProvider providerMissingSaleException
     *
     * @expectedException \octopus\classes\Exception
     * @expectedExceptionMessage Не найдено указанное объявление
     */
    public function testMissingSaleException($params)
    {
        $module = new \octopus\modules\Front\v1\Sales\SimilarList();

        $module->request($params);

    }

    public function providerMissingSaleException()
    {
        return [
            [
                []
            ],
            [
                [
                    'sale_id' => ''
                ]
            ],
            [
                [
                    'sale_id' => 0
                ]
            ],
            [
                [
                    'sale_id' => false
                ]
            ],
            [
                [
                    'sale_id' => -123
                ]
            ],
        ];
    }

    /**
     * @param array $params      Входные данные
     * @param array $total_sales Кол-во объявлений
     * @param array $expected    Ожидаемый результат
     *
     * @dataProvider providerRequest
     */
    public function testRequest($params, $total_sales, $expected)
    {
        $module = new \octopus\modules\Front\v1\Sales\SimilarList();

        // $repositoryCars
        $collectionSales = $this->getMockBuilder('\octopus\classes\Collection')
                                ->setMethods(['getTotal'])
                                ->getMock();
        $collectionSales->expects($this->any())
                        ->method('getTotal')->will($this->returnValue($total_sales));

        $repositoryCars = $this->getMockBuilder('\octopus\repositories\Cars')
                               ->setMethods(['getForSimilar'])
                               ->getMock();
        $repositoryCars->expects($this->any())
                       ->method('getForSimilar')->will($this->returnValue($collectionSales));

        // $businessSales
        $collectionSimilarSales = $this->getMockBuilder('\octopus\classes\Collection')
                                       ->setMethods(['getTotalAll'])
                                       ->getMock();
        $collectionSimilarSales->expects($this->any())
                               ->method('getTotalAll')->will($this->returnValue($expected['result']['count']));
        $similar_result = [
            'current' => $expected['result']['current'],
            'facets' => $expected['result']['facets'],
            'geo_id' => $expected['result']['geo_id'],
            'collectionCars' => $collectionSimilarSales
        ];
        $businessSales = $this->getMockBuilder('\octopus\business\Cars')
                              ->setMethods(['getSimilar'])
                              ->getMock();
        $businessSales->expects($this->any())
                      ->method('getSimilar')->will($this->returnValue($similar_result));

        if ($total_sales > 0) {
            // $businessModifications
            $collectionModifications = new \octopus\classes\Collection(new \octopus\entities\Modifications());

            $returnSimilarModificationsMap = [
                ['mark_id', true, [12, 345]],
                ['folder_id', true, [12987, 321, 9]],
            ];
            $collectionSimilarModifications = $this->getMockBuilder('\octopus\classes\Collection')
                                                   ->setMethods(['collect'])
                                                   ->getMock();
            $collectionSimilarModifications->expects($this->any())
                                           ->method('collect')->will($this->returnValueMap($returnSimilarModificationsMap));

            $businessModifications = $this->getMockBuilder('\octopus\business\Modifications')
                                          ->setMethods(['getThroughSales'])
                                          ->getMock();

            $businessModifications->expects($this->at(0))
                                  ->method('getThroughSales')->will($this->returnValue($collectionModifications));
            $businessModifications->expects($this->at(1))
                                  ->method('getThroughSales')->will($this->returnValue($collectionSimilarModifications));

            // $repositoryMarks
            $collectionMarks = new \octopus\classes\Collection(new \octopus\entities\Marks());

            $repositoryMarks = $this->getMockBuilder('\octopus\repositories\Marks')
                                    ->setMethods(['get'])
                                    ->getMock();
            $repositoryMarks->expects($this->any())
                            ->method('get')->will($this->returnValue($collectionMarks));

            // $repositoryFolders
            $collectionFolders = new \octopus\classes\Collection(new \octopus\entities\Folders());

            $repositoryFolders = $this->getMockBuilder('\octopus\repositories\Folders')
                                      ->setMethods(['getByFolderId'])
                                      ->getMock();
            $repositoryFolders->expects($this->any())
                              ->method('getByFolderId')->will($this->returnValue($collectionFolders));

            $module->setRepository('Marks', $repositoryMarks);
            $module->setRepository('Folders', $repositoryFolders);
            $module->setBusiness('Modifications', $businessModifications);
        }

        $builder = $this->getMockBuilder('\octopus\modules\Front\v1\Sales\SimilarListBuilder')
                                  ->setMethods(['build'])
                                  ->getMock();
        $builder->expects($this->any())
                          ->method('build')->will($this->returnValue($expected['result']['items']));


        $module->setRepository('Cars', $repositoryCars);
        $module->setBusiness('Cars', $businessSales);
        $module->setResource('\octopus\modules\Front\v1\Sales\SimilarListBuilder', $builder);

        $result = $module->request($params);
        $this->assertEquals($expected, $result);
    }

    public function providerRequest()
    {
        $item0 = new \stdClass();
        $item0->id = '12414';
        $item0->hash = 'dsbf65d';
        $item0->poi = 'dsbf65d';

        $item1 = new \stdClass();
        $item1->id = '12414';
        $item1->hash = 'dsbf65d';
        $item1->model = (object) [
            'name' => 'Model',
            'alias' => 'model'
        ];

        $items = [$item0, $item1];


        return [
            [
                [
                    'sale_id' => '1019072825',
                    'sale_hash' => '9ce8d1'
                ],
                0,
                [
                    'result' => [
                        'items' => [],
                        'count' => 0
                    ]
                ]
            ],
            [
                [
                    'sale_id' => '4',
                    'limit' => 100,
                    'geo_id' => '213',
                    'this_mark' => 1
                ],
                12,
                [
                    'result' => [
                        'current' => 'this_mark',
                        'facets' => [
                            'this_mark' => 8,
                            'this_seller' => 12,
                            'this_mark_seller' => 4,
                        ],
                        'items' => $items,
                        'count' => 63,
                        'geo_id' => 213
                    ]
                ]
            ],
            [
                [
                    'sale_id' => '12431',
                    'limit' => 50,
                    'page' => 1500,
                    'geo_id' => '213, 2',
                    'this_seller' => 1
                ],
                4,
                [
                    'result' => [
                        'current' => 'full',
                        'facets' => [
                            'this_mark' => 2,
                            'this_seller' => 1,
                            'this_mark_seller' => 0,
                        ],
                        'items' => $items,
                        'count' => 63
                    ]
                ]
            ],
        ];
    }
}

