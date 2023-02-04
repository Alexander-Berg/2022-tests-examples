({
    block: 'b-page',
    title: 'b-lang-switcher',
    head: [
        { elem: 'css', url: '_10lang-switcher_bem.css', ie: false },
        { elem: 'css', url: '_10lang-switcher_bem.ie.css', ie: 'lt IE 8' },
        { elem: 'js', url: '//yandex.st/jquery/1.4.2/jquery.min.js' },
        { elem: 'js', url: '_10lang-switcher_bem.js' }
    ],
    content: {
        block: 'i-lego-example',
        attrs: { style: 'padding: 140px 40px 10px 40px;' },
        content: [
            {
                block: 'b-lang-switcher',
                lang: { code: 'ru', name: 'Ru' },
                content: [
                    {
                        elem: 'lang',
                        lang: { code: 'ua', name: 'Ua' },
                        url: '#/bla'
                    },
                    {
                        elem: 'lang',
                        lang: { code: 'en', name: 'En' },
                        url: '#/bla'
                    },
                    {
                        elem: 'lang',
                        lang: { code: 'kz', name: 'Kz' },
                        url: '#/bla'
                    },
                    {
                        elem: 'lang',
                        lang: { code: 'by', name: 'By' },
                        url: '#/bla'
                    },
                    {
                        elem: 'lang',
                        selected: 'yes',
                        lang: { code: 'ru', name: 'Ru' },
                        url: '#/bla'
                    }
                ]
            },
            ' — ',
            {
                block: 'b-lang-switcher',
                lang: { code: 'ru', name: 'Ru' },
                content: [
                    {
                        elem: 'lang',
                        lang: { code: 'ua', name: 'Ua' },
                        url: '#/bla'
                    },
                    {
                        elem: 'lang',
                        lang: { code: 'en', name: 'En' },
                        url: '#/bla'
                    },
                    {
                        elem: 'lang',
                        lang: { code: 'ru', name: 'Ru' },
                        selected: 'yes',
                        url: '#/bla'
                    }
                ]
            }
        ]
    }
})