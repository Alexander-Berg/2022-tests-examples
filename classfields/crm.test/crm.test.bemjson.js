({
    block: 'page',
    attrs : { style : 'padding: 20px' },
    title: 'Form elements',
    head: [
        {elem: 'css', url: 'crm.test.css'}
    ],
    scripts: [{elem: 'js', url: 'crm.test.js'}],
    mods : { theme : 'islands' },
    content: [

        {
            block : 'button',
            mods : { theme : 'islands', size : 'm', type : 'submit' },
            text : 'Пополнить ещё'
        }

    ]
})