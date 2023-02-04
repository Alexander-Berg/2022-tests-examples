import { ApartmentType } from 'realty-core/types/siteCard';

import { getSiteApartmentPreparedType } from '../getSiteApartmentPreparedType';

import { getSiteCard } from './mocks';

describe('getSiteApartmentPreparedType', () => {
    it('В случае, если не определены isApartment и apartmentType возвращает FLATS', () => {
        const result = getSiteApartmentPreparedType(getSiteCard());

        expect(result).toBe(ApartmentType.FLATS);
    });

    it('В случае, если в качестве аргументов передан пустой объект возвращает FLATS', () => {
        const result = getSiteApartmentPreparedType(getSiteCard({}));

        expect(result).toBe('FLATS');
    });

    it('Возвращает FLATS в случае, если передан только isApartment равен false', () => {
        const result = getSiteApartmentPreparedType(getSiteCard({ isApartment: false }));

        expect(result).toBe('FLATS');
    });

    it('Возвращает APARTMENTS в случае, если передан только isApartment равен false', () => {
        const result = getSiteApartmentPreparedType(getSiteCard({ isApartment: true }));

        expect(result).toBe('APARTMENTS');
    });

    it('Возвращает переданное в apartmentType (кроме UNKNOWN)', () => {
        const apartmentTypes: ApartmentType[] = [
            ApartmentType.FLATS,
            ApartmentType.APARTMENTS,
            ApartmentType.APARTMENTS_AND_FLATS,
        ];

        apartmentTypes.forEach((apartmentType) => {
            const result1 = getSiteApartmentPreparedType(getSiteCard({ apartmentType }));
            const result2 = getSiteApartmentPreparedType(getSiteCard({ apartmentType, isApartment: true }));
            const result3 = getSiteApartmentPreparedType(getSiteCard({ apartmentType, isApartment: false }));

            expect(result1).toBe(apartmentType);
            expect(result2).toBe(apartmentType);
            expect(result3).toBe(apartmentType);
        });
    });

    it('Возвращает FLATS если apartmentType равен UNKNOWN и isApartment не передан', () => {
        const result = getSiteApartmentPreparedType(getSiteCard({ apartmentType: ApartmentType.UNKNOWN }));

        expect(result).toBe('FLATS');
    });

    it('Возвращает FLATS если apartmentType равен UNKNOWN и isApartment равен false', () => {
        const result = getSiteApartmentPreparedType(
            getSiteCard({ apartmentType: ApartmentType.UNKNOWN, isApartment: false })
        );

        expect(result).toBe('FLATS');
    });

    it('Возвращает APARTMENTS если apartmentType равен UNKNOWN и isApartment равен true', () => {
        const result = getSiteApartmentPreparedType(
            getSiteCard({ apartmentType: ApartmentType.UNKNOWN, isApartment: true })
        );

        expect(result).toBe('APARTMENTS');
    });
});
