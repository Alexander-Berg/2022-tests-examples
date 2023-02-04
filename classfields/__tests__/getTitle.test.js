/* eslint-disable max-len*/
import stubs from 'realty-core/app/test-utils/stubs';

import { titleBuild } from '..';

import specs from './specs';

describe('offer card get seo title', () => {
    describe('for residental category', () => {
        it('returns correct title for selling apartments', () => {
            expect(titleBuild(specs.sellAppartmentOffer))
                .toEqual('Купить 2-комнатную квартиру 79,7\u00a0м² по адресу Москва, Нахимовский проспект, 73 - id 4290853562119205321 - по цене 16,5 млн руб. на Яндекс.Недвижимости');
        });

        it('returns title with correct roomsTotal', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                roomsTotal: 4
            })).toContain('Купить 4-комнатную квартиру');
        });

        it('returns title with correct area', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                area: {
                    value: 100,
                    unit: 'ARE'
                }
            })).toContain('100\u00a0соток');
        });

        it('returns title with correct address', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                location: { address: 'Моя родная улица' }
            })).toContain('по адресу Моя родная улица');
        });

        it('returns title without address if it not specified', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                location: { address: null }
            })).not.toContain('по адресу');
        });

        it('returns title with correct price', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                price: {
                    currency: 'USD',
                    value: 4500000,
                    period: 'WHOLE_LIFE',
                    unit: 'WHOLE_OFFER',
                    trend: 'UNCHANGED',
                    valuePerPart: 207026,
                    unitPerPart: 'SQUARE_METER',
                    valueForWhole: 4500000,
                    unitForWhole: 'WHOLE_OFFER'
                }
            })).toContain('по цене $ 4,5 млн');
        });

        it('returns title with correct deal type', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                offerType: 'RENT'
            })).toContain('Снять 2-комнатную квартиру');
        });

        it('returns title with correct rent period', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                offerType: 'RENT',
                price: {
                    ...specs.sellAppartmentOffer.price,
                    period: 'PER_DAY'
                }
            })).toContain('Снять (посуточно) 2-комнатную квартиру');
        });

        it('returns title with correct category for a room', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                offerType: 'RENT',
                offerCategory: 'ROOMS',
                roomsOffered: 1
            })).toContain('Снять комнату в 2-комнатной квартире');
        });

        it('returns title with correct category for a house', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                offerCategory: 'HOUSE',
                house: { houseType: 'TOWNHOUSE' }
            })).toContain('Купить таунхаус');
        });

        it('returns title with correct category for a lot', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                offerCategory: 'LOT'
            })).toContain('Купить участок');
        });

        it('returns title with correct category for a garage', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                offerCategory: 'GARAGE'
            })).toContain('Купить гараж, машиноместо, бокс ');
        });

        it('returns correct title for selling apartments in site', () => {
            expect(titleBuild(specs.sellAppartmentOffer, specs.siteCard))
                .toEqual('Купить 2-комнатную квартиру 79,7\u00a0м² в клубном доме «Делагарди» по адресу Москва, Нахимовский проспект, 73 - id 4290853562119205321 - по цене 16,5 млн руб. на Яндекс.Недвижимости');
        });

        it('должен корректно выводить апартаменты', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                house: {
                    ...specs.sellAppartmentOffer.house,
                    apartments: true
                }
            }))
                .toEqual('Купить 2-комнатные апартаменты 79,7\u00a0м² по адресу Москва, Нахимовский проспект, 73 - id 4290853562119205321 - по цене 16,5 млн руб. на Яндекс.Недвижимости');
        });

        it('должен корректно выводить студию в апартаментах', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                house: {
                    ...specs.sellAppartmentOffer.house,
                    apartments: true,
                    studio: true
                }
            }))
                .toEqual('Купить апартаменты-студию 79,7\u00a0м² по адресу Москва, Нахимовский проспект, 73 - id 4290853562119205321 - по цене 16,5 млн руб. на Яндекс.Недвижимости');
        });

        it('должен корректно выводить комнату в апартаментах', () => {
            expect(titleBuild({
                ...specs.sellAppartmentOffer,
                house: {
                    ...specs.sellAppartmentOffer.house,
                    apartments: true
                },
                offerCategory: 'ROOMS',
                roomsOffered: 1
            }))
                .toEqual('Купить комнату в 2-комнатных апартаментах 31,3\u00a0м² по адресу Москва, Нахимовский проспект, 73 - id 4290853562119205321 - по цене 16,5 млн руб. на Яндекс.Недвижимости');
        });
    });

    describe('for non-residental category', () => {
        it('returns correct title for renting free-purpose commercial area', () => {
            expect(titleBuild({
                ...specs.rentCommercialFreePurposeOffer,
                regionInfo: {
                    rgid: 176337,
                    name: 'Москва',
                    locative: 'в Москве',
                    type: 'CITY',
                    parents: [
                        {
                            id: 213,
                            rgid: '587795',
                            name: 'Москва',
                            type: 'CITY'
                        },
                        {
                            id: 1,
                            rgid: '741964',
                            name: 'Москва и МО',
                            type: 'SUBJECT_FEDERATION'
                        },
                        {
                            id: 225,
                            rgid: '143',
                            name: 'Россия',
                            type: 'COUNTRY'
                        },
                        {
                            id: 0,
                            rgid: '0',
                            name: 'Весь мир',
                            type: 'UNKNOWN'
                        }
                    ]
                }
            })).toEqual('Снять помещение свободного назначения без посредников по адресу Москва, Братиславская улица, 5 - id 7785392089289599744 - по цене 108 000 руб. в месяц: аренда помещения свободного назначения в Москве на Яндекс.Недвижимости.');
        });

        it('returns correct title for garage sell', () => {
            expect(titleBuild({
                ...specs.saleGarageOffer,
                regionInfo: {
                    rgid: 176337,
                    name: 'Москва',
                    locative: 'в Москве',
                    type: 'CITY',
                    parents: [
                        {
                            id: 213,
                            rgid: '587795',
                            name: 'Москва',
                            type: 'CITY'
                        },
                        {
                            id: 1,
                            rgid: '741964',
                            name: 'Москва и МО',
                            type: 'SUBJECT_FEDERATION'
                        },
                        {
                            id: 225,
                            rgid: '143',
                            name: 'Россия',
                            type: 'COUNTRY'
                        },
                        {
                            id: 0,
                            rgid: '0',
                            name: 'Весь мир',
                            type: 'UNKNOWN'
                        }
                    ]
                }
            })).toEqual('Купить гараж, машиноместо, бокс по адресу Санкт-Петербург, Гаражный проезд, 2 - id 2444317576373650176 - по цене 2 млн руб.: продажа гаражей, машиномест, боксов в Москве на Яндекс.Недвижимости.');
        });
    });

    describe('to match snapshot', () => {
        stubs.everySearcherOfferMatchSnapshot(titleBuild, { checkVos: false });
    });
});
