<?php
namespace tests\modules\Front\v1;
use octopus\classes\Collection;
use octopus\entities\Folders;
use octopus\entities\Marks;
use octopus\entities\Reviews;

/**
 * Created by PhpStorm.
 * User: ichered
 * Date: 23.09.15
 * Time: 15:13
 */
class ReviewsListTest extends \PHPUnit_Framework_TestCase
{
    /**
     * Тестируем метод request
     *
     * @param $params        array Параметры вызова
     * @param $expected      mixed Предполагаемый ответ метода
     * @param $return_values array Массив устанавливаемых стабов
     *
     * @dataProvider providerTestRequest
     *
     * return @void
     */
    public function testRequest($params, $expected, $return_values = array())
    {
        $reviewsList = $this->getMockBuilder('\octopus\modules\Front\v1\ReviewsList')
            ->setMethods(['cache'])->getMock();
        $reviewsList->expects($this->any())->method('cache')
            ->will($this->returnValue($reviewsList));

        $reviewsRepository = $this->getMockBuilder('\octopus\repositories\Reviews')
            ->setMethods(['get', 'getTotalAll'])->getMock();
        $reviewsRepository->expects($this->any())->method('get')
            ->will($this->returnValue($return_values['repositories']['Reviews']['getTopRated']));

        $marksRepository = $this->getMockBuilder('\octopus\repositories\Marks')
            ->setMethods(['get'])->getMock();
        $marksRepository->expects($this->any())->method('get')
            ->will($this->returnValue($return_values['repositories']['Marks']['get']));

        $foldersRepository = $this->getMockBuilder('\octopus\repositories\Folders')
            ->setMethods(['getByFolderId'])->getMock();
        $foldersRepository->expects($this->any())->method('getByFolderId')
            ->will($this->returnValue($return_values['repositories']['Folders']['getByFolderId']));

        $reviewsList->setRepository('Reviews', $reviewsRepository);
        $reviewsList->setRepository('Marks', $marksRepository);
        $reviewsList->setRepository('Folders', $foldersRepository);

        $this->assertEquals($expected, $reviewsList->request($params));
    }

    /**
     * Провайдер для метода testRequest
     *
     * @return array
     */
    public function providerTestRequest()
    {
        $oReview = new Reviews();
        $oMark = new Marks();
        $oFolders = new Folders();
        $ReviewsCollection0 = new Collection($oReview);
        $ReviewsCollection0->fill($this->getReviews());
        $ReviewsCollection0->setTotalAll(2);
        $ReviewsCollection1 = new Collection($oReview);
        $ReviewsCollection1->fill([current($this->getReviews())]);
        $ReviewsCollection1->setTotalAll(1);
        $ReviewsCollection2 = new Collection($oReview);
        $ReviewsCollection2->fill([end($this->getReviews())]);
        $ReviewsCollection2->setTotalAll(1);
        $MarksCollection = new Collection($oMark);
        $MarksCollection->fill($this->getMarks());
        $FoldersCollection = new Collection($oFolders);
        $FoldersCollection->fill($this->getFolders());

        return [
            [
                [],
                [
                    'result' => [
                        'items' => [
                            0 => $this->getReviewsResult(4039289),
                            1 => $this->getReviewsResult(4039383),
                        ],
                        'count' => 2,
                    ]
                ],
                [
                    'repositories' => [
                        'Reviews' => [
                            'getTopRated' => $ReviewsCollection0
                        ],
                        'Marks' => [
                            'get' => $MarksCollection
                        ],
                        'Folders' => [
                            'getByFolderId' => $FoldersCollection
                        ],
                    ],
                    'storages' => [
                        'Db' => [
                            'Reviews' => [
                                'getTopMarkRateTotalCount' => 2
                            ]
                        ]
                    ]
                ]
            ],
            [
                ['limit' => -1],
                [
                    'result' => [
                        'items' => [
                            0 => $this->getReviewsResult(4039289),
                            1 => $this->getReviewsResult(4039383),
                        ],
                        'count' => 2,
                    ]
                ],
                [
                    'repositories' => [
                        'Reviews' => [
                            'getTopRated' => $ReviewsCollection0
                        ],
                        'Marks' => [
                            'get' => $MarksCollection
                        ],
                        'Folders' => [
                            'getByFolderId' => $FoldersCollection
                        ],
                    ],
                    'storages' => [
                        'Db' => [
                            'Reviews' => [
                                'getTopMarkRateTotalCount' => 2
                            ]
                        ]
                    ]
                ]
            ],
            [
                ['limit' => 'dasfsddsfsdfsd'],
                [
                    'result' => [
                        'items' => [
                            0 => $this->getReviewsResult(4039289),
                            1 => $this->getReviewsResult(4039383),
                        ],
                        'count' => 2,
                    ]
                ],
                [
                    'repositories' => [
                        'Reviews' => [
                            'getTopRated' => $ReviewsCollection0
                        ],
                        'Marks' => [
                            'get' => $MarksCollection
                        ],
                        'Folders' => [
                            'getByFolderId' => $FoldersCollection
                        ],
                    ],
                    'storages' => [
                        'Db' => [
                            'Reviews' => [
                                'getTopMarkRateTotalCount' => 2
                            ]
                        ]
                    ]
                ]
            ],
            [
                ['limit' => 1],
                [
                    'result' => [
                        'items' => [
                            0 => $this->getReviewsResult(4039289),
                        ],
                        'count' => 1,
                    ]
                ],
                [
                    'repositories' => [
                        'Reviews' => [
                            'getTopRated' => $ReviewsCollection1
                        ],
                        'Marks' => [
                            'get' => $MarksCollection
                        ],
                        'Folders' => [
                            'getByFolderId' => $FoldersCollection
                        ],
                    ],
                    'storages' => [
                        'Db' => [
                            'Reviews' => [
                                'getTopMarkRateTotalCount' => 1
                            ]
                        ]
                    ]
                ]
            ],
            [
                ['limit' => 1, 'page' => 1],
                [
                    'result' => [
                        'items' => [
                            0 => $this->getReviewsResult(4039383),
                        ],
                        'count' => 1,
                    ]
                ],
                [
                    'repositories' => [
                        'Reviews' => [
                            'getTopRated' => $ReviewsCollection2
                        ],
                        'Marks' => [
                            'get' => $MarksCollection
                        ],
                        'Folders' => [
                            'getByFolderId' => $FoldersCollection
                        ],
                    ],
                    'storages' => [
                        'Db' => [
                            'Reviews' => [
                                'getTopMarkRateTotalCount' => 1
                            ]
                        ]
                    ]
                ]
            ],
            [
                [
                    'limit' => 10000,
                ],
                [
                    'result' => [
                        'items' => [
                            0 => $this->getReviewsResult(4039289),
                            1 => $this->getReviewsResult(4039383),
                        ],
                        'count' => 2,
                    ]
                ],
                [
                    'repositories' => [
                        'Reviews' => [
                            'getTopRated' => $ReviewsCollection0
                        ],
                        'Marks' => [
                            'get' => $MarksCollection
                        ],
                        'Folders' => [
                            'getByFolderId' => $FoldersCollection
                        ],
                    ],
                    'storages' => [
                        'Db' => [
                            'Reviews' => [
                                'getTopMarkRateTotalCount' => 2
                            ]
                        ]
                    ]
                ]
            ],
        ];
    }

    protected function getReviews()
    {
        $reviews = [
            4039289 => [
                'id' => '4039289',
                'mark_id' => '134',
                'model_id' => '48902',
                'generation_id' => '9554503',
                'title' => 'test',
                'rating' => '4.4',
                'image' => '12345-0cd015d946ddf72ecb08c86cd83cad92.jpg'
            ],
            4039383 => [
                'id' => '4039383',
                'mark_id' => '273',
                'model_id' => '12197',
                'generation_id' => '12204',
                'title' => 'test2',
                'rating' => '4.8',
                'image' => '12345-0cd015d946ddf72ecb08c86cd83cad92.jpg'
            ]
        ];
        return $reviews;
    }

    protected function getMarks()
    {
        $marks = [
            134 => [
                'id' => '134',
                'name' => 'mark1',
                'alias' => 'mark1',
                'cyrillic_name' => 'марка1',
                'ya_code' => 'MARK1',
                'is_popular' => 1,
            ],
            273 => [
                'id' => '273',
                'name' => 'mark2',
                'alias' => 'mark2',
                'cyrillic_name' => 'марка2',
                'ya_code' => 'MARK2',
                'is_popular' => 1,
            ],
        ];
        return $marks;
    }

    protected function getFolders()
    {
        $folders = [
            48902 => [
                'id' => '48902',
                'name' => 'folder1',
                'alias' => 'folder1',
                'level' => 1,
                'cyrillic_name' => 'модель1',
                'parent_id' => 0,
                'ya_code' => 'MODEL1',
            ],
            9554503 => [
                'id' => '9554503',
                'name' => 'generation1',
                'alias' => 'generation1',
                'level' => 2,
                'cyrillic_name' => 'поколение1',
                'parent_id' => '48902',
                'ya_code' => 'GENERATION1',
            ],
            12197 => [
                'id' => '12197',
                'name' => 'folder2',
                'alias' => 'folder2',
                'level' => 1,
                'cyrillic_name' => 'модель2',
                'parent_id' => 0,
                'ya_code' => 'MODEL2',
            ],
            12204 => [
                'id' => '12204',
                'name' => 'generation2',
                'alias' => 'generation2',
                'level' => 2,
                'cyrillic_name' => 'поколение2',
                'parent_id' => '12197',
                'ya_code' => 'GENERATION2',
            ],
        ];
        return $folders;
    }

    protected function getReviewsResult($id)
    {
        $review = $this->getReviews()[$id];
        $mark = $this->getMarks()[$review['mark_id']];
        $folder = $this->getFolders()[$review['model_id']];
        $generation = $this->getFolders()[$review['generation_id']];
        $result = new \stdClass();
        $result->id = $review['id'];
        $result->mark = [
            'alias' => $mark['alias'],
            'name' => $mark['name']
        ];
        $result->model = [
            'alias' => $folder['alias'],
            'name' => $folder['name']
        ];
        $result->generation = [
            'alias' => $generation['alias'],
            'name' => $generation['name']
        ];
        $result->title = $review['title'];
        $result->rating = round(floatval($review['rating']), 1);
        $result->image = 'https://images.mds-proxy.dev.autoru.yandex.net/get-autoru/12345/0cd015d946ddf72ecb08c86cd83cad92/155x87';
        $result->images = [
            'full' => 'https://images.mds-proxy.dev.autoru.yandex.net/get-autoru/12345/0cd015d946ddf72ecb08c86cd83cad92/full',
            '140x105' => $result->image,
            '280x210' => 'https://images.mds-proxy.dev.autoru.yandex.net/get-autoru/12345/0cd015d946ddf72ecb08c86cd83cad92/320x240',
            'thumb_s' => 'https://images.mds-proxy.dev.autoru.yandex.net/get-autoru/12345/0cd015d946ddf72ecb08c86cd83cad92/thumb_s',
            'thumb_s_2x' => 'https://images.mds-proxy.dev.autoru.yandex.net/get-autoru/12345/0cd015d946ddf72ecb08c86cd83cad92/thumb_s_2x',
            'thumb_m' => 'https://images.mds-proxy.dev.autoru.yandex.net/get-autoru/12345/0cd015d946ddf72ecb08c86cd83cad92/thumb_m',
            'thumb_m_2x' => 'https://images.mds-proxy.dev.autoru.yandex.net/get-autoru/12345/0cd015d946ddf72ecb08c86cd83cad92/thumb_m_2x'
        ];
        return $result;
    }
}
