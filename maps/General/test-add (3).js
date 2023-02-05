define(['jsSpeedTest/test-case'], function (TestCase) {
    return new TestCase({
        name: 'Add',

        axes: ['Listeners'],

        beforeAll: function (x) {
            this.callbacks = [];
            for (var i = 0; i < x; i++) {
                this.callbacks[i] = ['click', function () {}];
            }
        },

        setUp: function (x) {
            this.oldManager = new oldYMaps.BaseOldManager();
            this.newManager = new ymaps.event.manager.Base();
            this.priorityManager = new oldYMaps.PriorityManager();
        },

        tearDown: function (x) {
            this.oldManager = null;
            this.newManager = null;
            this.priorityManager = null;
        },

        afterAll: function (x) {
        },

        testOldManagerAdd: function (x) {
            for (var i = 0; i < x; i++) {
                this.oldManager.add(this.callbacks[i][0], this.callbacks[i][1], this);
            }
        },

        testOldManagerAddI: function (x) {
            for (var i = 0; i < x; i++) {
                this.oldManager.add(this.callbacks[i][0] + i, this.callbacks[i][1], this);
            }
        },

        testNewManagerAdd: function (x) {
            for (var i = 0; i < x; i++) {
                this.newManager.add(this.callbacks[i][0], this.callbacks[i][1], this);
            }
        },

        testNewManagerAddI: function (x) {
            for (var i = 0; i < x; i++) {
                this.newManager.add(this.callbacks[i][0] + i, this.callbacks[i][1], this);
            }
        }//,
        //
        //        testNewManagerAddWithPriority: function (x) {
        //            for (var i = 0; i < x; i++) {
        //                this.newManager.add(this.callbacks[i][0], this.callbacks[i][1], this, i);
        //            }
        //        },
        //
        //        testNewManagerAddWithPriorityI: function (x) {
        //            for (var i = 0; i < x; i++) {
        //                this.newManager.add(this.callbacks[i][0] + i, this.callbacks[i][1], this, i);
        //            }
        //        },
        //
        //        testPriorityManagerAdd: function (x) {
        //            for (var i = 0; i < x; i++) {
        //                this.priorityManager.add(this.callbacks[i][0], this.callbacks[i][1], this);
        //            }
        //        },
        //
        //        testPriorityManagerAddI: function (x) {
        //            for (var i = 0; i < x; i++) {
        //                this.priorityManager.add(this.callbacks[i][0] + i, this.callbacks[i][1], this);
        //            }
        //        },
        //
        //        testPriorityManagerAddWithPriority: function (x) {
        //            for (var i = 0; i < x; i++) {
        //                this.priorityManager.add(this.callbacks[i][0], this.callbacks[i][1], this, i);
        //            }
        //        },
        //
        //        testPriorityManagerAddWithPriorityI: function (x) {
        //            for (var i = 0; i < x; i++) {
        //                this.priorityManager.add(this.callbacks[i][0] + i, this.callbacks[i][1], this, i);
        //            }
        //        }
    });
});
