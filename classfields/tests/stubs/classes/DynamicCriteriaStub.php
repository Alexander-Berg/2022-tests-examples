<?php


namespace tests\stubs\classes;
use octopus\classes\DynamicCriteria;


/**
 * DynamicCriteriaStub.php
 *
 * Date: 30.03.16 10:20
 */
class DynamicCriteriaStub extends DynamicCriteria
{
    protected static $valid_fields = [];

    /**
     * @param array $fields
     *
     * @return void
     */
    public function setValidFieldsDirectly(array $fields)
    {
        self::$valid_fields = $fields;
    }
}