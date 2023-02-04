<?php
namespace tests\modules\Front\v1;

use \octopus\classes\CurrencyConverter,
    \octopus\entities,
    \octopus\classes\Collection,
    \octopus\modules,
    \octopus\dictionaries,
    \octopus\storages\Db,
    \tests\stubs;

class SalesGetTest extends \PHPUnit_Framework_TestCase
{
    const FAKER_SEED_NUM = null;

    /** @var \Faker\Generator $_faker */
    private static $_faker;

    public static function setUpBeforeClass()
    {
        self::_getFaker();
    }

    /**
     * @group SalesGet
     * @return array
     */
    public function providerTestRequest()
    {
        $body_types = \Helpers_Array::rebuildForKey(
            dictionaries\Modifications::getProperties()[dictionaries\Modifications::TECH_PROPERTY_ID_BODY_TYPE]['selects'],
            'autoru_alias'
        );

        $faker = $this->_getFaker();

        return [
            [],
            [
                ['body_type' => $body_types['offroad_5d']['id']],
                [
                    'auto_group'   => $faker->randomElement(dictionaries\Catalog::getAutoGroups()),
                    'auto_segment' => $faker->randomElement(dictionaries\Catalog::getAutoSegments())
                ]
            ],
            [
                ['engine_volume' => null],
                [],
                [
                    'auto_group'   => $faker->randomElement(dictionaries\Catalog::getAutoGroups()),
                    'auto_segment' => $faker->randomElement(dictionaries\Catalog::getAutoSegments())
                ]
            ],
        ];
    }

    /**
     * @group SalesGet
     *
     * @dataProvider providerTestRequest
     */
    public function testRequest($aModification = [], $aModel = [], $aGeneration = [])
    {
        $moduleSalesGet = new stubs\modules\Front\v1\SalesGet();

        $session_user_id = self::$_faker->numberBetween(1, PHP_INT_MAX);

        $mockUser = $this->getMockBuilder('\lib5\classes\Current\User')
            ->setMethods(['getId'])
            ->getMock();
        $mockUser->expects($this->any())->method('getId')->will($this->returnValue($session_user_id));

        $acl = $this->getMockBuilder('\lib5\classes\Acl')
            ->setMethods(['getInstance', 'getUser'])
            ->getMock();
        $acl->expects($this->any())->method('getInstance')->will($this->returnSelf());
        $acl->expects($this->any())->method('getUser')->will($this->returnValue($mockUser));

        $moduleSalesGet->setResource('\lib5\classes\Acl', $acl);

        \lib5\classes\Helpers\UserOnline::setOnline($session_user_id);

        $aGeneratedSale = $this->_genSaleArray([
            'user_id' => $session_user_id
        ]);

        $aGeneratedModification = $this->_genModificationArray([
            'id'          => $aGeneratedSale['modification_id'],
            'sale_id'     => $aGeneratedSale['id'],
            'category_id' => $aGeneratedSale['category_id'],
            'mark_id'     => $aGeneratedSale['mark_id'],
            'folder_id'   => $aGeneratedSale['folder_id'],
        ] + $aModification);

        $aGeneratedMark = $this->_genMarkArray([
            'id' => $aGeneratedModification['mark_id']
        ]);

        $aGeneratedModel = $this->_genFolderArray([
            'mark_id' => $aGeneratedModification['mark_id'],
            'level'   => 1
        ] + $aModel);

        $aGeneratedGeneration = $this->_genFolderArray([
            'id'           => $aGeneratedModification['folder_id'],
            'parent_id'    => $aGeneratedModel['id'],
            'mark_id'      => $aGeneratedModification['mark_id'],
            'level'        => 2
        ] + $aGeneration);

        $aGeneratedFolders = [$aGeneratedModel, $aGeneratedGeneration];

        $aGeneratedContacts = $this->_genContactsArray([
            'sale_id' => $aGeneratedSale['id']
        ]);

        $aGeneratedServices = [
            $this->_genServicesArray([
                'sale_id' => $aGeneratedSale['id']
            ]),
            $this->_genServicesArray([
                'sale_id' => $aGeneratedSale['id'],
                'alias'   => \octopus\storages\Db\Services::SERVICE_VIRTUAL_PHONE
            ])
        ];

        $moduleSalesGet->setRepository('Cars', $this->_getRepositorySalesStub($aGeneratedSale));
        $moduleSalesGet->setRepository('Cars\Favorites', $this->_getRepositorySalesFavoritesStub());
        $moduleSalesGet->setRepository('Marks', $this->_getRepositoryMarksStub($aGeneratedMark));
        $moduleSalesGet->setRepository('Folders', $this->_getRepositoryFoldersStub($aGeneratedFolders));

        $moduleSalesGet->setBusiness('Modifications', $this->_getBusinessModificationsStub($aGeneratedModification));
        $moduleSalesGet->setBusiness('Contacts', $this->_getBusinessContactsStub($aGeneratedContacts));
        $moduleSalesGet->setBusiness('Favorites', $this->_getBusinessFavoritesStub());
        $moduleSalesGet->setBusiness('ServicesOld', $this->_getBusinessServicesOldStub($aGeneratedServices));

        $body_types_count = self::$_faker->numberBetween(1, 2);

        $storageModificationsStub = $this->getMockBuilder('\octopus\storages\Db\Catalog\Modifications')
            ->getMock();
        $storageModificationsStub->method('getTechSelectsCountInFolder')
            ->willReturn($body_types_count);

        $moduleSalesGet->setStorage('Db\Catalog\Modifications', $storageModificationsStub);

        $storageServicesStub = $this->getMockBuilder('\octopus\storages\Db\Cars\Services')
            ->getMock();
        $storageServicesStub->method('getActiveBySaleId')
            ->willReturn([
                ['service' => self::$_faker->word]
            ]);

        $moduleSalesGet->setStorage('Db\Cars\Services', $storageServicesStub);

        $result = $moduleSalesGet->request(['sale_id' => $aGeneratedSale['id'], 'sale_hash' => $aGeneratedSale['hash']]);

        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_ARRAY, $result, __LINE__);
        $this->assertArrayHasKey('result', $result, __LINE__);

        $oSale = $result['result'];
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale, __LINE__);

        $this->_checkSaleAttrs($oSale, $aGeneratedSale);
        $this->_checkSaleImages($oSale);
        $this->_checkSaleExtras($oSale);

        if ($body_types_count == 1) {
            $this->assertObjectHasAttribute('single_body_type', $oSale, __LINE__);
            $this->assertTrue($oSale->single_body_type, __LINE__);
        }

        $this->_checkModification($oSale, $aGeneratedModification);
        $this->_checkModel($oSale, $aGeneratedModel);
        $this->_checkGeneration($oSale, $aGeneratedGeneration);
        $this->_checkMark($oSale, $aGeneratedMark);
        $this->_checkContacts($oSale, $aGeneratedContacts);

        $this->assertObjectHasAttribute('services', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_ARRAY, $oSale->services, __LINE__);
    }

    private function _getRepositorySalesStub(array $aGeneratedSale)
    {
        $collectionCars = new Collection(new entities\Cars());
        $collectionCars->fill([$aGeneratedSale]);

        $entityCars = $collectionCars->get('id', $aGeneratedSale['id']);
        $entityCars->setResource(\octopus\classes\CurrencyConverter::class, new stubs\classes\CurrencyConverter());

        $repositorySalesStub = $this->getMockBuilder('\octopus\repositories\Cars')
            ->getMock();

        $repositorySalesStub->method('getForCard')
            ->willReturn($collectionCars);

        return $repositorySalesStub;
    }

    private function _getRepositorySalesFavoritesStub(array $aGeneratedSale = [])
    {
        $collectionCars = new Collection(new entities\Cars());
        $collectionCars->fill([$aGeneratedSale]);

        $repositorySalesFavoritesStub = $this->getMockBuilder('\octopus\repositories\Cars\Favorites')
            ->getMock();

        $repositorySalesFavoritesStub->method('getListByCategoryId')
            ->willReturn($collectionCars);

        return $repositorySalesFavoritesStub;
    }

    private function _getBusinessModificationsStub(array $aGeneratedModification)
    {
        $collectionModifications = new Collection(new entities\Modifications());

        if ($aGeneratedModification) {
            $collectionModifications->fill([$aGeneratedModification]);
        }

        $businessModificationsStub = $this->getMockBuilder('\octopus\business\Modifications')
            ->getMock();

        $businessModificationsStub->method('getThroughSales')
            ->willReturn($collectionModifications);

        return $businessModificationsStub;
    }

    private function _genHash()
    {
        $hash = '';
        for ($i = 0; $i < mt_rand(4, 6); $i++) {
            $hash .= self::$_faker->randomLetter();
        }

        return $hash;
    }

    private function _genSaleArray($extra = [])
    {
        $currencies = [
            CurrencyConverter::CUR_RUR,
            CurrencyConverter::CUR_EUR,
            CurrencyConverter::CUR_USD
        ];

        $vins = [
            'YV1DZ3150D2453036',
            'WAULC68E22A243150',
            'VF7DCRFNC76087236'
        ];

        $warranty = self::$_faker->numberBetween(0, 1);
        $warranty_expire = date('Y-m-d H:i:s', time() + 3600 * self::$_faker->numberBetween(1000, 2000));

        $sale_id = (string) self::$_faker->numberBetween(1, PHP_INT_MAX);

        $images = [
            [
                'sale_id' => $sale_id,
                'name' => '0a8906a153bf326d65573c68bc4d1dc9',
                'source' => 'oldsale',
                'main' => '1',
                'is_suspect' => '1'
            ],
            [
                'sale_id' => $sale_id,
                'name' => '63052-a8b070fc45796391ba74e25958ef4ba2',
                'source' => 'oldsale',
                'main' => '0',
                'is_suspect' => '1'
            ],
            [
                'sale_id' => $sale_id,
                'name' => '5614bddb973ae74fab9ad24b879a3d78',
                'source' => 'oldsale',
                'main' => '0',
                'is_suspect' => '2'
            ],
        ];

        $extras = [
            [
                'sale_id'  => $sale_id,
                'extra_id' => '153',
                'value'    => '208',
            ],
            [
                'sale_id'  => $sale_id,
                'extra_id' => '300',
                'value'    => '302',
            ],
            [
                'sale_id'  => $sale_id,
                'extra_id' => '39',
                'value'    => '365',
            ]
        ];

        $aSale = [
            'id'               => $sale_id,
            'hash'             => $this->_genHash(),
            'description'      => self::$_faker->realText(),
            'price'            => self::$_faker->numberBetween(300000, 1500000),
            'currency'         => self::$_faker->randomElement($currencies),
            'category_id'      => self::$_faker->numberBetween(1, 15),
            'section_id'       => self::$_faker->numberBetween(1, 2),
            'year'             => self::$_faker->numberBetween(1995, date('Y')),
            'mark_id'          => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'folder_id'        => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'modification_id'  => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'user_id'          => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'new_client_id'    => null,
            'salon_id'         => null,
            'salon_contact_id' => null,
            'poi_id'           => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'create_date'      => date('Y-m-d H:i:s', time() - 3600),
            'set_date'         => date('Y-m-d H:i:s', time()),
            'expire_date'      => date('Y-m-d H:i:s', time() + 3600),
            'status'           => self::$_faker->randomElement(array_keys(dictionaries\Status::getStatuses())),
            'availability'     => self::$_faker->randomElement(array_keys(dictionaries\Availability::getOptions())),
            'color'            => self::$_faker->randomElement(array_keys(dictionaries\Colors::getColors())),
            'custom'           => self::$_faker->randomElement(dictionaries\Custom::getOptions())['value'],
            'exchange'         => self::$_faker->randomElement(array_keys(dictionaries\Exchange::getOptions())),
            'extras'           => $extras,
            'images'           => $images,
            'metallic'         => self::$_faker->numberBetween(0, 1),
            'owners_number'    => self::$_faker->numberBetween(1, 3),
            'poi_id'           => null,
            'pts'              => self::$_faker->randomElement(array_keys(dictionaries\Pts::getOptions())),
            'purchase_date'    => date('Y-m-d H:i:s', time() - 3600 * self::$_faker->numberBetween(1000, 2000)),
            'registry_year'    => self::$_faker->numberBetween(1980, date('Y') - self::$_faker->numberBetween(1, 5)),
            'run'              => self::$_faker->numberBetween(10000, 150000),
            'state'            => self::$_faker->randomElement(array_keys(dictionaries\State::getOptions())),
            'vin'              => self::$_faker->randomElement($vins),
            'warranty'         => $warranty,
            'warranty_expire'  => $warranty_expire,
            'wheel'            => self::$_faker->randomElement(array_keys(dictionaries\Wheel::getWheels())),
            'year_my_owner'    => self::$_faker->numberBetween(2000, date('Y')),
            'video'            => [
                'provider_alias' => self::$_faker->randomElement(['Yandex', 'Youtube']),
                'parse_value'    => self::$_faker->randomElement(['rJTBoU4XT54', '5skiesqerp.6907']),
            ],
            'carinfo'          => [
                'status_response' => self::$_faker->randomElement(array_keys(dictionaries\CarInfo::getStatuses())),
            ],
            'haggle'           => self::$_faker->randomElement(array_keys(dictionaries\Haggle::getOptions())),
            'certification'    => [
                'sale_id' => $sale_id,
                'status'  => self::$_faker->randomElement(array_keys(dictionaries\Certification::getStatuses())),
                'file'    => 'cb677882fd84ede5621a7930ae484b5b.pdf'
            ]
        ];

        $aSale = array_merge($aSale, $extra);

        return $aSale;
    }

    private function _genModificationArray($extra = [])
    {
        $auto_names = [
            '1.6 MT (100 л.с.)',
            '2.0d AT (160 л.с.) 4WD',
            'Electro AT (316 кВт) 4WD'
        ];

        $aModification = [
            'id'                  => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'name'                => self::$_faker->word,
            'auto_name'           => self::$_faker->randomElement($auto_names),
            'body_type'           => self::$_faker->randomElement(
                dictionaries\Modifications::getProperties()[dictionaries\Modifications::TECH_PROPERTY_ID_BODY_TYPE]['selects']
            )['id'],
            'category_id'         => mt_rand(1, PHP_INT_MAX),
            'mark_id'             => mt_rand(1, PHP_INT_MAX),
            'folder_id'           => mt_rand(1, PHP_INT_MAX),
            'drive'               => self::$_faker->randomElement(
                dictionaries\Modifications::getProperties()[dictionaries\Modifications::TECH_PROPERTY_ID_DRIVE]['selects']
            )['id'],
            'engine_power'        => mt_rand(1, PHP_INT_MAX),
            'engine_type'         => self::$_faker->randomElement(
                dictionaries\Modifications::getProperties()[dictionaries\Modifications::TECH_PROPERTY_ID_ENGINE_TYPE]['selects']
            )['id'],
            'engine_volume'       => mt_rand(1, PHP_INT_MAX),
            'gearbox_type'        => self::$_faker->randomElement(
                dictionaries\Modifications::getProperties()[dictionaries\Modifications::TECH_PROPERTY_ID_GEARBOX_TYPE]['selects']
            )['id'],
            'auto_class'          => self::$_faker->randomElement(dictionaries\Modifications::getAutoClasses()),
            'label'               => self::$_faker->word,
            'ya_tech_param_id'    => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'ya_configuration_id' => self::$_faker->numberBetween(1, PHP_INT_MAX),
        ];

        $aModification = array_merge($aModification, $extra);

        return $aModification;
    }

    private function _genMarkArray($extra = [])
    {
        $aMark = [
            'id'            => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'name'          => self::$_faker->word,
            'alias'         => self::$_faker->word,
            'ya_code'       => mb_strtoupper(self::$_faker->word),
            'cyrillic_name' => self::$_faker->word
        ];

        $aMark = array_merge($aMark, $extra);

        return $aMark;
    }

    private function _getRepositoryMarksStub(array $aGeneratedMark)
    {
        $collectionMarks = new Collection(new entities\Marks());

        $collectionMarks->fill([$aGeneratedMark]);

        $repositoryMarksStub = $this->getMockBuilder('\octopus\repositories\Marks')
            ->getMock();

        $repositoryMarksStub->method('get')
            ->willReturn($collectionMarks);

        return $repositoryMarksStub;
    }

    private function _genFolderArray($extra = [])
    {
        $aFolder = [
            'id'            => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'parent_id'     => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'level'         => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'name'          => self::$_faker->word,
            'alias'         => self::$_faker->word,
            'ya_code'       => mb_strtoupper(self::$_faker->word),
            'cyrillic_name' => self::$_faker->word
        ];

        $aFolder = array_merge($aFolder, $extra);

        return $aFolder;
    }

    private function _getRepositoryFoldersStub(array $aGeneratedFolders)
    {
        $collectionFolders = new Collection(new entities\Folders());

        $collectionFolders->fill($aGeneratedFolders);

        $repositoryFoldersStub = $this->getMockBuilder('\octopus\repositories\Folders')
            ->getMock();

        $repositoryFoldersStub->method('getByFolderId')
            ->willReturn($collectionFolders);

        return $repositoryFoldersStub;
    }

    private function _genContactsArray($extra = [])
    {
        $aContacts = [
            'user_id'        => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'allow_messages' => self::$_faker->numberBetween(0, 1),
            'sale_id'        => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'geo_id'         => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'ya_city_id'     => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'ya_region_id'   => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'ya_country_id'  => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'address'        => self::$_faker->address,
            'lat'            => self::$_faker->latitude,
            'lng'            => self::$_faker->longitude,
            'phones'         => [
                [
                    'phone'      => self::$_faker->phoneNumber,
                    'call_from'  => self::$_faker->numberBetween(0, 23),
                    'call_till'  => self::$_faker->numberBetween(0, 23),
                    'phone_mask' => '1:3:7'
                ],
                [
                    'phone'      => self::$_faker->phoneNumber,
                    'call_from'  => self::$_faker->numberBetween(0, 23),
                    'call_till'  => self::$_faker->numberBetween(0, 23),
                    'phone_mask' => '1:3:7'
                ]
            ],
            'salon' => [
                'id'          => self::$_faker->numberBetween(1, PHP_INT_MAX),
                'hash'        => $this->_genHash(),
                'title'       => self::$_faker->sentence(3),
                'description' => self::$_faker->realText(),
                'url'         => self::$_faker->url,
                'sales_count' => self::$_faker->numberBetween(1, PHP_INT_MAX)
            ],
            'hide_phones'     => self::$_faker->numberBetween(0, 1),
            'type'            => self::$_faker->randomElement(['user', 'client']),
            'is_premium'      => self::$_faker->numberBetween(0, 1),
            'is_gold_partner' => self::$_faker->numberBetween(0, 1),
            'is_dealer'       => self::$_faker->numberBetween(0, 1),
            'logo'            => self::$_faker->url,
            'date'            => date('Y-m-d H:i:s', time() - 3600),
            'name'            => self::$_faker->name,
            'client_id'       => self::$_faker->numberBetween(1, PHP_INT_MAX)
        ];

        $aContacts = array_merge($aContacts, $extra);

        return $aContacts;
    }

    private function _genServicesArray($extra = [])
    {
        $aService = [
            'sale_id' => self::$_faker->numberBetween(1, PHP_INT_MAX),
            'alias'   => self::$_faker->word,
            'name'    => self::$_faker->word
        ];

        $aService = array_merge($aService, $extra);

        return $aService;
    }

    private function _getBusinessContactsStub(array $aGeneratedContacts)
    {
        $collectionContacts = new Collection(new entities\Contacts());

        $collectionContacts->fill([$aGeneratedContacts]);

        $businessContactsStub = $this->getMockBuilder('\octopus\business\Contacts')
            ->getMock();

        $businessContactsStub->method('getThroughSales')
            ->willReturn($collectionContacts);

        return $businessContactsStub;
    }

    private function _getBusinessFavoritesStub($is_in_favorites = true)
    {
        $businessFavoritesStub = $this->getMockBuilder('\octopus\business\Favorites')
            ->getMock();

        $businessFavoritesStub->method('isInFavorites')
            ->willReturn($is_in_favorites);

        return $businessFavoritesStub;
    }

    private function _getBusinessServicesOldStub(array $aGeneratedServices)
    {
        $collectionServices = new Collection(new entities\Services());

        $collectionServices->fill($aGeneratedServices);

        $businessServicesOldStub = $this->getMockBuilder('\octopus\business\ServicesOld')
            ->getMock();

        $businessServicesOldStub->method('getThroughSales')
            ->willReturn($collectionServices);

        return $businessServicesOldStub;
    }

    private function _convertDateToMs($date)
    {
        return (new \DateTime($date))->getTimestamp() * 1000;
    }

    private function _convertPrice($price, $currency)
    {
        $oPrice = new \stdClass();
        $oCurrencyConverter = new stubs\classes\CurrencyConverter();
        $currencies = [
            CurrencyConverter::CUR_RUR,
            CurrencyConverter::CUR_EUR,
            CurrencyConverter::CUR_USD
        ];
        foreach ($currencies as $cur) {
            $oPrice->$cur = floatval(number_format(
                    $oCurrencyConverter->convert($price, $currency, $cur),
                2, '.', ''
            ));
        }
        return $oPrice;
    }

    /**
     * @group SalesGet
     *
     * @dataProvider providerTestRequestException
     *
     * @expectedException \octopus\classes\Exception
     */
    public function testRequestException($sale_id, $sale_hash = '', $aGeneratedSale = null, $aGeneratedModification = null)
    {
        $moduleSalesGet = new stubs\modules\Front\v1\SalesGet();

        if ($aGeneratedSale !== null) {
            $moduleSalesGet->setRepository('Cars', $this->_getRepositorySalesStub($aGeneratedSale));
        }

        if ($aGeneratedModification !== null) {
            $moduleSalesGet->setBusiness('Modifications', $this->_getBusinessModificationsStub($aGeneratedModification));
        }

        $moduleSalesGet->request(['sale_id' => $sale_id, 'sale_hash' => $sale_hash]);
    }

    public function providerTestRequestException()
    {
        $faker = self::_getFaker();
        $invalid_sale_id = $faker->randomElement([0, -1, null]);

        $sale_id   = $faker->numberBetween(1, PHP_INT_MAX);
        $sale_hash = $faker->word;

        return [
            [$invalid_sale_id],
            [$sale_id, $sale_hash],
            [$sale_id, $sale_hash, ['id' => $sale_id, 'hash' => $faker->word]],
            [$sale_id, $sale_hash, ['id' => $sale_id, 'hash' => $sale_hash], [], []]
        ];
    }

    private static function _getFaker()
    {
        if (is_null(self::$_faker)) {
            self::$_faker = \Faker\Factory::create('ru_RU');
            self::$_faker->seed(self::FAKER_SEED_NUM);
        }

        return self::$_faker;
    }

    private function _checkSaleAttrs($oSale, $aGeneratedSale)
    {
        $this->assertObjectHasAttribute('id', $oSale, __LINE__);
        $this->assertSame($aGeneratedSale['id'], $oSale->id, __LINE__);

        $this->assertObjectHasAttribute('hash', $oSale, __LINE__);
        $this->assertSame($aGeneratedSale['hash'], $oSale->hash, __LINE__);

        $this->assertObjectHasAttribute('create_date', $oSale, __LINE__);
        $this->assertSame($this->_convertDateToMs($aGeneratedSale['create_date']), $oSale->create_date, __LINE__);

        $this->assertObjectHasAttribute('update_date', $oSale, __LINE__);
        $this->assertSame($this->_convertDateToMs($aGeneratedSale['set_date']), $oSale->update_date, __LINE__);

        $this->assertObjectHasAttribute('expire_date', $oSale, __LINE__);
        $this->assertSame($this->_convertDateToMs($aGeneratedSale['expire_date']), $oSale->expire_date, __LINE__);

        $this->assertObjectHasAttribute('category', $oSale, __LINE__);
        //TODO cars вынести
        $this->assertSame('cars', $oSale->category, __LINE__);

        $this->assertObjectHasAttribute('section', $oSale, __LINE__);
        //TODO определение алиаса used/new вынести
        $this->assertSame(($aGeneratedSale['section_id'] == 1 ? 'used' : 'new'), $oSale->section, __LINE__);

        $this->assertObjectHasAttribute('status', $oSale, __LINE__);
        $this->assertSame(dictionaries\Status::getStatuses()[$aGeneratedSale['status']], $oSale->status, __LINE__);

        $this->assertObjectHasAttribute('public_status', $oSale, __LINE__);
        $this->assertSame(dictionaries\Status::getPublicStatuses()[$aGeneratedSale['status']], $oSale->public_status, __LINE__);

        $this->assertObjectHasAttribute('description', $oSale, __LINE__);
        $this->assertSame($aGeneratedSale['description'], $oSale->description, __LINE__);

        $this->assertObjectHasAttribute('year', $oSale, __LINE__);
        $this->assertSame($aGeneratedSale['year'], $oSale->year, __LINE__);

        $this->assertObjectHasAttribute('run', $oSale, __LINE__);
        $this->assertSame($aGeneratedSale['run'], $oSale->run, __LINE__);

        $this->assertObjectHasAttribute('currency', $oSale, __LINE__);
        $this->assertSame($aGeneratedSale['currency'], $oSale->currency, __LINE__);

        $this->assertObjectHasAttribute('price', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->price, __LINE__);
        $this->assertObjectHasAttribute(CurrencyConverter::CUR_RUR, $oSale->price, __LINE__);
        $this->assertObjectHasAttribute(CurrencyConverter::CUR_EUR, $oSale->price, __LINE__);
        $this->assertObjectHasAttribute(CurrencyConverter::CUR_USD, $oSale->price, __LINE__);

        $converted_price = $this->_convertPrice($aGeneratedSale['price'], $aGeneratedSale['currency']);

        $this->assertSame($converted_price->{CurrencyConverter::CUR_RUR}, $oSale->price->{CurrencyConverter::CUR_RUR}, __LINE__);

        $this->assertObjectHasAttribute('color', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->color, __LINE__);

        $this->assertObjectHasAttribute('hex', $oSale->color, __LINE__);
        $this->assertSame(dictionaries\Colors::getColors()[$aGeneratedSale['color']], $oSale->color->hex, __LINE__);

        $this->assertObjectHasAttribute('metallic', $oSale->color, __LINE__);
        $this->assertSame((bool) $aGeneratedSale['metallic'], $oSale->color->metallic, __LINE__);

        $this->assertObjectHasAttribute('wheel', $oSale, __LINE__);
        $this->assertSame(dictionaries\Wheel::getWheels()[$aGeneratedSale['wheel']]['alias'], $oSale->wheel, __LINE__);

        $this->assertObjectHasAttribute('state', $oSale, __LINE__);
        $this->assertSame(dictionaries\State::getOptions()[$aGeneratedSale['state']], $oSale->state, __LINE__);

        $this->assertObjectHasAttribute('owners_number', $oSale, __LINE__);
        $this->assertSame($aGeneratedSale['owners_number'], $oSale->owners_number, __LINE__);

        $this->assertObjectHasAttribute('purchase_date', $oSale, __LINE__);
        $this->assertSame($this->_convertDateToMs($aGeneratedSale['purchase_date']), $oSale->purchase_date, __LINE__);

        $this->assertObjectHasAttribute('pts', $oSale, __LINE__);
        $this->assertSame(dictionaries\Pts::getOptions()[$aGeneratedSale['pts']], $oSale->pts, __LINE__);

        $this->assertObjectHasAttribute('warranty', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->warranty, __LINE__);

        $this->assertObjectHasAttribute('available', $oSale->warranty, __LINE__);
        $this->assertSame((bool) $aGeneratedSale['warranty'], $oSale->warranty->available, __LINE__);

        $this->assertObjectHasAttribute('expire', $oSale->warranty, __LINE__);
        $this->assertSame($this->_convertDateToMs($aGeneratedSale['warranty_expire']), $oSale->warranty->expire, __LINE__);

        $this->assertObjectHasAttribute('registry_year', $oSale, __LINE__);
        $this->assertSame($aGeneratedSale['registry_year'], $oSale->registry_year, __LINE__);

        $this->assertObjectHasAttribute('custom', $oSale, __LINE__);
        $this->assertSame(dictionaries\Custom::getOptions()[$aGeneratedSale['custom']]['alias'], $oSale->custom, __LINE__);

        $this->assertObjectHasAttribute('exchange', $oSale, __LINE__);
        $this->assertSame(dictionaries\Exchange::getOptions()[$aGeneratedSale['exchange']]['alias'], $oSale->exchange, __LINE__);

        $this->assertObjectHasAttribute('availability', $oSale, __LINE__);
        $this->assertSame(dictionaries\Availability::getOptions()[$aGeneratedSale['availability']]['alias'], $oSale->availability, __LINE__);

        $this->assertObjectHasAttribute('vin', $oSale, __LINE__);
        $this->assertSame($aGeneratedSale['vin'], $oSale->vin, __LINE__);

        $this->assertObjectHasAttribute('video', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->video, __LINE__);

        $this->assertObjectHasAttribute('provider', $oSale->video, __LINE__);
        $this->assertSame(mb_strtolower($aGeneratedSale['video']['provider_alias']), $oSale->video->provider, __LINE__);

        $this->assertObjectHasAttribute('name', $oSale->video, __LINE__);
        $this->assertSame($aGeneratedSale['video']['parse_value'], $oSale->video->name, __LINE__);

        if ($aGeneratedSale['carinfo']['status_response'] == dictionaries\CarInfo::STATUS_VALUE_CORRECT_FULL) {
            $this->assertObjectHasAttribute('carinfo', $oSale, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->carinfo, __LINE__);

            $this->assertObjectHasAttribute('status', $oSale->carinfo, __LINE__);
            $this->assertSame(dictionaries\CarInfo::STATUS_ALIAS_CORRECT_FULL, $oSale->carinfo->status, __LINE__);
        }

        $this->assertObjectHasAttribute('haggle', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->haggle, __LINE__);

        $this->assertObjectHasAttribute('alias', $oSale->haggle, __LINE__);
        $this->assertSame(dictionaries\Haggle::getOptions()[$aGeneratedSale['haggle']]['alias'], $oSale->haggle->alias, __LINE__);

        $this->assertObjectHasAttribute('is_owner', $oSale, __LINE__);
        $this->assertTrue($oSale->is_owner, __LINE__);

        $this->assertObjectHasAttribute('in_favorites', $oSale, __LINE__);
        $this->assertTrue($oSale->in_favorites, __LINE__);

        $this->assertObjectHasAttribute('seller_online', $oSale, __LINE__);
        $this->assertTrue($oSale->seller_online, __LINE__);

        $this->assertObjectHasAttribute('certification', $oSale, __LINE__);
        if ($aGeneratedSale['certification']['status'] == Db\Cars\Certification::STATUS_ACTIVE) {
            $this->assertObjectHasAttribute('status', $oSale->certification, __LINE__);
            $this->assertSame(dictionaries\Certification::CERTIFICATION_STATUS_ALIAS_ACTIVE, $oSale->certification->status, __LINE__);

            $this->assertObjectHasAttribute('url', $oSale->certification, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $oSale->certification->url, __LINE__);
        }
    }

    private function _checkSaleImages($oSale)
    {
        $this->assertObjectHasAttribute('images', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_ARRAY, $oSale->images, __LINE__);
        // У картинок проверяем только формат
        foreach ($oSale->images as $image) {
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $image, __LINE__);

            $this->assertObjectHasAttribute('main', $image, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_BOOL, $image->main, __LINE__);

            $this->assertObjectHasAttribute('is_suspect', $image, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_BOOL, $image->is_suspect, __LINE__);

            $this->assertObjectHasAttribute('urls', $image, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_ARRAY, $image->urls, __LINE__);

            $this->assertArrayHasKey('full', $image->urls, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $image->urls['full'], __LINE__);

            $this->assertArrayHasKey('small', $image->urls, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $image->urls['small'], __LINE__);

            $this->assertArrayHasKey('1200x900', $image->urls, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $image->urls['1200x900'], __LINE__);

            $this->assertArrayHasKey('120x90', $image->urls, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $image->urls['120x90'], __LINE__);

            $this->assertArrayHasKey('900x675', $image->urls, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $image->urls['900x675'], __LINE__);

            $this->assertArrayHasKey('248x186', $image->urls, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $image->urls['248x186'], __LINE__);

            $this->assertArrayHasKey('560x420', $image->urls, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $image->urls['560x420'], __LINE__);

            $this->assertArrayHasKey('410x308', $image->urls, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $image->urls['410x308'], __LINE__);

            $this->assertArrayHasKey('280x210', $image->urls, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $image->urls['280x210'], __LINE__);

            $this->assertArrayHasKey('205x154', $image->urls, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $image->urls['205x154'], __LINE__);
        }
    }

    private function _checkSaleExtras($oSale)
    {
        $this->assertObjectHasAttribute('extras_groups', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_ARRAY, $oSale->extras_groups, __LINE__);

        // У комплектаций проверяем только формат
        foreach ($oSale->extras_groups as $extras_group) {
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $extras_group, __LINE__);

            $this->assertObjectHasAttribute('alias', $extras_group, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $extras_group->alias, __LINE__);

            $this->assertObjectHasAttribute('extras', $extras_group, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_ARRAY, $extras_group->extras, __LINE__);
        }
    }

    private function _checkModification($oSale, $aGeneratedModification)
    {
        $this->assertObjectHasAttribute('auto_class', $oSale, __LINE__);
        $this->assertSame($aGeneratedModification['auto_class'], $oSale->auto_class, __LINE__);

        $this->assertObjectHasAttribute('modification', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->modification, __LINE__);

        $this->assertObjectHasAttribute('id', $oSale->modification, __LINE__);
        $this->assertSame((string) $aGeneratedModification['id'], $oSale->modification->id, __LINE__);

        $this->assertObjectHasAttribute('auto_name', $oSale->modification, __LINE__);
        $this->assertSame($aGeneratedModification['auto_name'], $oSale->modification->auto_name, __LINE__);

        $this->assertObjectHasAttribute('body_type', $oSale->modification, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->modification->body_type, __LINE__);

        $aGeneratedBodyType = dictionaries\Modifications::getProperties()[
            dictionaries\Modifications::TECH_PROPERTY_ID_BODY_TYPE]['selects'][$aGeneratedModification['body_type']
        ];

        $this->assertObjectHasAttribute('alias', $oSale->modification->body_type, __LINE__);
        $this->assertSame($aGeneratedBodyType['autoru_alias'], $oSale->modification->body_type->alias, __LINE__);
        $this->assertObjectHasAttribute('code', $oSale->modification->body_type, __LINE__);
        $this->assertSame($aGeneratedBodyType['alias'], $oSale->modification->body_type->code, __LINE__);

        $suv_body_types = [
            dictionaries\Modifications::TECH_SELECT_ALIAS_OFFROAD,
            dictionaries\Modifications::TECH_SELECT_ALIAS_OFFROAD_3D,
            dictionaries\Modifications::TECH_SELECT_ALIAS_OFFROAD_5D,
            dictionaries\Modifications::TECH_SELECT_ALIAS_VEZDEHOD
        ];

        if (in_array($aGeneratedBodyType['autoru_alias'], $suv_body_types)) {
            $this->assertObjectHasAttribute('auto_type', $oSale, __LINE__);
            $this->assertSame('suv', $oSale->auto_type, __LINE__);
        }

        $this->assertObjectHasAttribute('label', $oSale->modification, __LINE__);
        $this->assertSame($aGeneratedModification['label'], $oSale->modification->label, __LINE__);

        $aGeneratedEngineType = dictionaries\Modifications::getProperties()[
            dictionaries\Modifications::TECH_PROPERTY_ID_ENGINE_TYPE]['selects'][$aGeneratedModification['engine_type']
        ];

        $this->assertObjectHasAttribute('engine_type', $oSale->modification, __LINE__);
        $this->assertSame($aGeneratedEngineType['alias'], $oSale->modification->engine_type, __LINE__);

        $aGeneratedGearboxType = dictionaries\Modifications::getProperties()[
            dictionaries\Modifications::TECH_PROPERTY_ID_GEARBOX_TYPE]['selects'][$aGeneratedModification['gearbox_type']
        ];

        $this->assertObjectHasAttribute('gearbox_type', $oSale->modification, __LINE__);
        $this->assertSame($aGeneratedGearboxType['alias'], $oSale->modification->gearbox_type, __LINE__);

        $aGeneratedDrive = dictionaries\Modifications::getProperties()[
            dictionaries\Modifications::TECH_PROPERTY_ID_DRIVE]['selects'][$aGeneratedModification['drive']
        ];

        $this->assertObjectHasAttribute('drive', $oSale->modification, __LINE__);
        $this->assertSame($aGeneratedDrive['alias'], $oSale->modification->drive, __LINE__);

        if ($aGeneratedModification['engine_volume']) {
            $this->assertObjectHasAttribute('engine_volume', $oSale->modification, __LINE__);
            $this->assertSame($aGeneratedModification['engine_volume'], $oSale->modification->engine_volume, __LINE__);
        }

        $this->assertObjectHasAttribute('engine_power', $oSale->modification, __LINE__);
        $this->assertSame($aGeneratedModification['engine_power'], $oSale->modification->engine_power, __LINE__);

        $this->assertObjectHasAttribute('tech_param_id', $oSale->modification, __LINE__);
        $this->assertSame($aGeneratedModification['ya_tech_param_id'], $oSale->modification->tech_param_id, __LINE__);

        $this->assertObjectHasAttribute('configuration_id', $oSale->modification, __LINE__);
        $this->assertSame($aGeneratedModification['ya_configuration_id'], $oSale->modification->configuration_id, __LINE__);
    }

    private function _checkModel($oSale, $aGeneratedModel)
    {
        $this->assertObjectHasAttribute('model', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->model, __LINE__);

        $this->assertObjectHasAttribute('name', $oSale->model, __LINE__);
        $this->assertSame($aGeneratedModel['name'], $oSale->model->name, __LINE__);

        $this->assertObjectHasAttribute('alias', $oSale->model, __LINE__);
        $this->assertSame($aGeneratedModel['alias'], $oSale->model->alias, __LINE__);

        $this->assertObjectHasAttribute('code', $oSale->model, __LINE__);
        $this->assertSame($aGeneratedModel['ya_code'], $oSale->model->code, __LINE__);

        $this->assertObjectHasAttribute('cyrillic', $oSale->model, __LINE__);
        $this->assertSame($aGeneratedModel['cyrillic_name'], $oSale->model->cyrillic, __LINE__);

        if ($aGeneratedModel['auto_group']) {
            $this->assertObjectHasAttribute('auto_group', $oSale, __LINE__);
            $this->assertSame($aGeneratedModel['auto_group'], $oSale->auto_group, __LINE__);
        }

        if ($aGeneratedModel['auto_segment']) {
            $this->assertObjectHasAttribute('auto_segment', $oSale, __LINE__);
            $this->assertSame($aGeneratedModel['auto_segment'], $oSale->auto_segment, __LINE__);
        }
    }

    private function _checkGeneration($oSale, $aGeneratedGeneration)
    {
        $this->assertObjectHasAttribute('generation', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->generation, __LINE__);

        $this->assertObjectHasAttribute('name', $oSale->generation, __LINE__);
        $this->assertSame($aGeneratedGeneration['name'], $oSale->generation->name, __LINE__);

        $this->assertObjectHasAttribute('alias', $oSale->generation, __LINE__);
        $this->assertSame($aGeneratedGeneration['alias'], $oSale->generation->alias, __LINE__);

        $this->assertObjectHasAttribute('code', $oSale->generation, __LINE__);
        $this->assertSame($aGeneratedGeneration['ya_code'], $oSale->generation->code, __LINE__);

        $this->assertObjectHasAttribute('cyrillic', $oSale->generation, __LINE__);
        $this->assertSame($aGeneratedGeneration['cyrillic_name'], $oSale->generation->cyrillic, __LINE__);

        if ($aGeneratedGeneration['auto_group']) {
            $this->assertObjectHasAttribute('auto_group', $oSale, __LINE__);
            $this->assertSame($aGeneratedGeneration['auto_group'], $oSale->auto_group, __LINE__);
        }

        if ($aGeneratedGeneration['auto_segment']) {
            $this->assertObjectHasAttribute('auto_segment', $oSale, __LINE__);
            $this->assertSame($aGeneratedGeneration['auto_segment'], $oSale->auto_segment, __LINE__);
        }
    }

    private function _checkMark($oSale, $aGeneratedMark)
    {
        $this->assertObjectHasAttribute('mark', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->mark, __LINE__);

        $this->assertObjectHasAttribute('name', $oSale->mark, __LINE__);
        $this->assertSame($aGeneratedMark['name'], $oSale->mark->name, __LINE__);

        $this->assertObjectHasAttribute('alias', $oSale->mark, __LINE__);
        $this->assertSame($aGeneratedMark['alias'], $oSale->mark->alias, __LINE__);

        $this->assertObjectHasAttribute('code', $oSale->mark, __LINE__);
        $this->assertSame($aGeneratedMark['ya_code'], $oSale->mark->code, __LINE__);

        $this->assertObjectHasAttribute('cyrillic', $oSale->mark, __LINE__);
        $this->assertSame($aGeneratedMark['cyrillic_name'], $oSale->mark->cyrillic, __LINE__);
    }

    private function _checkContacts($oSale, $aGeneratedContacts)
    {
        $this->assertObjectHasAttribute('user', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->user, __LINE__);

        $this->assertObjectHasAttribute('id', $oSale->user, __LINE__);
        $this->assertSame($aGeneratedContacts['user_id'], $oSale->user->id, __LINE__);

        if ($aGeneratedContacts['allow_messages']) {
            $this->assertObjectHasAttribute('allow_messages', $oSale->user, __LINE__);
            $this->assertTrue($oSale->user->allow_messages, __LINE__);
        }

        $this->assertObjectHasAttribute('poi', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->poi, __LINE__);

        $this->assertObjectHasAttribute('geo_id', $oSale->poi, __LINE__);
        $this->assertSame($aGeneratedContacts['geo_id'], $oSale->poi->geo_id, __LINE__);

        $this->assertObjectHasAttribute('lat', $oSale->poi, __LINE__);
        $this->assertSame($aGeneratedContacts['lat'], $oSale->poi->lat, __LINE__);

        $this->assertObjectHasAttribute('lng', $oSale->poi, __LINE__);
        $this->assertSame($aGeneratedContacts['lng'], $oSale->poi->lng, __LINE__);

        $this->assertObjectHasAttribute('seller', $oSale, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->seller, __LINE__);

        $this->assertObjectHasAttribute('type', $oSale->seller, __LINE__);
        $this->assertSame($aGeneratedContacts['type'], $oSale->seller->type, __LINE__);

        if ($aGeneratedContacts['hide_phones']) {
            $this->assertObjectHasAttribute('hide_phones', $oSale->seller, __LINE__);
            $this->assertTrue($oSale->seller->hide_phones, __LINE__);
        }

        $this->assertObjectHasAttribute('phones', $oSale->seller, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_ARRAY, $oSale->seller->phones, __LINE__);

        // У телефонов проверяем только формат
        foreach ($oSale->seller->phones as $phone) {
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_ARRAY, $phone, __LINE__);

            $this->assertArrayHasKey('phone', $phone, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $phone['phone'], __LINE__);

            $this->assertArrayHasKey('call_from', $phone, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $phone['call_from'], __LINE__);

            $this->assertArrayHasKey('call_till', $phone, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $phone['call_till'], __LINE__);

            $this->assertArrayHasKey('phone_mask', $phone, __LINE__);
            $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_STRING, $phone['phone_mask'], __LINE__);
        }

        $this->assertObjectHasAttribute('salon', $oSale->seller, __LINE__);
        $this->assertInternalType(\PHPUnit_Framework_Constraint_IsType::TYPE_OBJECT, $oSale->seller->salon, __LINE__);

        $this->assertObjectHasAttribute('id', $oSale->seller->salon, __LINE__);
        $this->assertSame($aGeneratedContacts['salon']['id'], $oSale->seller->salon->id, __LINE__);

        $this->assertObjectHasAttribute('hash', $oSale->seller->salon, __LINE__);
        $this->assertSame($aGeneratedContacts['salon']['hash'], $oSale->seller->salon->hash, __LINE__);

        $this->assertObjectHasAttribute('title', $oSale->seller->salon, __LINE__);
        $this->assertSame($aGeneratedContacts['salon']['title'], $oSale->seller->salon->title, __LINE__);

        $this->assertObjectHasAttribute('description', $oSale->seller->salon, __LINE__);
        $this->assertSame($aGeneratedContacts['salon']['description'], $oSale->seller->salon->description, __LINE__);

        $this->assertObjectHasAttribute('url', $oSale->seller->salon, __LINE__);
        $this->assertSame($aGeneratedContacts['salon']['url'], $oSale->seller->salon->url, __LINE__);

        $this->assertObjectHasAttribute('sales_count', $oSale->seller->salon, __LINE__);
        $this->assertSame($aGeneratedContacts['salon']['sales_count'], $oSale->seller->salon->sales_count, __LINE__);

        $this->assertObjectHasAttribute('logo', $oSale->seller, __LINE__);
        $this->assertSame($aGeneratedContacts['logo'], $oSale->seller->logo, __LINE__);

        $this->assertObjectHasAttribute('is_premium', $oSale->seller, __LINE__);
        $this->assertSame((bool) $aGeneratedContacts['is_premium'], $oSale->seller->is_premium, __LINE__);

        $this->assertObjectHasAttribute('is_gold_partner', $oSale->seller, __LINE__);
        $this->assertSame((bool) $aGeneratedContacts['is_gold_partner'], $oSale->seller->is_gold_partner, __LINE__);

        $this->assertObjectHasAttribute('date', $oSale->seller, __LINE__);
        $this->assertSame($aGeneratedContacts['date'], $oSale->seller->date, __LINE__);

        $this->assertObjectHasAttribute('is_dealer', $oSale->seller, __LINE__);
        $this->assertSame((bool) $aGeneratedContacts['is_dealer'], $oSale->seller->is_dealer, __LINE__);

        $this->assertObjectHasAttribute('client_id', $oSale->seller, __LINE__);
        $this->assertSame($aGeneratedContacts['client_id'], $oSale->seller->client_id, __LINE__);

        $this->assertObjectHasAttribute('name', $oSale->seller, __LINE__);
        $this->assertSame($aGeneratedContacts['name'], $oSale->seller->name, __LINE__);
    }
}
