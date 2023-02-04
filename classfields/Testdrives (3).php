<?php

namespace all7\classes\UrlNavigator\Item;

use \all7\classes\UrlNavigator as UrlNavigator,
    \catalog7\classes\UrlNavigator\Item\ItemAbstract as ItemAbstract,
    \catalog7\classes\UrlNavigator\Action,
    \lib5\classes\UrlNavigator\Exception;

/**
 * Class Testdrives
 *
 * @package all7\classes\UrlNavigator\Item
 */
class Testdrives extends ItemAbstract
{
    const URL_ALIAS = 'testdrives';

    /**
     * Алиас
     *
     * @var string
     */
    protected $alias = self::URL_ALIAS;

    /**
     * Конструктор
     *
     * @return null
     */
    public function __construct()
    {
        $return = parent::__construct();
        return $return;
    }
    /**
     * Допустимые действия, если не null, переопределяем действия в соответствии с указанными
     *
     * @var null|array
     */
    protected $_actions_allowed = array(
        Action::ACTION_VIEW
    );

    /**
     * Получить имя сущности
     *
     * @return string
     */
    public function getName()
    {
        return UrlNavigator::ITEM_TYPE_TESTDRIVES;
    }

    /**
     * Парсит адрес
     *
     * @param array $url адрес
     *
     * @return array
     */
    public function parseUrl(array $url)
    {
        if (count($url) && current($url) == UrlNavigator::ITEM_TYPE_TESTDRIVES) {
            $this->_load_params = UrlNavigator::ITEM_TYPE_TESTDRIVES;
            array_shift($url);
            $this->parsingSuccess();
        } else {
            $this->parsingSkip();
        }
        return $url;
    }

    /**
     * Получаем короткий адрес для сущности
     *
     * @return array
     */
    public function getShortUrl()
    {
        $url = array();

        if ($this->_previous instanceof ItemAbstract) {
            $url = array_merge($url, $this->_previous->getShortUrl());
        }

        $url[] = $this->getName();

        return $url;
    }

    /**
     * Получаем адрес для сущности
     *
     * @return array
     */
    public function getUrl()
    {
        return $this->getPath();
    }
    /**
     * Загружаем секцию из бд
     *
     * @throws \lib5\classes\UrlNavigator\Exception
     *
     * @return bool
     */
    public function load()
    {
        if (empty($this->_load_params)) {
            return false;
        } else {
            $this->_chain = array((object) $this->_load_params);
        }
        return true;
    }

    /**
     * Добавляем сущность с головы
     *
     * @param string $type тип сущности
     * @param array  $data массив, описывающий сущность
     *
     * @throws \lib5\classes\UrlNavigator\Exception
     * @return \catalog7\classes\UrlNavigator\Item\ItemAbstract
     */
    public function push($type, $data = array())
    {
        return $this;
    }

    /**
     * Получаем сегмент адреса
     *
     * @return array
     */
    public function getPath()
    {
        $paths = array();

        if (false !== ($testdrives = end($this->_chain))) {
            $paths[] = UrlNavigator::ITEM_TYPE_TESTDRIVES;
        }

        if ($this->getPrevious() instanceof ItemAbstract) {
            $paths = array_merge($this->getPrevious()->getPath(), $paths);
        }

        return $paths;
    }
}