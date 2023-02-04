import noop from 'lodash/noop';

export const url = 'https://test.link.yandex.ru/shared-favorites/JUvxwp-OcZf.G8osc';

export const store = {
    user: {
        favorites: [],
    },
};

export const GateError = {
    create: () => Promise.reject(),
};

export const GateSuccess = {
    create: () => Promise.resolve({ url }),
};

export const GatePending = {
    create: () => new Promise(noop),
};
