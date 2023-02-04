const { ADGROUPS, CAMPAIGNS } = require('./SplashTypes.json');

const testCases = [
    {
        name: 'сравнение',
        props: {
            adjust: {
                campaign: CAMPAIGNS.TOUCH_COMPARE_SPLASH,
                adgroup: ADGROUPS.OLD_APPLOGO_WHITE,
            },
            type: 'touch_compare_splash',
        },
        expectedHref: {
            iOS: 'https://app.adjust.com/m1nelw7?campaign=touch_compare_splash&adgroup=old_applogo_white',
            Android: 'https://app.adjust.com/eb04l75?campaign=touch_compare_splash&adgroup=old_applogo_white',
        },
    },
];

module.exports = testCases;
