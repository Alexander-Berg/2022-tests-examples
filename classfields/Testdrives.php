<?php

namespace octopus\entities;

class Testdrives extends \octopus\classes\EntityAbstract
{
    const FIELD_TITLE = 'title';
    const FIELD_DESCRIPTION = 'description';
    const FIELD_IMAGES = 'images';
    const FIELD_URL = 'url';
    const FIELD_COUNT = 'count';

    protected $schema = [
        self::FIELD_TITLE,
        self::FIELD_DESCRIPTION,
        self::FIELD_IMAGES,
        self::FIELD_URL,
        self::FIELD_COUNT
    ];

    protected $title;
    protected $description;
    protected $images;
    protected $url;
    protected $count;

    public function getTitle()
    {
        return $this->title;
    }

    public function getDescription()
    {
        return $this->description;
    }

    public function getUrl()
    {
        return $this->url;
    }

    public function getImages()
    {
        $aImages = null;
        if (is_array($this->images)) {
            $aImages = [];
            $resizes = \Config::get('images_news7', 'octopus')['image']['resizes'];
            foreach ($this->images as $image) {
                $oImage = new \stdClass;
                foreach ($resizes as $resize) {
                    $oImage->{$resize[0]} = 'https://avatars.mds.yandex.net' . str_replace('orig', $resize[0], $image);
                }
                $aImages[] = $oImage;
            }
        }

        return $aImages;
    }

    public function getCount()
    {
        return $this->count;
    }
}
