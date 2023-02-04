jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});
jest.mock('auto-core/react/actions/phones', () => {
    return {
        addPhone: jest.fn(),
    };
});
jest.mock('auto-core/react/lib/wwwAuthApi');
jest.mock('auto-core/react/dataDomain/user/actions/renew');
jest.mock('auto-core/react/dataDomain/user/actions/fetchSessionWithUser', () => {
    return {
        'default': jest.fn(() => ({ type: 'actiona_fetchSessionWithUser' })),
    };
});

const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const userpic = require('autoru-frontend/mockData/images/userpic').default;

const LazyPhoneAuth = require('./LazyPhoneAuth');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const userWithAuthMock = require('auto-core/react/dataDomain/user/mocks/withAuth.mock');
const userWithoutAuthMock = require('auto-core/react/dataDomain/user/mocks/withoutAuth.mock');
const userWithPhonesMock = require('auto-core/react/dataDomain/user/mocks/withPhones.mock');
const wwwAuthApi = require('auto-core/react/lib/wwwAuthApi');
const { addPhone } = require('auto-core/react/actions/phones');

const gateApi = require('auto-core/react/lib/gateApi');
gateApi.getResource.mockImplementation(jest.fn());

const authRenew = require('auto-core/react/dataDomain/user/actions/renew').default;
authRenew.mockImplementation(jest.fn(() => () => {}));

const fetchSessionWithUser = require('auto-core/react/dataDomain/user/actions/fetchSessionWithUser').default;

let props;
let initialState;

const PHONE_MOCK = '79991234567';
const CODE_MOCK = '1234';

beforeEach(() => {
    initialState = {
        user: _.cloneDeep(userWithoutAuthMock),
    };
    props = {
        isMobile: false,
        onAuthSuccess: jest.fn(),
    };

    wwwAuthApi.mockReset();
});

describe('при маунте', () => {
    it('если пользователь не авторизован, ничего не будет делать', () => {
        shallowRenderComponent({ initialState, props });

        expect(gateApi.getResource).toHaveBeenCalledTimes(0);
    });

    describe('если пользователь авторизован,', () => {
        it('запросит телефоны, если их нет', () => {
            initialState.user = _.cloneDeep(userWithAuthMock);
            shallowRenderComponent({ initialState, props });

            expect(fetchSessionWithUser).toHaveBeenCalledTimes(1);
        });

        it('если есть телефоны, будет использовать их в саджесте', () => {
            initialState.user = _.cloneDeep(userWithPhonesMock);
            const page = shallowRenderComponent({ initialState, props });

            expect(fetchSessionWithUser).toHaveBeenCalledTimes(0);

            const phoneInput = page.find('.LazyPhoneAuth__phoneInput');
            const userPhones = _.map(userWithPhonesMock.data.phones, 'phone');

            expect(phoneInput.prop('suggest')).toEqual(userPhones);
            expect(phoneInput.prop('initialValue')).toEqual(userPhones[0]);
        });
    });
});

describe('при сабмите телефона', () => {
    it('если этот телефон уже привязан к пользователю, вызовет коллбэк', () => {
        initialState.user = _.cloneDeep(userWithPhonesMock);
        const userPhones = _.map(userWithPhonesMock.data.phones, 'phone');

        const page = shallowRenderComponent({ initialState, props });
        page.find('.LazyPhoneAuth__phoneInput').simulate('submit', null, userPhones[0]);

        expect(props.onAuthSuccess).toHaveBeenCalledTimes(1);
        expect(props.onAuthSuccess).toHaveBeenCalledWith(userPhones[0], { type: 'selected' });
    });

    describe('если передан флаг добавления нового телефона и пользователь авторизован', () => {
        it('запросит код подвтерждения для добавления', () => {
            expect.assertions(2);
            props.shouldAddNewPhone = true;
            initialState.user = _.cloneDeep(userWithAuthMock);
            addPhone.mockImplementationOnce(jest.fn(() => () => Promise.resolve()));

            return submitUserPhoneForAuthUser(PHONE_MOCK)
                .then(() => {
                    expect(addPhone).toHaveBeenCalledTimes(1);
                    expect(wwwAuthApi).toHaveBeenCalledTimes(0);
                });
        });

        it('если подтверждения не требуется, вызовет колбек', () => {
            expect.assertions(2);
            props.shouldAddNewPhone = true;
            initialState.user = _.cloneDeep(userWithAuthMock);
            addPhone.mockImplementationOnce(jest.fn(() => () => Promise.resolve({ status: 'SUCCESS', need_confirm: false })));

            return submitUserPhoneForAuthUser(PHONE_MOCK)
                .then(() => {
                    expect(addPhone).toHaveBeenCalledTimes(1);
                    expect(props.onAuthSuccess).toHaveBeenCalledTimes(1);
                });
        });
    });

    it('если передан флаг, вызовет коллбэк без запроса кода подтверждения', () => {
        initialState.user = _.cloneDeep(userWithAuthMock);
        props.shouldRequestConfirmationCode = false;

        const submitPromise = submitUserPhoneForAuthUser(PHONE_MOCK);

        return submitPromise
            .then(() => {
                expect(props.onAuthSuccess).toHaveBeenCalledTimes(1);
                expect(props.onAuthSuccess).toHaveBeenCalledWith(PHONE_MOCK, { type: 'unconfirmed' });
            });
    });

    describe('если нужен код подтверждения', () => {
        let page;
        const confirmationCodePromise = Promise.resolve([ { status: 'SUCCESS', code_length: CODE_MOCK.length } ]);

        beforeEach(() => {
            wwwAuthApi.mockImplementationOnce(() => confirmationCodePromise);

            page = submitUserPhoneForUnAuthUser(PHONE_MOCK);
        });

        it('запросит код подтверждения', () => {
            expect(wwwAuthApi).toHaveBeenCalledTimes(1);
            expect(wwwAuthApi).toHaveBeenCalledWith([ { params: { phone: PHONE_MOCK }, path: 'auth/login-or-register' } ]);
        });

        it('задизейблит инпут', () => {
            return confirmationCodePromise
                .then(() => {
                    const phoneInput = page.find('.LazyPhoneAuth__phoneInput');
                    expect(phoneInput.prop('disabled')).toBe(true);
                });
        });

        it('покажет инпут ввода кода', () => {
            return confirmationCodePromise
                .then(() => {
                    const codeInput = page.find('.LazyPhoneAuth__codeInput');
                    expect(codeInput).toHaveLength(1);
                });
        });
    });
});

describe('если при получении кода произошла ошибка', () => {
    let page;
    const confirmationCodePromise = Promise.resolve([ {} ]);

    beforeEach(() => {
        wwwAuthApi.mockImplementationOnce(() => confirmationCodePromise);

        page = submitUserPhoneForUnAuthUser(PHONE_MOCK);
    });

    it('отобразит ее на странице', () => {
        return confirmationCodePromise
            .then(() => {
                const error = page.find('.LazyPhoneAuth__error');
                expect(error.text()).toEqual(' Что-то пошло не так, попробуйте еще раз ');
            });
    });

    it('раздизейблит инпут', () => {
        return confirmationCodePromise
            .then(() => {
                const phoneInput = page.find('.LazyPhoneAuth__phoneInput');
                expect(phoneInput.prop('disabled')).toBe(false);
            });
    });
});

describe('при сабмите корректного кода', () => {
    it('проверит его на бэке', () => {
        const submitCodePromise = submitSmsCode(CODE_MOCK);

        return submitCodePromise
            .then(() => {
                expect(wwwAuthApi).toHaveBeenCalledTimes(2);
                expect(wwwAuthApi).toHaveBeenLastCalledWith([ { params: { code: CODE_MOCK, phone: PHONE_MOCK }, path: 'user/confirm' } ]);
            });
    });

    it('обновит авторизацию пользователя', () => {
        const submitCodePromise = submitSmsCode(CODE_MOCK);

        return submitCodePromise
            .then(() => {
                expect(authRenew).toHaveBeenCalledTimes(1);
                expect(authRenew).toHaveBeenCalledWith({
                    auth: true,
                    emails: [
                        {
                            email: 'john.doe@yandex.ru',
                            confirmed: true,
                        },
                    ],
                    id: 1234567,
                    name: 'J.DOE',
                    phones: undefined,
                    profile: {
                        autoru: {
                            alias: 'J.DOE',
                            userpic: {
                                sizes: {
                                    '24x24': userpic,
                                    '48x48': userpic,
                                    '200x200': userpic,
                                },
                            },
                            birthday: '1899-11-26',
                            about: 'привет!',
                            show_card: true,
                            show_mail: true,
                            allow_messages: true,
                            driving_year: 1963,
                            country_id: '1',
                            region_id: '87',
                            city_id: '1123',
                            full_name: 'кукуруз',
                            geo_id: 213,
                            geo_name: 'Москва',
                        },
                    },
                    status: 'user',
                });
            });
    });

    it('вызовет коллбэк', () => {
        const submitCodePromise = submitSmsCode(CODE_MOCK);

        return submitCodePromise
            .then(() => {
                expect(props.onAuthSuccess).toHaveBeenCalledTimes(1);
                expect(props.onAuthSuccess).toHaveBeenCalledWith(PHONE_MOCK, { type: 'confirmed' });
            });
    });
});

it('не будет проверять код на бэке, если его длина меньше чем необходимо', () => {
    const submitCodePromise = submitSmsCode(CODE_MOCK.slice(0, 3));

    return submitCodePromise
        .then(() => {
            expect(wwwAuthApi).toHaveBeenCalledTimes(1);
        });
});

it('если при проверке кода бэк вернул ошибку, покажет её на странице', () => {
    const confirmationCodePromise = Promise.resolve([ { status: 'SUCCESS', code_length: CODE_MOCK.length } ]);
    wwwAuthApi.mockImplementationOnce(() => confirmationCodePromise);
    const checkCodePromise = Promise.reject({ error: { body: { error: 'CONFIRMATION_CODE_NOT_FOUND' } } });
    wwwAuthApi.mockImplementationOnce(() => checkCodePromise);

    const page = submitUserPhoneForUnAuthUser(PHONE_MOCK);

    return confirmationCodePromise
        .then(() => {
            const codeInput = page.find('.LazyPhoneAuth__codeInput');
            codeInput.simulate('change', CODE_MOCK);

            return checkCodePromise;
        })
        .then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            () => {
                const codeInput = page.find('.LazyPhoneAuth__codeInput');
                expect(codeInput.prop('error')).toBe('Неверный код');
            },
        );
});

function submitUserPhoneForAuthUser(phone, sessionUserResponse) {
    const sessionUserPromise = Promise.resolve(sessionUserResponse);
    gateApi.getResource.mockImplementation(() => sessionUserPromise);

    const page = shallowRenderComponent({ initialState, props });

    return sessionUserPromise
        .then(() => {
            const phoneInput = page.update().find('.LazyPhoneAuth__phoneInput');
            phoneInput.simulate('submit', null, phone);
        });
}

function submitUserPhoneForUnAuthUser(phone) {
    const page = shallowRenderComponent({ initialState, props });
    const phoneInput = page.update().find('.LazyPhoneAuth__phoneInput');
    phoneInput.simulate('submit', null, phone);

    return page;
}

function submitSmsCode(code) {
    const confirmationCodePromise = Promise.resolve([ { status: 'SUCCESS', code_length: CODE_MOCK.length } ]);
    wwwAuthApi.mockImplementationOnce(() => confirmationCodePromise);
    const checkCodePromise = Promise.resolve([ { user: userWithAuthMock.data } ]);
    wwwAuthApi.mockImplementationOnce(() => checkCodePromise);

    const page = submitUserPhoneForUnAuthUser(PHONE_MOCK);

    return confirmationCodePromise
        .then(() => {
            const codeInput = page.find('.LazyPhoneAuth__codeInput');
            codeInput.simulate('change', code);

            return checkCodePromise;
        });
}

function shallowRenderComponent({ initialState, props }) {
    const store = mockStore(initialState);

    return shallow(
        <LazyPhoneAuth { ...props } store={ store }/>,
    ).dive();
}
