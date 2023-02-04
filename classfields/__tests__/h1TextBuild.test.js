import stubs from 'realty-core/app/test-utils/stubs';

import { h1Build } from '..';

import specs from './specs';

/* eslint-disable max-len*/
describe('offer card get h1 text', () => {
    describe('for residental category', () => {
        it('должен возвращать заголовок для продажи', () => {
            expect(h1Build(specs.sellAppartmentOffer))
                .toEqual('Купить 2-комнатную квартиру 79,7\u00a0м², 18/24\u00a0этаж');
        });

        it('должен возвращать заголовок для аренды', () => {
            expect(h1Build({
                ...specs.sellAppartmentOffer,
                offerType: 'RENT'
            })).toEqual('Снять 2-комнатную квартиру 79,7\u00a0м², 18/24\u00a0этаж');
        });

        it('должен возвращать заголовок для квартиры с указанием количества комнат', () => {
            expect(h1Build({
                ...specs.sellAppartmentOffer,
                roomsTotal: 7
            })).toEqual('Купить 7-комнатную квартиру 79,7\u00a0м², 18/24\u00a0этаж');
        });

        it('должен возвращать заголовок для квартиры без указания этажности здания', () => {
            expect(h1Build({
                ...specs.sellAppartmentOffer,
                floorsTotal: undefined
            })).toEqual('Купить 2-комнатную квартиру 79,7\u00a0м², 18\u00a0этаж');
        });

        it('должен возвращать заголовок для квартиры без указания количества комнат', () => {
            expect(h1Build({
                ...specs.sellAppartmentOffer,
                roomsTotal: undefined
            })).toEqual('Купить квартиру 79,7\u00a0м², 18/24\u00a0этаж');
        });

        it('должен возвращать заголовок для комнаты с указанием количества комнат', () => {
            expect(h1Build({
                ...specs.sellAppartmentOffer,
                offerCategory: 'ROOMS',
                roomsOffered: 1
            })).toEqual('Купить комнату в 2-комнатной квартире 31,3\u00a0м², 18/24\u00a0этаж');
        });

        it('должен возвращать заголовок для комнаты без указания количества комнат', () => {
            expect(h1Build({
                ...specs.sellAppartmentOffer,
                offerCategory: 'ROOMS',
                roomsTotal: undefined,
                roomsOffered: 1
            })).toEqual('Купить комнату в квартире 31,3\u00a0м², 18/24\u00a0этаж');
        });

        it('должен возвращать заголовок для дома', () => {
            expect(h1Build({
                ...specs.saleHouseOffer
            })).toEqual('Купить дом 155\u00a0м² с участком 10\u00a0соток');
        });

        it('должен возвращать заголовок для части дома', () => {
            expect(h1Build({
                ...specs.saleHouseOffer,
                house: {
                    housePart: true,
                    houseType: 'PARTHOUSE'
                }
            })).toEqual('Купить часть дома 155\u00a0м² с участком 10\u00a0соток');
        });

        it('должен корректно выводить апартаменты', () => {
            expect(h1Build({
                ...specs.sellAppartmentOffer,
                house: {
                    ...specs.sellAppartmentOffer.house,
                    apartments: true
                }
            })).toEqual('Купить 2-комнатные апартаменты 79,7\u00a0м², 18/24\u00a0этаж');
        });

        it('должен корректно выводить комнату в апартаментах', () => {
            expect(h1Build({
                ...specs.sellAppartmentOffer,
                house: {
                    ...specs.sellAppartmentOffer.house,
                    apartments: true
                },
                offerCategory: 'ROOMS',
                roomsOffered: 1
            })).toEqual('Купить комнату в 2-комнатных апартаментах 31,3\u00a0м², 18/24\u00a0этаж');
        });

        it('должен корректно выводить студию в апартаментах', () => {
            expect(h1Build({
                ...specs.sellAppartmentOffer,
                house: {
                    ...specs.sellAppartmentOffer.house,
                    apartments: true,
                    studio: true
                }
            })).toEqual('Купить апартаменты-студию 79,7\u00a0м², 18/24\u00a0этаж');
        });

        it('должен возвращать заголовок для квартиры в ЖК', () => {
            expect(h1Build(specs.sellAppartmentOffer, specs.siteCard))
                .toEqual('Купить 2-комнатную квартиру 79,7\u00a0м², 18/24\u00a0этаж в клубном доме «Делагарди»');
        });
    });

    describe('for commercial category', () => {
        it('returns correct h1 for renting free-purpose commercial area', () => {
            expect(h1Build({
                ...specs.rentCommercialFreePurposeOffer
            })).toEqual('Помещение свободного назначения, 70\u00a0м²');
        });

        it('returns correct title for garage sell', () => {
            expect(h1Build({
                ...specs.saleGarageOffer
            })).toEqual('Гараж, 30\u00a0м²');
        });
    });

    describe('to match snapshot', () => {
        stubs.everySearcherOfferMatchSnapshot(h1Build, { checkVos: false });
    });
});
