<?php
/**
 * AUTO.RU Framework
 *
 * @category Lib5
 */

/**
 * Хелпер для удобного проведения unit-тестирования.
 *
 * Для работы данного класса  предполагается,  что будет тестироваться модель,  класс  или контроллер  из любого проекта
 * lib5,  для которого имена используемых баз данных  хранятся в конфиге проекта и считываются при объявлении экземпляра
 * моделей из него же.
 *
 * Используемые базы данных и все таблицы из них подменяются на аналогичные тестовые, заполненные случайными условно ре-
 * альными данными. Дополнительные ограничения и требования к формату данных можно указать дополнительно.
 *
 * Один экземпляр данного класса должен использоваться для всех тестов одного проекта.
 * Пример использования:
 *
 * 1. Начало теста - создаём тестовое окружение
 * $test = new Helpers_Test('api');
 * $test->substituteProjectDatabases();
 *
 * 2. Продолжение теста в другом файле - подключаем созданное ранее тестовое окружение
 * $test = new Helpers_Test('api');
 * $test->substituteProjectDatabases(true);
 *
 * 3. Завершение тестов - удаляем тестовое окружение
 * $test->rollbackProjectDatabases();
 *
 * @name Helpers_Test
 * @since 30.11.2011
 * @version 0.1
 * @package classes
 * @author Vorotnikov Boris <borissv@auto.ru>
 */
class Helpers_Test
{

    /* region variables */

    /**
     * Имя проекта
     *
     * @var string
     */
    private $project_name;

    
    /**
     * Суффикс, добавляемый к реальным именам баз данных
     *
     * @var string
     */
    private $suffix;

    /**
     * Префикс для тестовых БД
     *
     * @const string
     */
    const PREFIX = 'tests_';

    /**
     * Опция, определяющая, нужно ли хелперу самому пытаться определить имена баз данных в конфиге тестируемого проекта.
     *
     * Если эта опция включена и в настройках хелпера указаны имена баз данных, то хелпер будет пытаться определить име-
     * на баз данных, и те из них, которые будут совпадать с указанными в настройках, будут переопределены.
     *
     * В отношении таблиц БД  эта опция  в значении true  будет заставлять хелпер получать  все таблицы  из реальных баз
     * данных и копировать их структуру в тестовые БД.  С отключенной опцией нужно обязательно указывать список таблиц в
     * настройках хелпера.
     *
     * Если не хотите вольностей, просто укажите в настройках имена баз данных и отключите эту опцию.
     *
     * @var bool
     */
    private $try_to_observe = true;


    /**
     * Массив подменяемых баз данных
     *
     * @var array
     */
    private $databases = array();

    /**
     * Массив имён тестовых баз данных
     *
     * @var array
     */
    private $databases_test = array();


    /**
     * Заполнение тестовых таблиц путём копирования реальной информации
     * @const int
     */
    const FILL_TYPE_COPY = 0;

    /**
     * Заполнение тестовых таблиц случайными данными
     * @const int
     */
    const FILL_TYPE_RAND = 1;

    /**
     * Способ заполнения тестовых таблиц
     *
     * @var int
     */
    private $fill_type = self::FILL_TYPE_COPY;

    /**
     * Ограничение на количество копируемых строк в БД
     * $this->fill_limit = 0 - нет ограничения
     * $this->fill_limit > 0 - ограничение на все таблицы
     * // TODO: добавить в конфиг настройки для всех таблиц в отдельности
     *
     * @var int
     */
    private $fill_limit = 50;

    /**
     * Конфиг
     * Представляет собой массив параметров, определяющих поведение хелпера.
     *
     * $config['databases'] - массив имён баз данных, которые нужно подменить
     * $config['tablenames'] - массив таблиц БД в формате db_name.table_name
     * $config['try_to_observe'] - переопределяет опцию $this->try_to_observe
     * $config['fill_type'] - способ заполнения тестовых таблиц
     * $config['fill_limit'] - ограничение на количество копируемых строк в БД
     * // TODO: добавить настройки для задания формата отдельных полей при рандомном формировании данных
     *
     * @var array
     */
    private $config;

    /* endregion variables */





    /**
     * Конструктор
     *
     * @param string       $project_name Имя проекта
     * @param array|string $config       Массив параметров или имя файла конфига
     */
    public function __construct($project_name = '', $config = null)
    {
        if ($project_name != '') {
            $this->checkProjectName($project_name);
            $this->project_name = $project_name;
        }

        $this->setConfig($config);
    }

    /**
     * Устанавливает имя проекта.
     * Следует использовать для отложенного назначения имени проекта или подобных случаев.
     * Имя проекта назначается один раз за всю работу экземпляра класса.
     *
     * @param string $project_name Имя проекта
     *
     * @return void
     */
    public function setProjectName($project_name)
    {
        if (is_null($this->project_name) && ($project_name != '')) {
            $this->checkProjectName($project_name);
            $this->project_name = $project_name;
        }
    }

    /**
     * Проверяет имя проекта
     *
     * @param string $project_name Имя проекта
     *
     * @return void
     *
     * @throws Exception
     */
    private function checkProjectName($project_name)
    {
        if (!preg_match('/[a-zA-Z0-9]+/', $project_name)) {
            throw new Exception('Указанное имя проекта не подходит под формат');
        }
    }

    /**
     * Устанавливает конфиг.
     * В отличие от имени проекта конфиг можно устанавливать не один раз, но это не рекомендуется делать.
     * Для переопределения отдельных параметров используйте setParams или setParam
     * 
     * @param array|string $config Массив параметров или имя файла конфига
     *
     * @return void
     */
    public function setConfig($config)
    {
        switch (true) {
            // Если массив, то воспринимаем его как готовый конфиг
            case is_array($config):
                $this->config = $config;
                break;
            // Если строка, то воспринимаем как имя файла, откуда нужно забрать конфиг
            case is_string($config):
                // TODO: just do it!
                break;
            default:
                $this->config = array();
        }

        $param_names = array(
            'databases',
            'tablenames',
            'try_to_observe',
            'fill_type',
            'fill_limit'
        );
        foreach ($param_names as $p_name) {
            if (isset($this->config[$p_name])) {
                $this->try_to_observe = $this->config[$p_name];
            }
        }
    }

    /**
     * Устанавливает параметр
     *
     * @param string $param Ключ
     * @param mixed  $value Значение
     *
     * @return void
     */
    public function setParam($param, $value)
    {
        $this->config[$param] = $value;
    }

    /**
     * Устанавливает параметры
     *
     * @param array $params Параметры
     *
     * @return void
     */
    public function setParams($params = array())
    {
        foreach ($params as $key => $value) {
            $this->config[$key] = $value;
        }
    }

    /**
     * Выполняет подготовку к тесту:
     * - создаёт тестовые БД
     * - подменяет в хранящемся в памяти конфиге проекта имена тестовых БД
     *
     * @param bool $reload Флаг, указывающий, инициализируем мы тестовое окружение ($reload = false)
     *                     или подгружаем уже созданное ($reload = true)
     *
     * @return bool
     */
    public function substituteProjectDatabases($reload = false)
    {
        if (is_null($this->project_name)) {
            return false;
        }

        // Подгружаем конфиг
        $project_config = Config::getProjectConfig($this->project_name, false);

        // Собираем массив подменяемых БД
        $databases = array();
        if ($this->try_to_observe) {
            // Пытаемся определить имена баз данных из конфига
            foreach ($project_config['general'] as $key => $value) {
                if (preg_match('/^database\_name/', $key)) {
                    $databases[$key] = $value;
                }
            }
            if (isset($this->config['databases'])) {
                $this->databases = array_merge($databases, $this->config['databases']);
            } else {
                $this->databases = $databases;
            }
        } else {
            $this->databases = $this->config['databases'];
        }

        if ($reload) {
            // Когда мы подключаем уже готовое тестовое окружение, нужно сначала определить суффикс
            $this->getSuffix($reload);
        }
        foreach ($this->databases as $key => $real_db_name) {
            // Генерим имена для тестовых БД
            $this->databases_test[$key] = $this->getTestDatabaseName($real_db_name);

            if (!$reload) {
                // Создаём тестовые БД
                $this->createTestDb($this->databases_test[$key]);
            }
        }

        // Подменяем БД в конфиге
        $project_config['general'] = array_merge($project_config['general'], $this->databases_test);
        Config::setProjectConfig($this->project_name, $project_config);
        
        return true;
    }

    /**
     * Возвращает суффикс. При необходимости генерит его.
     *
     * @param bool $reload Флаг, указывающий, инициализируем мы тестовое окружение ($reload = false)
     *                     или подгружаем уже созданное ($reload = true)
     *
     * @return string
     */
    private function getSuffix($reload = false)
    {
        if (empty($this->suffix)) {
            if ($reload) {
                reset($this->databases);
                $sql = '
                    SELECT
                        `SCHEMA_NAME`
                    FROM
                        `information_schema`.`SCHEMATA`
                    WHERE
                        `SCHEMA_NAME` LIKE "' .
                            self::PREFIX .
                            current($this->databases) . '\_' .
                            Storage::get('DEVELOP_NAME') . '%"
                    ORDER BY
                        `SCHEMA_NAME` DESC;
                ';
                $test_db_name = Db::result($sql);
                $error_msg = 'Не удалось определить суффикс текущего тестового окружения';
                if (!$test_db_name) {
                    throw new Exception($error_msg);
                }
                if (preg_match('/^' .
                        self::PREFIX .
                        current($this->databases) . '\_' .
                        Storage::get('DEVELOP_NAME') .
                        '\_(\d+)\_([a-z0-9]{4})$' .
                        '/', $test_db_name, $match)) {
                    $datetime = $match[1];
                    $rand = $match[2];

                    $this->suffix = '_' . Storage::get('DEVELOP_NAME') . '_' . $datetime . '_' . $rand;

                } else {
                    throw new Exception($error_msg);
                }
                die;
            } else {
                // date - для фиксирования даты. Если что, можно будет удалять старые некорректно завершённые тестовые БД.
                // немного случайности для того, чтобы случайно запущенный второй тест мог спокойно сосуществовать с первым.
                $this->suffix = '_' . Storage::get('DEVELOP_NAME') . '_' . date('YmdHis') . '_' . substr(md5(time()), 0, 4);
            }
        }
        return $this->suffix;
    }

    /**
     * Возвращает имя тестовой БД по имени реальной БД
     *
     * @param string $real_database_name Имя реальной базы данных
     *
     * @return string
     */
    private function getTestDatabaseName($real_database_name)
    {
        $test_database_name = self::PREFIX . $real_database_name . $this->getSuffix();
        return $test_database_name;
    }

    /**
     * Возвращает имя реальной БД по имени тестовой БД
     *
     * @param string $test_database_name Имя тестовой базы данных
     *
     * @return string
     */
    private function getRealDatabaseName($test_database_name)
    {
        $real_database_name = preg_replace(
            array(
                '/^'.self::PREFIX.'/',
                '/'.$this->getSuffix().'$/'
            ),
            array('', ''),
            $test_database_name
        );

        return $real_database_name;
    }

    /**
     * Создаёт тестовую БД
     *
     * @param string $test_database_name Имя тестовой БД
     *
     * @return void
     */
    private function createTestDb($test_database_name)
    {
        // Создаём тестовую БД
        $sql = '
            CREATE DATABASE `' . $test_database_name . '`
        ';
        Db::q($sql);

        $real_database_name = $this->getRealDatabaseName($test_database_name);

        if ($this->try_to_observe) {
            // Получаем список всех таблиц из реальной БД
            $sql = '
                SHOW TABLES FROM `' . $real_database_name . '`
            ';
            $real_tables = array();
            $rows = Db::fetchQuery(Db::q($sql));
            foreach ($rows as $row) {
                $table = current($row);
                if ($table == '_db_version') {
                    continue;
                }
                $real_tables[] = $table;
            }
        } else {
            // Иначе берём из конфига хелпера
            if (empty($this->config['tablenames'])) {
                // ибо таблицы не заданы и делать нечего
                return;
            }
            $real_tables = array();
            foreach ($this->config['tablenames'] as $t_name) {
                $t_name = str_replace($real_database_name . '.', '', $t_name, $cnt);
                if ($cnt == 1) {
                    $real_tables[] = $t_name;
                }
            }
        }


        foreach ($real_tables as $real_table) {
            // Создаём дубликаты всех таблиц из реальной БД в тестовой
            $sql = '
        		CREATE TABLE IF NOT EXISTS
        			`' . $test_database_name. '`.`' . $real_table . '`
        		LIKE
        			`' . $real_database_name. '`.`' . $real_table . '`
            ';
            Db::q($sql);

            // Заполняем тестовые таблицы данными
            $this->fillTestTable($test_database_name, $real_table);
        }
    }

    /**
     * Заполняет тестовую таблицу данными
     *
     * @param string $test_database_name Имя тестовой базы данных
     * @param string $tablename          Имя тестовой таблицы
     * 
     * @return void
     */
    private function fillTestTable($test_database_name, $tablename)
    {
        $test_tablename = '`'.$test_database_name . '`.`' . $tablename . '`';
        $real_tablename = '`'.$this->getRealDatabaseName($test_database_name) . '`.`' . $tablename . '`';

        // Получаем список столбцов
        $sql = '
            SHOW COLUMNS FROM ' . $test_tablename . '
        ';
        $fields = Db::fetchQuery(Db::q($sql));
        /*
         * $field => array(
         *     'Field'   => 'field_name' //id
         *     'Type'    => 'int(11) unsigned'
         *     'Null'    => 'NO'
         *     'Key'     => 'PRI' //UNI, '',
         *     'Default' =>
         *     'Extra'   => 'auto_increment'
         * );
         */

        switch ($this->fill_type) {
            case self::FILL_TYPE_COPY:

                $field_names = array();
                foreach ($fields as $field) {
                    $field_names[] = $field['Field'];
                }
                $field_list = join(', ', $field_names);

                $sql = '
                    INSERT
                        INTO ' . $test_tablename . ' (' . $field_list . ')
                        SELECT *
                        FROM ' . $real_tablename;
                if ($this->fill_limit) {
                    $sql .= '
                        LIMIT ' . $this->fill_limit . '
                ';
                }
                Db::q($sql);

                break;


            case self::FILL_TYPE_RAND:

                if (!$this->fill_limit) {
                    break;
                }

                // Поля таблицы
                $field_names = array();

                // Данные, которые будут добавлены в таблицы - несколько строк
                $data_rows = array();

                foreach ($fields as $field) {
                    if (($field['Key'] == 'PRI') && ($field['Extra'] == 'auto_increment')) {
                        continue;
                    }

                    if (!preg_match('/^([a-z]+)\((.+)\)/', $field['Type'], $matches)) {
                        $type = $field['Type'];
                        $ext = null;
                    } else {
                        $type = $matches[1];
                        $ext = $matches[2];
                    }

                    for ($i = 0; $i < $this->fill_limit; $i++) {
                        $data_rows[$i][] = "'".Db::escape(Helpers_Random::getDbFieldValue($type, $ext, $field['Field']))."'";
                    }

                    $field_names[] = $field['Field'];
                }
                $field_list = join(', ', $field_names);

                // Формируем SQL-запрос на вставку рандомных данных
                $sql = '
                    INSERT INTO ' . $test_tablename . '
                        (' . $field_list . ')
                    VALUES
                        (';
                $i = 0;
                foreach ($data_rows as $row) {
                    $sql .= join(', ', $row);
                    if ($i != count($data_rows) - 1) {
                        $sql .= '),
                        (';
                    }
                    $i++;
                }
                $sql .= ')';
                Db::q($sql);

                break;
        }

    }



    /**
     * Удаляет тестовые БД, приводя всё к нормальному виду
     *
     * @return void
     */
    public function rollbackProjectDatabases()
    {
        foreach ($this->databases_test as $test_db_name) {
            $this->deleteTestDb($test_db_name);
        }
    }

    /**
     * Удаляет тестовую БД
     *
     * @param string $test_database_name Имя тестовой БД
     *
     * @return void
     */
    private function deleteTestDb($test_database_name)
    {
        $sql = '
            DROP DATABASE `' . $test_database_name . '`
        ';
        Db::q($sql);
    }

    /**
     * Деструктор
     */
    public function __destruct()
    {
        //
    }
}