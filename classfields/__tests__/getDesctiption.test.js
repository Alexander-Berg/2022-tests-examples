/* eslint-disable max-len*/
import stubs from 'realty-core/app/test-utils/stubs';

import { descriptionBuild } from '..';

import specs from './specs';

describe('offer card get seo description', () => {
    describe('for residental category', () => {
        it('returns correct description for selling apartments', () => {
            expect(descriptionBuild(specs.sellAppartmentOffer))
                .toEqual('Купить жильё - 79,7\u00a0м² по адресу Москва, Нахимовский проспект, 73: фото и подробная информация об объекте. Купить 2-комнатную квартиру - id 4290853562119205321 - по цене 207 026 ₽ за м² на Яндекс.Недвижимости.');
        });

        it('returns correct description for selling apartments in site', () => {
            expect(descriptionBuild(specs.sellAppartmentOffer, specs.siteCard))
                .toEqual('Клубный дом «Делагарди» по адресу Москва, Нахимовский проспект, 73 - купить 2-комнатную квартиру 79,7\u00a0м² - id 4290853562119205321 - по цене 207 026 ₽ за м². Фото, транспортная доступность, инфраструктура и подробная информация об объекте.');
        });

        it('должен корректно выводить апартаменты', () => {
            expect(descriptionBuild({
                ...specs.sellAppartmentOffer,
                house: {
                    ...specs.sellAppartmentOffer.house,
                    apartments: true
                }
            }))
                .toEqual('Купить жильё - 79,7\u00a0м² по адресу Москва, Нахимовский проспект, 73: фото и подробная информация об объекте. Купить 2-комнатные апартаменты - id 4290853562119205321 - по цене 207 026 ₽ за м² на Яндекс.Недвижимости.');
        });

        it('должен корректно выводить студию в апартаментах', () => {
            expect(descriptionBuild({
                ...specs.sellAppartmentOffer,
                house: {
                    ...specs.sellAppartmentOffer.house,
                    apartments: true,
                    studio: true
                }
            }))
                .toEqual('Купить жильё - 79,7\u00a0м² по адресу Москва, Нахимовский проспект, 73: фото и подробная информация об объекте. Купить апартаменты-студию - id 4290853562119205321 - по цене 207 026 ₽ за м² на Яндекс.Недвижимости.');
        });

        it('должен корректно выводить комнату в апартаментах', () => {
            expect(descriptionBuild({
                ...specs.sellAppartmentOffer,
                house: {
                    ...specs.sellAppartmentOffer.house,
                    apartments: true
                },
                offerCategory: 'ROOMS',
                roomsOffered: 1
            }))
                .toEqual('Купить жильё - 31,3\u00a0м² по адресу Москва, Нахимовский проспект, 73: фото и подробная информация об объекте. Купить комнату в 2-комнатных апартаментах - id 4290853562119205321 - по цене 207 026 ₽ за м² на Яндекс.Недвижимости.');
        });
    });

    describe('for non-residental category', () => {
        it('returns correct description for renting free-purpose commercial area', () => {
            expect(descriptionBuild({
                ...specs.rentCommercialFreePurposeOffer,
                regionInfo: {
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
            })).toEqual('Сдается помещение свободного назначения без посредников по адресу Москва, Братиславская улица, 5 - id 7785392089289599744. Цена: 108 000 руб. в месяц. Описание: Сдам в аренду помещение свободного назначения по адресу ул.Братиславская, дом 5. Располагается в трех минутах пешком от метро Братиславская(не больше), дом виден от метро. Вход отдельный с фасада (пешеходная зона, проезжая часть, метро). Первая линия домов. 1/17 этажного жилого дома 2000-го года …');
        });

        it('должен корректно склад без описания', () => {
            expect(descriptionBuild({
                ...specs.rentCommercialFreePurposeOffer,
                description: null
            }))
                .toEqual('Сдается помещение свободного назначения без посредников по адресу Москва, Братиславская улица, 5 - id 7785392089289599744. Цена: 108 000 руб. в месяц. Яндекс.Недвижимость: объявления о покупке, продаже, аренде квартир, домов, и коммерческой недвижимости %{region}. Вторичное жилье и новостройки. Офисы и торговые помещения. Цены на недвижимость %{region}.');
        });
    });

    describe('to match snapshot', () => {
        stubs.everySearcherOfferMatchSnapshot(descriptionBuild, { checkVos: false });
    });
});
