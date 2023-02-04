import type { Salon } from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import getSingleOfficialMark from './getSingleOfficialMark';

describe('getSingleOfficialMark', () => {
    it('вернула инфу про марку, если в car_marks одна марка', () => {
        const dealerMock = {
            car_marks: [
                {
                    code: 'DATSUN',
                    logo: {
                        sizes: {
                            logo: 'pips',
                        },
                    },
                    name: 'Datsun',
                    ru_name: 'Датсун',
                },
            ],
        } as unknown as Salon;

        expect(getSingleOfficialMark(dealerMock)).toEqual({
            code: 'DATSUN',
            logo: {
                sizes: {
                    logo: 'pips',
                },
            },
            name: 'Datsun',
            ru_name: 'Датсун',
        });
    });

    it('вернула undefined, если в car_marks больше одной марки', () => {
        const dealerMock = {
            car_marks: [
                {},
                {
                    code: 'DATSUN',
                    logo: {
                        sizes: {
                            logo: 'pips',
                        },
                    },
                    name: 'Datsun',
                    ru_name: 'Датсун',
                },
                {},
            ],
        } as unknown as Salon;

        expect(getSingleOfficialMark(dealerMock)).toBeUndefined();
    });

    it('вернула undefined, если нет car_marks', () => {
        const dealerMock = {} as unknown as Salon;

        expect(getSingleOfficialMark(dealerMock)).toBeUndefined();
    });

    it('вернула undefined, если передали null', () => {
        expect(getSingleOfficialMark(null)).toBeUndefined();
    });
});
