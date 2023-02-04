export const fixtures = {
    'StringsReplacer Метод replace Корректно возвращает текст неизменным При наличии совпадений текста, но с другим регистром':
        {
            text: 'тойота камри - прекрасный автомобиль, как и киа рио',
        },

    'StringsReplacer Метод replace Заменяет единственное вхождение текста на ссылку При отсутствии другого текста': {
        text: 'Kia Rio',
    },
    'StringsReplacer Метод replace Заменяет единственное вхождение текста на ссылку Корректно обрабатывая конец слова (не заменяя части слова)':
        {
            text: 'FordFocus - это слитное написание для "Ford Focus"',
        },
    'StringsReplacer Метод replace Заменяет единственное вхождение текста на ссылку При отсутствии других ссылок в тексте':
        {
            text: 'Текст <span>про</span> Киа Рио и только про неё',
        },
    'StringsReplacer Метод replace Заменяет единственное вхождение текста на ссылку При наличии ссылки в тексте, ссылка остается нетронутой':
        {
            text: '<p>Текст про Rio, <a href="https://auto.ru/moskva/cars/kia/rio/all/" target="_blank">Kia Rio</a> рулит</p>',
        },

    'StringsReplacer Метод replace Заменяет множественные вхождения текста на ссылки При нулевом минимальном расстоянии между ссылками В простом тексте':
        {
            text: 'Camry-Camry (1,2) Фокус (3) FocusFocus Ford Focus (4) РиоFocus Фокус (5)',
        },
    'StringsReplacer Метод replace Заменяет множественные вхождения текста на ссылки При нулевом минимальном расстоянии между ссылками При наличии html-тегов в тексте':
        {
            text: '<p>Ехал camry через камри видит <span>Camry</span> (1) -CamryCamry Фокус (2) <a href="/">Focus Focus</a> Ford Focus (3) РиоFocus</p>',
        },
    'StringsReplacer Метод replace Заменяет множественные вхождения текста на ссылки При нулевом минимальном расстоянии между ссылками При наличии html-сущностей в тексте':
        {
            text: '<p>Ехал camry через камри видит Camry&nbsp;(1)CamryCamry Фокус (2) &#60;&nbsp;Ford Focus (3) РиоFocus</p>',
        },
    'StringsReplacer Метод replace Заменяет множественные вхождения текста на ссылки При ненулевом минимальном расстоянии между ссылками В простом тексте':
        {
            text: 'Camry (1) Camry Фокус Focus (2) Focus Focus Ford Focus (3) РиоFocus Фокус (4)',
        },
    'StringsReplacer Метод replace Заменяет множественные вхождения текста на ссылки При ненулевом минимальном расстоянии между ссылками При наличии html-тегов в тексте':
        {
            text: '<p><span>Camry</span> (1) <span>Camry</span> <span>Camry</span> Фокус (2) <div class="test">Focus Focus Ford Focus (3)</div></p>',
        },
    'StringsReplacer Метод replace Заменяет множественные вхождения текста на ссылки При ненулевом минимальном расстоянии между ссылками При наличии html-ссылок в тексте':
        {
            text: '<p>снова Camry (1) <span>Rio</span>-<a href="/">Camry Camry</a> Фокус (2) Focus Focus Ford Focus (3) Rio Rio</p>',
        },
    'StringsReplacer Метод replace Заменяет множественные вхождения текста на ссылки При ненулевом минимальном расстоянии между ссылками При наличии html-сущностей в тексте':
        {
            text: '<p>снова Camry (1) камри camry Camry&nbsp;(2)CamryCamry Фокус (3)&#60;&nbsp;Ford Focus РиоFocus</p>',
        },
    'StringsReplacer Метод replace Заменяет множественные вхождения текста на ссылки Учитывая лимит на количество ссылок':
        {
            text: '<p>Ехал Camry через Camry видит Camry Camry Camry</p>',
        },
};
