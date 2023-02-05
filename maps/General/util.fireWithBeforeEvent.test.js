ymaps.modules.define(util.testfile(), [
    "util.fireWithBeforeEvent",
    "event.Manager",
    "Event"
], function (provide, fireWithBeforeEvent, EventManager, Event) {
    describe("util.fireWithBeforeEvent", function () {
        var eventContext = {},
            eventManager;

        beforeEach(function () {
            eventManager = new EventManager({
                context: eventContext
            });
        });

        afterEach(function () {
        });

        it("Бросание before события без mutableFields и mutableHandlers", function (done) {
            eventManager.add("beforetestchange", function (event) {
                expect(event.get("type")).to.be("beforetestchange");
                expect(event.get("testData")).to.be("test");
            });

            var callbackContext = {};
            fireWithBeforeEvent(eventManager, {
                type: "testchange",
                testData: "test"
            }, {
                successCallback: function (originalEvent, beforeEvent) {
                    expect(beforeEvent.get("testData")).to.be("test");
                    expect(this).to.be(callbackContext);
                    done();
                },
                preventCallback: function () {
                    expect().fail();
                    done();
                },
                context: callbackContext
            });
        });

        it("Бросание before события c mutableFields и без mutableHandlers", function (done) {
            eventManager.add("beforetestchange", function (event) {
                expect(event.get("testData")).to.be("test");
                event.callMethod("setTestData", "some")
            });

            fireWithBeforeEvent(eventManager, {
                type: "testchange",
                testData: "test"
            }, {
                successCallback: function (originalEvent, beforeEvent) {
                    expect(beforeEvent.get("testData")).to.be("some");
                    done();
                },
                preventCallback: function () {
                    expect().fail();
                    done();
                },
                mutableFields: ["testData"]
            });
        });

        it("Бросание before события c mutableFields и с mutableHandlers", function (done) {
            eventManager.add("beforetestchange", function (event) {
                expect(event.get("testData")).to.be("test");
                event.callMethod("setTestData", "some")
            });

            var isMutableHandlerCalled;

            fireWithBeforeEvent(eventManager, {
                type: "testchange"
            }, {
                successCallback: function (originalEvent, beforeEvent) {
                    expect(isMutableHandlerCalled).to.be.ok();
                    expect(beforeEvent.get("testData")).to.be("some");
                    done();
                },
                preventCallback: function () {
                    expect().fail();
                    done();
                },
                mutableFields: ["testData"],
                mutableHandlers: {
                    testData: function (eventData, newValue, oldValue) {
                        isMutableHandlerCalled = true;
                        expect(newValue).to.be("some");
                        expect(oldValue).to.be("test");
                    }
                },
                sourceEvent: new Event({
                    type: "change",
                    testData: "test"
                })
            });
        });
    });

    provide();
});
