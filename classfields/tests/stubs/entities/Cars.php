<?php
namespace tests\stubs\entities;

class Cars extends \octopus\entities\Cars
{
    public function setServices(array $services)
    {
        return $this->services = $services;
    }
}
