<?php
namespace tests\modules\Front\v1\Sales;

class ComplaintsAddTest extends \PHPUnit_Framework_TestCase
{
    /**
     * @param array $params Входные данные
     *
     * @dataProvider providerInvalidDataException
     *
     * @expectedException \octopus\classes\Exception
     * @expectedExceptionMessage Ошибка входных данных
     */
    public function testInvalidDataException($params)
    {
        $module = new \octopus\modules\Front\v1\Sales\ComplaintsAdd();
        $module->request($params);
    }

    public function providerInvalidDataException()
    {
        return [
            [
                []
            ],
            [
                [
                    'sale_id' => 4,
                    'text' => 'some text',
                    'email' => 'wrong_email'
                ]
            ]
        ];
    }

    /**
     * @param array $params Входные данные
     *
     * @dataProvider providerMissingSalesException
     *
     * @expectedException \octopus\classes\Exception
     * @expectedExceptionMessage Объявление не найдено
     */
    public function testMissingSalesException($params)
    {
        $module = new \octopus\modules\Front\v1\Sales\ComplaintsAdd();

        $collectionSales = $this->getMockBuilder('\octopus\classes\Collection')
                                ->setMethods(['getTotal'])
                                ->getMock();
        $collectionSales->expects($this->any())->method('getTotal')->will($this->returnValue(0));

        $repositoryCars = $this->getMockBuilder('\octopus\repositories\Cars')
                               ->setMethods(['get'])
                               ->getMock();
        $repositoryCars->expects($this->any())->method('get')->will($this->returnValue($collectionSales));
        $module->setRepository('Cars', $repositoryCars);

        $module->request($params);
    }

    public function providerMissingSalesException()
    {
        return [
            [
                ['sale_id' => 5, 'text' => 'some text']
            ]
        ];
    }

    /**
     * @param array $params   Входные данные
     * @param array $expected Ожидаемый результат
     * @param mixed $user_id  User_id
     *
     * @dataProvider providerRequest
     */
    public function testRequest($params, $expected, $user_id = false)
    {
        $module = new \octopus\modules\Front\v1\Sales\ComplaintsAdd();

        $repositoryComplaints = $this->getMockBuilder('\octopus\repositories\Cars\Complaints')
                       ->setMethods(['add', 'make'])
                       ->getMock();
        $repositoryComplaints->expects($this->any())->method('add')->will($this->returnValue(true));
        $repositoryComplaints->expects($this->any())->method('make')->will($this->returnValue([]));

        $collectionSales = $this->getMockBuilder('\octopus\classes\Collection')
                                ->setMethods(['getTotal'])
                                ->getMock();
        $collectionSales->expects($this->any())->method('getTotal')->will($this->returnValue(1));

        $repositoryCars = $this->getMockBuilder('\octopus\repositories\Cars')
                    ->setMethods(['get'])
                    ->getMock();
        $repositoryCars->expects($this->any())->method('get')->will($this->returnValue($collectionSales));

        $entityUser = $this->getMockBuilder('\octopus\entities\Users')
                                ->setMethods(['getEmail'])
                                ->getMock();
        $entityUser->expects($this->any())->method('getEmail')->will($this->returnValue(['user@auto.ru']));

        $collectionUsers = $this->getMockBuilder('\octopus\classes\Collection')
                         ->setMethods(['get'])
                         ->getMock();
        $collectionUsers->expects($this->any())->method('get')->will($this->returnValue($entityUser));

        $repositoryUsers = $this->getMockBuilder('\octopus\repositories\Users')
                                ->setMethods(['getUsers'])
                                ->getMock();
        $repositoryUsers->expects($this->any())->method('getUsers')->will($this->returnValue($collectionUsers));

        $mockUser = $this->getMockBuilder('\lib5\classes\Current\User')
                    ->setMethods(['getId'])
                    ->getMock();
        $mockUser->expects($this->any())->method('getId')->will($this->returnValue([$user_id]));

        $acl = $this->getMockBuilder('\lib5\classes\Acl')
                    ->setMethods(['getInstance', 'getUser'])
                    ->getMock();
        $acl->expects($this->any())->method('getInstance')->will($this->returnSelf());
        $acl->expects($this->any())->method('getUser')->will($this->returnValue($mockUser));

        $module->setRepository('Cars\Complaints', $repositoryComplaints);
        $module->setRepository('Users', $repositoryUsers);
        $module->setRepository('Cars', $repositoryCars);
        $module->setResource('\lib5\classes\Acl', $acl);

        $result = $module->request($params);
        $this->assertEquals($expected, $result);
    }

    public function providerRequest()
    {
        return [
            [
                [
                    'sale_id' => 4,
                    'text' => 'some text'
                ],
                [
                    'result' => [
                        'status' => true
                    ]
                ],
                1
            ],
            [
                [
                    'sale_id' => 4,
                    'text' => 'some text',
                    'email' => 'test@auto.ru'
                ],
                [
                    'result' => [
                        'status' => true
                    ]
                ]
            ],
        ];
    }
}
