import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { ICardContactsModalProps } from '../';

const backCallTrafficInfo = {
    trafficFrom: 'string',
    utmCampaign: 'string',
    utmContent: 'string',
    utmMedium: 'string',
    utmSource: 'string',
    utmTerm: 'string',
};

const weekTimetable = [
    {
        dayFrom: 1,
        dayTo: 5,
        timePattern: [{ open: '10:00', close: '19:00' }],
    },
];

interface ITestCase {
    title: string;
    props: ICardContactsModalProps;
    options: { viewport: { width: number; height: number } };
}

const developerWithCallbackAndChat = {
    title: 'Застройщик с обратным звонком и чатом',
    props: {
        name: 'ТД Невский',
        image: generateImageUrl({ width: 165, height: 75 }),
        siteId: '123',
        backCallTrafficInfo,
        phones: ['+78127482181'],
        timetableZoneMinutes: 180,
        weekTimetable,
        hasChat: true,
        isDeveloper: true,
        withBilling: true,
    },
    options: { viewport: { width: 800, height: 700 } },
};

const developerWithCallback = {
    title: 'Застройщик с обратным звонком',
    props: {
        name: 'ТД Невский',
        image: generateImageUrl({ width: 70, height: 40 }),
        siteId: '123',
        backCallTrafficInfo,
        phones: ['+78127482181'],
        timetableZoneMinutes: 180,
        weekTimetable,
        isDeveloper: true,
        withBilling: true,
    },
    options: { viewport: { width: 800, height: 550 } },
};

const ownerWithChat = {
    title: 'Собственник с чатом',
    props: {
        name: 'Иван Калита',
        image: generateImageUrl({ width: 200, height: 200 }),
        phones: ['+78127482181'],
        hasChat: true,
    },
    options: { viewport: { width: 800, height: 500 } },
};

const ownerWithSeveralPhones = {
    title: 'Собственник с несколькими телефонами',
    props: {
        name: 'Лю Цысинь',
        phones: ['+78127482181', '+78127482182'],
    },
    options: { viewport: { width: 800, height: 400 } },
};

const agentFromAgency = {
    title: 'Агент из агентства',
    props: {
        name: 'Сладкий пирожок',
        category: 'Агентство «Тортик»',
        image: generateImageUrl({ width: 60, height: 60 }),
        phones: ['+78127482181'],
    },
    options: { viewport: { width: 800, height: 400 } },
};

export const testCases: ITestCase[] = [
    developerWithCallbackAndChat,
    developerWithCallback,
    ownerWithChat,
    ownerWithSeveralPhones,
    agentFromAgency,
];
