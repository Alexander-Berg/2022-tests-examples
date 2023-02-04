<?php
namespace octopus\modules\Front\v1\Sales;

use \lib5\classes\Helpers\Structure;
use octopus\entities\Cars;

class TestdrivesGet extends \octopus\classes\ResourceAbstract
{
    use \octopus\traits\Requires;

    protected $require_storages = [
        'Db\Cars'
    ];

    protected $require_repositories = [
        'Testdrives'
    ];

    function request($params)
    {
        $sale_id   = Structure::get($params, 'sale_id');
        if ($sale_id < 1) {
            throw new \octopus\classes\Exception(
                'Не указан параметр sale_id',
                \octopus\classes\Exception::ERROR_CODE_HTTP_NOT_FOUND
            );
        }
        /** @var \octopus\storages\Db\Cars $storageCars */
        $storageCars = $this->getStorage('Db\Cars');
        $aSale = $storageCars->cache()->findByPk($sale_id);

        /** @var \octopus\repositories\Testdrives $testdrivesRepository */
        $testdrivesRepository = $this->getRepository('Testdrives');
        $testdrivesCollection = $testdrivesRepository->cache()->getForCard(
            \Helpers_Array::iget($aSale, Cars::FIELD_CATEGORY_ID),
            \Helpers_Array::iget($aSale, Cars::FIELD_MARK_ID, 0),
            \Helpers_Array::iget($aSale, Cars::FIELD_FOLDER_ID, 0)
        );

        if ($testdrivesCollection->getTotal() == 0) {
            $result = new \stdClass();
        } else {
            $testdrivesBuilder = new TestdrivesGetBuilder();
            $testdrivesBuilder->setTestdrive($testdrivesCollection->getItems()[0]);
            $result = $testdrivesBuilder->build();
        }
        return [
            'result' => $result
        ];

    }
}
