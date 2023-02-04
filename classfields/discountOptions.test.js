const _ = require('lodash');

// Mocks
jest.mock('./formChange');
const formChange = require('./formChange');
formChange.mockImplementation(() => {
    return {
        type: 'FORM_CHANGE',
    };
});
const mockStore = require('autoru-frontend/mocks/mockStore').default;

// Constants
const PRICE = 100000000;
const STORE_MOCK = {
    currencies: { data: { RUR: '1', USD: '65' } },
    formFields: {
        data: {
            price: { value: PRICE },
            currency: { value: 'RUR' },
        },
    },
    state: {
        techOptionsSection: {},
    },
};

// Actions
const { changeDiscountOptions } = require('./discountOptions');

let store;
beforeEach(() => {
    store = mockStore(STORE_MOCK);
});

it('должен вызвать formChange с переданными discount_options', async() => {
    const expected = {
        discount_options: {
            max_discount: {
                message: '',
                value: 10,
            },
            message: '',
        },
    };

    await store.dispatch(changeDiscountOptions({
        max_discount: { value: 10 },
    }));

    expect(formChange).toHaveBeenCalledWith(expected);
});

it('должен добавить валидационное сообщение о превышении скидкой цены', async() => {
    const expected = {
        discount_options: {
            credit: {
                message: 'Скидка должна быть меньше цены ТС',
                value: PRICE,
            },
            message: '',
        },
    };

    await store.dispatch(changeDiscountOptions({
        credit: { value: PRICE },
    }));

    expect(formChange).toHaveBeenCalledWith(expected);
});

it('должен добавить валидационное сообщение о превышении размера максимальной скидки, если максимальная скидка больше половины цены', async() => {
    const discount = PRICE / 2 + 1;
    const expected = {
        discount_options: {
            max_discount: {
                message: `Недопустимое значение максимальной скидки. Проверьте размер указанных скидок.
    Если вы действительно готовы продавать данное ТС по цене ${ PRICE - discount } ₽,
    пожалуйста, обратитесь в техническую поддержку help@auto.yandex.ru`,
                value: discount,
            },
            message: '',
        },
    };

    await store.dispatch(changeDiscountOptions({
        max_discount: { value: discount },
    }));

    expect(formChange).toHaveBeenCalledWith(expected);
});

it('должен ограничить скидку максимальным значением', async() => {
    const expected = {
        discount_options: {
            credit: {
                message: 'Скидка должна быть меньше цены ТС',
                value: 1000000000,
            },
            message: '',
        },
    };

    await store.dispatch(changeDiscountOptions({
        credit: { value: Number.MAX_SAFE_INTEGER },
    }));

    expect(formChange).toHaveBeenCalledWith(expected);
});

it('должен добавить общее валидационное сообщение о превышении размера максимальной скидки, если сумма скидок больше половины цены', async() => {
    const discount = Math.floor(PRICE / 3);
    const expected = {
        discount_options: {
            credit: {
                message: '',
                value: discount,
            },
            insurance: {
                message: '',
                value: discount,
            },
            message: `Недопустимое значение максимальной скидки. Проверьте размер указанных скидок.
    Если вы действительно готовы продавать данное ТС по цене ${ PRICE - 2 * discount } ₽,
    пожалуйста, обратитесь в техническую поддержку help@auto.yandex.ru`,
        },
    };

    await store.dispatch(changeDiscountOptions({
        credit: { value: discount },
        insurance: { value: discount },
    }));

    expect(formChange).toHaveBeenCalledWith(expected);
});

it('не должен добавлять общее валидационное сообщение о превышении размера максимальной скидки, если максимальная скидка невалидна', async() => {
    const maxDiscount = PRICE / 2 + 1;
    const discount = Math.floor(PRICE / 3);
    const expected = {
        discount_options: {
            credit: {
                message: '',
                value: discount,
            },
            insurance: {
                message: '',
                value: discount,
            },
            max_discount: {
                message: `Недопустимое значение максимальной скидки. Проверьте размер указанных скидок.
    Если вы действительно готовы продавать данное ТС по цене ${ PRICE - maxDiscount } ₽,
    пожалуйста, обратитесь в техническую поддержку help@auto.yandex.ru`,
                value: maxDiscount,
            },
            message: '',
        },
    };

    await store.dispatch(changeDiscountOptions({
        credit: { value: discount },
        insurance: { value: discount },
        max_discount: { value: maxDiscount },
    }));

    expect(formChange).toHaveBeenCalledWith(expected);
});

describe('цена в долларах, а скидка всегда в рублях', () => {
    let store;
    beforeEach(() => {
        const storeMock = _.cloneDeep(STORE_MOCK);
        storeMock.formFields.data.currency.value = 'USD';
        storeMock.formFields.data.price.value = 200;
        store = mockStore(storeMock);
    });

    it('должен вызвать formChange с переданными discount_options без валидационных сообщений', async() => {
        const expected = {
            discount_options: {
                credit: {
                    message: '',
                    value: 1299,
                },
                message: '',
            },
        };

        await store.dispatch(changeDiscountOptions({
            credit: { value: 1299 },
        }));

        expect(formChange).toHaveBeenCalledWith(expected);
    });

    it('должен добавить валидационное сообщение о превышении скидкой цены', async() => {
        const expected = {
            discount_options: {
                credit: {
                    message: 'Скидка должна быть меньше цены ТС',
                    value: 13500,
                },
                message: '',
            },
        };

        await store.dispatch(changeDiscountOptions({
            credit: { value: 13500 },
        }));

        expect(formChange).toHaveBeenCalledWith(expected);
    });
});

it('должен сохранить значение touched', async() => {
    const expected = {
        discount_options: {
            credit: {
                message: '',
                value: 1234,
            },
            message: '',
            touched: true,
        },
    };

    await store.dispatch(changeDiscountOptions({
        credit: { value: 1234 },
        touched: true,
    }));

    expect(formChange).toHaveBeenCalledWith(expected);
});
