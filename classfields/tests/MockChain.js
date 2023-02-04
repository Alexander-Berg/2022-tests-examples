const _ = require('lodash');
const MockChainContext = require('./MockChainContext');

class MockChain {
    static [Symbol.hasInstance](obj) {
        if (obj && obj.constructor === MockChain) return true;
    }

    constructor(defaultData = {}) {
        this._registeredVariants = {};

        if (_.isFunction(defaultData)) {
            defaultData = defaultData();
        }

        this._defaultData = _.cloneDeepWith(defaultData, (value) => {
            if (value && (value instanceof MockChain)) {
                return value;
            }
        });

        const self = this;

        this._proxy = new Proxy(self._defaultData, {
            get(defaultData, name) {
                if (
                    self._registeredVariants[name] ||
                    defaultData[name] && defaultData[name] instanceof Object
                ) {
                    return self.createRunContext()[name];
                }

                return defaultData[name];
            },

            has(target, name) {
                if (self._registeredVariants[name]) {
                    return true;
                }

                return name in target;
            },
        });

        const newProto = Object.create(MockChain.prototype);

        newProto.value = MockChain.prototype.value;
        newProto.constructor = MockChain.prototype.constructor;
        newProto.registerMock = MockChain.prototype.registerMock;
        newProto.createRunContext = MockChain.prototype.createRunContext;
        newProto.fillByProps = MockChain.prototype.fillByProps;

        newProto.__proto__ = this._proxy;

        this.__proto__ = newProto;
    }

    value() {
        return _.cloneDeepWith(this._defaultData, (value) => {
            if (value instanceof MockChain) {
                return value.value();
            }
        });
    }

    registerMock(name, dataGenerator) {
        if (dataGenerator instanceof MockChain) {
            this._defaultData[name] = dataGenerator;
        } else {
            this._registeredVariants[name] = dataGenerator;
        }
    }

    createRunContext(upperContext) {
        return new MockChainContext({
            upperContext,
            registeredVariants: this._registeredVariants,
            data: this._defaultData,
        });
    }


    fillByProps(newData) {
        const context = this.createRunContext();

        context.fillByProps(newData);

        return context;
    }
}

MockChainContext.MockChain = MockChain;

module.exports = MockChain;
