const fs = require('fs');
const path = require('path');
const chai = require('chai');
global.should = chai.should();
global.PO = require('./page-object.js').PO;

const chromeOpt = {
    acceptInsecureCerts : true,
    'goog:chromeOptions' : {
        args: ['--ignore-gpu-blocklist', '--ignore-certificate-errors']
    }
};

module.exports = {
    gridUrl : 'http://localhost:4444/wd/hub',
    compareOpts: {
        shouldCluster: true,
        clustersSize: 1,
    },
    sets : {
        smoke : {
            files : [
                './tests/actions/*.js',
                './tests/bugs/*.js',
                './tests/layers/*.js',
                './tests/objects/*.js',
            ],
            browsers : ['chrome', 'chrome_57', 'yandex']
        },
        func : {
            files : './tests/',
            browsers : ['chrome', 'yandex']
        },
        design : {
            files : 'test-design/test.js',
            browsers : ['chrome']
        },
        designRegress : {
            files : 'test-design/testRegress.js',
            browsers : ['chrome']
        },
        old : {
            files : './tests/actions/*.js',
            browsers : ['chrome_57']
        },
        support : {
            files : ['test-support/*.js'],
            browsers : ['chrome_57']
        },
        performanceStartTime : {
            files : ['test-performance/start-time.js'],
            browsers : ['chrome']
        },
        customization: {
          files: ['test-customization/test.js'],
          browsers: ['chrome']
        }
    },
    windowSize : {
        width : 1920,
        height : 1080
    },
    testsPerSession : 15,
    waitTimeout : 5000,
    sessionRequestTimeout: 150000,
    tolerance : process.env.FLEX ? 6 : 2,
    antialiasingTolerance : process.env.FLEX ? 15 : 10,
    screenshotDelay : 300,
    baseUrl : '',
    retry : +process.env.RETRY,
    system : {
        debug : false,
        mochaOpts : {
            timeout : 120000
        },
        patternsOnReject : [
            /timeout/i,
            /timedout/i,
            /timed out/i,
            /It may have died/,
            /session/,
            /selenium server/,
            /CLIENT_GONE/,
            /CLIENT_STOPPED_SESSION/,
            /was terminated due to CLIENT_GONE/,
            /cannot forward the request/,
            /unexpected alert open/,
            /unknown server-side error/,
            /FORWARDING_TO_NODE_FAILED/,
            /PROXY_REREGISTRATION/,
            /not available and is not among the last/
        ]
    },
    sessionsPerBrowser : 15,
    browsers : {
        chrome : {
            desiredCapabilities : {
                browserName : 'chrome',
                browserVersion : '91.0',
                ...chromeOpt
            }
        },
        yandex : {
            desiredCapabilities : {
                browserName : 'chrome',
                browserVersion : '21.8.0.1968',
                ...chromeOpt
            }
        },
        chrome_57 : {
            desiredCapabilities : {
                browserName : 'chrome',
                browserVersion : '57.0',
                ...chromeOpt
            }
        }
    },
    prepareBrowser : function (browser) {
        const commands = path.resolve(__dirname, 'commands');

        fs.readdirSync(commands)
            .filter(function(name) {
                return path.extname(name) === '.js' && fs.statSync(path.resolve(commands, name)).isFile();
            })
            .forEach(function(filename) {
                browser.addCommand(path.basename(filename, '.js'), require(path.resolve(commands, filename)));
            });
    },
    screenshotsDir : (test) => path.join(path.dirname(test.file), 'screenshots',
        path.basename(test.file, '.js'), test.browserId),
    plugins : {
        'html-reporter/hermione' : {
            path : 'reports/hermione',
            defaultView : 'failed'
        },
        'hermione-global-hook': {
            enabled : true,
            afterEach : function() {
                return this.browser.verifyNoErrors();
            }
        },
        'hermione-passive-browsers': {
            enabled : true,
            browsers : ['chrome_57', 'yandex'],
            commandName : 'enable'
        },
        '@yandex-int/hermione-block-diff-comparison-plugin': {
            enabled: true,
            size: {
                width: 800,
                height: 700
            },
            restrictions: {
                maxBlockSize: 3,
                // Allow the 20 pixels diff
                maxDiffPixelsRatio: 20 / 800 / 700
            }
        },
        '@yandex-int/hermione-surfwax-router': {
            enabled: Boolean(process.env.CI)
        }
    }
};
