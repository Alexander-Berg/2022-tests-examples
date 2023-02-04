import { pluralize } from 'realty-core/view/react/libs/pluralize';
import { getOwnerText, getEGRNFloorDiscrepancy, getEGRNAreaDiscrepancy } from '../';
import t from '../i18n';

describe('EGRNgetOwnerText', () => {
    it('should return default texts when passed no arguments', () => {
        expect(getOwnerText()).toEqual({
            title: t('owner'),
            description: t('fullOwnership')
        });
    });

    it('should return default texts when passed empty owner array', () => {
        expect(getOwnerText([])).toEqual({
            title: t('owner'),
            description: t('fullOwnership')
        });
    });

    it('should return joint ownership texts when owner array length is greater than 1', () => {
        expect(getOwnerText([ 'owner1', 'owner2' ])).toEqual({
            title: pluralize('owner', 2),
            description: t('jointOwnership')
        });
    });

    it('should return type + name as title when both name and title are present in owner object', () => {
        expect(getOwnerText([ { name: 'Ромашка', type: 'JURIDICAL_PERSON' } ])).toEqual({
            title: `${t('ownerTypeJuridical')} Ромашка`,
            description: t('fullOwnership')
        });
    });

    it('should return ownership type as title when name isn\'t present in owner object', () => {
        expect(getOwnerText([ { type: 'JURIDICAL_PERSON' } ])).toEqual({
            title: t('ownerTypeJuridical'),
            description: t('fullOwnership')
        });
    });

    it('should return name as title when type is NATURAL_PERSON', () => {
        expect(getOwnerText([ { name: 'Петя', type: 'NATURAL_PERSON' } ])).toEqual({
            title: 'Петя',
            description: t('fullOwnership')
        });
    });

    it('should return name as title when type is not present in owner object', () => {
        expect(getOwnerText([ { name: 'Петя' } ])).toEqual({
            title: 'Петя',
            description: t('fullOwnership')
        });
    });

    it('should return ownership share if it is passed', () => {
        expect(getOwnerText([ { type: 'JURIDICAL_PERSON' } ], '1/2')).toEqual({
            title: t('ownerTypeJuridical'),
            description: t('partialOwnership', { share: '1/2' })
        });
    });
});

const createOfferMock = ({ egrnFloorString, egrnArea, offerFloor, offerArea } = {}) => ({
    excerptReport: {
        flatReport: {
            buildingInfo: {
                floorString: egrnFloorString,
                area: egrnArea
            }
        }
    },
    area: {
        value: offerArea
    },
    floorsOffered: [ offerFloor ]
});

describe('getEGRNFloorDiscrepancy', () => {
    it('should return discrepancy when egrnFloorString and offerFloor are not equal', () => {
        const offer = createOfferMock({ egrnFloorString: '1', offerFloor: 2 });

        expect(getEGRNFloorDiscrepancy(offer)).toEqual({
            EGRNFloor: '1',
            offerFloor: 2,
            hasDiscrepancy: true
        });
    });

    it('should return no discrepancy when egrnFloorString and offerFloor are equal', () => {
        const offer = createOfferMock({ egrnFloorString: '1', offerFloor: 1 });

        expect(getEGRNFloorDiscrepancy(offer)).toEqual({
            EGRNFloor: '1',
            offerFloor: 1,
            hasDiscrepancy: false
        });
    });

    it('should return discrepancy when egrnFloorString is a non-number string and offerFloor is a number', () => {
        const offer = createOfferMock({ egrnFloorString: 'чердак', offerFloor: 1 });

        expect(getEGRNFloorDiscrepancy(offer)).toEqual({
            EGRNFloor: 'чердак',
            offerFloor: 1,
            hasDiscrepancy: true
        });
    });

    it('should return discrepancy when egrnFloorString is a non-number string and offerFloor is falsy', () => {
        const offer = createOfferMock({ egrnFloorString: 'чердак' });

        expect(getEGRNFloorDiscrepancy(offer)).toEqual({
            EGRNFloor: 'чердак',
            hasDiscrepancy: true
        });
    });

    it('should return falsy EGRNFloor when egrnFloorString is falsy', () => {
        const offer = createOfferMock({ offerFloor: 1 });

        expect(getEGRNFloorDiscrepancy(offer)).toEqual({
            offerFloor: 1,
            hasDiscrepancy: true
        });
    });

    it('should return falsy EGRNFloor when offerFloor is falsy', () => {
        const offer = createOfferMock();

        expect(getEGRNFloorDiscrepancy(offer)).toEqual({
            hasDiscrepancy: true
        });
    });
});

describe('getEGRNAreaDiscrepancy', () => {
    it('should return no discrepancy if difference between egrn and offer is <= 1', () => {
        const offer = createOfferMock({ egrnArea: 10, offerArea: 9 });

        expect(getEGRNAreaDiscrepancy(offer)).toEqual({
            EGRNArea: 10,
            offerArea: 9,
            hasDiscrepancy: false
        });
    });

    it('should return discrepancy if difference between egrn and offer is > 1', () => {
        const offer = createOfferMock({ egrnArea: 10, offerArea: 8.9 });

        expect(getEGRNAreaDiscrepancy(offer)).toEqual({
            EGRNArea: 10,
            offerArea: 8.9,
            hasDiscrepancy: true
        });
    });

    it('should return no discrepancy if diff between egrn and offer is >= 1 and < 4% of offer area', () => {
        const offer = createOfferMock({ egrnArea: 96, offerArea: 100 });

        expect(getEGRNAreaDiscrepancy(offer)).toEqual({
            EGRNArea: 96,
            offerArea: 100,
            hasDiscrepancy: false
        });
    });

    it('should return discrepancy if diff between egrn and offer is > 4% of offer area', () => {
        const offer = createOfferMock({ egrnArea: 96, offerArea: 101 });

        expect(getEGRNAreaDiscrepancy(offer)).toEqual({
            EGRNArea: 96,
            offerArea: 101,
            hasDiscrepancy: true
        });
    });
});
