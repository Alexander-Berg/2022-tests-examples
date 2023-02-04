<?php

namespace lib5\classes\VertiSubs;

/*
* TODO: переделать в юнит-тест в prod/tests/projects/lib5. Запускать prod/tests/paratest.php
* Скрипт создаёт и удаляет подписки
* Запуск:
*    $ AUTORU_CONFIG_PHP_LIB5=config_dev.php php classes/Vertis/test.php
*/

use lib5\classes\ApiClient\Exception;

ini_set('memory_limit', -1);
set_time_limit(0);
set_include_path('./lib5' . PATH_SEPARATOR . get_include_path());

require_once ('functions.php');
require_once ('Traits/DI.php');
require_once ('interfaces/iDependencyInjection.php');
require_once ('classes/Curl.php');
require_once ('classes/ExternalRequests.php');
require_once ('classes/Helpers/Pinba.php');
require_once ('classes/DiContainer.php');
require_once ('classes/Helpers/Ip.php');
require_once ('classes/Helpers/Array.php');
require_once ('classes/Debug/Engine/EngineAbstract.php');
require_once ('classes/Debug/Engine/Memcache.php');
require_once ('classes/Core.php');
require_once ('interfaces/iStorage.php');
require_once ('interfaces/iSingleton.php');
require_once ('classes/Storage.php');
require_once ('traits/Singleton.php');
require_once ('classes/Lib5Exception.php');
require_once ('classes/Config.php');
require_once ('classes/Debug/Environment.php');
require_once ('classes/Debug/Event/Level.php');
require_once ('classes/Debug/Event/Type.php');
require_once ('classes/Debug/iTags.php');
require_once ('classes/Debug.php');
require_once ('classes/Config.php');
require_once ('classes/App.php');


\Config::set('log_dir', './lib5/');
\Config::set('vertisubs_url', 'http://csback01ht.vs.yandex.net:36134');


require_once ('classes/VertiSubs/Base.php');
require_once ('classes/VertiSubs/Connector.php');
require_once ('classes/VertiSubs/Exception.php');


$SUBS_1 = <<<EOT
   { "request": {"http_query": "price_from=1000000&price_to=2000000&year_from=2013&year_to=2015&state=USED&image=true&in_stock=true&body_type=ALLROAD&body_type=CROSSOVER&rid=213&locale=ru&lang=ru&currency=RUR"},
     "delivery": {"email": {"address": "sergei-nko@yandex.ru","period": 60}},
     "view": {"title": "внедорожник, кроссовер; от 1 000 000 до 2 000 000 руб.",
              "body": "год выпуска: от 2013 до 2015; с пробегом; с фото; в наличии",
              "tld": "ru",
              "language": "ru",
              "currency": "RUR",
              "frontend_http_query": "price_from=1000000&price_to=2000000&year_from=2013&year_to=2015&state=USED&image=true&in_stock=true&body_type=ALLROAD&body_type=CROSSOVER&rid=213"
              }
   }
EOT;

$subs1 = json_decode($SUBS_1, 1);

function clear_subscriptions($user_id = null, $session_id = null)
{
    $connector = new \lib5\classes\VertiSubs\Connector();
    foreach ($connector->listSubscriptions($user_id, $session_id) as $subs) {
        try {
            $connector->deleteSubscription($subs['id'], $user_id, $session_id);
            echo "Удалили подписку " . $subs['id'] . "\n";
        } catch (\lib5\classes\VertiSubs\NotFoundException $e) {
            echo "Не могу удалить " . $subs['id'] . ". Возможно, лаг в Подписках.\n";
            echo "Попробуй удалить руками:\n";
            echo "curl -XDELETE " . $e->response->url . "\n";
        }
    }
}

function test_subs_creation($subs, $user_id = null, $session_id = null)
{
    echo "Очищаем подписки для " . $user_id . " " . $session_id . "\n";
    clear_subscriptions($user_id, $session_id);
    $connector = new \lib5\classes\VertiSubs\Connector();
    echo "Создаём подписку для " . $user_id . " " . $session_id . "\n";
    $connector->addSubscriptionAutoru($subs, $user_id, $session_id);
    foreach ($connector->listSubscriptionsAutoru($user_id, $session_id) as $subs) {
        echo "Редактируем подписку " . $subs['id'] . " для " . $user_id . " " . $session_id . "\n";
        $subs['request']["http_query"] = $subs['request']["http_query"] . "&state=NEW";
        $connector->editSubscription($subs['id'], $subs, $user_id, $session_id);
    }
    // clear_subscriptions($user_id, $session_id);
}


function test_user_link($yaauto_user_id, $user_id = null, $session_id = null)
{
    echo "Создаём линк\n";
    $connector = new \lib5\classes\VertiSubs\Connector();
    try {
        $connector->linkYAAutoUser($yaauto_user_id, $user_id, $session_id);
    } catch (\lib5\classes\VertiSubs\Exception $e) {
        print_r($e);
    }
}


$USER_ID = 1;
$SESSION_ID = '42f';

test_user_link('209384029348', $USER_ID);

test_subs_creation($subs1, $USER_ID);
test_subs_creation($subs1, null, $SESSION_ID);











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
