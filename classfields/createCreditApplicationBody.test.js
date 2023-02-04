const createCreditApplicationBody = require('./createCreditApplicationBody');
const MockDate = require('mockdate');

const timestamp = '2019-10-24T07:24:24.000Z';

beforeEach(() => {
    MockDate.set('2019-10-24 10:24:24');
});

afterEach(() => {
    MockDate.reset();
});

const baseData = {
    fullName: 'Петров Петр Петрович',
    email: 'email@test.ru',
    phone: '79168570321',
    amount: 435700,
    term: 36,
    fee: 127000,
    gender: 'MALE',
    gids: [ 213, 445 ],
};

const basePreparedData = {
    requirements: {
        max_amount: 435700,
        initial_fee: 127000,
        term_months: 36,
        geobase_ids: [ 213, 445 ],
    },
    info: {
        okb_statement_agreement: {
            is_agree: true,
            timestamp,
        },
        advert_statement_agreement: {
            is_agree: true,
            timestamp,
        },
    },
    borrower_person_profile: {
        name: {
            name_entity: {
                name: 'Петр',
                surname: 'Петров',
                patronymic: 'Петрович',
            },
        },
        phones: {
            phone_entities: [ { phone: '79168570321', phone_type: 'PERSONAL' } ],
        },
        emails: {
            email_entities: [ { email: 'email@test.ru' } ],
        },
        gender: {
            gender_type: 'MALE',
        },
    },
    forced_sending: true,
};

it('не добавляет блок с оффером, если оффера нет', () => {
    expect(createCreditApplicationBody(baseData)).toEqual(basePreparedData);
});

it('добавляет блок с оффером, если оффер есть', () => {
    expect(createCreditApplicationBody({
        ...baseData,
        offerID: '11111-22222',
        offerCategory: 'CARS',
    })).toEqual({
        ...basePreparedData,
        payload: {
            autoru: {
                offers: [ {
                    id: '11111-22222',
                    category: 'CARS',
                } ],
            },
        },
    });
});

it('отдает предпочтение уточненным данным по ФИО из dadata', () => {
    const refinedFIO = {
        name: 'Олег',
        surname: 'Олеговский',
        patronymic: 'Олегович',
    };
    const expectedData = {
        ...basePreparedData,
        borrower_person_profile: {
            ...basePreparedData.borrower_person_profile,
            name: { name_entity: refinedFIO },
        },
    };

    expect(createCreditApplicationBody({
        ...baseData,
        ...refinedFIO,
    })).toEqual(expectedData);
});
