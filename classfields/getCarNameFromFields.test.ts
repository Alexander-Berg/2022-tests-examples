import type { C2bCarInfo } from 'auto-core/server/blocks/c2bAuction/types';

import { getCarNameFromFields } from './getCarNameFromFields';

describe('getCarNameFromFields', () => {
    it('возвращает строку, состоящую из марки, модели и года', () => {
        const mockCarInfo = {
            mark: 'Audi',
            model: 'A3',
            year: 2017,
        } as C2bCarInfo;

        expect(getCarNameFromFields(mockCarInfo)).toBe('Audi A3, 2017');
    });
});
