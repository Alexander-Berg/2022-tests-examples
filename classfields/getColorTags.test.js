const getColorTags = require('auto-core/react/dataDomain/crossLinks/helpers/getColorTags');

it('Должен отдать перекрестные ссылки для цветов', () => {
    const searchParameters = {
        catalog_filter: [
            {
                mark: 'BMW',
            },
        ],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {
        mark: {
            name: 'BMW',
        },
    };

    const crossLinks = {
        colors: [ {
            colorRus: 'коричневого',
            value: 'KORICHNEVYJ',
            key: 'BMW_KORICHNEVYJ',
            color: '200204',
        }, { colorRus: 'пурпурного', value: 'PURPURNYJ', key: 'BMW_PURPURNYJ', color: '660099' }, {
            colorRus: 'жёлтого',
            value: 'ZHELTYJ',
            key: 'BMW_ZHELTYJ',
            color: 'FFD600',
        }, { colorRus: 'зелёного', value: 'ZELENYJ', key: 'BMW_ZELENYJ', color: '007F00' }, {
            colorRus: 'чёрного',
            value: 'CHERNYJ',
            key: 'BMW_CHERNYJ',
            color: '040001',
        }, { colorRus: 'бежевого', value: 'BEZHEVYJ', key: 'BMW_BEZHEVYJ', color: 'C49648' }, {
            colorRus: 'серебристого',
            value: 'SEREBRISTYJ',
            key: 'BMW_SEREBRISTYJ',
            color: 'CACECB',
        } ],
    };

    expect(getColorTags(searchParameters, mmmInfo, crossLinks.colors)).toMatchSnapshot();
});

it('Не должен отдать перекрестные ссылки для цветов если нет марки', () => {
    const searchParameters = {
        catalog_filter: [],
        section: 'all',
        category: 'cars',
    };

    const mmmInfo = {};

    const crossLinks = {
        colors: [ {
            colorRus: 'коричневого',
            value: 'KORICHNEVYJ',
            key: 'BMW_KORICHNEVYJ',
            color: '200204',
        }, { colorRus: 'пурпурного', value: 'PURPURNYJ', key: 'BMW_PURPURNYJ', color: '660099' }, {
            colorRus: 'жёлтого',
            value: 'ZHELTYJ',
            key: 'BMW_ZHELTYJ',
            color: 'FFD600',
        }, { colorRus: 'зелёного', value: 'ZELENYJ', key: 'BMW_ZELENYJ', color: '007F00' }, {
            colorRus: 'чёрного',
            value: 'CHERNYJ',
            key: 'BMW_CHERNYJ',
            color: '040001',
        }, { colorRus: 'бежевого', value: 'BEZHEVYJ', key: 'BMW_BEZHEVYJ', color: 'C49648' }, {
            colorRus: 'серебристого',
            value: 'SEREBRISTYJ',
            key: 'BMW_SEREBRISTYJ',
            color: 'CACECB',
        } ],
    };

    expect(getColorTags(searchParameters, mmmInfo, crossLinks.colors)).toEqual([]);
});
