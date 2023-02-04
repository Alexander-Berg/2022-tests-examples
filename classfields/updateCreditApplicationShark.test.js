const de = require('descript');
const createContext = require('auto-core/server/descript/createContext');
const { toCamel } = require('snake-camel');

const updateCreditApplicationShark = require('./updateCreditApplicationShark');

const sharkApi = require('auto-core/server/resources/baseHttpBlockSharkApi.nock.fixtures');

const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const createHttpRes = require('autoru-frontend/mocks/createHttpRes');

const borrowerPersonProfile = {
    name: {
        nameEntity: {
            name: 'Петр',
            surname: 'Алексеев',
            patronymic: 'Сергеевич',
        },
    },

    foreignPassport: {
        no: {},
    },

    passportRf: {
        passportRfEntity: {
            series: '123123',
            number: '33333',
            issueDate: '2017-10-24T00:00:00Z',
            departCode: '456-676',
            departName: 'ОТДЕЛЕНИЕМ ОФМС РОССИИ ПО РЕСП. АДЫГЕЯ В Г. МАЙКОПЕ',
        },
    },

    phones: {
        phoneEntities: [
            {
                phone: '1111111111',
                phoneType: 'ADDITIONAL',
            },
            {
                phone: '2222222222',
                phoneType: 'WORK',
            },
            {
                phone: '1234567890',
                phoneType: 'PERSONAL',
            },
        ],
    },
};

let context;
let req;
let res;
beforeEach(() => {
    req = createHttpReq();
    res = createHttpRes();
    context = createContext({ req, res });
});

it('AUTORUFRONT-19286: проверяем что адреса дописываются', () => {
    const registrationAddress = {
        addressEntity: { id: 'test' },
    };
    sharkApi
        .post('/api/1.x/credit-application/get', {
            byId: {
                creditApplicationId: 2,
            },
        })
        .reply(200, {
            creditApplication: {
                state: 'DRAFT',
                borrowerPersonProfile: {
                    ...borrowerPersonProfile,
                    registrationAddress: registrationAddress,
                },
            },
            result: { ok: {} },
        });

    sharkApi
        .post('/api/1.x/credit-application/update/2', {
            state: 'ACTIVE',
            borrowerPersonProfile: {
                ...borrowerPersonProfile,
                registrationAddress: registrationAddress,
                residenceAddress: registrationAddress,
            },

            info: {
                controlWord: {
                    word: '!!!',
                },
            },
        })
        .reply(200, {
            creditApplication: {
                state: 'ACTIVE',
                info: {
                    controlWord: {
                        word: '!!!',
                    },
                },
                borrowerPersonProfile: {
                    ...borrowerPersonProfile,
                    registrationAddress: registrationAddress,
                },
            },
            result: { ok: {} },
        });

    return de.run(updateCreditApplicationShark, {
        params: {
            id: 2,
            body: {
                state: 'ACTIVE',
                info: {
                    control_word: {
                        word: '!!!',
                    },
                },
                borrower_person_profile: toCamel(borrowerPersonProfile),
                flags: { is_the_same_address: true },
            },
        },
        context,
    })
        .then(result => {
            expect(result).toMatchSnapshot();
        });
});

it('загружает и обновляет кредитную заявку дозволенными данными', () => {
    sharkApi
        .post('/api/1.x/credit-application/get', {
            byId: {
                creditApplicationId: 1,
            },
        })
        .reply(200, {
            creditApplication: {
                state: 'DRAFT',
                borrowerPersonProfile,
            },
            result: { ok: {} },
        });

    sharkApi
        .post('/api/1.x/credit-application/update/1', {
            state: 'ACTIVE',
            borrowerPersonProfile,

            info: {
                controlWord: {
                    word: 'олала!',
                },
            },
        })
        .reply(200, {
            creditApplication: {
                state: 'ACTIVE',
                info: {
                    controlWord: {
                        word: 'олала!',
                    },
                },
                borrowerPersonProfile,
            },
            result: { ok: {} },
        });

    return de.run(updateCreditApplicationShark, {
        params: {
            id: 1,
            body: {
                state: 'ACTIVE',
                info: {
                    control_word: {
                        word: 'олала!',
                    },
                },
                borrower_person_profile: toCamel(borrowerPersonProfile),
            },
        },
        context,
    })
        .then(result => {
            expect(result).toMatchSnapshot();
        });
});
