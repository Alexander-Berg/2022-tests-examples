util.createRequestStubber = function createRequestStubber(args) {
    var debug = args.debug;
    var vow = ymaps.vow;

    function UnhandledReqError() {}

    function RequestStubber(args) {
        RequestStubber.super.call(this);

        this._args = util.object.assign({
            instantFailOnUnmatched: true
        }, args || {});
        this.stubs = [];
        this._init();
    }

    util.inherit(RequestStubber, util.EventEmitter);

    RequestStubber.prototype._init = function () {
        this.unmatched = this.stub(/^.?/, '<unmatched>');

        if (this._args.instantFailOnUnmatched) {
            this.unmatched.on('request', function (req) {
                throw new Error(debug.str('Unmatched request:', req.url));
            });
        }
    };
    RequestStubber.prototype.handle = function (req) {
        for (var i = 0; i < this.stubs.length; i++) {
            if (this.stubs[i].tryHandle(req)) {
                return;
            }
        }

        throw new Error('Cannot handle request');
    };
    RequestStubber.prototype.stub = function (match, name) {
        var stub = new RequestStub(match, name);
        this.stubs.unshift(stub);
        stub.on('unstub', function () { util.array.remove(this.stubs, stub); }.bind(this));

        return stub;
    };
    RequestStubber.prototype.allPending = function () {
        var pending = [];
        for (var i = 0; i < this.stubs.length; i++) {
            pending = pending.concat(this.stubs[i].pending);
        }
        return pending;
    };
    RequestStubber.prototype.unstubAll = function () {
        var unhandled = [];

        var stubs = this.stubs.slice();
        for (var i = 0; i < stubs.length; i++) {
            unhandled = unhandled.concat(stubs[i].unstub());
        }

        this._init();

        if (unhandled.length) {
            throw new Error(debug.str('There was', unhandled.length, 'unhandled request(s):\n', urls(unhandled)));
        }

        return new vow.Promise(function (resolve) { util.nextTick(resolve); });
    };
    RequestStubber.prototype.destroy = function () {
        return this.unstubAll().then(function () { this.emit('destroy'); }, this);
    };

    function RequestStub(matcher, name) {
        RequestStub.super.call(this);

        this._mode = 'fail';
        this._data = new Error('stub behavior was not specified');

        this._play = false;

        this.processed = [];
        this.pending = [];

        this._matcher = matcher;
        this._matcherStr = name || (
            typeof this._matcher === 'function' ?
                (this._matcher.$name || this._matcher.name || '<function without $name/name>') :
                String(this._matcher)
        );
        debug('stub', this._matcherStr);
    }

    util.inherit(RequestStub, util.EventEmitter);

    RequestStub.prototype.matches = function (req) {
        if (typeof this._matcher === 'string') return req.url === this._matcher;
        if (this._matcher instanceof RegExp) return this._matcher.test(req.url);
        if (this._matcher instanceof Function) return this._matcher(req);

        throw new Error(debug.str('unknown matcher', this._matcher));
    };
    RequestStub.prototype.tryHandle = function (req) {
        if (!this.matches(req)) {
            return false;
        }

        debug(req.uid, 'handled by', this._matcherStr);

        this.pending.push(req);
        this.emit('request', req);
        req.on('processed', function () { util.array.remove(this.pending, req); }.bind(this));

        if (this._play) {
            this._processSingle(req);
        }

        return true;
    };
    RequestStub.prototype.play = function () {
        this._play = true;
        this.process();
        return this;
    };
    RequestStub.prototype.pause = function () {
        this._play = false;
        return this;
    };
    RequestStub.prototype.unstub = function () {
        var unhandled = this.pending.filter(function (x) { return !x.handled; });
        debug('unstub', this._matcherStr, unhandled.length, 'unhandled');
        this.emit('unstub', unhandled);

        var originalMatcher = this._matcher;
        this._matcher = function () { throw new Error(debug.str('unstubbed', originalMatcher)); }

        return unhandled;
    };
    RequestStub.prototype.completeWith = function (data) {
        this._mode = 'complete';
        this._data = data;
        return this;
    };
    RequestStub.prototype.failWith = function (data) {
        this._mode = 'fail';
        this._data = data;
        return this;
    };
    RequestStub.prototype._processSingle = function (req) {
        req.process(this._mode, this._data);
        this.processed.push(req);
    };
    RequestStub.prototype.process = function () {
        for (var i = 0; i < this.pending.length; i++) {
            this._processSingle(this.pending[i]);
        }
    };
    RequestStub.prototype.once = function (args) {
        args = util.object.assign({timeout: util.defaults.timeout}, args);
        var promise = new vow.Promise(function (resolve) {
            var fn = function () {
                this.pending[0].on('processed', resolve);
                this._processSingle(this.pending[0]);
                this.off('request', fn);
            }.bind(this);

            if (this.pending.length > 0) {
                fn();
            } else {
                this.on('request', fn);
            }
        }.bind(this));

        return promise.timeout(args.timeout)
            .catch(function (error) {
                throw error instanceof ymaps.vow.TimedOutError ?
                    new ymaps.vow.TimedOutError(debug.str(this._matcherStr, 'once timeout', args.timeout, 'ms')) :
                    error;
            }.bind(this));
    }

    Request.uid = 0;
    function Request(tag, url) {
        Request.super.call(this);

        this.tag = tag;
        this.url = util.url.normalize(url);
        this.query = util.url.parseSearchParams(this.url);
        this.uid = String('#' + ++Request.uid);
        this._handled = false;

        debug(this.uid, url);
    }

    util.inherit(Request, util.EventEmitter);

    Request.prototype.startsWith = function (base) {
        return this.url.indexOf(util.url.normalize(base)) === 0;
    };

    Request.prototype.process = function (mode, data) {
        if (this._handled) {
            throw new Error(this.uid + ' (' + this.url + ') already handled');
        }

        this._handled = true;

        util.nextTick(function () {
            if (typeof data === 'function') {
                data = data(this);
            }

            if (mode === 'complete') {
                args.completeRequest(this, data);
            } else {
                args.failRequest(this, data);
            }
            this.emit('processed');

            if (mode === 'complete') {
                debug(this.uid, 'complete');
            } else {
                debug(this.uid, data instanceof UnhandledReqError ? 'unhandled' : 'fail');
            }
        }.bind(this));
    };

    function urls(reqs) {
        return reqs.map(function (x) { return '- ' + x.url + '\n'; }).join('');
    }

    RequestStubber.UnhandledReqError = UnhandledReqError;
    RequestStubber.RequestStub = RequestStub;
    RequestStubber.Request = Request;

    return RequestStubber;
};
