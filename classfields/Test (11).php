<?php
namespace subscribe7\classes\Subscribe\Handlers\Recipients;

use \subscribe7\classes\iHandler;

/**
 * Class Test
 *
 * @package subscribe7\classes\Subscribe\Handlers\Recipients
 */
class Test implements iHandler
{
    use \lib5\Traits\DI;

    /**
     * Тестовый метод
     *
     * @param array $job_params Данные джобы
     *
     * @throws \Lib5Exception
     *
     * @return array
     */
    public function get(array $job_params)
    {
        $recipient = [
            'denis@auto.ru',
            [
                'recipient' => 'slider@auto.ru',
                'data'      => ['var1' => '111', 'var2' => '222'],
            ],
        ];

        return $recipient;
    }
}
