<?php
/**
 * Created by PhpStorm.
 * User: ichered
 * Date: 09.10.15
 * Time: 11:52
 */

namespace tests\modules\Front\v1\Sales;


use octopus\classes\Collection;
use octopus\classes\CurrencyConverter;
use octopus\entities\Cars;
use octopus\entities\Folders;
use octopus\entities\Marks;

class FavoritesListTest extends \PHPUnit_Framework_TestCase
{
    /**
     * Тестируем метод request
     *
     * @param mixed $expected      С чем сравнивать
     * @param array $return_values Возвращаемые значения репозиториев, хранилищ и бизнесов
     *
     * @dataProvider providerTestRequest
     *
     * @return void
     */
    public function testRequest($expected, $return_values = array())
    {
        $favoritesList = new \octopus\modules\Front\v1\Sales\FavoritesList();

        $favoritesBusiness = $this->getMockBuilder('\octopus\business\Favorites')
            ->setMethods(['getFavoritesKey'])->getMock();
        $favoritesBusiness->expects($this->any())->method('getFavoritesKey')
            ->will($this->returnValue('test'));

        $favoritesRepository = $this->getMockBuilder('\octopus\repositories\Cars\Favorites')
            ->setMethods(['getFavoritesList'])->getMock();
        $favoritesRepository->expects($this->any())->method('getFavoritesList')
            ->will($this->returnValue($return_values['repositories']['Cars\Favorites']['getFavoritesList']));

        $marksRepository = $this->getMockBuilder('\octopus\repositories\Marks')
            ->setMethods(['get'])->getMock();
        $marksRepository->expects($this->any())->method('get')
            ->will($this->returnValue($return_values['repositories']['Marks']['get']));

        $foldersRepository = $this->getMockBuilder('\octopus\repositories\Folders')
            ->setMethods(['getByFolderId'])->getMock();
        $foldersRepository->expects($this->any())->method('getByFolderId')
            ->will($this->returnValue($return_values['repositories']['Folders']['getByFolderId']));

        if (isset($return_values['resources']['FavoritesListBuilder']['build'])) {
            $favoritesListBuilder = $this->getMockBuilder('\octopus\modules\Front\v1\Sales\FavoritesListBuilder')
                ->setMethods(['build'])->getMock();
            $favoritesListBuilder->expects($this->any())->method('build')
                ->will($this->returnValue($return_values['resources']['FavoritesListBuilder']['build']));
            $favoritesList->setResource('\octopus\modules\Front\v1\Sales\FavoritesListBuilder', $favoritesListBuilder);
        }

        $favoritesList->setBusiness('Favorites', $favoritesBusiness);
        $favoritesList->setRepository('Cars\Favorites', $favoritesRepository);
        $favoritesList->setRepository('Marks', $marksRepository);
        $favoritesList->setRepository('Folders', $foldersRepository);
        $this->assertEquals($expected, $favoritesList->request());
    }

    public function providerTestRequest()
    {
        $oCars = new Cars();
        $oMarks = new Marks();
        $oFolders = new Folders();
        $FavoritesCollection0 = new Collection($oCars);
        $FavoritesCollection0->fill($this->getSales());
        $MarksCollection = new Collection($oMarks);
        $MarksCollection->fill($this->getMarks());
        $FoldersCollection = new Collection($oFolders);
        $FoldersCollection->fill($this->getFolders());
        return [
            [
                [
                    'result' => [
                        'items' => [
                            0 => $this->getFavoritesResult(4),
                        ],
                        'count' => 1,
                    ]
                ],
                [
                    'repositories' => [
                        'Cars\Favorites' => [
                            'getFavoritesList' => $FavoritesCollection0,
                        ],
                        'Marks' => [
                            'get' => $MarksCollection,
                        ],
                        'Folders' => [
                            'getByFolderId' => $FoldersCollection
                        ]
                    ],
                    'resources' => [
                        'FavoritesListBuilder' => [
                            'build' => [$this->getFavoritesResult(4)]
                        ]
                    ]
                ]
            ]
        ];
    }

    protected function getFavoritesResult($id)
    {
        $sale = $this->getSales()[$id];
        $mark = $this->getMarks()[$sale['mark_id']];
        $generation = $this->getFolders()[$sale['folder_id']];
        $folder = $this->getFolders()[$generation['parent_id']];
        $result = new \stdClass();
        $result->id = $sale['id'];
        $result->hash = $sale['hash'];
        $result->currency = 'RUR';
        $result->year = $sale['year'];
        $result->mark = new \stdClass();
        $result->mark->alias = $mark['alias'];
        $result->mark->name = $mark['name'];
        $result->model = new \stdClass();
        $result->model->alias = $folder['alias'];
        $result->model->name = $folder['name'];
        $result->price = new \stdClass();
        $result->price->RUR = 970000.00;
        $result->price->USD = 100.00;
        $result->price->EUR = 200.00;
        $result->body_type = new \stdClass();
        $result->body_type->alias = 'minivan';
        $result->body_type->code = 'minivan';
        $result->generation = new \stdClass();
        $result->generation->alias = $generation['alias'];
        $result->generation->name = $generation['name'];
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