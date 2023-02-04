/*
    Yandex Test Manager.
 */
YUI.add('test-manager', function(Y) {
    // Глобальная ссылка для того, чтобы тестируемые страницы могли обращаться.
    TestManager = {
        pages: [],

        frame: null,

        testCounter: 0,

        startTesting: function (frame, pages, pageData) {
            // Массив тестовых страниц.
            this.pages = pages;
            // Данные для страниц.
            this.pageData = pageData;
            // Ссылка на фрейм.
            this.frame = frame;
            // Номер исполняемого теста.
            this.testCounter = 0;
            // Непрошедшие тесты.
            this.failedPages = [];

            this.loadNext();
        },

        stopTesting: function () {
            if (this.frame.src != 'about:blank') {
                this.stopReport();
                this.loadPage('about:blank');
            }
        },

        loadNext: function () {
            this.loadPage(this.pages[this.testCounter++] || (this.finalReport(), 'about:blank'));
        },

        loadPage: function (page) {
            if (this.frame) {
                this.frame.src = page + "?mode=debug";
            }
        },

        finalReport: function () {
            Y.log(
                this.failedPages.length ?
                this.failedPages.length + " of " + this.pages.length + " tests <span style='color:#f00; font-weight: bold;'>FAILED</span>:<br />&nbsp;&nbsp;" +
                this.failedPages.join("<br />&nbsp;&nbsp;") :
                "All tests are successfully <span style='color:#070; font-weight: bold;'>PASSED</span>!",
                "finish", "TestRunner"
            );
        },

        stopReport: function () {
            Y.log(
                this.failedPages.length ?
                this.failedPages.length + " of " + this.pages.length + " tests <span style='color:#f00; font-weight: bold;'>FAILED</span>:<br />&nbsp;&nbsp;" +
                this.failedPages.join("<br />&nbsp;&nbsp;") :
                (this.testCounter - 1) + " of " + this.pages.length + " tests are successfully <span style='color:#070; font-weight: bold;'>PASSED</span>!",
                "stop", "TestRunner"
            );
        },

        onPageLoad: function (testRunner) {
            testRunner.subscribe(testRunner.BEGIN_EVENT, TestUtils.bindContext(this.onTestRunnerBegin, this));
            testRunner.subscribe(testRunner.COMPLETE_EVENT, TestUtils.bindContext(this.onTestRunnerComplete, this));
            testRunner.run();
        },

        onTestRunnerBegin: function () {
            //
        },

        onTestRunnerComplete: function (data) {
            var currentTestPageString = this.testCounter + ":" + this.pages.length +
                                        " <a href='" + this.frame.src + "'>" +
                                        (this.frame.contentDocument || this.frame.contentWindow.document).title +
                                        "</a>";
            if (data.results.failed) {
                this.failedPages.push(currentTestPageString);
            }
            Y.log(
                currentTestPageString + "<br />" +
                "passed: " + data.results.passed + ", failed: " + data.results.failed + ", ignored: " + data.results.ignored + "<br />" +
                (data.results.messages ? "<br />" + data.results.messages.join("<br />") : ""),
                data.results.failed ? "fail" : "pass", "TestRunner"
            );

            if (this.pageData) {
                var callback = this.pageData instanceof Array ? this.pageData[this.testCounter].callback : this.pageData.callback;
                if (typeof callback == "function") {
                    callback(data.results);
                }
            }
            this.loadNext();
        },

        getPageData: function () {
            return this.pageData instanceof Array ? this.pageData[this.testCounter] : this.pageData;
        }
    };
}, '0.0.1');
