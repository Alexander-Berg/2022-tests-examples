import { IOfferData } from 'realty-core/view/react/common/types/egrnPaidReport';

import { getOfferFeatures } from '../';

describe('getOfferFeatures', () => {
    it('правильно парсит этаж числом', () => {
        expect(getOfferFeatures({ numberOfFloors: '6', floor: '4' } as IOfferData)[0].value).toEqual(
            '4\u00A0из\u00A06'
        );
    });

    it('правильно парсит этаж строкой', () => {
        expect(getOfferFeatures({ numberOfFloors: '6', floor: 'чердак' } as IOfferData)[0].value).toEqual('чердак');
    });

    it('правильно форматирует площадь объекта', () => {
        expect(getOfferFeatures({ area: 12 } as IOfferData)[0].value).toEqual('12\u00A0м²');
        expect(getOfferFeatures({ area: 12.4 } as IOfferData)[0].value).toEqual('12,4\u00A0м²');
    });

    it('правильно форматирует площадь кухни объекта', () => {
        expect(getOfferFeatures({ area: 12 } as IOfferData)[0].value).toEqual('12\u00A0м²');
        expect(getOfferFeatures({ kitchenArea: 12.4 } as IOfferData)[0].value).toEqual('12,4\u00A0м²');
    });

    it('правильно форматирует высоту потолков в сантиметрах', () => {
        expect(getOfferFeatures({ ceilingHeight: 124 } as IOfferData)[0].value).toEqual('1,24\u00A0м');
        expect(getOfferFeatures({ ceilingHeight: 122.4 } as IOfferData)[0].value).toEqual('1,224\u00A0м');
    });
});
