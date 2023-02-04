const getAllureMock = () => {
    const mock = {};

    return new Proxy(mock, {
        get(target, prop, receiver) {
            return Reflect.get(target, prop, receiver) || (() => {});
        }
    });
};

module.exports = {
    getAllureMock
};
