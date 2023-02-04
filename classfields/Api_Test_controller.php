<?php

class Api_Test_controller extends \api2\classes\Controller
{
    /**
     * Проверяем добавление сообщений
     *
     * @return array
     */
    protected function API_testMessage()
    {
        $this->getMessageContainer()->addMessage('Это сообщение было добавлено через метод ' . __METHOD__ . '. ', \api2\classes\MessageContainer::TYPE_TEXT);

        return [
            'result' => [
                'message' => 'Сообщенька добавлена!'
            ],
        ];
    }

    /**
     * Тестовый метод, выбрасывает исключение
     *
     * @param array $params Параметры запроса
     *
     * @return array
     *
     * @throws Exception
     */
    protected function API_testException($params)
    {
        if (!isset($params['code'])) {
            return [
                'result' => [
                    'message' => 'Введите код исключения, напр. code=2'
                ],
            ];
        }

        throw new Exception('', Helpers_Array::iget($params, 'code'));
    }

    /**
     * Тестовый метод, выбрасывает исключение
     *
     * @param array $params Параметры запроса
     *
     * @return array
     */
    protected function API_testCaptcha($params)
    {
        //$oAntifraud = $this->getDi()->getResource('\antifraud\classes\Antifraud');
        //$checkAccess = $oAntifraud->checkAccess(\antifraud\classes\Antifraud::EMPTY_IP, \antifraud\classes\Antifraud::SID_TYPE_API, $params['uuid'], false);
        //if ($checkAccess['result'] == \antifraud\classes\Antifraud::RESULT_CAPTCHA_SHOW) {
        if (1) {
            return [
                'result' => [
                    'url'    => \Helpers_Url::l(\Config::get('form_api_url@captcha7') . '?uuid=' . $params['uuid'] . '&captcha_show=1'),
                    'url_ok' => \Helpers_Url::l(\Config::get('form_api_url_ok@captcha7')),
                ],
                'error' => [
                    'code'   => \api2\classes\Exception::NEED_CAPTCHA,
                ]
            ];
        } else {
            return [
                'result' => [
                    'message' => 'Ввод Captcha не требуется',
                ],
            ];
        }
    }

}