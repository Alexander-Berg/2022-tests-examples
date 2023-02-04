const fs = require('fs'),
    path = require('path'),
    chai = require('chai'),
    os = require('os');

function getHostname () {
    var cwd = process.cwd();
    var wwwdir = path.resolve(process.env['HOME'], 'www');
    var relativePath = path.relative(wwwdir, cwd).split('/')[0];
    return 'node.' + relativePath + '.' + process.env['USER'] + '.' + os.hostname();
}
const tunnelerLocalPort = process.env.LOCAL_PORT || 8007;
global.testsPath = '';
global.apiPath = '';
// Тесты можно запустить на alexa:
// предварительно выключив @yandex-int/tunneler/hermione -> enabled: false
//global.testsPath = 'https://jsapi.sergeiiak.alexa.maps.dev.yandex.ru/jsapi-v2.1/tests/';
//global.apiPath = 'https://api-maps.tst.c.maps.yandex.ru/2.1.72';
basePath = './';
global.should = chai.should();
global.assert = chai.assert;
global.PO = require(basePath + 'pageObject.js').PO;
global.apiVersion = require(basePath + 'pageObject.js').apiVersion;
global.cs = require(basePath + 'pageObject.js').cs;
var warn = console.warn;
console.warn = function(message) {
    if(typeof message === 'string' && message.match(/This command is not part of the W3C WebDriver spec and won't be supported in future versions of the driver./)) {
        return;
    }
    warn.apply(console, arguments);
};
module.exports = {
    baseUrl: 'https://localhost/',
    waitTimeout: 10000,
    sessionsPerBrowser: 5,
    retry: 6,
    tolerance: 10,
    testsPerSession : 3,
    antialiasingTolerance: 15,
    windowSize: '1280x900',
    sets: {
        func: {
            files: basePath + 'tests/',
            browsers: ['opera', 'yandex', 'chrome', 'firefox']
        }
    },
    browsers: {
        edge: {
            desiredCapabilities: {
                browserName: 'MicrosoftEdge',
                enableVNC: true,
                enableVideo: true
            },
            compositeImage: true
        },
        chrome: {
            desiredCapabilities: {
                browserName: 'chrome',
                version: '70.0',
                unexpectedAlertBehaviour : 'accept'
            }
        },
        yandex: {
            desiredCapabilities: {
                browserName: "yandex-browser",
                version: "18.10.0.444",
                unexpectedAlertBehaviour : 'accept'
            }
        },
        firefox: {
            desiredCapabilities: {
                browserName: 'firefox',
                version: '65.0',
                enableVNC: true,
                acceptSslCerts: true,
                acceptInsecureCerts: true,
                unexpectedAlertBehaviour : 'accept'
            }
        },
        ie11: {
            desiredCapabilities: {
                browserName: 'internet explorer',
                version: '11'
            }
        },
        ie10: {
            desiredCapabilities: {
                browserName: 'internet explorer',
                version: '10'
            }
        },
        ie8: {
            desiredCapabilities: {
                browserName: 'internet explorer',
                version: '8'
            }
        },
        opera: {
            desiredCapabilities: {
                browserName: 'opera',
                version: '54.0',
                operaOptions: {
                    binary: '/usr/bin/opera'
                },
                enableVNC: true,
                enableVideo: true
            },
            compositeImage: true
        }
    },
    prepareBrowser: function (browser) {
        const commandsDir = path.resolve(__dirname, basePath + 'commands');
        browser.extendOptions({deprecationWarnings: false});
        fs.readdirSync(commandsDir)
            .filter(function(name) {
                return path.extname(name) === '.js' && fs.statSync(path.resolve(commandsDir, name)).isFile();
            })
            .forEach(function(filename) {
                browser.addCommand(path.basename(filename, '.js'), require(path.resolve(commandsDir, filename)));
            });
    },
    screenshotsDir: (test) => {
        const filePath = path.dirname(test.file).split('/');
        const fileBasename = path.basename(test.file, '.js');
        const hermioneIndex = filePath.indexOf('hermione') + 1;
        return '/' + path.join(...filePath.slice(0, hermioneIndex), 'screenshots', ...filePath.slice(hermioneIndex + 1), fileBasename);
    },
    plugins: {
        '@yandex-int/tunneler/hermione': {
            enabled: true,
            tunnelerOpts: {
                localport: tunnelerLocalPort,
                user: 'buildfarm',
                sshRetries: 5,
                ports: {
                    min: 1001,
                    max: 65535
                }
            }
        },
        'html-reporter/hermione': {
            path: 'reports/hermione',
            defaultView: 'failed',
            scaleImages: true
        },
        '@yandex-int/wdio-polyfill': {
            enabled: true,
            browsers: {
                firefox: [
                    'moveToObject',

                    'buttonUp',
                    'buttonDown',
                    'buttonPress',

                    'getValue',
                    'getAttribute'
                ]
            }
        }
    }
};
