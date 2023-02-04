import { ItemSimple } from '@yandex-lego/components/Menu';

import { getDetailData, getDocTypes } from '../utils';
import { PrintDocType } from 'common/constants';
import { getInitialState } from 'common/reducers/edit-client';
import { DetailId } from 'common/types/client';

describe('admin - editclient', () => {
    describe('блокировка поля Агентство', () => {
        it('задан идентификатор агентства, поле блокируется', () => {
            //@ts-ignore
            delete window.location;
            //@ts-ignore
            window.location = {
                search: '?agency-id=1000'
            };
            let state = getInitialState();
            let dd = getDetailData({
                editClient: state,
                name: DetailId.isAgency,
                perms: []
            });
            expect(dd.disabled).toBeTruthy();
            //@ts-ignore
            window.location = {
                search: '?AGENCY_ID=1000'
            };
            state = getInitialState();
            dd = getDetailData({
                editClient: state,
                name: DetailId.isAgency,
                perms: []
            });
            expect(dd.disabled).toBeTruthy();
        });
        it('не задан идентификатор агентства, поле не блокируется', () => {
            //@ts-ignore
            delete window.location;
            //@ts-ignore
            window.location = {
                search: '?agency-id='
            };
            let state = getInitialState();
            let dd = getDetailData({
                editClient: state,
                name: DetailId.isAgency,
                perms: []
            });
            expect(dd.disabled).toBeFalsy();
        });
        it('редактирование клиента, поле блокируется', () => {
            let state = getInitialState().setIn(['client', 'id'], 1000);
            let dd = getDetailData({
                editClient: state,
                name: DetailId.isAgency,
                perms: []
            });
            expect(dd.disabled).toBeTruthy();
        });
    });
    describe('проставление звездочки обязательного поля', () => {
        describe('нерезидент', () => {
            it('не заполнены Валюта расчетов и/или Полное название', () => {
                let state = getInitialState().setIn(['client', DetailId.isNonResident], true);
                let dd = getDetailData({
                    editClient: state,
                    name: DetailId.isoCurrencyPayment,
                    perms: []
                });
                expect(dd.required).toBeTruthy();
                dd = getDetailData({
                    editClient: state,
                    name: DetailId.fullname,
                    perms: []
                });
                expect(dd.required).toBeTruthy();
            });
        });
    });
    describe('значения выпадающего списка Типы печатных документов', () => {
        it('резидент', () => {
            const docTypes: ItemSimple[] = getDocTypes({
                isNonResident: false,
                isAgency: false,
                printableDocsType: ''
            });
            expect(docTypes.length).toBe(2);
            expect(docTypes[0].value).toBe('0');
            expect(docTypes[1].value).toBe('2');
        });

        it('нерезидент', () => {
            const docTypes: ItemSimple[] = getDocTypes({
                isNonResident: true,
                isAgency: false,
                printableDocsType: ''
            });
            expect(docTypes.length).toBe(2);
            expect(docTypes[0].value).toBe('1');
            expect(docTypes[1].value).toBe('3');
        });

        it('агентство', () => {
            const docTypes: ItemSimple[] = getDocTypes({
                isNonResident: false,
                isAgency: true,
                printableDocsType: ''
            });
            expect(docTypes.length).toBe(Object.keys(PrintDocType).length);
        });
    });
});
