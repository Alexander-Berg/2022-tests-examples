#!/usr/bin/php -q
<?php
$_GET = array();
$_GET['event_log'] = 0;
$_GET['test_debug']  = false;
$_GET['debug_level'] = false;

$shell_script = __FILE__;
require_once (dirname(__FILE__) . '/../../lib5/common.php');

ignore_user_abort(true);

$mailRecipients = isset($argv[1]) ? $argv[1] : 'autoru.tester@mail.ru';
$mailUnsubscribe = isset($argv[2]) ? $argv[1] : 'amisyura@auto.ru';

$result = \Mail::send(array(
    'priority' => 'high',
    'to' => $mailRecipients,
    'subject' => 'Тестовая рассылка с хедером',
    'body' => 'test list-unsubscribe',
    'headers' => array(
        'List-Unsubscribe' => sprintf('<mailto:%s>', $mailUnsubscribe)
    ),
));

if ($result) {
    echo sprintf('Mail send to "%s", unsubscribe "%s"', $mailRecipients, $mailUnsubscribe) . "\n";
} else {
    echo "Mail send failed\n";
}