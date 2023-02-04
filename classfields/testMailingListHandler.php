#!/usr/bin/php
<?php
$_GET['test_debug'] = 0;
$_GET['debug_level'] = 5;
ini_set('max_execution_time', 0);

require_once (dirname(__FILE__) . '/../../lib5/common.php');

if (empty($argv[3])) {
    help();
} else {
    echo 'Запуск ' . $argv[1] . ' / ' . $argv[2] . ' / ' . $argv[3] . PHP_EOL . PHP_EOL;

    try {
        $job_params = empty($argv[4]) ? [] : json_decode($argv[4], true);
        if (!empty($job_params)) {
            echo 'Ппараметры задания:' . PHP_EOL . PHP_EOL;
            print_r($job_params);
            echo PHP_EOL;
        }
        echo 'Результаты выполнения:' . PHP_EOL . PHP_EOL;
        /* @codingStandardsIgnoreStart */
        var_dump(testHandler($argv[1], $argv[2], $argv[3], $job_params));
        /* @codingStandardsIgnoreEnd */
        echo PHP_EOL;
    } catch (\Exception $e) {
        echo $e . PHP_EOL;
    }
}


/**
 * Вывод помощи
 *
 * @return @void
 */
function help()
{
    global $argv;
    echo 'Cкрипт тестирпования обработчиков рассылок.

Использование: ' . $argv[0] . ' HANDLER_TYPE PROJECT HANDLER_CLASS [JSON_JOB_PARAMS]

Параметры:

  HANDLER_TYPE      data / recipients / recipients_data

  PROJECT           parts5 / all7 / office7 и т.п.

  HANDLER_CLASS     Имя класса, например SalesReport

  JSON_JOB_PARAMS   Параметры задания в JSON, например: "{\"var1\":\"aaa\",\"var2\":\"bbb\"}"

';
}

/**
 * Запуск обработчика
 *
 * @param string $type       Тип: data / recipients / recipients_data
 * @param string $project    Проект
 * @param string $handler    Класс обработчика
 * @param array  $job_params Массив параметров задания
 *
 * @throws \Exception
 *
 * @return mixed
 */
function testHandler($type, $project, $handler, $job_params = [])
{
    $type = strtolower($type);
    $class = '\\' . $project . '\classes\Subscribe\Handlers';
    switch ($type) {
        case 'recipients':
            $class .= '\Recipients';
            break;
        case 'recipients_data':
            $class .= '\RecipientsData';
            break;
        case 'data':
            $class .= '\Data';
            break;
    }

    $class .= '\\' . $handler;
    if (!class_exists($class)) {
        throw new \Exception('Класс обработчика ' . $class . ' не найден.');
    }

    $instance = new $class();

    if ($instance instanceof \subscribe7\classes\iHandler == false) {
        throw new \Exception('Класс обработчика ' . $class . ' не iHandler.');
    }
    if (!method_exists($instance, 'get')) {
        throw new \Exception('Метод get обработчика ' . $class . ' не найден.');
    }

    return $instance->get($job_params);
}