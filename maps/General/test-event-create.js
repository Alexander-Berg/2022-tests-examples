define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'DomEventVSHashDomEvent',

        axes: ['Overrides'],

        beforeAll: function () {
            this.clickDomEvent = simulateMouseEvent(document.getElementById('test'), 'click');
        },

        setUp: function () {
        },

        tearDown: function () {
        },

        afterAll: function () {
        },

        testDomEvent: function () {
            var event = new ymaps.DomEvent(this.clickDomEvent);
            var a = event.get('target');
        },

        testObjectCreate: function () {
            var event = augmentWithObjectCreate(this.clickDomEvent);
            var a = event.test;
        },

        testNew: function () {
            var event = augmentWithNew(this.clickDomEvent);
            var a = event.test;
        }
    });
});

var overrides = {
    test: {
        get: function () {
            return 10;
        }
    },
    test2: {
        get: function () {
            return 10;
        }
    },
    test3: {
        get: function () {
            return 10;
        }
    },
    test4: {
        get: function () {
            return 10;
        }
    },
    test5: {
        get: function () {
            return 10;
        }
    },
    test6: {
        get: function () {
            return 10;
        }
    }
};

function augmentWithObjectCreate (event) {
    return Object.create(event, overrides);
}

function augmentWithNew (event) {
    var F = function () {};
    F.prototype = event;
    var F2 = function () {};
    F2.prototype = new F();
    return;
}
