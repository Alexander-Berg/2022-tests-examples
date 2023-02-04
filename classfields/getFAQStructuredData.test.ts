import getCatalogStructuredData from './getFAQStructuredData';

const data = [
    {
        question: 'Зачем проверять авто по VIN или государственному номеру перед покупкой?',
        answer: 'Проверка авто по VIN или госномеру покажет всё это.',
    },
    {
        question: 'Откуда вы берёте данные об истории автомобиля?',
        answer: 'Мы пользуемся официальными базами, данными из тысяч СТО по всей стране, информацией от официальных дилеров.',
    },
];

it('Должен отдать структурированные данные для FAQ', () => {
    expect(getCatalogStructuredData(data)).toMatchSnapshot();
});
