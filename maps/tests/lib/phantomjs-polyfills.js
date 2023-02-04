// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Function/bind#Polyfill
if (!Function.prototype.bind) {
    Function.prototype.bind = function (oThis) {
        if (typeof this !== 'function') {
            // closest thing possible to the ECMAScript 5
            // internal IsCallable function
            throw new TypeError('Function.prototype.bind - what is trying to be bound is not callable');
        }

        var aArgs = Array.prototype.slice.call(arguments, 1),
            fToBind = this,
            fNOP = function () { },
            fBound = function () {
                return fToBind.apply(this instanceof fNOP
                    ? this
                    : oThis,
                    aArgs.concat(Array.prototype.slice.call(arguments)));
            };

        if (this.prototype) {
            // Function.prototype doesn't have a prototype property
            fNOP.prototype = this.prototype;
        }
        fBound.prototype = new fNOP();

        return fBound;
    };
}

if (!window.Set) {
    var Set = window.Set = function (source) {
        this._data = Array.prototype.slice(source);
    };

    Object.defineProperty(Set.prototype, 'size', {
        get: function () { return this._data.length; }
    });

    Set.prototype.add = function (value) {
        if (this._data.indexOf(value) === -1) {
            this._data.push(value);
        }
        return this;
    };

    Set.prototype.clear = function () {
        this._data = [];
    };

    Set.prototype.delete = function (value) {
        var ix = this._data.indexOf(value);
        if (ix !== -1) {
            this._data.splice(ix, 1);
            return true;
        }
        return false;
    };

    Set.prototype.forEach = function (cb, thisArg) {
        this._data.forEach(function (value) {
            cb.call(thisArg, value, value, this);
        }, this);
    };

    Set.prototype.has = function (value) {
        return this._data.indexOf(value) !== -1;
    };
}

//https://github.com/polygonplanet/weakmap-polyfill/blob/master/weakmap-polyfill.js
(function (self) {
    'use strict';

    if (self.WeakMap) {
        return;
    }

    var hasOwnProperty = Object.prototype.hasOwnProperty;
    var defineProperty = function (object, name, value) {
        if (Object.defineProperty) {
            Object.defineProperty(object, name, {
                configurable: true,
                writable: true,
                value: value
            });
        } else {
            object[name] = value;
        }
    };

    self.WeakMap = (function () {

        // ECMA-262 23.3 WeakMap Objects
        function WeakMap() {
            if (this === void 0) {
                throw new TypeError("Constructor WeakMap requires 'new'");
            }

            defineProperty(this, '_id', genId('_WeakMap'));

            // ECMA-262 23.3.1.1 WeakMap([iterable])
            if (arguments.length > 0) {
                // Currently, WeakMap `iterable` argument is not supported
                throw new TypeError('WeakMap iterable is not supported');
            }
        }

        // ECMA-262 23.3.3.2 WeakMap.prototype.delete(key)
        defineProperty(WeakMap.prototype, 'delete', function (key) {
            checkInstance(this, 'delete');

            if (!isObject(key)) {
                return false;
            }

            var entry = key[this._id];
            if (entry && entry[0] === key) {
                delete key[this._id];
                return true;
            }

            return false;
        });

        // ECMA-262 23.3.3.3 WeakMap.prototype.get(key)
        defineProperty(WeakMap.prototype, 'get', function (key) {
            checkInstance(this, 'get');

            if (!isObject(key)) {
                return void 0;
            }

            var entry = key[this._id];
            if (entry && entry[0] === key) {
                return entry[1];
            }

            return void 0;
        });

        // ECMA-262 23.3.3.4 WeakMap.prototype.has(key)
        defineProperty(WeakMap.prototype, 'has', function (key) {
            checkInstance(this, 'has');

            if (!isObject(key)) {
                return false;
            }

            var entry = key[this._id];
            if (entry && entry[0] === key) {
                return true;
            }

            return false;
        });

        // ECMA-262 23.3.3.5 WeakMap.prototype.set(key, value)
        defineProperty(WeakMap.prototype, 'set', function (key, value) {
            checkInstance(this, 'set');

            if (!isObject(key)) {
                throw new TypeError('Invalid value used as weak map key');
            }

            var entry = key[this._id];
            if (entry && entry[0] === key) {
                entry[1] = value;
                return this;
            }

            defineProperty(key, this._id, [key, value]);
            return this;
        });


        function checkInstance(x, methodName) {
            if (!isObject(x) || !hasOwnProperty.call(x, '_id')) {
                throw new TypeError(
                    methodName + ' method called on incompatible receiver ' +
                    typeof x
                );
            }
        }

        function genId(prefix) {
            return prefix + '_' + rand() + '.' + rand();
        }

        function rand() {
            return Math.random().toString().substring(2);
        }


        defineProperty(WeakMap, '_polyfill', true);
        return WeakMap;
    })();


    function isObject(x) {
        return Object(x) === x;
    }
})(
    typeof self !== 'undefined' ? self :
        typeof window !== 'undefined' ? window :
            typeof global !== 'undefined' ? global : this
);
