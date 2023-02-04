<?php
namespace lib5\tests\unit\classes\Email;

use \lib5\classes\Email\Transport,
    \lib5\classes\Email\Exception;

/**
 * Тест класса Email\Transport
 */
class SenderTransportTest extends \PHPUnit_Framework_TestCase
{

    const TEMPLATE = 'Users8.Welcome';
    const TO_EMAIL = 'noreply@auto.ru';
    const TO_NAME = 'lib5 test username';

    /**
     * Проверяем, что функция отправки работает.
     * Предполагаем, что дёрнется ручка отправки рассылки 'Users8.Welcome' на test.sender.yandex-team.ru
     *
     */
    public function testSendMail()
    {
        \lib5\classes\Email\Transport::send(
            self::TEMPLATE,
            self::TO_EMAIL,
            self::TO_NAME,
            []
        );
    }
}
