<?php
namespace tests\modules\Front\v1\Sales;

class SpecialListBuilderTest extends \PHPUnit_Framework_TestCase
{
    const EUR_COURSE = 70;
    const USD_COURSE = 62.9;
    /**
     * @param array $sales_data Входные данные
     * @param array $expected   Ожидаемый результат
     *
     * @dataProvider providerBuild
     */
    public function testBuild($sales_data, $expected)
    {
        $builder = new \octopus\modules\Front\v1\Sales\SpecialListBuilder();

        // $mockMark
        $mockMark = $this->getMockBuilder('\octopus\entities\Marks')
                         ->setMethods(['getName', 'getAlias'])
                         ->getMock();
        $mockMark->expects($this->any())
                 ->method('getName')->will($this->returnValue('Mark'));
        $mockMark->expects($this->any())
                 ->method('getAlias')->will($this->returnValue('mark'));

        // $mockModel
        $mockModel = $this->getMockBuilder('\octopus\entities\Folders')
                          ->setMethods(['getLevel', 'getName', 'getAlias'])
                          ->getMock();
        $mockModel->expects($this->any())
                  ->method('getLevel')->will($this->returnValue(1));
        $mockModel->expects($this->any())
                  ->method('getName')->will($this->returnValue('Model'));
        $mockModel->expects($this->any())
                  ->method('getAlias')->will($this->returnValue('model'));

        // $mockGeneration
        $mockGeneration = $this->getMockBuilder('\octopus\entities\Folders')
                               ->setMethods(['getLevel', 'getName', 'getAlias', 'getParentId'])
                               ->getMock();
        $mockGeneration->expects($this->any())
                       ->method('getLevel')->will($this->returnValue(2));
        $mockGeneration->expects($this->any())
                       ->method('getName')->will($this->returnValue('Generation'));
        $mockGeneration->expects($this->any())
                       ->method('getAlias')->will($this->returnValue('generation'));
        $mockGeneration->expects($this->any())
                       ->method('getParentId')->will($this->returnValue(43));

        // моки Контактов
        $mockContact_0 = $this->getMockBuilder('\octopus\entities\Contacts')
                              ->setMethods(['getGeoId'])
                              ->getMock();
        $mockContact_0->expects($this->any())->method('getGeoId')->will($this->returnValue(0));

        $mockContact_1 = $this->getMockBuilder('\octopus\entities\Contacts')
                              ->setMethods(['getGeoId'])
                              ->getMock();
        $mockContact_1->expects($this->any())->method('getGeoId')->will($this->returnValue(213));

        // массив с моками объявлений
        $mocksSales = $this->_getSalesMocks($sales_data);


        // $collectionMarks
        $collectionMarks = $this->getMockBuilder('\octopus\classes\Collection')
                                ->setMethods(['get'])
                                ->getMock();
        $collectionMarks->expects($this->any())
                        ->method('get')->will($this->returnValue($mockMark));

        // $collectionFolders
        $returnFoldersMap = [
            ['id', 43, $mockModel],
            ['id', 123, $mockGeneration],
        ];
        $collectionFolders = $this->getMockBuilder('\octopus\classes\Collection')
                                  ->setMethods(['get'])
                                  ->getMock();
        $collectionFolders->expects($this->any())
                          ->method('get')->will($this->returnValueMap($returnFoldersMap));

        // $collectionContacts
        $returnContactsMap = [
            ['sale_id', '12345', $mockContact_0],
            ['sale_id', '12251345', $mockContact_1],
        ];
        $collectionContacts = $this->getMockBuilder('\octopus\classes\Collection')
                                   ->setMethods(['get'])
                                   ->getMock();
        $collectionContacts->expects($this->any())
                           ->method('get')->will($this->returnValueMap($returnContactsMap));

        // $collectionSales
        $collectionSales = $this->getMockBuilder('\octopus\classes\Collection')
                                ->setMethods(['getItems'])
                                ->getMock();
        $collectionSales->expects($this->any())
                        ->method('getItems')->will($this->returnValue($mocksSales));

        $builder->setMarks($collectionMarks);
        $builder->setFolders($collectionFolders);
        $builder->setContacts($collectionContacts);
        $builder->setSales($collectionSales);

        $result = $builder->build();
        $this->assertEquals($expected, $result);
    }

    public function providerBuild()
    {
        $sales_data = [
            [
                'id'         => '12345',
                'hash'       => 'us453w',
                'mark_id'    => 12,
                'folder_id'  => 43,
                'price'      => 349000,
                'section_id' => 1,
                'year'       => 2003,
                'images'     => [
                    [
                        'name'       => '328746528',
                        'main'       => 1,
                        'is_suspect' => 2
                    ],
                    [
                        'name' => '712t321',
                    ]
                ]
            ],
            [
                'id'         => '12251345',
                'mark_id'    => 12,
                'folder_id'  => 123,
                'price'      => 700000,
                'section_id' => 2,
                'year'       => 2009,
            ],
        ];

        $resultSalesData = [];
        foreach ($sales_data as $sale_data) {
            $resultItem = new \stdClass();
            $resultItem->id = $sale_data['id'];
            $resultItem->hash = isset($sale_data['hash']) ? $sale_data['hash'] : '';
            $resultItem->year = $sale_data['year'];
            $resultItem->section = $sale_data['section_id'] == 2 ? 'new' : 'used';

            $resultItem->price = new \stdClass();
            $resultItem->price->RUR = $sale_data['price'];
            $resultItem->price->USD = floatval($sale_data['price'] * self::USD_COURSE);
            $resultItem->price->EUR = floatval($sale_data['price'] * self::EUR_COURSE);

            $resultItem->mark = new \stdClass();
            $resultItem->mark->name = 'Mark';
            $resultItem->mark->alias = 'mark';

            $resultItem->model = [
                'name' => 'Model',
                'alias' => 'model',
            ];

            if ($sale_data['folder_id'] == 123) {
                $resultItem->generation = [
                    'name' => 'Generation',
                    'alias' => 'generation',
                ];
            }

            $resultItem->poi = new \stdClass();
            if ($sale_data['id'] == '12251345') {
                $resultItem->poi->geo_id = 213;
            }

            if (isset($sale_data['images'])) {
                $resultItem->images = $this->_getImages($sale_data['images']);;
            }

            $resultSalesData[] = $resultItem;
        }

        return [
            [
                $sales_data,
                $resultSalesData
            ]
        ];
    }

    private function _getSalesMocks($sales_data)
    {
        $resultSalesData = [];
        foreach ($sales_data as $sale_data) {
            $mockSale = $this->getMockBuilder('\octopus\entities\Cars')
                             ->setMethods(['getMarkId', 'getFolderId', 'getId', 'getHash',
                                           'getPrice', 'getSection', 'getYear', 'getImages'])
                             ->getMock();
            $mockSale->expects($this->any())
                     ->method('getMarkId')->will($this->returnValue($sale_data['mark_id']));
            $mockSale->expects($this->any())
                     ->method('getFolderId')->will($this->returnValue($sale_data['folder_id']));
            $mockSale->expects($this->any())
                     ->method('getId')->will($this->returnValue($sale_data['id']));

            $sale_hash = empty($sale_data['hash']) ? '' : $sale_data['hash'];
            $mockSale->expects($this->any())
                     ->method('getHash')->will($this->returnValue($sale_hash));

            $price = new \stdClass();
            $price->RUR = $sale_data['price'];
            $price->USD = floatval($sale_data['price'] * self::USD_COURSE);
            $price->EUR = floatval($sale_data['price'] * self::EUR_COURSE);
            $mockSale->expects($this->any())
                     ->method('getPrice')->will($this->returnValue($price));

            $section = new \stdClass();
            $section->alias = $sale_data['section_id'] == 1 ? 'used' : 'new';
            $mockSale->expects($this->any())
                     ->method('getSection')->will($this->returnValue($section));
            $mockSale->expects($this->any())
                     ->method('getYear')->will($this->returnValue($sale_data['year']));

            $aImages = [];
            if (isset($sale_data['images'])) {
                $aImages = $this->_getImages($sale_data['images']);
            }

            $mockSale->expects($this->any())
                     ->method('getImages')->will($this->returnValue($aImages));

            $resultSalesData[] = $mockSale;
        }

        return $resultSalesData;
    }

    private function _getImages($images)
    {
        $img_sizes_to_replace = [
            '720x540' => '560x420',
            '300x225' => '280x210',
            '456x342' => '410x308',
            '300x225' => [
                '280x210' => '300x225',
                '205x154' => '300x225'
            ]
        ];

        $aImages = [];
        $oLib5Image = new \Image2(\Config::get('avatarnica_sales_images', 'octopus'));
        foreach ($images as $image) {
            $oImage = new \stdClass();
            $oLib5Image->setNodeId(null);

            if (isset($image['main'])) {
                $oImage->main = ($image['main'] == 1);
            }

            if (isset($image['is_suspect'])) {
                $oImage->is_suspect = ($image['is_suspect'] == 2);
            }

            if (!empty($image['name'])) {
                $oImage->urls = array_filter($oLib5Image->getUrlsById($image['name']));
                $replaced_urls = [];
                foreach ($oImage->urls as $size => $url) {
                    $new_size = isset($img_sizes_to_replace[$size])
                        ? $img_sizes_to_replace[$size]
                        : $size;

                    if (is_array($new_size)) {
                        foreach ($new_size as $new_size_alias => $old_size_alias) {
                            if (isset($oImage->urls[$old_size_alias])) {
                                $replaced_urls[$new_size_alias] = $oImage->urls[$old_size_alias];
                            }
                        }
                    } else {
                        $replaced_urls[$new_size] = $url;
                    }
                }
                $oImage->urls = $replaced_urls;

                $aImages[] = $oImage;
            }
        }
        return $aImages;
    }

}

