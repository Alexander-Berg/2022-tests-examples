({
    block: 'b-page',
    title: 'Поисковая форма с однотонным фоном и поисковой подсказкой',
    head: [
        { elem: 'css', url: '_40head-search-double_columns-head_bem.css', ie: false },
        { elem: 'css', url: '_40head-search-double_columns-head_bem.ie.css', ie: 'lt IE 8' },
        { block: 'i-jquery', elem: 'core' },
        { elem: 'js', url: '_40head-search-double_columns-head_bem.js' }
    ],
    content: [
        {
            block: 'l-head',
            content: [
                { elem: 'g' },
                {
                    elem: 'l',
                    content: {
                        block: 'b-head-logo',
                        content: {
                            elem: 'logo',
                            content: {
                                elem: 'link',
                                content: { elem: 'img' }
                            }
                        }
                    }
                },
                { elem: 'gl' },
                {
                    elem: 'c',
                    content: [
                        {
                            block: 'b-head-tabs',
                            mods: { preset: 'search-and-content' }
                        },
                        {
                            block: 'b-head-search',
                            content: {
                                block: 'b-search',
                                action: '//ya.ru',
                                mods: { type: 'double' },
                                content: [
                                    {
                                        elem: 'row',
                                        content: [
                                            {
                                                elem: 'col',
                                                mix: [ { elem: 'label-1' } ],
                                                content: {
                                                    elem: 'label',
                                                    attrs: { 'for': 'from' },
                                                    content: 'Что: '
                                                }
                                            },
                                            {
                                                elem: 'col',
                                                mix: [ { elem: 'input' } ],
                                                content: {
                                                    block: 'b-form-input',
                                                    id: 'from',
                                                    name: 'from',
                                                    mods: { size: 16 },
                                                    content: [
                                                        { elem: 'hint', content: 'откуда' },
                                                        { elem: 'input' }
                                                    ]
                                                }
                                            },
                                            {
                                                elem: 'col',
                                                mix: [ { elem: 'label-2' } ],
                                                content: {
                                                    elem: 'label',
                                                    attrs: { 'for': 'to' },
                                                    content: 'где: '
                                                }
                                            },
                                            {
                                                elem: 'col',
                                                mix: [{ elem: 'input' }],
                                                content: {
                                                    block: 'b-form-input',
                                                    id: 'to',
                                                    name: 'to',
                                                    mods: { size: 16 },
                                                    content: [
                                                        { elem: 'hint', content: 'куда' },
                                                        { elem: 'input' }
                                                    ]
                                                }
                                            },
                                            {
                                                elem: 'col',
                                                mix: [{ elem: 'button' }],
                                                content: {
                                                    block: 'b-form-button',
                                                    mods: { height: 26, theme: 'grey-no-transparent-26' },
                                                    type: 'submit',
                                                    content: 'Найти'
                                                }
                                            }
                                        ]
                                    },
                                    {
                                        elem: 'row',
                                        mix: [ { elem: 'under'} ],
                                        content: [
                                            {
                                                elem: 'col',
                                                mix: [{ elem: 'under-empty' }]
                                            },
                                            {
                                                elem: 'col',
                                                mix: [{ elem: 'under' }],
                                                content: {
                                                    elem: 'sample',
                                                    name: 'from',
                                                    content: [
                                                        { elem: 'intro', content: 'Например,' },
                                                        '&#160;',
                                                        { block: 'b-pseudo-link', content: 'тест' }
                                                    ]
                                                }
                                            },
                                            {
                                                elem: 'col',
                                                mix: [{ elem: 'under-empty' }]
                                            },
                                            {
                                                elem: 'col',
                                                mix: [{ elem: 'under' }],
                                                content: [
                                                    {
                                                        elem: 'advanced',
                                                        url: '#!/advanced/',
                                                        content: 'расширенный поиск'
                                                    },
                                                    {
                                                        elem: 'sample',
                                                        name: 'to',
                                                        content: [
                                                            { elem: 'intro', content: 'Например,' },
                                                            '&#160;',
                                                            { block: 'b-pseudo-link', content: 'тест' }
                                                        ]
                                                    }
                                                ]
                                            }
                                        ]
                                    }
                                ]
                            }
                        }
                    ]
                },
                { elem: 'gr' },
                {
                    elem: 'r',
                    content: {
                        block: 'b-head-userinfo',
                        content: [
                            {
                                elem: 'row',
                                content: [
                                    { elem: 'td' },
                                    {
                                        elem: 'entry',
                                        content: {
                                            block: 'b-pseudo-link',
                                            url: 'http://passport.yandex.ru/passport?mode=auth&msg=',
                                            content: 'Войти'
                                        }
                                    }
                                ]
                            },
                            {
                                elem: 'row',
                                content: {
                                    elem: 'td',
                                    attrs: { colspan: 2 },
                                    content: [
                                        {
                                            elem: 'setup',
                                            content: {
                                                elem: 'link',
                                                url: '/tunes',
                                                content: 'Настройка'
                                            }
                                        },
                                        {
                                            elem: 'help',
                                            content: {
                                                elem: 'link',
                                                url: '//help.yandex.ru/begemot',
                                                content: 'Помощь'
                                            }
                                        }
                                    ]
                                }
                            },
                            {
                                elem: 'row',
                                content: {
                                    elem: 'td',
                                    attrs: { colspan: 2 },
                                    content: { elem: 'service' }
                                }
                            },
                            {
                                elem: 'row',
                                content: {
                                    elem: 'td',
                                    attrs: { colspan: 2 },
                                    content: {
                                        elem: 'region',
                                        content: [
                                            {
                                                elem: 'link',
                                                url: '/tunes',
                                                content: 'Регион'
                                            },
                                            ': Москва'
                                        ]
                                    }
                                }
                            }
                        ]
                    }
                },
                { elem: 'g' }
            ]
        }
    ]
})