import buildAdjustUrl from './buildAdjustUrl';

type TestCase = [ string, string, Record<string, string> | undefined, string ];

it.each<TestCase>([
    [
        'урл, начинающийся со слеша',
        '/history',
        undefined,
        'https://sb76.adj.st/history?adjust_deeplink=autoru%3A%2F%2Fapp%2Fhistory&adjust_t=m1nelw7_eb04l75',
    ],
    [
        'урл без параметров',
        'history',
        undefined,
        'https://sb76.adj.st/history?adjust_deeplink=autoru%3A%2F%2Fapp%2Fhistory&adjust_t=m1nelw7_eb04l75',
    ],
    [
        'урл с параметром',
        'cars/used/bmw/x4/034234-234242/history',
        {
            adjust_campaign: 'mobweb_history_card',
        },
        'https://sb76.adj.st/cars/used/bmw/x4/034234-234242/history?' +
            'adjust_deeplink=autoru%3A%2F%2Fapp%2Fcars%2Fused%2Fbmw%2Fx4%2F034234-234242%2Fhistory' +
            '&adjust_t=m1nelw7_eb04l75' +
            '&adjust_campaign=mobweb_history_card',
    ],
    [
        'pathname с параметром',
        '/link/proauto-report/?history_entity_id=1085562758&history_entity_id=1970f439',
        {
            adjust_campaign: 'mobweb_history_card',
        },
        'https://sb76.adj.st/link/proauto-report/?history_entity_id=1085562758&history_entity_id=1970f439' +
            '&adjust_deeplink=autoru%3A%2F%2Fapp%2Flink%2Fproauto-report%2F%3Fhistory_entity_id%3D1085562758%26history_entity_id%3D1970f439' +
            '&adjust_t=m1nelw7_eb04l75' +
            '&adjust_campaign=mobweb_history_card',
    ],
    [
        'урл с параметрами, среди которых есть перетирающие дефолтные',
        'history',
        {
            adjust_campaign: 'mobweb_history_standalone',
            adjust_t: '35423dg_rewrww',
        },
        'https://sb76.adj.st/history?adjust_deeplink=autoru%3A%2F%2Fapp%2Fhistory&adjust_t=35423dg_rewrww&adjust_campaign=mobweb_history_standalone',
    ],
])(`должен правильно построить %s`, (title, webUrl, params, result) => {
    expect(buildAdjustUrl(webUrl, params).toString()).toEqual(result);
});
