<?php
namespace tests\helpers;

class SearchTest extends \PHPUnit_Framework_TestCase
{
    /**
     * @param $params
     * @param $expected
     *
     * @dataProvider providerGetParams
     */
    public function testGetParams($params, $expected)
    {
        $storageFolders = $this->getMockBuilder('\octopus\storages\Db\Catalog\Folders')
            ->setMethods(['getIdsByCodes'])
            ->getMock();
        $storageFolders->expects($this->any())->method('getIdsByCodes')->will($this->returnValue([45,567,7654]));

        $storageMarks = $this->getMockBuilder('\octopus\storages\Db\Catalog\Marks')
                               ->setMethods(['cache', 'findAllByParams'])
                               ->getMock();
        $storageMarks->expects($this->any())->method('cache')->will($this->returnSelf());
        $storageMarks->expects($this->any())
            ->method('findAllByParams')
            ->will($this->returnValue([['id' => 12],['id' => 984]]));

        $helper = new \octopus\helpers\Search();
        $helper->setStorage('Db\Catalog\Folders', $storageFolders);
        $helper->setStorage('Db\Catalog\Marks', $storageMarks);
        $result = $helper->getParams($params);
        $this->assertEquals($expected, $result);
    }

    public function providerGetParams()
    {
        return [
            [
                [],
                []
            ],
            [
                [
                    'section' => 'new',
                    'custom' => null,
                    'price_from' => 500000,
                    'price_to' => 700000,
                    'mark_country_id' => '96',
                    'body_type' => 'sedan,hatchback_4_doors,hatchback_5_doors',
                    'transmission' => 'automatic',
                ],
                [
                    'section' => (object) [
                        'filter' => 'section_id = #section_id',
                        'params' => ['section_id' => 2]
                    ],
                    'price' => (object) [
                        'filter' => 'price >= #price_from AND price <= #price_to',
                        'params' => ['price_from' => 500000, 'price_to' => 700000]
                    ],
                    'body_type' => (object) [
                        'filter' => 'body_type IN (#body_type)',
                        'params' => ['body_type' => [120, 1324, 121]]
                    ],
                    'transmission' => (object) [
                        'filter' => 'gearbox IN (#gearbox)',
                        'params' => ['gearbox' => [1414]]
                    ],
                    'mark' => (object) [
                        'filter' => 'mark_id IN (#mark_id)',
                        'params' => ['mark_id' => [8,15,28,30,120,165,170,197,208,212,237,261,273,276,280,2394,12367128]]
                    ],
                ]
            ],
            [
                [
                    'custom' => 'cleared_by_customs',
                    'state' => 'onrun',
                    'price_to' => 480000,
                    'year_from' => 2011,
                    'km_age_to' => 50000,
                    'engine_type' => 'gasoline,diesel',
                    'mark_group' => 'domestic',
                    'image' => 'true',
                ],
                [
                    'custom' => (object) [
                        'filter' => 'custom IN (#custom)',
                        'params' => ['custom' => 1]
                    ],
                    'state' => (object) [
                        'filter' => 'state IN (#state)',
                        'params' => ['state' => 1]
                    ],
                    'price' => (object) [
                        'filter' => 'price <= #price_to',
                        'params' => ['price_from' => 0, 'price_to' => 480000]
                    ],
                    'year' => (object) [
                        'filter' => 'year >= #year_from',
                        'params' => ['year_from' => 2011, 'year_to' => 0]
                    ],
                    'run' => (object) [
                        'filter' => 'km_age <= #km_age_to',
                        'params' => ['km_age_from' => 0, 'km_age_to' => 50000]
                    ],
                    'engine_type' => (object) [
                        'filter' => 'engine_type IN (#engine_type)',
                        'params' => ['engine_type' => [1259,1260]]
                    ],
                    'mark' => (object) [
                        'filter' => 'mark_id IN (#mark_id)',
                        'params' => ['mark_id' => [232,288,292,296,297,299,311,316,336,895,1038,1132]]
                    ],
                    'image' => (object) [
                        'filter' => 'photo = #photo',
                        'params' => ['photo' => 1]
                    ],
                ]
            ],
            [
                [
                    'mark' => 'OPEL,BMW,MARK',
                    'model' => 'X5,ASTRA',
                    'generation' => 'I',
                    'year_from' => 2012,
                    'year_to' => 2012,
                ],
                [
                    'mark' => (object) [
                        'filter' => 'folder_id IN (#folder_id)',
                        'params' => ['folder_id' => [45,567,7654]]
                    ],
                    'year' => (object) [
                        'filter' => 'year = #year',
                        'params' => ['year' => 2012]
                    ],
                ]
            ]
        ];
    }

    /**
     * @param $sort_key
     * @param $expected
     *
     * @dataProvider providerGetSort
     */
    public function testGetSort($sort_key, $expected)
    {
        $helper = new \octopus\helpers\Search();
        $result = $helper->getSort($sort_key);
        $this->assertEquals($expected, $result);
    }

    public function providerGetSort()
    {
        return [
            [
                'somestr',
                []
            ],
            [
                'rand',
                [
                    'RAND()'
                ]
            ],
            [
                'create_date-desc',
                [
                    'create_date DESC'
                ]
            ]
        ];
    }
}
