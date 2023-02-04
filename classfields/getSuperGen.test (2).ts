import salesMarkModels from '../mocks/salesMarkModels';

import getSuperGen from './getSuperGen';

it('должен уметь поддерживать строку в качестве параметр mark_model', () => {
    expect(
        getSuperGen(
            { super_gen: [
                '21398591', //поколение для марки BMW
                '7879464',
            ] },
            { mark_model: 'AUDI#100' },
            salesMarkModels),
    ).toEqual([ '7879464' ]);
});

it('должен уметь поддерживать строку в качестве параметр super_gen', () => {
    expect(
        getSuperGen(
            { super_gen: '21398591' },
            { mark_model: 'AUDI#100' },
            salesMarkModels),
    ).toEqual([ ]);
});

it('должен удалить поколение, если пользователь удалил марку', () => {
    expect(
        getSuperGen(
            { super_gen: [
                '21398591', //поколение для марки BMW
                '7879464',
            ] },
            { mark_model: [ 'AUDI#100' ] },
            salesMarkModels),
    ).toEqual([ '7879464' ]);
});

it('должен удалить поколение, если пользователь удалил модель', () => {
    expect(
        getSuperGen(
            { super_gen: [
                '7879464',
            ] },
            { mark_model: [ 'AUDI' ] },
            salesMarkModels),
    ).toEqual([ ]);
});
