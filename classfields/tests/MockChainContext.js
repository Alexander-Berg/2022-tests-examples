const _ = require('lodash');
let contextId = 0;

class MockChainContext {
    static [Symbol.hasInstance](obj) {
        if (obj && obj.constructor === MockChainContext) return true;
    }

    constructor({
        upperContext,
        registeredVariants = [],
        data,
    } = {}) {
        this._id = contextId++;
        this._registeredVariants = registeredVariants;
        this._upperContext = upperContext;
        this._data = _.cloneDeepWith(data, (value) => {
            if (value && (value instanceof MockChainContext.MockChain || value instanceof MockChainContext)) {
                return value;
            }
        });

        const self = this;

        this._proxy = new Proxy(self._data, {
            get(currentData, name) {
                if (self._registeredVariants[name]) {
                    return (...args) => {
                        const newData = self._registeredVariants[name].call(self, currentData, ...args);

                        if (newData) {
                            if (Array.isArray(currentData)) {
                                currentData.splice(0, currentData.length, ...newData);
                            } else {
                                const propsToDelete = _.difference(
                                    Object.keys(currentData),
                                    Object.keys(newData)
                                );

                                propsToDelete.forEach((prop) => {
                                    delete currentData[prop];
                                });

                                Object.keys(newData).forEach((prop) => {
                                    currentData[prop] = newData[prop];
                                });
                            }
                        }

                        return self;
                    };
                }

                if (currentData[name]) {
                    if (currentData[name] instanceof MockChainContext.MockChain) {
                        currentData[name] = currentData[name].createRunContext(self);
                    }

                    if (currentData[name] instanceof MockChainContext) {
                        return currentData[name];
                    } else if (currentData[name] instanceof Object && !(currentData[name] instanceof Function)) {
                        currentData[name] = new MockChainContext({
                            upperContext: self,
                            data: currentData[name],
                        });
                    }
                }

                return currentData[name];
            },

            set(currentData, name, value) {
                currentData[name] = value;
                return true;
            },

            has(currentData, name) {
                if (self._registeredVariants[name]) {
                    return true;
                }

                return name in currentData;
            },
        });

        const newProto = Object.create(MockChainContext.prototype);

        newProto.up = MockChainContext.prototype.up;
        newProto.value = MockChainContext.prototype.value;
        newProto.constructor = MockChainContext.prototype.constructor;
        newProto.fillByProps = MockChainContext.prototype.fillByProps;

        newProto.__proto__ = this._proxy;

        this.__proto__ = newProto;
    }

    up() {
        return this._upperContext;
    }

    value() {
        return _.cloneDeepWith(this._data, (value) => {
            if (value && (value instanceof MockChainContext.MockChain || value instanceof MockChainContext)) {
                return value.value();
            }
        });
    }

    fillByProps(newData) {
        newData = _.cloneDeepWith(this._data, (value, key) => {
            if (key in newData) {
                const newValue = newData[key];

                if (value && (value instanceof MockChainContext.MockChain || value instanceof MockChainContext)) {
                    return value.fillByProps(newValue);
                }

                return newValue;
            }
        });

        Object.keys(this._data).forEach((key) => {
            this._data[key] = newData[key];
        });

        return this;
    }
}

module.exports = MockChainContext;
