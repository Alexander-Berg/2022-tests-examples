<?php
namespace octopus\repositories;

use lib5\Traits\Cacheable;

class Testdrives extends \octopus\classes\RepositoryAbstract
{
    use \octopus\traits\Requires;
    use Cacheable;

    protected $require_storages = [
        'Db\Catalog\Folders',
        'Db\News\Categories',
        'Db\News\Marks',
        'Db\News\Items',
        'Db\Comments',
    ];

    protected $require_entities = [
        'Testdrives'
    ];

    public function getForCard($category_id, $mark_id, $folder_id)
    {
        $collectionTestdrives = new \octopus\classes\Collection($this->getEntity('Testdrives'));
        /** @var \octopus\storages\Db\News\Categories $mCategories */
        $mCategories = $this->getStorage('Db\News\Categories');
        $news_categories = $mCategories->cache()->findAllByParams([
            'type'            => $mCategories::TYPE_TESTDRIVE,
            'is_category_old' => 0,
            'api'             => 1,
        ]);

        if (empty($news_categories)) {
            return [];
        }

        $news_categories_ids = array_keys(\Helpers_Array::rebuildForKey($news_categories, 'id'));

        $marksParams = [];

        $marksParams['marks_category_id'] = $category_id;

        $marksParams['catalog7_mark_id'] = $mark_id;

        /** @var \octopus\storages\Db\Catalog\Folders $mFolders */
        $mFolders = $this->getStorage('Db\Catalog\Folders');
        $aFolder = $mFolders->cache()->findByPk($folder_id);
        if (count($aFolder) && $aFolder['mark_id'] == $mark_id) {
            $foldersIDS = $mFolders->cache()->getChildsIdByFoldersId([$folder_id]);
            $foldersIDS[] = $folder_id;
            // Не факт что у этой папки есть дочерние, кстати
            $marksParams['folder_id'] = array_unique($foldersIDS);
        }

        /** @var \octopus\storages\Db\News\Marks $mMarks */
        $mMarks = $this->getStorage('Db\News\Marks');
        $marks = $mMarks->cache()->findAllByParams($marksParams);
        $itemIDS = array_column($marks, 'item_id');

        /** @var \octopus\storages\Db\News\Items $mItems */
        $mItems = $this->getStorage('Db\News\Items');
        $items = $mItems->cache()->findAllByParams(['category_id' => $news_categories_ids, 'id' => $itemIDS, 'limit' => 5]);
        if (!empty($items)) {
            $itemIDS = array_column($items, 'id');
            /** @var \octopus\storages\Db\Comments $mComments */
            $mComments = $this->getStorage('Db\Comments');
            $comments_count = $mComments->cache()->getCommentsCountByItemsForTestdrives($itemIDS);
            if (!empty($comments_count)) {
                $item = reset($comments_count);
                $testdrive = $mItems->cache()->findByPk($item['subject_id']);
                $category = \Helpers_Array::rebuildForKey($news_categories, 'id')[$testdrive['category_id']];
                $mds_image = $mItems->getImage($testdrive['main_image']);
                $aTestdrive = [
                    'title' => $testdrive['title'],
                    'description' => $testdrive['short_html'],
                    'images' => $mds_image ? [$mds_image] : [],
                    'url' => '/article/category/' . $category['alias'] . '/' . $testdrive['id'] . '_' . $testdrive['slug'],
                    'count' => count($itemIDS),
                ];
                $collectionTestdrives->fill([$aTestdrive]);
            }
        }

        return $collectionTestdrives;
    }
}
