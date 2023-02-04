export function createBlackbox(options = { methods: {} }) {
    function runMethod(methodName, opts) {
        const method = options.methods[methodName];

        if (! method) {
            console.error(`Method ${methodName} is not mocked`); // eslint-disable-line no-console
            return Promise.reject(`Method ${methodName} is not mocked`);
        }
        const responseP = method(opts);

        if (! responseP.then) {
            throw 'Response is not a promise. Probably need to wrap mock response in Promise.resolve()';
        }

        return responseP;
    }

    return { runMethod };
}

export function createBlackboxMock(methods) {
    return {
        methods,
        extend(newMethods) {
            return createBlackboxMock({ ...methods, ...newMethods });
        }
    };
}
