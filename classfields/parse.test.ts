import parse from './parse';

const advantages = [
    '• Комфортные передние сиденья',
    '• Кожаное рулевое колесо',
    '• Велюровые коврики',
    '• Цвет передней панели: слоновая кость',
    '• Обивка потолка салона "Антрацит"',
    '• Легкосплавные диски',
    '• Площадка для отдыха левой ноги',
    '• Автодоводчик дверей',
    '• Знак аварийной остановки',
    '• Передний подлокотник',
    '• Пакет для курящих',
];

it('текст не содержит символа новой строки - должен просто обернуть текст в массив', () => {
    const text = 'Какое-то простое описание от ленивого дилера';

    expect(parse(text)).toEqual([ text ]);
});

it('текст содержит переносы строк, но не содержит списков - должен оставить одной строкой и обернуть в массив', () => {
    const str1 = 'Дилер увидел что его тачки не покупают.';
    const str2 = 'Ему всё ещё жутко лень, но он решил добавить пару лишний строк текста.';
    const str3 = 'Правда ему это всё равно не помогает';

    const text = [ str1, str2, str3 ].join('\n');

    expect(parse(text)).toEqual([ text ]);
});

it('текст содержит список, но количество строк в нём меньше необходимого - должен отдать его как обычные строки', () => {
    const str1 = 'Пришло время платить за ипотеку, но тачки всё ещё не покупают.';
    const str2 = 'Дилер напрягся и решил написать список, чтобы тачка резко стала супер-привлекательной.';

    const shortList = advantages.slice(3);

    const text = [ str1, str2, ...shortList ].join('\n');

    expect(parse(text)).toEqual([ str1 + '\n', str2 + '\n', shortList.join('\n') ]);
});

it('текст содержит список, соответствующий критериям - должен вынести его в подмассив строк', () => {
    const str1 = 'Сроки жмут';
    const str2 = 'Дилер нервничает.';
    const str3 = 'Пальцы начинают стучать по клавиатуре.';

    const text = [ str1, str2, str3, ...advantages ].join('\n');

    expect(parse(text)).toEqual([ str1 + '\n', str2 + '\n', str3 + '\n', advantages.map(element => element.slice(2)) ]);
});

it('текст содержит список, но в нём есть слишком длинные строки - должен отдать его как обычные строки', () => {
    const str1 = 'Вдруг тачки начали покупать.';
    const str2 = 'Вроде как и на платёж по ипотеке хватает.';
    const str3 = 'Но дилер уже вошел в раж. Его не остановить. Список растёт и растёт!';
    const subStr12 = '• И вообще это самая офигенная тачка, какую вы только могли себе представить!' +
        'Я сам таких четыре купил! И жене, и маме, и даже тёще. Представляете! ТЕЩЁ!' +
        'Потому что ну как не купить такую ласточку, такую замечательную, такую красивую, с такими мягонькими ковриками!';

    const listWithLongerStr = [ ...advantages, subStr12 ];

    const text = [ str1, str2, str3, ...listWithLongerStr ].join('\n');

    expect(parse(text)).toEqual([
        str1 + '\n', str2 + '\n', str3 + '\n',
        listWithLongerStr.join('\n'),
    ]);
});

it('текст содержит несколько списков с разными маркерами и проблемную строку - должен правильно обработать каждый список и строку', () => {
    const str1 = 'Тачка - огонь!';
    const str2 = 'Бери сразу десять!';
    const strWithPseudoMarker = 'А то ни одной не останется - все сметут.';
    const str3 = 'Отвечаю - не пожалеешь!';

    const shortList = advantages.slice(5);
    const listWihtOtherMarkers = advantages.map((s) => s.replace('• ', '* '));
    const listWithLongerStr = [ advantages[0].repeat(20), ...advantages ];

    const text = [
        ...shortList,
        ...listWihtOtherMarkers,
        str1, str2,
        ...advantages,
        strWithPseudoMarker,
        ...listWithLongerStr,
        str3,
    ].join('\n');

    expect(parse(text)).toEqual([
        shortList.join('\n') + '\n',
        listWihtOtherMarkers.map(element => element.slice(2)),
        str1 + '\n', str2 + '\n',
        advantages.map(element => element.slice(2)),
        strWithPseudoMarker + '\n',
        listWithLongerStr.join('\n') + '\n',
        str3,
    ]);
});
