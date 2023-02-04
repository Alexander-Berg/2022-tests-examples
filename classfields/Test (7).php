<?php
/**
 * Created by PhpStorm.
 * User: ridrisov
 * Date: 28.02.17
 * Time: 20:45
 */

namespace lib5\classes\Session;


class Test
{
    function __get($name)
    {
        // TODO: Implement __get() method.
    }

    function __set($name, $value)
    {
        // TODO: Implement __set() method.
    }

    function __call($name, $arguments)
    {
        // TODO: Implement __call() method.

        var_dump("__call");

        var_dump($name);

        $res = call_user_func_array(['\lib5\classes\Session\Passport', $name], $arguments);

        var_dump($res);

        return $res;
    }

    function __callStatic($name, $arguments)
    {
        // TODO: Implement __call() method.

        var_dump("__callStatic");

        var_dump($name);

        $res = call_user_func_array(['\lib5\classes\Session\Passport', $name], $arguments);

        var_dump($res);

        return $res;
    }
}