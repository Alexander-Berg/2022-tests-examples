<?php
namespace octopus\modules\Front\v1\Sales;

class TestdrivesGetBuilder extends \octopus\classes\Builder
{
    /** @var \octopus\entities\Testdrives  */
    protected $entityTestdrive;

    public function setTestdrive(\octopus\entities\Testdrives $entityTestdrive)
    {
        $this->entityTestdrive = $entityTestdrive;
    }

    public function build()
    {
        $oTestdrive = new \stdClass();
        $oTestdrive->url = $this->entityTestdrive->getUrl();
        $oTestdrive->images = $this->entityTestdrive->getImages();
        $oTestdrive->description = $this->entityTestdrive->getDescription();
        $oTestdrive->title = $this->entityTestdrive->getTitle();
        $oTestdrive->total_count = $this->entityTestdrive->getCount();
        return $oTestdrive;
    }
}