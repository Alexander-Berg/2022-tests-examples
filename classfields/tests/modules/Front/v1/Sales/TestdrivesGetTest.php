<?php
namespace tests\modules\Front\v1\Sales;

class TestdrivesGetTest extends \PHPUnit_Framework_TestCase
{
    /**
     * @param array $params Входные данные
     *
     * @dataProvider providerInvalidSaleIdException
     *
     * @expectedException \octopus\classes\Exception
     * @expectedExceptionMessage Не указан параметр sale_id
     */
    public function testInvalidSaleIdException($params)
    {
        $module = new \octopus\modules\Front\v1\Sales\TestdrivesGet();
        $module->request($params);
    }

    public function providerInvalidSaleIdException()
    {
        return [
            [
                []
            ],
            [
                [
                    'sale_id' => ''
                ]
            ]
        ];
    }

    /**
     * @expectedException \octopus\classes\Exception
     * @expectedExceptionMessage Не найдено ни одного тестдрайва
     */
    public function testTestdrivesNotFoundException()
    {
        $module = new \octopus\modules\Front\v1\Sales\TestdrivesGet();

        $storageCars = $this->getMockBuilder('\octopus\storages\Db\Cars')
                            ->setMethods(['cache', 'findByPk'])
                            ->getMock();
        $storageCars->expects($this->any())->method('cache')->will($this->returnValue($storageCars));
        $storageCars->expects($this->any())->method('findByPk')
                    ->will($this->returnValue([]));

        $collectionTestdrives = $this->getMockBuilder('\octopus\classes\Collection')
                                     ->setMethods(['getTotal'])
                                     ->getMock();
        $collectionTestdrives->expects($this->any())->method('getTotal')->will($this->returnValue(0));

        $repositoryTestdrives = $this->getMockBuilder('\octopus\repositories\Testdrives')
                                     ->setMethods(['cache', 'getForCard'])
                                     ->getMock();
        $repositoryTestdrives->expects($this->any())->method('cache')->will($this->returnValue($repositoryTestdrives));
        $repositoryTestdrives->expects($this->any())->method('getForCard')->will($this->returnValue($collectionTestdrives));

        $module->setStorage('Db\Cars', $storageCars);
        $module->setRepository('Testdrives', $repositoryTestdrives);

        $module->request(['sale_id' => 4]);
    }

    /**
     * @param array $params     Входные данные
     * @param array $testdrives Тестдрайвы
     * @param array $expected   Ожидаемый результат
     *
     * @dataProvider providerRequest
     */
    public function testRequest($params, $testdrives, $expected)
    {
        $module = new \octopus\modules\Front\v1\Sales\TestdrivesGet();

        $storageCars = $this->getMockBuilder('\octopus\storages\Db\Cars')
                            ->setMethods(['cache', 'findByPk'])
                            ->getMock();
        $storageCars->expects($this->any())->method('cache')->will($this->returnValue($storageCars));
        $storageCars->expects($this->any())->method('findByPk')
                    ->will($this->returnValue([
                        'category_id' => 15,
                        'mark_id' => 6,
                        'folder_id' => 12345
                    ]));

        $collectionTestdrives = $this->getMockBuilder('\octopus\classes\Collection')
                                ->setMethods(['getTotal', 'getItems'])
                                ->getMock();
        $collectionTestdrives->expects($this->any())->method('getTotal')->will($this->returnValue(2));
        $collectionTestdrives->expects($this->any())->method('getItems')->will($this->returnValue($testdrives));

        $repositoryTestdrives = $this->getMockBuilder('\octopus\repositories\Testdrives')
                                     ->setMethods(['cache', 'getForCard'])
                                     ->getMock();
        $repositoryTestdrives->expects($this->any())->method('cache')->will($this->returnValue($repositoryTestdrives));
        $repositoryTestdrives->expects($this->any())->method('getForCard')->will($this->returnValue($collectionTestdrives));

        $module->setStorage('Db\Cars', $storageCars);
        $module->setRepository('Testdrives', $repositoryTestdrives);

        $result = $module->request($params);
        $this->assertEquals($expected, $result);
    }


    public function providerRequest()
    {
        $data = [
            [
                [
                    'url' => '/article/category/testdrives_comparison/663_ford_focus_vs_vw_golf_liga_chempionov',
                    'images' => [
                        'small' => '74d5ff11'
                    ],
                    'description' => 'Начнем с основного:',
                    'title' => 'Тестируем новый Focus',
                    'count' => 2
                ],
                [
                    'url' => '/some_url',
                    'images' => [
                        'small' => 'img'
                    ],
                    'description' => 'Description',
                    'title' => 'Title',
                    'count' => 2
                ]
            ],
            [
                [
                    'url' => '/some_url',
                    'images' => [
                        'small' => 't74d5fer'
                    ],
                    'description' => 'Описание тест-драйва:',
                    'title' => 'Заголовок тест-драйва',
                    'count' => 3
                ],
                [
                    'url' => '/some_url',
                    'images' => [
                        'small' => 'img'
                    ],
                    'description' => 'Description',
                    'title' => 'Title',
                    'count' => 2
                ]
            ]
        ];

        $data_for_test = [];
        foreach ($data as $value) {
            $collectionTestdrives = new \octopus\classes\Collection(new \octopus\entities\Testdrives());
            $collectionTestdrives->fill($value);
            $items = $collectionTestdrives->getItems();
            $expected = new \stdClass();
            $expected->url = $value[0]['url'];
            $expected->images = [];
            if (is_array($value[0]['images'])) {
                $image3 = \lib5\classes\Image3::factory(\Config::get('images_news7', 'octopus'));
                foreach ($value[0]['images'] as $image) {
                    $oImage = $image3->load($image)->getUrls();
                    $expected->images[] = $oImage;
                }
            }
            $expected->description = $value[0]['description'];
            $expected->title = $value[0]['title'];
            $expected->total_count = $value[0]['count'];

            $data_for_test[] = [
                'items' => $items,
                'expected' => $expected
            ];
        }

        return [
            [
                [
                    'sale_id' => 4,
                ],
                $data_for_test[0]['items'],
                [
                    'result' => $data_for_test[0]['expected'],
                ],
            ],
            [
                [
                    'sale_id' => 4,
                ],
                $data_for_test[1]['items'],
                [
                    'result' => $data_for_test[1]['expected'],
                ]
            ],
        ];
    }

}

