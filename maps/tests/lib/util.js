var util = {
    defaults: {
        timeout: 100
    },

    initAPI: function (options) {
        var url = options.root + '/init.js?' + [
            'lang=' + options.lang,
            'mode=' + options.mode,
            'counters=all'
        ].join('&');

        util.apiUrlRoot = options.root;

        document.write('<script src="' + url + '"></script>');
    },

    getSearch: function (key, defaultValue) {
        var value = util.url.parseSearchParams(location.search)[key];
        return value === undefined ? defaultValue : value;
    },

    object: {
        // Copypasta from builder/init.js/ym.utils.js
        assign: Object.assign ? Object.assign : function objectAssign (target) {
            for (var i = 1, l = arguments.length; i < l; i++) {
                var object = arguments[i];
                if (object == null) {
                    continue;
                }

                for (var key in object) {
                    if (util.hop.call(object, key)) {
                        target[key] = object[key];
                    }
                }
            }

            return target;
        }
    },

    array: {
        isArray: function (object) { return Object.prototype.toString.call(object) === '[object Array]'; },
        includes: function (array, entry) { return array.indexOf(entry) !== -1; },

        remove: function (array, entry) {
            var ix = array.indexOf(entry);
            if (ix !== -1) {
                array.splice(array, 1);
            }
        }
    },

    url: {
        normalize: function (url) {
            var m = url.match(/^(?:(\w+:\/\/)([^/]*))?([^?]*)(.*)$/);
            return (m[1] || '') + (m[2] || '') + m[3].replace(/\/{2,}/g, '/') + m[4].replace(/&{2,}/g, '&');
        },
        parseSearchParams: function (url) {
            try {
                var searchParamsStr = url.slice((url + '?').indexOf('?') + 1);
                return searchParamsStr.split('&')
                    .map(function (param) {
                        var ix = param.indexOf('=');
                        var key = param.slice(0, ix);
                        var value = param.slice(ix + 1);
                        return {key: decodeURIComponent(key), value: decodeURIComponent(value)};
                    })
                    .reduce(function (res, kv) {
                        res[kv.key] = kv.value;
                        return res;
                    }, {});
            } catch (e) {
                throw new Error('Failed to parse: ' + url);
            }
        }
    },

    sniffEvents: function (eventManager) {
        var events = [];
        eventManager.addController({
            onBeforeEventFiring: function (eventManager, type) {
                if (!/defaultaction$/.test(type)) {
                    events.push(type);
                }
            }
        });

        return events;
    },

    waitEventOnce: function (eventManager, event) {
        return new ymaps.vow.Promise(function (resolve) {
            eventManager.once(event, resolve);
        });
    },

    waitEventCount: function (eventManager, event, count) {
        return new ymaps.vow.Promise(function (resolve) {
            eventManager.add(event, function handler () {
                if (--count === 0) {
                    eventManager.remove(event, handler);
                    resolve();
                }
            });
        })
    },

    waitDataManagerField: function (manager, field, checkOrValue, args) {
        args = util.object.assign({timeout: util.defaults.timeout}, args);
        var debug = util.waitDataManagerField.debug;
        var check = typeof checkOrValue === 'function' ? checkOrValue : function (value) { return value === checkOrValue };

        if (check(manager.get(field))) {
            debug(field, 'initially ok');
            return ymaps.vow.Promise.resolve();
        }

        var monitor = new ymaps.Monitor(manager);
        var promise = new ymaps.vow.Promise(function (resolve) {
            monitor.add(field, function (x) {
                if (check(x)) {
                    debug(field, 'ok');
                    resolve();
                }
            });
        });

        return promise.timeout(args.timeout)
            .catch(function (error) {
                throw error instanceof ymaps.vow.TimedOutError ?
                    new ymaps.vow.TimedOutError(debug.str(field, 'is not ok in', args.timeout, 'ms')) :
                    error;
            })
            .always(function (p) {
                monitor.removeAll();
                return p;
            });
    },

    /** Like `util.poll` but for easier use in .then chains. */
    poll$: function (fn, args) { return util.poll.bind(null, fn, args); },

    poll: function (fn, args) {
        args = util.object.assign({timeout: util.defaults.timeout}, args);

        util.poll.id = util.poll.id || 0;
        var uid = '#' + ++util.poll.id;

        var debug = util.poll.debug;

        return new ymaps.vow.Promise(function (resolve, reject) {
            var startTs = Date.now();

            debug(uid, args, fn.toString());

            (function poll () {
                var ok = false;
                try {
                    ok = fn();
                } catch (e) {
                    // ignore
                }

                if (ok) {
                    debug(uid, 'resolve');
                    resolve();
                } else if (Date.now() - startTs > args.timeout) {
                    reject(new ymaps.vow.TimedOutError(debug.str(uid, 'timed out, ', args.timeout, 'ms: ', fn.toString())));
                } else {
                    setTimeout(poll);
                }
            })();
        });
    },

    createMapContainer: function (size) {
        size = size || [400, 400];

        var container = document.createElement('div');
        container.style.width = size[0] + 'px';
        container.style.height = size[1] + 'px';
        document.body.appendChild(container);

        return container;
    },

    destroyMapAndContainer: function (map) {
        var container = map.container.getParentElement();
        map.destroy();
        container.parentElement.removeChild(container);
    },

    env: function (ymaps) {
        return ymaps.modules.__modules._sandbox.env;
    },

    nullGif: 'data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACwAAAAAAQABAAACAkQBADs=',
    nop: function () {},
    hop: Object.prototype.hasOwnProperty,

    tile: {
        /** Shorthand to use with util.mocha.mock.imageLoader. */
        filled$: function (args) { return function (req) { return util.tile.filled(util.object.assign({}, args, req.query)); }; },
        filled: function (args) {
            args = util.object.assign({color: '#faf7f0', opacity: '1', $x: 128, $y: 136, id: 'TILE'}, args);
            args.$y1 = args.$y - 10;
            args.$y2 = args.$y + 10;
            var svg = [
                '<!--{{id}}/{{x}}.{{y}}.{{z}}-->',
                '<svg width="256" height="256" viewBox="0 0 256 256" xmlns="http://www.w3.org/2000/svg">',
                '<rect x="0" y="0" width="256" height="256" fill-opacity="{{opacity}}" fill="{{color}}" stroke="black"/>',
                '<text text-anchor="middle" x="{{$x}}" y="{{$y1}}">{{id}}</text>',
                '<text text-anchor="middle" x="{{$x}}" y="{{$y2}}">{{x}}.{{y}}.{{z}}</text>',
                '</svg>'
            ].join('\n').replace(/\{\{([^}]*)\}\}/g, function (_, name) { return args[name] !== undefined ? args[name] : ''; });
            return 'data:image/svg+xml;utf-8,' + encodeURIComponent(svg);
        }
    },

    isEnabled: function (name, patterns) {
        for (var i = 0; i < patterns.length; i++) {
            var pattern = patterns[i];
            if (pattern === name) {
                return true;
            }

            if (pattern[pattern.length - 1] === '*') {
                var prefix = pattern.slice(0, pattern.length - 1);
                if (name.indexOf(prefix) === 0) {
                    return true;
                }
            }
        }

        return false;
    },

    debug: function (name) {
        var enabled = util.isEnabled(name, util.getSearch('debug', '').split(',').filter(Boolean));

        util.debug.available = util.debug.available || [];
        util.debug.available.push({name: name, enabled: enabled});

        var prepare = function (args) { return ['[' + name + ']'].concat([].slice.call(args)); }
        var enabledLogger = function () { console.log.apply(console, prepare(arguments)); };
        var effectiveLogger = enabled ? enabledLogger : util.nop.bind(null);

        effectiveLogger.str = function () { return prepare(arguments).join(' '); };
        effectiveLogger.force = enabledLogger;
        effectiveLogger.enabled = enabled;

        return effectiveLogger;
    },

    nextTick: function (arg) {
        var nextTick = ymaps.modules.requireSync('system.nextTick');
        if (!nextTick) {
            var error = 'util.nextTick: system.nextTick is not inited yet';
            console.error(error);
            throw new Error(error);
        }

        util.nextTick = nextTick;
        return util.nextTick(arg);
    },

    mocha: {
        mock: {
            jsonp: function (args) { return util.mocha.mock.__impl(util.mocha.mock.jsonp, args); },
            imageLoader: function (args) { return util.mocha.mock.__impl(util.mocha.mock.imageLoader, args); },

            __impl: function (fn, args) {
                var holder = {mock: null};
                beforeEach(function () { holder.mock = fn.init(args); });
                afterEach(function () { return holder.mock.destroy(); });
                return holder;
            }
        },

        ymaps: {
            module: function (args) {
                args = util.object.assign({name: null, url: null}, args);

                var holder = {module: null};

                before(function () {
                    var loaded = !args.url ? ymaps.vow.Promise.resolve() : new ymaps.vow.Promise(function (resolve, reject) {
                        var script = document.createElement('script');
                        script.onload = resolve;
                        script.onerror = reject;
                        script.src = args.url;
                        document.getElementsByTagName('head')[0].appendChild(script);
                    });
                    return loaded
                        .then(function () { return ymaps.modules.require([args.name]); })
                        .spread(function (x) { return holder.module = x; });
                });

                return holder;
            }
        }
    },

    EventEmitter: (function () {
        function EventEmitter () {
            this._subsEventEmitter = {};
        };
        EventEmitter.prototype.on = function (event, sub) {
            this._subsEventEmitter[event] = this._subsEventEmitter[event] || [];
            this._subsEventEmitter[event].push(sub);
            return this;
        };
        EventEmitter.prototype.off = function (event, sub) {
            if (!this._subsEventEmitter[event]) return;
            util.array.remove(this._subsEventEmitter[event], sub);
            return this;
        };
        EventEmitter.prototype.emit = function (event) {
            if (!this._subsEventEmitter[event]) return;
            var args = [].slice.call(arguments, 1);
            this._subsEventEmitter[event].forEach(function (fn) { fn.apply(null, args); });
            return this;
        };
        return EventEmitter;
    })(),

    testfile: (function () {
        function testfile (args) {
            var url = document.currentScript ? document.currentScript.src : null;
            if (!url) {
                try {
                    throw new Error();
                } catch (e) {
                    url = e.stack.split('\n').filter(function (x) { return /src\/.*\.test\.js/.test(x); })[0];
                }
            }

            var name = 'test.' + url.match(/\/src\/(.*)\.test\.js/)[1].replace(/\//g, '.');
            testfile.register(util.object.assign({name: name}, args || {}));
            return name;
        };

        testfile._modules = {};
        testfile.register = function (args) {
            testfile._modules[args.name] = args || {};
        };
        testfile.modules = function () {
            return Object.keys(testfile._modules);
        };

        return testfile;
    })(),

    inherit: function (Derived, Base) {
        Derived.prototype = Object.create(Base.prototype);
        Derived.prototype.constructor = Derived;
        Derived.super = Base;
    }
};

util.extend = util.object.assign;

util.waitDataManagerField.debug = util.debug('util.waitDataManagerField');
util.poll.debug = util.debug('util.poll');
