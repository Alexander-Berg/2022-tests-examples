<?php
/**
 * Test.php
 *
 * Date: 30.03.16 10:19
 */

namespace tests\classes;

use \tests\stubs\classes\DynamicCriteriaStub;

class DynamicCriteriaTest extends \PHPUnit_Framework_TestCase
{
    const CLASS_ATTRIBUTE_CRITERIA = 'criteria';

    /**
     * @var DynamicCriteriaStub
     */
    protected $obj;

    /**
     * @param $filter
     * @param $valid_fields
     * @param $expected
     *
     * @dataProvider providerTestSet
     */
    public function testSet($filter, $valid_fields, $expected)
    {
        $this->obj->setValidFieldsDirectly($valid_fields);

        $this->obj->set($filter);

        self::assertAttributeEquals($expected, self::CLASS_ATTRIBUTE_CRITERIA, $this->obj, __LINE__);
    }

    public function providerTestSet()
    {
        return [
            [
                [
                    'mark_id' => 5,
                    'folder_id' => 7,
                    '@#*($&@#(*$@&' => 4,
                    'status' => 0,
                    'prop' =>  '3',
                ],

                [
                    'mark_id'   => [],
                    'folder_id' => ['non-empty'],
                    'status'    => []
                ],

                [
                    'mark_id'   => 5,
                    'folder_id' => 7,
                    'status'    => 0
                ]
            ]
        ];
    }


    /**
     * @param $attempt
     *
     * @dataProvider providerTestMagic__setWithException
     *
     * @expectedException \octopus\classes\Exception
     */
    public function testMagic__setWithException($attempt)
    {
        $valid_fields = [
            'catalog7_mark_id' => [],
            'folder_id' => [],
        ];

        $this->obj->setValidFieldsDirectly($valid_fields);

        $this->obj->{$attempt} = 0;
    }

    public function providerTestMagic__setWithException()
    {
        return [
            ['mark_id'],
            ['catalog7'],
            ['folder'],
        ];
    }


    /**
     * @param $attempt
     *
     * @dataProvider providerTestMagic__set
     */
    public function testMagic__set($attempt)
    {
        $valid_fields = [
            'catalog7_mark_id' => [],
            'folder_id' => [],
            'JFDI3j2dm-i_kewf' => []
        ];

        $this->obj->setValidFieldsDirectly($valid_fields);

        foreach ($attempt as $key => $item) {
            $this->obj->{$key} = $item;
        }

        self::assertAttributeSame($attempt, self::CLASS_ATTRIBUTE_CRITERIA, $this->obj, __LINE__);

    }

    /**
     * Провайдер содержит как единичные значениия так и массивы фильтров
     *
     * @return array
     */
    public function providerTestMagic__set()
    {
        return  [
            [['JFDI3j2dm-i_kewf' => '1']],
            [['catalog7_mark_id' => 0]],
            [['folder_id'        => 'String']],
            [
                [
                    'JFDI3j2dm-i_kewf' => 'zeal',
                    'folder_id'        => 1
                ]
            ]
        ];
    }

    /**
     * @param $valid_fields
     * @param $filter
     * @param $expected_sql_pattern
     * @param $expected_params
     * @param $prepend_and
     *
     * @dataProvider providerTestGet
     */
    public function testGet($valid_fields, $filter, $expected_sql_pattern, $expected_params, $prepend_and)
    {
        $this->obj->setValidFieldsDirectly($valid_fields);

        $this->obj->set($filter);

        $this->obj->setPrependAnd($prepend_and);

        $actual = $this->obj->get();

        self::assertSame($expected_params, $actual['params'], __LINE__);
        self::assertRegExp($expected_sql_pattern, $actual['sql_part'], __LINE__);
    }

    public function providerTestGet()
    {
        return [
            [
                [
                    'mark_id'   => ['ALTERNATIVES' => ['catalog7_mark_id']],
                    'folder_id' => ['ALTERNATIVES' => ['model_id', 'catalog7_model_id']],
                    'year'      => [],
                    'status'    => []
                ],

                [
                    'fihefuf_fjerwf' => 1123,
                    'folder_id' => '118',
                    'year'   => 2004,
                    'status' => 0,
                    'geo_id' => '1156'
                ],

                '~\(\s*                                 # (
                        `folder_id`=\#folder_id\s+OR\s+   #   folder_id=#folder_id OR
                        `model_id`=\#folder_id\s+OR\s+    #   model_id=#folder_id  OR
                        `catalog7_model_id`=\#folder_id   #   catalog7_model_id=#folder_id OR
                    \)\s+AND\s+                         # )
                    `year`=\#year\s+AND\s+                # year=#year
                    `status`=\#status                     # status=#status
                    \s*~x',

                [
                    'folder_id' => '118',
                    'year' => 2004,
                    'status' => 0
                ],

                false
            ],

            [

                [
                    'mark_id'   => ['ALTERNATIVES' => ['catalog7_mark_id']],
                    'folder_id' => ['ALTERNATIVES' => ['catalog7_model_id']],
                    'body_type' => []
                ],

                [
                    'mark_id'   => 50,
                    'folder_id' => 675,
                    'body_type' => 'g_cabrio'
                ],

                '~\s+AND\s+\(\s*                    # \ AND (
                    `mark_id`=\#mark_id\s+OR\s+       #   mark_id=#mark_id OR
                    `catalog7_mark_id`=\#mark_id      #   catalog7_mark_id=#mark_id
                \s*\)                               # )
                    \s+AND\s+                       # AND
                \(\s*                               # (
                    `folder_id`=\#folder_id\s+OR\s+   #   folder_id=#folder_id OR
                    `catalog7_model_id`=\#folder_id   #   catalog7_model_id=#folder_id
                \s*\)                               # )
                    \s+AND\s+                       # AND
                `body_type`=\#body_type               # body_type=#body_type
                    ~x',

                [
                    'mark_id'   => 50,
                    'folder_id' => 675,
                    'body_type' => 'g_cabrio'
                ],

                true
            ]
        ];
    }

    /**
     * Когда критерия не задана
     *
     * @expectedException \octopus\classes\Exception
     * @expectedExceptionMessage Search criteria is not set!
     *
     * @return void
     */
    public function testGetWhenCriteriaIsntSet()
    {
        $this->obj->get();
    }

    /**
     * Для надежности
     *
     * @return void
     */
    public function setUp()
    {
        parent::setUp();

        $this->obj = new DynamicCriteriaStub();
    }

}
