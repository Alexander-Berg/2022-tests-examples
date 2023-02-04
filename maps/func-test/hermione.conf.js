const fs = require('fs'),
    path = require('path'),
    os = require('os'),
    pageObject = require('./page-object');

const hostname = process.env.HOSTNAME || os.hostname(),
    yEnv = process.env.YENV || 'development',
    isCi = !!process.env.CI,
    isFast = !!process.env.FAST,
    localPort = process.env.LOCAL_PORT,
    gridLogin = process.env.SELENIUM_GRID_LOGIN || 'geo',
    isSaveScreenshots = !process.env.NO_SAVE_SCREENSHOTS,
    baseUrlsByEnv = {
        development: 'https://' + hostname.replace(/\.net$/, '.'),
        testing: 'https://nmaps.tst.maps.yandex.'
    };

module.exports = {
    gridUrl: localPort?
        'http://127.0.0.1:' + localPort + '/wd/hub' :
        'http://' + gridLogin + '@sw.yandex-team.ru:80/v0',
    baseUrl: baseUrlsByEnv[yEnv],
    httpTimeout: 90000,
    sessionQuitTimeout: 5000,
    sessionRequestTimeout: 150000,
    waitTimeout: isFast? 5000 : 10000,
    sessionsPerBrowser: isFast? 5 : 15,
    testsPerSession: 5,
    retry: isFast? 0 : 4,
    windowSize: {
        width: 1280,
        height: 1280
    },
    system: {
        debug: !!process.env.DEBUG,

        mochaOpts: {
            timeout: 120000
        },
        patternsOnReject: [
            /timeout/i,
            /timedout/i,
            /timed out/i,
            /It may have died/,
            /CLIENT_GONE/,
            /CLIENT_STOPPED_SESSION/,
            /was terminated due to CLIENT_GONE/,
            /cannot forward the request/,
            /unknown server-side error/,
            /FORWARDING_TO_NODE_FAILED/,
            /PROXY_REREGISTRATION/,
            /not available and is not among the last/
        ]
    },

    sets: {
        common: {
            files: ['func-test/tests']
        }
    },

    screenshotPath: isSaveScreenshots ? 'func-test/fail-screenshots' : null,
    screenshotsDir: (test) => path.join(path.dirname(test.file).replace('tests', 'screenshots'),
        path.basename(test.file, '.js')),
    antialiasingTolerance: 8,
    buildDiffOpts: {
        ignoreAntialiasing: true,
        ignoreCaret: true
    },
    resetCursor: false,
    plugins: {
        'html-reporter/hermione': {
            path: 'func-test/report',
            defaultView: 'failed',
            scaleImages: true,
            baseHost: baseUrlsByEnv[yEnv]
        },
        'hermione-ignore': {
            globalIgnore: {
                // Selectors will be covered with black rect
                ignoreElements: [
                    pageObject.welcomeScreenVideo()
                ],
                // Selectors will be hidden with `display: none`
                hideElements: [
                    pageObject.geoObjViewerView.historyLink.date(),
                    pageObject.linkIcon(),
                    pageObject.sidebarView.island2()
                ],
                // Selectors will be hidden with `opacity: 0`
                invisibleElements: [
                    pageObject.commitView.date(),
                    pageObject.commitView.state(),
                    pageObject.eventView.date(),
                    pageObject.helpViewLiveMap(),
                    pageObject.helpViewLiveGeoObjIcon(),
                    pageObject.helpViewLiveEventLinks()
                ]
            }
        },
        '@yandex-int/hermione-surfwax-router': {
            enabled: isCi,
            surfwaxHostname: 'sw.yandex-team.ru'
        }
    },

    browsers: {
        chrome: {
            desiredCapabilities: {
                browserName: 'chrome',
                version: '91.0',
                unexpectedAlertBehaviour: 'accept',
                enableVideo: true,
                acceptSslCerts: true,
                acceptInsecureCerts: true
            }
        }
    },
    prepareBrowser: function(browser) {
        browser.extendOptions({ deprecationWarnings: false });

        const commandsDir = path.resolve(__dirname, 'tools/commands');

        fs.readdirSync(commandsDir).forEach(function(filename) {
            const name = path.basename(filename, '.js'),
                command = require(path.resolve(commandsDir, filename));
            browser.addCommand(name, command);
        });
    }
};
