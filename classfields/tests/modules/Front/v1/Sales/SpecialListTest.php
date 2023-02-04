<?php
namespace tests\modules\Front\v1\Sales;

class SpecialListTest extends \PHPUnit_Framework_TestCase
{
    /**
     * @param array $params      Входные данные
     * @param array $sales_items Сущности объявлений
     * @param array $expected    Ожидаемый результат
     *
     * @dataProvider providerRequest
     */
    public function testRequest($params, $sales_items, $expected)
    {
        $module = new \octopus\modules\Front\v1\Sales\SpecialList();

        $businessGeo = $this->getMockBuilder('\octopus\business\Geo')
                       ->setMethods(['extendRegionForSalesWithService'])
                       ->getMock();

        $search_params = [];
        if (isset($params['section'])) {
            $search_params['section_id'] = $params['section'] == 'used' ? 1 : 2;
        }
        $extendGeoMap = [
            [['some'], $search_params, $module::SPECIAL_SALES_MIN_COUNT, \octopus\storages\Db\Services::SERVICE_ALL_SALE_SPECIAL,
                ['geo_id' => 0]],
            [[213], $search_params, $module::SPECIAL_SALES_MIN_COUNT, \octopus\storages\Db\Services::SERVICE_ALL_SALE_SPECIAL,
                ['geo_id' => 213, 'param_name' => 'ya_city_id', 'param_value' => 213]],
            [[213, 2], $search_params, $module::SPECIAL_SALES_MIN_COUNT, \octopus\storages\Db\Services::SERVICE_ALL_SALE_SPECIAL,
                ['geo_id' => 225, 'param_name' => 'ya_country_id', 'param_value' => 225]],
            [[213, 159], $search_params, $module::SPECIAL_SALES_MIN_COUNT, \octopus\storages\Db\Services::SERVICE_ALL_SALE_SPECIAL,
                ['geo_id' => 0]],
        ];
        $businessGeo->expects($this->any())
                    ->method('extendRegionForSalesWithService')
                    ->will($this->returnValueMap($extendGeoMap));


        $collectionSales = $this->getMockBuilder('\octopus\classes\Collection')
                            ->setMethods(['collect', 'getTotalAll', 'getTotal', 'getItems'])
                            ->getMock();
        $collectMap = [
            ['mark_id', true, [12,3]],
            ['folder_id', true, [5,12,67]],
        ];
        $collectionSales->expects($this->any())
                    ->method('collect')->will($this->returnValueMap($collectMap));
        $collectionSales->expects($this->any())
                    ->method('getTotalAll')->will($this->returnValue(63));
        $collectionSales->expects($this->any())
                    ->method('getTotal')->will($this->returnValue(5));
        $collectionSales->expects($this->any())
                    ->method('getItems')->will($this->returnValue($sales_items));

        $repositoryCars = $this->getMockBuilder('\octopus\repositories\Cars')
                               ->setMethods(['getSpecials'])
                               ->getMock();
        $repositoryCars->expects($this->any())
                       ->method('getSpecials')->will($this->returnValue($collectionSales));

        $collectionMarks = new \octopus\classes\Collection(new \octopus\entities\Marks());
        $aMarks = [
            [
                'id' => 12,
                'name' => 'Mark1'
            ],
            [
                'id' => 3,
                'name' => 'Mark2'
            ],
        ];
        $collectionMarks->fill($aMarks);

        $repositoryMarks = $this->getMockBuilder('\octopus\repositories\Marks')
                               ->setMethods(['get'])
                               ->getMock();
        $repositoryMarks->expects($this->any())
                       ->method('get')->will($this->returnValue($collectionMarks));

        $collectionFolders = new \octopus\classes\Collection(new \octopus\entities\Folders());
        $aFolders = [
            [
                'id' => 5,
                'name' => 'Folder1'
            ],
            [
                'id' => 67,
                'name' => 'Folder2'
            ],
        ];
        $collectionFolders->fill($aFolders);

        $repositoryFolders = $this->getMockBuilder('\octopus\repositories\Folders')
                               ->setMethods(['getByFolderId'])
                               ->getMock();
        $repositoryFolders->expects($this->any())
                       ->method('getByFolderId')->will($this->returnValue($collectionFolders));

        $collectionContacts = new \octopus\classes\Collection(new \octopus\entities\Contacts());
        $aContacts = [
            [
                'geo_id' => 213,
                'sale_id' => 12345
            ],
            [
                'geo_id' => 67,
                'sale_id' => 9876,
                'client_id' => 3425
            ],
        ];
        $collectionContacts->fill($aContacts);

        $businessContacts = $this->getMockBuilder('\octopus\business\Contacts')
                               ->setMethods(['getThroughSales'])
                               ->getMock();
        $businessContacts->expects($this->any())
                       ->method('getThroughSales')->will($this->returnValue($collectionContacts));

        $builder = $this->getMockBuilder('\octopus\modules\Front\v1\Sales\SpecialListBuilder')
                        ->setMethods(['build'])
                        ->getMock();
        $builder->expects($this->any())
                ->method('build')->will($this->returnValue($expected['result']['items']));

        $cache = $this->getMockBuilder('stdClass')
                        ->setMethods(['incrementIntegerValue'])
                        ->getMock();
        $cache->expects($this->any())
                ->method('incrementIntegerValue')->will($this->returnValue(true));

        $module->setRepository('Cars', $repositoryCars);
        $module->setRepository('Marks', $repositoryMarks);
        $module->setRepository('Folders', $repositoryFolders);
        $module->setBusiness('Geo', $businessGeo);
        $module->setBusiness('Contacts', $businessContacts);
        $module->setResource('\octopus\modules\Front\v1\Sales\SpecialListBuilder', $builder);
        $module->setResource('Cache_membase', $cache);

        $result = $module->request($params);
        $this->assertEquals($expected, $result);
    }

    public function providerRequest()
    {
        $collectionSales = new \octopus\classes\Collection(new \octopus\entities\Cars());
        $sales_data = [
            [
                'id' => 123445,
                'hash' => 'ufd9de',
                'price' => 500000
            ],
            [
                'id' => 98765,
                'price' => 230000
            ]
        ];
        $collectionSales->fill($sales_data);
        $items = $collectionSales->getItems();

        $result_items = [];
        foreach ($sales_data as $item) {
            $oItem = new \stdClass();
            $oItem->id = $item['id'];
            if (!empty($item['hash'])) {
                $oItem->hash = $item['hash'];
            }
            $oItem->price = $item['price'];
            $result_items[] = $oItem;
        }

        return [
            [
                [],
                $items,
                [
                    'result' => [
                        'items' => $result_items,
                        'count' => 63
                    ]
                ]
            ],
            [
                [
                    'geo_id' => 'some',
                    'section' => 'some'
                ],
                $items,
                [
                    'result' => [
                        'items' => $result_items,
                        'count' => 63,
                        'geo_id' => 0
                    ]
                ]
            ],
            [
                [
                    'geo_id' => '213',
                    'section' => 'new',
                    'page' => 4,
                    'limit' => 4,
                ],
                $items,
                [
                    'result' => [
                        'items' => $result_items,
                        'count' => 63,
                        'geo_id' => 213
                    ]
                ]
            ],
            [
                [
                    'geo_id' => '213,2',
                    'section' => 'used'
                ],
                $items,
                [
                    'result' => [
                        'items' => $result_items,
                        'count' => 63,
                        'geo_id' => 225
                    ]
                ]
            ],
            [
                [
                    'geo_id' => '213,159',
                    'limit' => 100
                ],
                $items,
                [
                    'result' => [
                        'items' => $result_items,
                        'count' => 63,
                        'geo_id' => 0
                    ]
                ]
            ],
        ];
    }
}
