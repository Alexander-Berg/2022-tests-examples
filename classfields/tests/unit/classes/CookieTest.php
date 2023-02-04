<?php
namespace lib5\tests\unit\classes;

/**
 * Test class for Cookie.
 */
class CookieTest extends \PHPUnit_Framework_TestCase
{

    const TEST_NAME = 'testName';
    const TEST_VALUE = 'testValue';
    const TEST_TIME = 1;
    const TEST_DOMAIN = 'local';
    const TEST_DOMAIN2 = 'local2';

    /**
     * Инстанс печенюшек
     *
     * @var \Cookie
     */
    protected $cookie;

    /**
     * Тестируем установку печенег
     *
     * @return void
     */
    public function testSet()
    {
        $data = $_COOKIE;

        $name1 = $this->_getName();
        \Cookie::set($name1, self::TEST_VALUE, self::TEST_TIME);
        static::assertArrayHasKey($name1, $_COOKIE);

        $name2 = $this->_getName();
        \Cookie::set($name2, self::TEST_VALUE, self::TEST_TIME, self::TEST_DOMAIN);
        static::assertArrayHasKey($name2, $_COOKIE);

        $name3 = $this->_getName();
        \Config::set('cookie_domain', self::TEST_DOMAIN2);
        \Cookie::set($name3, self::TEST_VALUE, self::TEST_TIME);
        static::assertArrayHasKey($name3, $_COOKIE);

        static::assertArrayNotHasKey($name1, $data);
        static::assertArrayNotHasKey($name2, $data);
        static::assertArrayNotHasKey($name3, $data);
    }

    /**
     * Получаем префикс печенег
     *
     * @return string
     */
    protected function _getName()
    {
        return md5(microtime(true));
    }
}
