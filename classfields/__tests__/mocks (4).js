export default {
    adAgencyClientMock: {
        user: {
            permissions: [ 'edit_any_client' ]
        },
        client: {
            profile: {
                data: {
                    name: 'Летний Сад',
                    clientType: 'CLIENT_TYPE_DEVELOPER',
                    clientId: 'bil_36119272',
                    adAgency: { uid: '403388763', name: 'Лидер Инвест (23 ЖК)', clientId: '403388763' },
                    balances: [ {
                        sum: 0,
                        description: 'Яндекс.Комм.Недвижимость, Летний Сад',
                        lastIncomeDate: '2018-11-06T12:45:53.114Z'
                    } ],
                    users: [ { name: 'romanpoleguev', uid: '1120000000112888' } ],
                    logins: [ {
                        login: 'realestate-mgcom',
                        compartnerLink: 'https://compartner.realty.test.vertis.yandex.ru/feeds?shadow_name=realestate-mgcom'
                    } ],
                    regions: [ 'Москва и МО' ],
                    phones: [ '+74951060833' ],
                    emails: [ 'etalon.mgcom.realty@gmail.com' ],
                    siteDescription: 'ЖК «Летний Сад» расположен в Дмитровском районе Москвы...',
                    note: '',
                    links: {
                        site: 'http://etalonsad.ru',
                        pd: '',
                        moderation: 'https://moderation.test.vertis.yandex-team.ru/realty?author_uid=bil_36119272',
                        balance: 'https://admin-balance.greed-ts.paysys.yandex.ru/passports.xml?tcl_id=36119272'
                    },
                    offers: { total: 248, errored: 248, valid: 0, vas: 0 },
                    clients: [],
                    campaignsStats: {
                        village: { },
                        site: { }
                    }
                },
                network: {
                    bindUsersStatus: 'loaded',
                    bindNoteStatus: 'loaded',
                    bindPipeDriveStatus: 'loaded'
                }
            }
        }
    },
    normalClientMock: {
        user: {
            permissions: [ 'edit_any_client' ]
        },
        client: {
            profile: {
                data: {
                    id: '531782539',
                    name: 'ООО «Метриум Премиум»',
                    clientType: 'CLIENT_TYPE_AGENCY',
                    clientId: '531782539',
                    balances: [ {
                        sum: 1535000,
                        description: 'Яндекс.Недвижимость, ООО «Метриум - Жилая недвижимость»',
                        lastIncomeDate: '2019-03-21T07:26:07.307Z'
                    }, { sum: 0, description: 'Яндекс.Комм.Недвижимость, ООО «Метриум - Жилая недвижимость»' } ],
                    users: [ { name: 'romanpoleguev', uid: '1120000000112888' } ],
                    logins: [ { login: 'metrium-elite' } ],
                    regions: [ 'Москва и МО', 'Республика Крым' ],
                    phones: [ '+74992702020' ],
                    emails: [ 'vitaliy.kozin@metrium.ru' ],
                    siteDescription: '',
                    note: '',
                    campaignsStats: {
                        village: { total: 87, active: 78, disabled: 9 },
                        site: { total: 95, active: 51, disabled: 44 }
                    },
                    links: {
                        site: 'http://www.metrium.ru',
                        lk: 'https://realty.test.vertis.yandex.ru/management-new/?moderator_mode_required&vos_user_login=531782539',
                        feeds: 'https://realty.test.vertis.yandex.ru/management-new/feeds/?moderator_mode_required&vos_user_login=531782539',
                        verba: 'https://verba2.test.vertis.yandex-team.ru/#!services/realty/companies/709333',
                        pd: '',
                        moderation: 'https://moderation.test.vertis.yandex-team.ru/realty?author_uid=531782539',
                        balance: 'https://admin-balance.greed-ts.paysys.yandex.ru/passports.xml?tcl_id=34760732'
                    },
                    offers: { total: 341, errored: 1, valid: 340, vas: 0 },
                    clients: []
                }
            }
        }
    },
    adAgencyMock: {
        user: {
            permissions: [ 'edit_any_client' ]
        },
        client: {
            profile: {
                data: {
                    id: '514776617',
                    name: 'Петровский квартал',
                    clientType: 'CLIENT_TYPE_AD_AGENCY',
                    clientId: '514776617',
                    balances: [],
                    users: [],
                    logins: [ {
                        login: 'focus-realty',
                        compartnerLink: 'https://compartner.realty.test.vertis.yandex.ru/feeds?shadow_name=focus-realty'
                    } ],
                    regions: [ 'Московская область', 'Москва и МО' ],
                    phones: [ '+79264846919', '+74951250912' ],
                    emails: [
                        'n.sivale@focus-marketing-group.ru',
                        'focus-realty@yandex.ru'
                    ],
                    siteDescription: 'Seven ONE – это современный бизнес-центр класса ...',
                    note: '',
                    links: {
                        site: 'https://gogolpark.ru/',
                        pd: '',
                        moderation: 'https://moderation.test.vertis.yandex-team.ru/realty?author_uid=514776617',
                        balance: 'https://admin-balance.greed-ts.paysys.yandex.ru/passports.xml?tcl_id=33776163'
                    },
                    offers: { total: 1208, errored: 155, valid: 1053, vas: 0 },
                    campaignsStats: {
                        village: { total: 87, active: 78, disabled: 9 },
                        site: { total: 95, active: 51, disabled: 44 }
                    }
                }
            }
        }
    }
};
