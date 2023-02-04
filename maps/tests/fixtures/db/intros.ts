import {Branch, Table} from 'app/types/consts';
import {IntroBackgroundType} from 'app/types/db/intros';
import {nextTestDateDay, prevTestDateDay} from './date-helper';

const title = {
    text: 'Интро 1',
    color: '#000000'
};

const subtitle = {
    text: 'Подзаголовок 1',
    color: '#a9a9a9'
};

const background = {
    type: IntroBackgroundType.COLOR,
    value: '#ffffff'
};

const primaryButton = {
    backgroundColor: '#c0c0c0',
    text: 'Установить',
    textColor: '#000000',
    deeplink: 'yandexmaps://maps.yandex.ru/?mode=showcase'
};

const rows = [
    {
        branch: Branch.DRAFT,
        start_date: prevTestDateDay,
        buttons: [primaryButton],
        background,
        title,
        uid: '5bedcf9e-028a-435e-b4bb-f06d688b0391',
        geo_region_ids: [213],
        segments: ['segment-1'],
        end_date: nextTestDateDay,
        priority: 2,
        image: 'https://avatars.mds.yandex.net/get-discovery-int/218162/2a00000171160bf5f0d0aa71b4a0ef44700e/%s',
        subtitle
    },
    {
        branch: Branch.DRAFT,
        start_date: prevTestDateDay,
        uid: 'c8892b3b-dcbe-444e-b933-a6575d8bcbc4',
        background: {
            type: 'color',
            value: '#fff'
        },
        title: {
            text: 'Наше предложение (другое)',
            color: '#000'
        },
        buttons: [
            {
                text: 'Купить',
                deeplink: './',
                textColor: '#000',
                backgroundColor: '#333'
            }
        ]
    },
    {
        branch: Branch.PUBLIC,
        draft_id: '1',
        start_date: prevTestDateDay,
        buttons: [{text: 'only text'}],
        background,
        title,
        uid: '5bedcf9e-028a-435e-b4bb-f06d688b0391',
        geo_region_ids: [213],
        segments: ['segment-1'],
        end_date: nextTestDateDay,
        priority: 2,
        image: 'https://avatars.mds.yandex.net/get-discovery-int/218162/2a00000171160bf5f0d0aa71b4a0ef44700e/%s',
        subtitle
    },
    {
        branch: Branch.PUBLIC,
        draft_id: '2',
        start_date: prevTestDateDay,
        uid: 'c8892b3b-dcbe-444e-b933-a6575d8bcbc4',
        background: {
            type: 'color',
            value: '#fff'
        },
        title: {
            text: 'Наше предложение (другое)',
            color: '#000'
        },
        buttons: [
            {
                text: 'Купить',
                deeplink: './',
                textColor: '#000',
                backgroundColor: '#333'
            }
        ],
        geo_region_ids: [213],
        segments: []
    }
];

const intros = {
    table: Table.INTROS,
    rows: rows.map((item) => ({
        ...item,
        buttons: JSON.stringify(item.buttons),
        background: JSON.stringify(item.background),
        title: JSON.stringify(item.title),
        subtitle: JSON.stringify(item.subtitle)
    }))
};

export {intros};
