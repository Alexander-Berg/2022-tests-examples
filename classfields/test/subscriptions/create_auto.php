#!/usr/bin/php -q
<?php
$_GET = array();
$_GET['test_debug']  = 0;
$_GET['debug_level'] = 2;

$shell_script = __FILE__;
require_once (dirname(__FILE__) . '/../../../../lib5/common.php');

ignore_user_abort(true);
set_time_limit(0);

$SUBS_1 = <<<EOT
   { "request": {"http_query": "price_from=1000000&price_to=2000000&year_from=2013&year_to=2015&state=USED&image=true&in_stock=true&body_type=ALLROAD&body_type=CROSSOVER&rid=213&locale=ru&lang=ru&currency=RUR"},
     "delivery": {"email": {"address": "alexandrov@auto.ru","period": 60}},
     "view": {"title": "внедорожник, кроссовер; от 1 000 000 до 2 000 000 руб.",
              "body": "год выпуска: от 2013 до 2015; с пробегом; с фото; в наличии",
              "tld": "ru",
              "language": "ru",
              "currency": "RUR",
              "frontend_http_query": "price_from=1000000&price_to=2000000&year_from=2013&year_to=2015&state=USED&image=true&in_stock=true&body_type=ALLROAD&body_type=CROSSOVER&rid=213"
              }
   }
EOT;

$SUBS_2 = <<<EOT
   { "request": {"http_query": "price_from=100000&price_to=300000&year_from=2013&year_to=2015&state=USED&image=true&in_stock=true&mark=BMW&model=3ER&rid=213&locale=ru&lang=ru&currency=RUR"},
     "delivery": {"email": {"address": "alexandrov@auto.ru","period": 60}},
     "view": {"title": "внедорожник, кроссовер; от 1 000 000 до 2 000 000 руб.",
              "body": "год выпуска: от 2013 до 2015; с пробегом; с фото; в наличии",
              "tld": "ru",
              "language": "ru",
              "currency": "RUR",
              "frontend_http_query": "price_from=100000&price_to=300000&year_from=2013&year_to=2015&state=USED&image=true&in_stock=true&mark=BMW&model=3ER&rid=213"
              }
   }
EOT;

$subs1 = json_decode($SUBS_1, 1);
$subs2 = json_decode($SUBS_2, 1);

$searches = new \all7\models\Sales\Search\Auto;
$searches->truncate();

function test_subs_creation($subs, $uid = null, $yandexuid = null)
{
    $connector = new \lib5\classes\VertiSubs\Connector();
    echo "Создаём подписку для " . $uid . " " . $yandexuid . "\n";
    $searches = new \all7\models\Sales\Search\Auto;
    $crc = md5(mt_rand(10000000, 20000000));
    $link_data = ['email' => 'alexandrov@auto.ru', 'crc' => $crc];
    $link = \Helpers_Url::l('http://auto.ru/my_search/link/' . $crc . '/');
    if (!is_null($uid)) {
        foreach ($connector->listSubscriptions($connector::IDENTITY_TYPE_UID, $uid) as $existing) {
            //$connector->deleteSubscription($connector::IDENTITY_TYPE_UID, $uid, $existing['id']);
        }
        $link_data['uid'] = $uid;
        try {
            $connector->addSubscription($connector::IDENTITY_TYPE_UID, $uid, $subs);
        } catch (\Exception $e) {
        
        }
    }
    if (!is_null($yandexuid)) {
        foreach ($connector->listSubscriptions($connector::IDENTITY_TYPE_YANDEXUID, $yandexuid) as $existing) {
            //$connector->deleteSubscription($connector::IDENTITY_TYPE_YANDEXUID, $yandexuid, $existing['id']);
        }
        $link_data['yandexuid'] = $yandexuid;
        try {
            $connector->addSubscription($connector::IDENTITY_TYPE_YANDEXUID, $yandexuid, $subs);
        } catch (\Exception $e) {

        }
    }
    $searches->create($link_data);
    echo "http:" . $link . "\n";
    // clear_subscriptions($user_id, $session_id);
}

test_subs_creation($subs1, null, 1364055401455781217);
test_subs_creation($subs2, null, 1364055401455781217);
test_subs_creation($subs1, '77716596', null);
test_subs_creation($subs2, '77716596', null);











/*
$connector = new \lib5\classes\VertiSubs\Connector();

try {
    // Добавляем подписку
    $json_data = $connector->addSubscription($subs1, $USER_ID);
    echo "Ответ бэкэнда:";
    print_r($json_data);

} catch (\lib5\classes\VertiSubs\ConnectException $e) {
    echo "Ошибка соединения:".$e->response->error;

} catch (\lib5\classes\VertiSubs\ConflictException $e) {
    echo "Конфликт, такой поиск у пользователя уже есть:".$e->response->content;

} catch (\lib5\classes\VertiSubs\NotFoundException $e) {
    echo "Ошибка, бэкэнд не нашел чего-то:".$e->response->content;

} catch (\lib5\classes\VertiSubs\HTTPException $e) {
    echo "Бэкэнд ответил ошибкой. Код:".$e->http_status." ответ:".$e->response->content;

} catch (\lib5\classes\VertiSubs\Exception $e) {
    echo "Неизвестная ошибка";
    print_r($e);
}
*/
