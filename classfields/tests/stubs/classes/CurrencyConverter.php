<?php
namespace tests\stubs\classes;

class CurrencyConverter extends \octopus\classes\CurrencyConverter
{
    protected static $exchangeRates = [
        self::CUR_RUR => 1.0,
        self::CUR_EUR => 71.1738,
        self::CUR_USD => 62.6309
    ];
}
