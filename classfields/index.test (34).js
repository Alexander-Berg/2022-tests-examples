const resultMock = require('../result.mock');
const DivCard = require('./index');
const PrepareResultForMorda = require('www-search-app/lib/prepareResultForMorda');
const pseudoShuffle = arr => arr;

const urlMatcher = /"https:\/\/auto([\s\S]+?)"/g;
const hasBadUrl = (divCard, content) => Boolean(divCard.match(urlMatcher).find((url) => {
    let cond;
    if (content) {
        cond = !(url.includes('from') &&
            url.includes('utm_campaign') &&
            url.includes(`utm_content=${ content }`) &&
            url.includes('utm_source') &&
            url.includes('utm_medium'));
    } else {
        cond = !(url.includes('from') &&
            url.includes('utm_campaign') &&
            url.includes(`utm_content`) &&
            url.includes('utm_source') &&
            url.includes('utm_medium'));
    }

    if (cond) {
        throw new Error('bad utm for ' + url);
    }

    return cond;
}),
);

it('Должен отдать divjson для desktop', () => {
    const opts = {
        platform: 'desktop',
        shuffle: false,
        experiments: [],
    };

    const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

    const data = preparer.getJson();
    expect(new DivCard(data, opts)).toMatchSnapshot();
});

it('Должен отдать divjson для touch', () => {
    const opts = {
        platform: 'touch',
        shuffle: false,
        experiments: [],
    };

    const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

    const data = preparer.getJson();
    expect(new DivCard(data, opts)).toMatchSnapshot();
});

it('Должен отдать divjson для touch и добавить название эксперимента в utm_content', () => {
    const expName = 'showmore_etalon';
    const opts = {
        platform: 'touch',
        shuffle: false,
        experiments: [ expName ],
    };

    const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

    const data = preparer.getJson();
    expect(new DivCard(data, opts)).toMatchSnapshot();
});

it('Не должен построить двухэтажный блок, если есть эксп doubledeck_hotstart и campaign = recommended-coldstart', () => {
    const opts = {
        platform: 'desktop',
        shuffle: false,
        experiments: [ 'doubledeck_hotstart' ],
    };

    const resultMockColdStart = {
        ...resultMock,
        recommended: {
            ...resultMock.recommended,
            campaign: 'recommended-coldstart',
        },
    };

    const preparer = new PrepareResultForMorda(resultMockColdStart, opts, pseudoShuffle);

    const data = preparer.getJson();

    expect(new DivCard(data, opts)).toMatchSnapshot();
});

it('Все ссылки должны содержать utm-метки', () => {
    const opts = {
        platform: 'desktop',
        shuffle: false,
        experiments: [],
    };

    const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

    const data = preparer.getJson();
    const divJson = JSON.stringify(new DivCard(data, opts));
    expect(hasBadUrl(divJson)).toEqual(false);
});

it('Все ссылки экспа doubledeck должны содержать utm-метки', () => {
    const expName = 'doubledeck';
    const opts = {
        platform: 'desktop',
        shuffle: false,
        experiments: [ expName ],
    };

    const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

    const data = preparer.getJson();
    const divJson = JSON.stringify(new DivCard(data, opts));
    expect(hasBadUrl(divJson, expName)).toEqual(false);
});

it('Должен отдать divjson для desktop и указать utm_content=tabs, если есть такой эксп', () => {
    const expName = 'tabs';
    const opts = {
        platform: 'desktop',
        shuffle: false,
        experiments: [ expName ],
    };

    const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

    const data = preparer.getJson();
    const divJson = JSON.stringify(new DivCard(data, opts));
    expect(hasBadUrl(divJson, expName)).toEqual(false);
});

it('Эксп addcard_3 должен содержать ссылку на добавление карточки', () => {
    const expName = 'addcard_3';
    const opts = {
        platform: 'desktop',
        shuffle: false,
        experiments: [ expName ],
    };

    const preparer = new PrepareResultForMorda(resultMock, opts, pseudoShuffle);

    const data = preparer.getJson();
    const divJson = JSON.stringify(new DivCard(data, opts));

    expect(divJson).toContain('https://auto.ru/cars/used/add/');
});

it('Блок на десктопе содержит указанный бейджик', () => {
    const opts = {
        platform: 'desktop',
        shuffle: false,
    };
    const resultMockWithRotate = {
        ...resultMock,
        geo: resultMock.geo.map(geoTab => ({ ...geoTab, offers: geoTab.offers.map(offer => ({ ...offer, badge: 'VinResolution' })) })),
    };

    const preparer = new PrepareResultForMorda(resultMockWithRotate, opts, pseudoShuffle);

    const data = preparer.getJson();
    const divJson = JSON.stringify(new DivCard(data, opts));

    expect(divJson).toContain('Отчёт по VIN');
});
