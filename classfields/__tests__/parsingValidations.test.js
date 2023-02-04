import { getAreasValidations } from 'view/libs/parsingValidations';

describe('getAreasValidations', () => {
    const defaultValidation = {
        area: false,
        lotArea: true,
        kitchenSpace: true,
        livingSpace: true
    };

    it('right area validation for category APARTMENT', () => {
        const incorrectAreaValidation = {
            ...defaultValidation
        };
        const correctAreaValidation = {
            ...defaultValidation,
            area: true
        };

        expect(getAreasValidations({ area: '', category: 'APARTMENT' })).toMatchObject(incorrectAreaValidation);
        expect(getAreasValidations({ category: 'APARTMENT' })).toMatchObject(incorrectAreaValidation);
        expect(getAreasValidations({ area: 10, category: 'APARTMENT' })).toMatchObject(correctAreaValidation);
        expect(getAreasValidations({ area: '123.10', category: 'APARTMENT' })).toMatchObject(correctAreaValidation);
    });

    it('right area and lotArea validations for category HOUSE', () => {
        const correctAreaAndLotAreaValidation = {
            ...defaultValidation,
            area: true
        };

        const incorrectAreaAndLotAreaValidation = {
            ...defaultValidation,
            area: false,
            lotArea: false
        };

        expect(getAreasValidations({ area: 12, category: 'HOUSE' }))
            .toMatchObject(correctAreaAndLotAreaValidation);
        expect(getAreasValidations({ lotArea: '12.3', category: 'HOUSE' }))
            .toMatchObject(correctAreaAndLotAreaValidation);
        expect(getAreasValidations({ area: '12.5', lotArea: 12, category: 'HOUSE' }))
            .toMatchObject(correctAreaAndLotAreaValidation);
        expect(getAreasValidations({ category: 'HOUSE' }))
            .toMatchObject(incorrectAreaAndLotAreaValidation);
        expect(getAreasValidations({ area: '', category: 'HOUSE' }))
            .toMatchObject(incorrectAreaAndLotAreaValidation);
        expect(getAreasValidations({ lotArea: '', category: 'HOUSE' }))
            .toMatchObject(incorrectAreaAndLotAreaValidation);
    });

    it('right kitchenSpace validation', () => {
        const correctKitchenSpaceWithAreaValidation = {
            ...defaultValidation,
            area: true
        };

        const incorrectKitchenSpaceWithAreaValidation = {
            ...defaultValidation,
            area: true,
            kitchenSpace: false
        };

        const incorrectKitchenSpaceWithLotAreaValidation = {
            ...defaultValidation,
            lotArea: true,
            kitchenSpace: false
        };

        expect(getAreasValidations({ area: '12.3', category: 'APARTMENT', kitchenSpace: '12.9' }))
            .toMatchObject(incorrectKitchenSpaceWithAreaValidation);
        expect(getAreasValidations({ lotArea: 1, category: 'APARTMENT', kitchenSpace: '122.9' }))
            .toMatchObject(incorrectKitchenSpaceWithLotAreaValidation);
        expect(getAreasValidations({ area: '12.3', category: 'APARTMENT', kitchenSpace: 1 }))
            .toMatchObject(correctKitchenSpaceWithAreaValidation);
        expect(getAreasValidations({ area: '12.3', category: 'APARTMENT' }))
            .toMatchObject(correctKitchenSpaceWithAreaValidation);
    });

    it('right LivingSpace validation', () => {
        const correctLivingSpaceWithAreaValidation = {
            ...defaultValidation,
            area: true
        };

        const incorrectLivingSpaceAreaValidation = {
            ...defaultValidation,
            area: true,
            livingSpace: false
        };

        expect(getAreasValidations({ area: '12.3', category: 'APARTMENT', livingSpace: '12.9' }))
            .toMatchObject(incorrectLivingSpaceAreaValidation);
        expect(getAreasValidations({ area: '12.3', category: 'APARTMENT', livingSpace: 1 }))
            .toMatchObject(correctLivingSpaceWithAreaValidation);
        expect(getAreasValidations({ area: '12.3', category: 'APARTMENT' }))
            .toMatchObject(correctLivingSpaceWithAreaValidation);
    });
});
