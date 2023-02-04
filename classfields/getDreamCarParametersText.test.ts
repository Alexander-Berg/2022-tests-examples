import { ndash } from 'auto-core/react/lib/html-entities';

import type { Card } from 'auto-core/types/proto/auto/api/vin/garage/garage_api_model';

import getDreamCarParametersText from './getDreamCarParametersText';

describe('getDreamCarParametersText', () => {
    it('вернет текст параметров автомобиля мечты', () => {
        const card = {
            vehicle_info: {
                car_info: {
                    super_gen: {
                        year_from: 2016,
                        year_to: 2018,
                        name: 'IV',
                    },
                    tech_param: {
                        displacement: 2000,
                        power: 239,
                    },
                },
            },
        } as Card;

        const actual = getDreamCarParametersText(card);

        expect(actual).toBe(`2016 ${ ndash } 2018 IV, 2.0 л, 239 л.с.`);
    });

    it('вернет текст параметров автомобиля мечты без объема двигателя', () => {
        const card = {
            vehicle_info: {
                car_info: {
                    super_gen: {
                        year_from: 2016,
                        year_to: 2018,
                        name: 'IV',
                    },
                    tech_param: {
                        power: 239,
                    },
                },
            },
        } as Card;

        const actual = getDreamCarParametersText(card);

        expect(actual).toBe(`2016 ${ ndash } 2018 IV, 239 л.с.`);
    });

    it('вернет текст параметров автомобиля без мощности', () => {
        const card = {
            vehicle_info: {
                car_info: {
                    super_gen: {
                        year_from: 2016,
                        year_to: 2018,
                        name: 'IV',
                    },
                    tech_param: {
                        displacement: 2000,
                    },
                },
            },
        } as Card;

        const actual = getDreamCarParametersText(card);

        expect(actual).toBe(`2016 ${ ndash } 2018 IV, 2.0 л`);
    });

    it('вернет текст параметров автомобиля без поколения', () => {
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

        const actual = getDreamCarParametersText(card);

        expect(actual).toBe('2.0 л, 239 л.с.');
    });

});
