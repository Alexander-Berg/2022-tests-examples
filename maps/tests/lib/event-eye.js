function EventEye () {
    this._objects = [];
    this._events = [];
    this._eventStack = [];
    this._eventWait = {};
    this._testCase = null;

    var eventHandler = function (event) {
        this._eventStack.push(event);
        var type = event.get("type");
        if (this._eventWait.hasOwnProperty(type)) {
            var callback = this._eventWait[type];
            this._eventWait = {};
            this._testCase.resume(callback);
            this._testCase = null;
        }
    };

    this.observe = function (object, events) {
        this.stop();

        this._objects = [].concat(object);
        this._events = [].concat(events);

        for (var i = 0; i < this._objects.length; i++) {
            this._objects[i].events.add(this._events, eventHandler, this)
        }
    };

    this.get = function (index) {
        return this._eventStack[index];
    };

    this.prop = function (index, property) {
        return this._eventStack[index].get(property);
    };

    this.check = function (index, properties) {
        for (var name in properties) {
            if (this._eventStack[index].get(name) != properties[name]) {
                return false;
            }
        }
        return true;
    };

    this.length = function () {
        return this._eventStack.length;
    };

    this.reset = function () {
        this._eventStack.length = 0;
    };

    this.stop = function () {
        for (var i = 0; i < this._objects.length; i++) {
            this._objects[i].events.remove(this._events, eventHandler, this)
        }

        this.reset();
        this._objects = [];
        this._events = [];
        this._eventWait = {};
        this._testCase = null;
    };

    this.wait = function (testCase, events, callback) {
        events = [].concat(events);
        this._testCase = testCase;
        for (var i = 0, l = events.length; i < l; i++) {
            this._eventWait[events[i]] = callback;
        }
        this._testCase.wait();
    }
}

var eventEye = new EventEye();
