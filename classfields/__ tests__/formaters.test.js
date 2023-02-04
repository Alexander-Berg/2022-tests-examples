import formaters from '../formaters';

describe('apartment number formater', () => {
    const formatter = apartment => formaters.apartment_number({ apartment });
    const rightApartmentNumbers = [ '420', '17а', 'K-19', 'c4', '7-я' ];
    const wrongApartmentNumbers = [ 'кв', '-кв', 'wrong', 'o-1o', 'o1-o', 'o1o' ];

    it.each(rightApartmentNumbers)('%s is valid apartment number', number => {
        expect(formatter(number)).toBeNull();
    });

    it.each(wrongApartmentNumbers)('%s is invalid apartment number', number => {
        expect(formatter(number)).not.toBeNull();
    });
});

describe('floor number for no comertical formaters', () => {
    it('checks floor number formater', () => {
        const formatter = formaters.floor_minFloor;

        expect(formatter({ category: 'COMMERCIAL', floor: -1 })).toBeNull();
        expect(formatter({ category: 'APARTMENT', floor: 1 })).toBeNull();

        expect(formatter({ category: 'APARTMENT', floor: -1 })).not.toBeNull();
        expect(formatter({ category: 'COMMERCIAL', floor: -6 })).not.toBeNull();
    });
});
