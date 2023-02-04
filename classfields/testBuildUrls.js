module.exports = {
    autotest: new Map([
        [ 'gf-desktop', [
            'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_General_Desktop_RunAllDesktopTests&branch=',
        ] ],
        [ 'gf-mobile', [
            'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_General_Mobile_RunAllMobileTests&branch=',
        ] ],
    ]),
    'perfomance-test': new Map([
        [ 'gf-desktop', [
            'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_Speed_OYandex_TestRelease_Desktop_Run&branch=',
        ] ],
        [ 'gf-mobile', [
            'https://hub.vertis.yandex.net/v0/teamcity/build?buildTypeId=Verticals_Testing_Speed_OYandex_TestRelease_Mobile_Run&branch=',
        ] ],
    ]),
};
