import convertFormDataToApiData from './convertFormDataToApiData';

const mock = {
    address: 'улица ленина',
    birthDate: '1987-04-21T00:00:00Z',
    birthPlace: 'гор.ярославль',
    fmsCode: '760-002',
    fmsUnit: 'ОТДЕЛЕНИЕМ УФМС РОССИИ ПО ЯРОСЛАВСКОЙ ОБЛ. В ЛЕНИНСКОМ РАЙОНЕ Г. ЯРОСЛАВЛЯ',
    name: 'Игорь ',
    surname: 'Сергеевич ',
    patronymic: 'Стуев ',
    passport: '1234123456',
    passportDate: '2018-02-11T00:00:00Z',
    phone: '+79112223344',
    role: 'SELLER',
};

it('должен правильно перевести из формата в формы в формат api', () => {
    const expected = {
        by_buyer: {
            person_profile_update: {
                name_entity: {
                    name: 'Игорь',
                    patronymic: 'Стуев',
                    surname: 'Сергеевич',
                },
                passport_rf_entity: {
                    address: 'улица ленина',
                    birth_date: '1987-04-21T00:00:00Z',
                    birth_place: 'гор.ярославль',
                    depart_code: '760-002',
                    depart_name: 'ОТДЕЛЕНИЕМ УФМС РОССИИ ПО ЯРОСЛАВСКОЙ ОБЛ. В ЛЕНИНСКОМ РАЙОНЕ Г. ЯРОСЛАВЛЯ',
                    issue_date: '2018-02-11T00:00:00Z',
                    number: '123456',
                    series: '1234',
                },
                phone_entity: {
                    phone: '+79112223344',
                },
            },
        },
    };

    expect(convertFormDataToApiData(mock)).toEqual(expected);
});
