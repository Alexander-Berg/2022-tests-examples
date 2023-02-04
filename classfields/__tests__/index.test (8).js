import { advanceTo } from 'jest-date-mock';

import dayjs from '@realty-front/dayjs';
import i18n from 'realty-core/view/react/libs/i18n';

import { getOfferTimeText } from '..';

const date = dateStr => dayjs(dateStr, 'DD.MM.YYYY HH:mm').utcOffset(0).toISOString();
const today = date('01.01.2020 12:00');

advanceTo(new Date(today));

/* eslint-disable max-len */
const fixturesFullText = [
    [
        date('31.12.2019 12:00'),
        date('31.12.2020 12:00'),
        'размещено вчера, 365\u00a0дней до\u00a0снятия'
    ],
    [
        date('27.12.2019 12:00'),
        date('11.01.2020 12:00'),
        'размещено 27\u00a0декабря 2019, 10\u00a0дней до\u00a0снятия'
    ],
    [
        date('27.12.2019 12:00'),
        date('24.01.2020 12:00'),
        'размещено 27\u00a0декабря 2019, 23\u00a0дня до\u00a0снятия'
    ],
    [
        date('27.12.2019 12:00'),
        date('22.01.2020 12:00'),
        'размещено 27\u00a0декабря 2019, 21\u00a0день до\u00a0снятия'
    ],
    [
        date('27.12.2019 12:00'),
        date('02.01.2020 12:00'),
        'размещено 27\u00a0декабря 2019, 1\u00a0день до\u00a0снятия'
    ],
    [
        date('27.12.2019 12:00'),
        date('01.01.2020 13:00'),
        'размещено 27\u00a0декабря 2019, 1\u00a0час до\u00a0снятия'
    ],
    [
        date('27.12.2019 12:00'),
        date('01.01.2020 19:00'),
        'размещено 27\u00a0декабря 2019, 7\u00a0часов до\u00a0снятия'
    ],
    [
        date('27.12.2019 12:00'),
        date('02.01.2020 11:00'),
        'размещено 27\u00a0декабря 2019, 23\u00a0часа до\u00a0снятия'
    ],
    [
        date('27.12.2019 12:00'),
        date('01.01.2020 12:00'),
        'размещено 27\u00a0декабря 2019, меньше минуты до\u00a0снятия'
    ]
];

const paidRenewalFixturesText = [
    [ date('31.12.2019 12:00'), date('01.01.2020 22:00'), 'размещено вчера, 10\u00a0часов до\u00a0продления' ],
    [ date('27.12.2019 12:00'), date('04.01.2020 19:00'), 'размещено 27\u00a0декабря 2019, 3\u00a0дня до\u00a0продления' ],
    [ date('27.12.2019 12:00'), date('01.01.2020 12:30'), 'размещено 27\u00a0декабря 2019, 30 минут до продления' ],
    [ date('27.12.2019 12:00'), date('01.01.2020 11:30'), 'размещено 27\u00a0декабря 2019, продление уже сегодня' ]
];
/* eslint-enable max-len */

describe('getOfferTimeText', () => {
    beforeEach(() => {
        i18n.setLang('ru');
    });

    test.each(fixturesFullText)(
        'Should return correct text for created at %s and expires after %s offers',
        (created, expires, expected) => {
            const offersTimeInfo = getOfferTimeText({ created, expires }, today);

            const [ creationDateText, expireText ] = expected.split(', ');

            expect(offersTimeInfo.expireText).toBe(expireText);
            expect(offersTimeInfo.creationDateText).toBe(creationDateText);
        });

    it('Should return correct text for created status', () => {
        const created = date('27.12.2019 12:00');
        const expires = date('31.12.2020 13:00');
        const expectedCreationDateText = 'создан 27\u00a0декабря 2019';

        const { creationDateText } = getOfferTimeText({ created, expires, status: 'created' }, today);

        expect(creationDateText).toBe(expectedCreationDateText);
    });

    it('Should return correct text for offer with showUpdateTime', () => {
        const created = date('01.01.2020 12:00');
        const expires = date('31.12.2020 13:00');
        const expectedCreationDateText = 'создан 1\u00a0января';

        const { creationDateText } = getOfferTimeText(
            { created, expires, status: 'created' },
            today,
            { showUpdateTime: true }
        );

        expect(creationDateText).toBe(expectedCreationDateText);
    });

    test.each(paidRenewalFixturesText)(
        'Should return correct text for paid offers created at %s and renewal after %s offers',
        (created, placementEnd, expected) => {
            const placementInfo = {
                paymentRequired: {}
            };

            const placementProduct = {
                end: placementEnd
            };

            const offersTimeInfo = getOfferTimeText({ created, placementInfo, placementProduct }, today);

            const [ creationDateText, expireText ] = expected.split(', ');

            expect(offersTimeInfo.expireText).toBe(expireText);
            expect(offersTimeInfo.creationDateText).toBe(creationDateText);
        });
});
