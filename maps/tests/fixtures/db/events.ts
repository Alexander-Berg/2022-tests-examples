import {Table} from 'app/types/consts';

const rows = [
    {
        external_id: 'onlayn-pokaz-spektaklyagamlet',
        branch: 'draft',
        locale: 'ru_RU',
        content: JSON.stringify({
            oid: '1390849720',
            type: 'poi',
            endDate: '3020-06-03T20:59:59.000Z',
            poiData: {
                iconTags: ['theatre'],
                label: {
                    title: 'Спектакль\\n«Гамлет» '
                },
                subtitle: 'Спектакль\\n«Гамлет» ',
                coordinate: {lat: 55.760198, lon: 37.613035}
            },
            eventData: {
                title: 'Онлайн-показ спектакля «Гамлет» ',
                buttons: [],
                images: [
                    {
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a000001725601f33669a81b3aec5470ec82/%s'
                    }
                ],
                description: 'Спектакль с Константином Хабенским, Михаилом Трухиным и Михаилом Пореченковым'
            },
            eventMeta: {userLogin: 'vitaly-vasin'},
            startDate: '2008-06-03T20:59:59.000Z',
            zoomRange: {max: 23, min: 17},
            geoRegionId: 213
        })
    }
];

const events = {
    table: Table.EVENTS,
    rows
};
export {events};
