import safeDealMock from 'auto-core/react/dataDomain/safeDeal/mocks/safeDeal.mock';

import convertApiDataToFormData from './convertApiDataToFormData';

it('должен правильно перевести формат из api в формат формы', () => {
    const expected = {
        address: 'улица ленина',
        birthDate: '1987-04-21T00:00:00Z',
        birthPlace: 'гор.ярославль',
        fmsCode: '760-002',
        fmsUnit: 'ОТДЕЛЕНИЕМ УФМС РОССИИ ПО ЯРОСЛАВСКОЙ ОБЛ. В ЛЕНИНСКОМ РАЙОНЕ Г. ЯРОСЛАВЛЯ',
        name: 'Игорь',
        surname: 'Сергеевич',
        patronymic: 'Стуев',
        passport: '1234123456',
        passportDate: '2018-02-11T00:00:00Z',
        phone: '79112223344',
    };
    const profile = {
        profile: safeDealMock.deal.party?.seller?.person_profile,
    };
    expect(convertApiDataToFormData(profile)).toEqual(expected);
});
