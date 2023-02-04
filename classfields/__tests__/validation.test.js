import getActiveControls from 'app/lib/add-form/get-active-controls';
import { getValidOfferData } from '../validation';

describe('getValidOfferData', () => {
    const commonOfferFormData = {
        category: 'ROOMS',
        type: 'SELL',
        builtYear: '-'
    };

    const activeControls = getActiveControls(commonOfferFormData);

    it('converts non-numeric value to null', () => {
        const result = getValidOfferData(commonOfferFormData, activeControls);

        expect(result).toEqual({
            _apartmentRequired: true,
            category: 'ROOMS',
            type: 'SELL',
            builtYear: null,
            currency: 'RUB'
        });
    });

    it('converts String value to Number', () => {
        const offerFormData = {
            ...commonOfferFormData,
            builtYear: '123'
        };

        const result = getValidOfferData(offerFormData, activeControls);

        expect(result).toEqual({
            _apartmentRequired: true,
            category: 'ROOMS',
            type: 'SELL',
            builtYear: 123,
            currency: 'RUB'
        });
    });

    it('converts negative String value to Number', () => {
        const offerFormData = {
            ...commonOfferFormData,
            builtYear: '-123'
        };

        const result = getValidOfferData(offerFormData, activeControls);

        expect(result).toEqual({
            _apartmentRequired: true,
            category: 'ROOMS',
            type: 'SELL',
            builtYear: -123,
            currency: 'RUB'
        });
    });

    it('skips incorrect room area values', () => {
        const offerFormData = {
            ...commonOfferFormData,
            roomsTotal: 4,
            roomsOffered: 3,
            rooms: [ 10, null, 20 ]
        };

        const activeControlsWithRooms = getActiveControls(offerFormData);

        const result = getValidOfferData(offerFormData, activeControlsWithRooms);

        expect(result).toEqual({
            _apartmentRequired: true,
            category: 'ROOMS',
            type: 'SELL',
            builtYear: null,
            currency: 'RUB',
            roomsTotal: 4,
            roomsOffered: 3,
            rooms: [ 10, 20 ]
        });
    });
});
