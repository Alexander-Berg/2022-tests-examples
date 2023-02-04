import tSites from 'realty-core/view/react/modules/sites/i18n';

import getSiteAreasRange from '../getSiteAreasRange';

import { getSiteCardWithCustomAreas, getRoomsData } from './mocks';

describe('getSiteAreasRange', () => {
    it('Корректно отрабатывает наличие данных о всех комнатностях', () => {
        const props = {
            common: {
                areaFrom: 24,
                areaTo: 78,
            },
            rooms: getRoomsData(['STUDIO', '1', '2', '3', 'PLUS_4', 'OPEN_PLAN']),
        };

        const result = getSiteAreasRange(getSiteCardWithCustomAreas(props), tSites);

        expect(result).toBe('от\u00A023,3 до\u00A0100\u00A0м²');
    });

    it('Корректно отрабатывает отсутствие данных о части комнатностей', () => {
        const props = {
            common: {
                areaFrom: 24,
                areaTo: 78,
            },
            rooms: getRoomsData(['1', '2', 'PLUS_4']),
        };

        const result = getSiteAreasRange(getSiteCardWithCustomAreas(props), tSites);

        expect(result).toBe('от\u00A034,6 до\u00A0100\u00A0м²');
    });

    it('Корректно отрабатывает при наличии распроданных комнатностей', () => {
        const props = {
            common: {
                areaFrom: 24,
                areaTo: 78,
            },
            rooms: getRoomsData(['STUDIO', '1', '2', '3', 'PLUS_4', 'OPEN_PLAN'], ['STUDIO', '1', 'PLUS_4']),
        };

        const result = getSiteAreasRange(getSiteCardWithCustomAreas(props), tSites);

        expect(result).toBe('от\u00A049,5 до\u00A083,8\u00A0м²');
    });

    it('Корректно отрабатывает фоллбек, если все комнатности распроданны', () => {
        const props = {
            common: {
                areaFrom: 24,
                areaTo: 78,
            },
            rooms: getRoomsData(
                ['STUDIO', '1', '2', '3', 'PLUS_4', 'OPEN_PLAN'],
                ['STUDIO', '1', '2', '3', 'PLUS_4', 'OPEN_PLAN']
            ),
        };

        const result = getSiteAreasRange(getSiteCardWithCustomAreas(props), tSites);

        expect(result).toBe('от\u00A024 до\u00A078\u00A0м²');
    });

    it('Возвращает null при отсутствии данных', () => {
        const result = getSiteAreasRange(getSiteCardWithCustomAreas({}), tSites);

        expect(result).toBe(null);
    });
});
