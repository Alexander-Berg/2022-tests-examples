<?php
namespace subscribe7\classes\Subscribe\Handlers\RecipientsData;

use \subscribe7\classes\iHandler;

class Test implements iHandler {
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
        $result = [
            'var' => __CLASS__ . ' - ' . time(),

            /** E-mail на который будет отправлено письмо, можно
             * использовать для получения user_id и вставки например username */
            'recipient' => $job_params['recipient'],
        ];

        return $result;
    }
}
