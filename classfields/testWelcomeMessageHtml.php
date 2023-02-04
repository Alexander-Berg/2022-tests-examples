#!/usr/bin/php
<?php
/**
 * Добавляет html-сообщение тестовому пользователю
 * Для https://st.yandex-team.ru/AUTORUAPI-1666
 *
 * Где искать темплейты:
 * team-php-01-iva6:~$ mysql -u auto -p --port=3308 -h mysql-02-sas.test.vertis.yandex.net message
 * mysql> select * from messages_templates where alias='Users8.Welcome';
 *
 * mysql> alter table messages.messages_templates add column is_html tinyint(1) default 0;
 *
 */


set_time_limit(0);
ini_set('memory_limit', -1);

$_GET['event_log'] = 0;
$_GET['test_debug'] = 0;

$shell_script = __FILE__;
require_once(dirname(__FILE__) . '/../../lib5/common.php');


$TEMPLATE_TITLE = 'Подтверждение добавления объявления';

$TEMPLATE_NAME = 'confirm_mail';

$TEMPLATE_TEXT = '<p>Всё готово — Ваше объявление появится через час или даже раньше.

<p>Чтобы покупатели видели объявление в 20 раз чаще, активируйте «Турбо-продажу».

<p>Привлечь внимание к объявлению, отредактировать его и увидеть количество просмотров можно 
<a href="{url}?utm_source=message">на странице объявления</a>.

<p>Если у вас возникнут вопросы, пожалуйста, обратитесь <a href="http://helpdesk.auto.ru/?utm_source=message">в техническую поддержку</a>

';

// <!-- вместо 777 впиши свой номер счётчика --><img src="https://mc.yandex.ru/watch/777?utm_medium=autoru.message&utm_source=template_{template_alias}"  style="position:absolute; left:-9999px;" alt="">

$mTemplates = new \users8\models\MessagesTemplates();

$tmpl = $mTemplates->findByParams(['alias' => $TEMPLATE_NAME]);
var_dump($tmpl);
$data = [
    'alias' => $TEMPLATE_NAME,
    'title' => $TEMPLATE_TITLE,
    'template' => $TEMPLATE_TEXT,
    'is_html' => 1
];

if($tmpl){
    echo "update";
    $mTemplates->update($tmpl['id'], $data);
} else {
    echo "create";
    $mTemplates->create($data);
}

$mUsers = new \users8\models\Users();

$hNotifier = new \users8\classes\Helpers\Notifier();

$user_id = $mUsers->getByEmail('lavrinenko@yandex-team.ru')['id'];
$mAnketa = new \users8\models\Anketa();

$hNotifier->sendMessage(array(
    'user_id' => $user_id,
    'template_alias' => $TEMPLATE_NAME,
    'template_params' => array(
        'example' => '',
        'url' => 'http://auto.ru/',
        'region_id' => $mAnketa->getUserAnketa($user_id)['region_id'])
));
