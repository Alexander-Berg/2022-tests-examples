({
    block : 'page',
    title : 'bem-components: button',
    mods : { theme : 'islands' },
    head : [
        { elem : 'css', url : '_gemini.css' },
        { elem : 'js', url : '_gemini.js' }
    ],
    content : [
        { tag : 'h2', content : 'islands' },

        { tag : 'h3', content : 'view' },
        { tag : 'p', content : [
            {
                block : 'button',
                mods : { theme : 'islands', size : 'm' },
                text : 'normal',
                cls : 'gemini-test-button-islands-enabled'
            },
            '&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                mods : { theme : 'islands', size : 'm', disabled : true },
                text : 'normal',
                cls : 'gemini-test-button-islands-disabled'
            },
            '&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                mods : { theme : 'islands', size : 'm', view : 'plain' },
                text : 'plain',
                cls : 'gemini-test-button-plain-enabled'
            },
            '&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                mods : { theme : 'islands', size : 'm', view : 'plain', disabled : true },
                text : 'plain',
                cls : 'gemini-test-button-plain-disabled'
            },
            '&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                mods : { theme : 'islands', size : 'm', view : 'pseudo' },
                text : 'pseudo',
                cls : 'gemini-test-button-pseudo-enabled'
            },
            '&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                mods : { theme : 'islands', size : 'm', view : 'pseudo', disabled : true },
                text : 'pseudo',
                cls : 'gemini-test-button-pseudo-disabled'
            },
            '&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                mods : { theme : 'islands', size : 'm', view : 'action' },
                text : 'action',
                cls : 'gemini-test-button-action-enabled'
            },
            '&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                mods : { theme : 'islands', size : 'm', view : 'action', disabled : true },
                text : 'action',
                cls : 'gemini-test-button-action-disabled'
            }
        ] },

        /* TODO: tests are not implemented
        { tag : 'h3', content : 'size' },
        { tag : 'p', content : ['s', 'm', 'l', 'xl']
            .reduce(function(prev, size, i, arr) {
                prev.push({
                    block : 'button',
                    mods : { theme : 'islands', size : size },
                    text : 'size ' + size,
                    cls : 'gemini-test-button-size-' + size
                });

                if (i < arr.length - 1) prev.push('&nbsp;&nbsp;&nbsp;');

                return prev;
            }, [])
        },
        */

        { tag : 'h3', content : 'link' },
        { tag : 'p', content : [
            {
                block : 'button',
                mods : {
                    theme : 'islands',
                    size : 'm',
                    type : 'link'
                },
                url : '#',
                text : 'default link',
                cls : 'gemini-test-button-islands-link-enabled'
            },
            '&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                mods : {
                    theme : 'islands',
                    size : 'm',
                    type : 'link',
                    disabled : true
                },
                url : '#',
                text : 'disabled link',
                cls : 'gemini-test-button-islands-link-disabled'
            }
        ] },

        { tag : 'h3', content : 'icon' },
        { tag : 'p', content : [
            {
                block : 'button',
                mods : { theme : 'islands', size : 's' },
                // ?????????? ???????????? ?????????????????? ?????????? content, ?????????? ?????????????????? ?? deps.js
                content : { block : 'icon', mods : { action : 'download' } },
                cls : 'gemini-test-button-islands-icon-enabled'
            },
            '&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                mods : { theme : 'islands', size : 's', disabled : true },
                icon : { block : 'icon', mods : { action : 'download' } },
                cls : 'gemini-test-button-islands-icon-disabled'
            }
            /* TODO: tests are not implemented
            '&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                text : 'download',
                mods : { theme : 'islands', size : 's' },
                icon : { block : 'icon', mods : { action : 'download' } }
            },
            '&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                text : 'download',
                mods : { theme : 'islands', size : 's', disabled : true },
                icon : { block : 'icon', mods : { action : 'download' } }
            },
            '&nbsp;&nbsp;&nbsp;',
            {
                block : 'button',
                mods : { theme : 'islands', size : 's' },
                content : [
                    { block : 'icon', mods : { action : 'left' } },
                    { elem : 'text', content : 'set width' },
                    { block : 'icon', mods : { action : 'right' } }
                ]
            }
            */
        ] },

        { tag : 'h3', content : 'togglable' },
        { tag : 'p', content : {
            block : 'button',
            mods : { theme : 'islands', size : 'm', togglable : 'check' },
            text : 'islands check',
            cls : 'gemini-test-button-islands-check'
        } },
        { tag : 'p', content : {
            block : 'button',
            mods : { theme : 'islands', size : 'm', view : 'plain', togglable : 'check' },
            text : 'plain check',
            cls : 'gemini-test-button-plain-check'
        } }
    ]
});
