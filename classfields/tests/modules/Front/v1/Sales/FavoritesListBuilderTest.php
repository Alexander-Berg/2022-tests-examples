<?php
/**
 * Created by PhpStorm.
 * User: ichered
 * Date: 09.10.15
 * Time: 11:52
 */

namespace tests\modules\Front\v1\Sales;


use octopus\classes\Collection;
use octopus\entities\Cars;
use octopus\entities\Folders;
use octopus\entities\Marks;
use octopus\modules\Front\v1\Sales\FavoritesListBuilder;

class FavoritesListBuilderTest extends \PHPUnit_Framework_TestCase
{
    /**
     * Тестируем метод build
     *
     * @param mixed $expected      С чем сравнивать
     * @param array $return_values Возвращаемые значения репозиториев, хранилищ и бизнесов
     *
     * @dataProvider providerTestBuild
     *
     * @return void
     */
    public function testBuild($expected, $collections)
    {
        $builder = new FavoritesListBuilder();
        if (isset($collections['sales'])) {
            $builder->setSales($collections['sales']);
        }
        if (isset($collections['folders'])) {
            $builder->setFolders($collections['folders']);
        }
        if (isset($collections['marks'])) {
            $builder->setMarks($collections['marks']);
        }
        $this->assertEquals($expected, $builder->build());
    }

    public function providerTestBuild()
    {
        $oCars = $this->getMockBuilder('\octopus\entities\Cars')->setMethods(['getPrice'])->getMock();
        $oCars->expects($this->any())->method('getPrice')->will($this->returnValue($this->getFavoritesResult(4)->price));
        $collectionSales = new Collection($oCars);
        $collectionSales->fill($this->getSales());

        $oFolders = new Folders();
        $collectionFolders = new Collection($oFolders);
        $collectionFolders->fill($this->getFolders());

        $oMarks = new Marks();
        $collectionMarks = new Collection($oMarks);
        $collectionMarks->fill($this->getMarks());
        return [
            [
                [],
                [],
            ],
            [
                [
                    0 => $this->getFavoritesResult(4, false, false),
                ],
                [
                    'sales' => $collectionSales
                ]
            ],
            [
                [
                    0 => $this->getFavoritesResult(4),
                ],
                [
                    'sales' => $collectionSales,
                    'folders' => $collectionFolders,
                    'marks' => $collectionMarks
                ]
            ]
        ];
    }

    protected function getFavoritesResult($id, $with_mark = true, $with_folder = true)
    {
        $result = new \stdClass();
        $sale = $this->getSales()[$id];
        if ($with_mark) {
            $mark = $this->getMarks()[$sale['mark_id']];
            $result->mark = new \stdClass();
            $result->mark->alias = $mark['alias'];
            $result->mark->name = $mark['name'];
        }
        if ($with_folder) {
            $generation = $this->getFolders()[$sale['folder_id']];
            $folder = $this->getFolders()[$generation['parent_id']];
            $result->model = new \stdClass();
            $result->model->alias = $folder['alias'];
            $result->model->name = $folder['name'];
            $result->generation = new \stdClass();
            $result->generation->alias = $generation['alias'];
            $result->generation->name = $generation['name'];
        }

        $result->id = $sale['id'];
        $result->hash = $sale['hash'];
        $result->currency = 'RUR';
        $result->year = $sale['year'];
        $result->price = new \stdClass();
        $result->price->RUR = 970000.00;
        $result->price->USD = 100.00;
        $result->price->EUR = 100.00;
        $result->body_type = new \stdClass();
        $result->body_type->alias = 'minivan';
        $result->body_type->code = 'minivan';
        $result->section = 'used';
        return $result;
    }

    protected function getSales()
    {
        $sales = [
            4 => [
                'id' => '4',
                'hash' => 'test',
                'description' => 'testdescription',
                'price' => '970000.00',
                'year' => 2012,
                'run' => '19800',
                'mark_id' => '109',
                'folder_id' => '51004',
                'modification_id' => '76235',
                'body_type' => '1368',
                'engine_type' => '1260',
                'engine_volume' => '2497',
                'engine_power' => '170',
                'metallic' => '1',
                'video' => null,
                'currency' => 'RUR',
                'drive' => '181',
                'gearbox' => '1414',
                'category_id' => '15',
                'section_id' => '1',
                'color' => '4',
                'wheel' => '1',
                'state' => '1',
                'expire_date' => '1244709296000',
                'update_date' => '1244709296000',
                'create_date' => '1244709296000'
            ],
        ];
        return $sales;
    }

    protected function getMarks()
    {
        $marks = [
            109 => [
                'id' => '109',
                'name' => 'mark1',
                'alias' => 'mark1',
                'cyrillic_name' => 'марка1',
                'ya_code' => 'MARK1',
                'is_popular' => 1,
            ],
        ];
        return $marks;
    }

    protected function getFolders()
    {
        $folders = [
            1 => [
                'id' => '1',
                'name' => 'folder1',
                'alias' => 'folder1',
                'level' => 1,
                'cyrillic_name' => 'модель1',
                'parent_id' => 0,
                'ya_code' => 'MODEL1',
            ],
            51004 => [
                'id' => '51004',
                'name' => 'generation1',
                'alias' => 'generation1',
                'level' => 2,
                'cyrillic_name' => 'поколение1',
                'parent_id' => '1',
                'ya_code' => 'GENERATION1',
            ],
        ];
        return $folders;
    }
}