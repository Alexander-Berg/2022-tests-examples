<?php
namespace tests\stubs\modules\Front\v1;

use \octopus\modules;

class SalesGet extends modules\Front\v1\SalesGet
{
    public function cache($flush = false, $noTags = false)
    {
        return $this;
    }
}
