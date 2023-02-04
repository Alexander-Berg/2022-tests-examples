<?php
namespace lib5\tests\unit\classes\Email;

use \lib5\classes\Email\Backend\Sender\Message\TransactMessage;
use \lib5\classes\Email\Backend\Sender\Message\FreestyleTransactMessage;

/**
 * Тест класса Email\Transport
 */
class SenderMessaleTest extends \PHPUnit_Framework_TestCase
{

    const TEMPLATE = 'Users8.Welcome';
    const TO_EMAIL = 'noreply@auto.ru';
    const TO_NAME = 'lib5 test username';

    /**
     * Проверяем, что функция отправки работает.
     * Предполагаем, что дёрнется ручка отправки рассылки 'Users8.Welcome' на test.sender.yandex-team.ru
     *
     */

    protected function createTransactMessage()
    {
        return new TransactMessage(self::TEMPLATE);
    }

    protected function createFreestyleTransactMessage()
    {
        return new FreestyleTransactMessage(self::TEMPLATE);
    }

    protected function setMessageData($msg)
    {
        $msg->addAttachment('text1.txt', 'text/plain', 'Lorem ipsum 1');
        $msg->addAttachment('text2.txt', 'text/plain', 'Lorem ipsum 2');
        $msg->addHeaders(['X-AutoRu-Sendr-Message-Test-Header' => 'YES']);
        $msg->from_email = 'lavrinenko@yandex-team.ru';
        return $msg;
    }

    public function testSendTransactMessage()
    {
        $msg = $this->createTransactMessage();
        $this->setMessageData($msg);
        $msg->send([], self::TO_EMAIL, self::TO_NAME);
    }

    public function testSendTransactMessageWithPostData()
    {

        $filename1 = tempnam(sys_get_temp_dir(), 'sendrtst');
        $handle1 = fopen($filename1, "w");
        fwrite($handle1, "text1");
        fclose($handle1);

        $msg = $this->createTransactMessage();
        $msg->addPostFiles(["file1" => ["name" => "files1.txt", "type" => "text/plain", "tmp_name" => $filename1]]);
        $msg->send([], self::TO_EMAIL, self::TO_NAME);


        $msg = $this->createTransactMessage();
        $msg->addAttachmentLegacy(["name" => "files1.txt", "type" => "text/plain", "path" => $filename1]);
        $msg->send([], self::TO_EMAIL, self::TO_NAME);

        $msg = $this->createTransactMessage();
        $msg->addAttachmentLegacy(["name" => "files1.txt", "type" => "text/plain", "content" => "content1"]);
        $msg->send([], self::TO_EMAIL, self::TO_NAME);



        unlink($filename1);
    }


    public function testSendFreestyleTransactMessage()
    {
        $msg = $this->createFreestyleTransactMessage();
        $this->setMessageData($msg);
        $msg->subject = 'Это freestyle transact message';
        $msg->html = '<html>Это freestyle transact message body</html>html>';
        $msg->send([], self::TO_EMAIL, self::TO_NAME);
    }
}
