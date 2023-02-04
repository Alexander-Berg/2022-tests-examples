import { nbsp } from 'auto-core/react/lib/html-entities';

import { mapAppraisalResponseToModel } from './mapAppraisalResponseToModel';

describe('mapAppraisalResponseToModel', () => {
    it('правильно маппит значения по умолчанию', () => {
        const vehicle = mapAppraisalResponseToModel({});

        expect(vehicle).toEqual({
            mileage: '',
            name: '',
            generationName: '',
            engineName: '',
            year: undefined,
            offerId: undefined,
            draftId: undefined,
            garageCardId: undefined,
            description: '',
            imageSrc: '',
            colorHex: '',
        });
    });

    it('правильно маппит полные существующие данные', () => {
        const vehicle = mapAppraisalResponseToModel({
            car_info: {
                mileage: 102,
                mark: 'BMW',
                model: 'X5',
                engine: '730i 2.0 AT (249 л.с.)',
                generation: 'VI (G11/G12) Рестайлинг',
                year: 2020,
            },
            price_range: {
                from: '1000',
                to: '10000',
            },
            offerId: '1',
            draftId: '2',
            garageCardId: '3',
            photo_from_catalog: 'url',
        });

        expect(vehicle).toEqual({
            mileage: '102\u00a0км',
            name: `BMW${ nbsp }X5`,
            generationName: 'VI (G11/G12) Рестайлинг',
            engineName: '730i 2.0 AT (249 л.с.)',
            year: 2020,
            selfSellPriceRange: {
                from: 970,
                to: 11000,
            },
            buyoutPriceRange: {
                from: 1000,
                to: 10000,
            },
            offerId: '1',
            draftId: '2',
            garageCardId: '3',
            description: '',
            imageSrc: 'url',
            colorHex: '',
        });
    });

    it('правильно маппит неполные существующие данные', () => {
        const vehicle = mapAppraisalResponseToModel({
            car_info: {
                mileage: 102,
                mark: 'BMW',
                model: 'X5',
                year: 2020,
            },
            price_range: {
                from: '1000',
                to: '10000',
            },
            offerId: '1',
            draftId: '2',
            garageCardId: '3',
        });

        expect(vehicle).toEqual({
            mileage: '102\u00a0км',
            name: `BMW${ nbsp }X5`,
            generationName: '',
            engineName: '',
            year: 2020,
            selfSellPriceRange: {
                from: 970,
                to: 11000,
            },
            buyoutPriceRange: {
                from: 1000,
                to: 10000,
            },
            offerId: '1',
            draftId: '2',
            garageCardId: '3',
            description: '',
            imageSrc: '',
            colorHex: '',
        });
    });

    it('правильно маппит данные машины по-умолчанию', () => {
        const vehicle = mapAppraisalResponseToModel({
            car_info: {
                mileage: 17000,
                mark: 'SKODA',
                model: 'OCTAVIA',
                year: 2019,
            },
            price_range: {
                from: '1000',
                to: '10000',
            },
            isDefaultVehicle: true,
        });

        expect(vehicle).toEqual({
            mileage: `17${ nbsp }000${ nbsp }км`,
            name: `Skoda${ nbsp }Octavia`,
            year: 2019,
            selfSellPriceRange: {
                from: 970,
                to: 11000,
            },
            buyoutPriceRange: {
                from: 1000,
                to: 10000,
            },
            generationName: '',
            engineName: '',
            description: '',
            imageSrc: '',
            colorHex: '',
            offerId: undefined,
            draftId: undefined,
            garageCardId: undefined,
            isDefaultVehicle: true,
        });
    });
});
