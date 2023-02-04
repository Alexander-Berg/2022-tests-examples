import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';

import getCurrentCarParametersText from './getCurrentCarParametersText';

describe('getCurrentCarParametersText', () => {
    it('вернет текст параметров автомобиля', () => {
        const card = {
            vehicle_info: {
                documents: {
                    year: 2020,
                },
                car_info: {
                    tech_param: {
                        displacement: 2000,
                        power: 239,
                    },
                },
            },
        } as Card;

        const actual = getCurrentCarParametersText(card);

        expect(actual).toBe('2020, 2.0 л, 239 л.с.');
    });

    it('вернет текст параметров автомобиля без объема двигателя', () => {
        const card = {
            vehicle_info: {
                documents: {
                    year: 2020,
                },
                car_info: {
                    tech_param: {
                        power: 239,
                    },
                },
            },
        } as Card;

        const actual = getCurrentCarParametersText(card);

        expect(actual).toBe('2020, 239 л.с.');
    });

    it('вернет текст параметров автомобиля без мощности', () => {
        const card = {
            vehicle_info: {
                documents: {
                    year: 2020,
                },
                car_info: {
                    tech_param: {
                        displacement: 2000,
                    },
                },
            },
        } as Card;

        const actual = getCurrentCarParametersText(card);

        expect(actual).toBe('2020, 2.0 л');
    });

    it('вернет текст параметров автомобиля без года', () => {
        const card = {
            vehicle_info: {
                car_info: {
                    tech_param: {
                        displacement: 2000,
                        power: 239,
                    },
                },
            },
        } as Card;

        const actual = getCurrentCarParametersText(card);

        expect(actual).toBe('2.0 л, 239 л.с.');
    });
});
