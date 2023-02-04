/* eslint-disable max-len */
import { DeepPartial } from 'utility-types';

import { IStore } from 'view/common/reducers';

export const getStore = (): DeepPartial<IStore> => ({
    billingRequisites: {
        ids: ['14170490', '14170277', '13475104', '134751041'],
        byId: {
            '13475104': {
                id: '13475104',
                name: 'ztuzmin6',
                longname: 'ztuzmin6',
                phone: '+79042166212',
                email: 'ztuzmin6@yandex.ru',
                legaladdress: 'dddddddd',
                inn: '7730108183',
                kpp: '773001001',
                postcode: '33333333',
                postaddress: 'dddddddd',
                hidden: false,
                personType: 'ur',
                clientId: 187700886,
                date: '2020-12-25T14:56:11+03:00',
            },
            '134751041': {
                id: '13475104',
                name: 'nokpp',
                longname: 'nokpp',
                phone: '+79042166212',
                email: 'nokpp@yandex.ru',
                legaladdress: 'nokpp',
                inn: '7730108183',
                postcode: '33333333',
                postaddress: 'nokpp',
                hidden: false,
                personType: 'ur',
                clientId: 187700886,
                date: '2020-12-25T14:56:11+03:00',
            },
            '14170277': {
                id: '14170277',
                lname: 'Башмачкин',
                fname: 'Акакий',
                mname: 'Акакиевич',
                phone: '+74953222222',
                email: 'ztuzmin6@yandex.ru',
                legaladdress: '',
                is_partner: false,
                hidden: false,
                personType: 'ph',
                clientId: 187700886,
                date: '2021-02-15T12:11:58+03:00',
            },
            '14170490': {
                id: '14170490',
                name: 'Лютики',
                longname: 'ООО "Лютики"',
                phone: '+79453333333',
                email: 'ztuzmin6@yandex.ru',
                legaladdress:
                    'Мурманская область, городской округ Полярные Зори, населённый пункт Африканда, Африканда-2, улица Ленина, 2Б',
                inn: '7730108183',
                kpp: '773001001',
                postcode: '333333',
                postaddress: 'а/я 44',
                hidden: false,
                personType: 'ur',
                clientId: 187700886,
                date: '2021-02-15T12:28:47+03:00',
            },
        },
    },
    settings: {
        requisites: {
            name: {
                id: 'name',
                value: '',
                shouldDisplayMessage: false,
            },
            email: {
                id: 'email',
                value: '',
                shouldDisplayMessage: false,
            },
            phone: {
                id: 'phone',
                value: '',
                shouldDisplayMessage: false,
            },
            legaladdress: {
                id: 'legaladdress',
                value: '',
                shouldDisplayMessage: false,
            },
            postaddress: {
                id: 'postaddress',
                value: '',
                shouldDisplayMessage: false,
            },
            postcode: {
                id: 'postcode',
                value: '',
                shouldDisplayMessage: false,
            },
            kpp: {
                id: 'kpp',
                value: '',
                shouldDisplayMessage: false,
            },
            required_kpp: {
                id: 'required_kpp',
                value: '',
                shouldDisplayMessage: false,
            },
            inn: {
                id: 'inn',
                value: '',
                shouldDisplayMessage: false,
            },
            personType: {
                id: 'personType',
                value: 'ur',
                shouldDisplayMessage: false,
            },
            fname: {
                id: 'fname',
                value: '',
                shouldDisplayMessage: false,
            },
            lname: {
                id: 'lname',
                value: '',
                shouldDisplayMessage: false,
            },
            mname: {
                id: 'mname',
                value: '',
                shouldDisplayMessage: false,
            },
        },
    },
});
