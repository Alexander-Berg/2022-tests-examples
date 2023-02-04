const _ = require('lodash');

const eventParamsMock = {
    timestamp: '2019-12-23T13:39:04.479Z',
    card_show_event: {
        category: 'CARS',
        card_id: '1092301084-9f60cbba',
        card_from: 'MORDA',
    },
    web_referer: 'https://auto.ru/',
    utm_content: 'test',
};
const preparer = require('./prepareBody');

const versionMock = '{{DEBIAN_VERSION}}';
const contextReqMock = {
    experimentsData: {
        bucket: 2,
    },
    geoIds: [ 213 ],
};
const eventsMock = new Array(2).fill(eventParamsMock);

const paramsMock = {
    events: JSON.stringify(eventsMock),
};

it('должен правильно распарсить и обогатить параметры', () => {
    expect(preparer(contextReqMock, paramsMock, versionMock)).toMatchSnapshot();
});

it('должен правильно распарсить и обогатить параметры, если параметры пришли массивом', () => {
    expect(preparer(contextReqMock, { events: eventsMock }, versionMock)).toMatchSnapshot();
});

it('при отсутствии данных об экспериментах, должен установить нулевой бакет', () => {
    expect(preparer({ geoIds: [ 213 ] }, paramsMock, versionMock)).toMatchSnapshot();
});

it('если utm-данные переданы в виде массива, должен привести их к строке', () => {
    const modifiedEventParams = _.cloneDeep(eventParamsMock);

    modifiedEventParams.utm_content = [ 'foo', 'bar' ];

    expect(preparer(contextReqMock, {
        events: JSON.stringify(
            new Array(2).fill(modifiedEventParams),
        ),
    }, versionMock)).toMatchSnapshot();
});

it('не должен падать если не переданы события', () => {
    expect(preparer(contextReqMock, undefined, versionMock)).toBeNull();
});

it('не должен падать если переданы некорректные данные', () => {
    expect(preparer(contextReqMock, { events: 'bla-bla' }, versionMock)).toBeNull();
});
