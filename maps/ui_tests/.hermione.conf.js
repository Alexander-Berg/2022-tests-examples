module.exports = {
    gridUrl: process.env.GRID_URL,
    sets: {
        all: {
            files: 'tests',
        }
    },
    windowSize: {
        width: 1024,
        height: 768
    },
    sessionsPerBrowser: 15,
    browsers: {
        chrome: {
            desiredCapabilities: {
                browserName: 'chrome',
                version: '68.0',
                acceptSslCerts : true,
                acceptInsecureCerts : true,
                applicationCacheEnabled: false,
                chromeOptions : {
                    args : [
                        '--enable-webgl-draft-extensions',
                        '--enable-webgl-image-chromium',
                        '--ignore-gpu-blacklist',
                        '--disable-application-cache',
                        '--disable-cache'
                    ]
                },
                enableVNC : true
            }
        },
        firefox: {
            desiredCapabilities: {
                browserName: 'firefox',
                browserVersion: '64.0',
                acceptInsecureCerts: true
            }
        },
        ie11: {
            desiredCapabilities: {
                browserName: 'internet explorer',
                browserVersion: '11'
            }
        }
    },

    assertViewOpts: {
        allowViewportOverflow: true
    },

    prepareBrowser : function (browser) {
        browser.addCommand('openPanorama', require('./commands/open_panorama'));
        browser.addCommand('waitUntilPanoramaLoaded', require('./commands/wait_until_panorama_loaded'));
        browser.addCommand('assertViewPanorama', require('./commands/assert_view_panorama'));
        browser.addCommand('setViewportSize', require('./commands/viewport_size'));
        browser.addCommand('clickPanorama', require('./commands/click_panorama'));
        browser.addCommand('gridView', require('./commands/grid_view'));
        browser.addCommand('moveToPoint', require('./commands/move_to_point'));
    },
    retry: 2, 
    tolerance: 2,
    antialiasingTolerance: 10,
    plugins : {
        'html-reporter/hermione' : {
            path : 'reports',
            defaultView : 'failed',
            scaleImages : true
        },
        '@yandex-int/wdio-polyfill': {
            enabled: true,
            browsers: {
                firefox: ['moveTo', 'buttonUp', 'buttonDown', 'buttonPress', 'getValue', 'timeouts'],
                ie11: ['moveTo', 'buttonUp', 'buttonDown',  'buttonPress', 'getValue', 'timeouts']
            }
        }
    }
};
