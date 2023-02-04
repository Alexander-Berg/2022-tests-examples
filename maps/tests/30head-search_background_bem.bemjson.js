({
    block: 'b-page',
    title: 'Поисковая форма с однотонным фоном',
    head: [
        { elem: 'css', url: '_30head-search_background_bem.css', ie: false },
        { elem: 'css', url: '_30head-search_background_bem.ie.css', ie: 'lt IE 8' },
        { block: 'i-jquery', elem: 'core' },
        { elem: 'js', url: '_30head-search_background_bem.js' }
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
                                attrs: { action: '' },
                                content: [
                                    {
                                        elem: 'row',
                                        content: [
                                            {
                                                elem: 'col',
                                                mix: [{ elem: 'input' }],
                                                content: {
                                                    block: 'b-form-input',
                                                    mods: { size: 16 },
                                                    content: { elem: 'input' }
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
                                        content: [
                                            {
                                                elem: 'col',
                                                mix: [{ block: 'b-search', elem: 'under' }],
                                                attrs: { colspan: 2 },
                                                content: [
                                                    { elem: 'advanced', url: '/advanced.xml', content: 'расширенный поиск' }
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