const got = require('got');
const pMap = require('p-map');
const buildViewerUrl = require('../utils/build-viewer-url');

const tests = [
    {
        name: 'Интерактивность / Рубричные организации',
        center: [55.759379, 37.620113],
        zoom: 16
    },
    {
        name: 'Интерактивность / Горные вершины',
        center: [55.914987, 92.748172],
        zoom: 15
    },
    {
        name: 'Интерактивность / ж/д, остановки, МЦД, метро',
        center: [55.757384, 37.662371],
        zoom: 17
    },
    {
        name: 'Интерактивность / Метротрам',
        center: [48.773155, 44.574367],
        zoom: 16
    },
    {
        name: 'Интерактивность / Метро, фуникулер, пристани',
        center: [41.025517, 28.973399],
        zoom: 16
    },
    {
        name: 'Интерактивность / Морской вокзал, пристани',
        center: [44.616460, 33.526972],
        zoom: 17
    },
    {
        name: 'Интерактивность / Фуникулер',
        center: [43.661823, 40.262868],
        zoom: 15
    },
    {
        name: 'Интерактивность / Метро и остановки',
        center: [50.447839, 30.525168],
        zoom: 16
    },
    {
        name: 'Интерактивность / Аэропорты',
        center: [55.520118, 37.828762],
        zoom: 8
    },
    {
        name: 'Слои / Пробки Москва',
        center: [55.750136, 37.623095],
        zoom: 14,
        layer: 'traffic'
    },
    {
        name: 'Слои / Пробки Стамбул',
        center: [41.008952, 28.959415],
        zoom: 15,
        layer: 'traffic'
    },
    {
        name: 'Слои / Парковки Москва',
        center: [55.759379, 37.620113],
        zoom: 16,
        layer: 'carparks'
    },
    {
        name: 'Слои / Парковки Стамбул',
        center: [41.008952, 28.959415],
        zoom: 15,
        layer: 'carparks'
    }
];

function buildInteractivityTests() {
    const result = [];

    for (const {name, center, zoom, layer} of tests) {
        const dayMetainfo = {zoom, center: center.join(','), layer};
        const nightMetainfo = {...dayMetainfo, night: true};

        result.push(
            {name, result: {metaInfo: dayMetainfo}},
            {name, result: {metaInfo: nightMetainfo}}
        );
    }

    return result;
}

function createInteractivitySuites(poolId) {
    return pMap(buildInteractivityTests(), (test) => got.post('https://sandbox.toloka.yandex.ru/api/v1/task-suites/', {
        headers: {
            Authorization: `OAuth ${process.env.TOLOKA_TOKEN}`
        },
        json: true,
        body: {
            pool_id: poolId,
            tasks: [
                {
                    input_values: {
                        name: test.name,
                        url: buildViewerUrl(test)
                    }
                }
            ],
            infinite_overlap: true
        },
        query: {
            open_pool: true
        }
    }).then(({body}) => body), {concurrency: process.env.CONCURRENCY || 2});
}

module.exports = createInteractivitySuites;
