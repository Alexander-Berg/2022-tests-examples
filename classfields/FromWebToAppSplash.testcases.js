const { ADGROUPS, CAMPAIGNS } = require('./SplashTypes.json');

const testCases = [
    {
        name: 'добавление, черный баннер с 3d лого',
        props: {
            adjust: {
                campaign: CAMPAIGNS.TOUCH_ADDOFFER_SPLASH,
                adgroup: ADGROUPS.LOGO_3D_DARK,
            },
        },
    },
    {
        name: 'добавление, белый баннер со списком приложений',
        props: {
            adjust: {
                campaign: CAMPAIGNS.TOUCH_ADDOFFER_SPLASH,
                adgroup: ADGROUPS.LOGO_PLAIN_CHOSE_ICON,
            },
        },
    },
    {
        name: 'добавление, анимированный баннер со списком приложений',
        props: {
            adjust: {
                campaign: CAMPAIGNS.TOUCH_ADDOFFER_SPLASH,
                adgroup: ADGROUPS.LOGO_3D_ANIMATED,
            },
        },
    },
    {
        name: 'темный баннер, полный отчёт',
        props: {
            adjust: {
                campaign: CAMPAIGNS.TOUCH_FULL_REPORT_SPLASH,
                adgroup: ADGROUPS.LOGO_3D_DARK,
            },
        },
    },
];

module.exports = testCases;
