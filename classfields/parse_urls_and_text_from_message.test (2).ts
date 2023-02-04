import parse_urls_and_text_from_message from './parse_urls_and_text_from_message';

it('вернет массив с оригинальным текстом, если внутри нет ссылок', () => {
    expect(parse_urls_and_text_from_message('Это скучное сообщение без ссылок')).toStrictEqual([ 'Это скучное сообщение без ссылок' ]);
});

it('вернет массив, в котором будет текст и распарсенные ссылки на яндекс и авто.ру и добавит https:// при необходимости', () => {
    expect(parse_urls_and_text_from_message(
        'Это сообщение заметно веселее, потому что внутри есть ссылки на https://auto.ru/cars/all/?price_to=333333 и yandex.com/help/me',
    ))
        .toStrictEqual([
            'Это сообщение заметно веселее, потому что внутри есть ссылки на ',
            {
                text: 'https://auto.ru/cars/all/?price_to=333333',
                url: 'https://auto.ru/cars/all/?price_to=333333',
            },
            ' и ',
            {
                text: 'yandex.com/help/me',
                url: 'https://yandex.com/help/me',
            },
            '',
        ]);
});

it('вернет массив, в котором будет текст и распарсенная ссылка на какой-то внешний ресурс, но без указания url', () => {
    expect(parse_urls_and_text_from_message(
        'Это сообщение самое веселое, ведь несет в себе ссылку на pornhub.com',
    ))
        .toStrictEqual([
            'Это сообщение самое веселое, ведь несет в себе ссылку на ',
            {
                text: 'pornhub.com',
                url: undefined,
            },
            '',
        ]);
});
