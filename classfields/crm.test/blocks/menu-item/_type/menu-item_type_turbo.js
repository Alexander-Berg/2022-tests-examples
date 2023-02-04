modules.define(
    'menu-item',
    function(provide, menuItem)
    {
        provide(menuItem.decl({modName : 'type', modVal : 'turbo'}, {
            onSetMod: {
                'js': {
                    'inited': function () {
                        this.__base.apply(this, arguments);
                        this.on('change', function() {
                            //debugger;
                        });
                    }
                },
                'checked': {
                    'true': function() {

                    },
                    '': function() {

                    }
                }
            }
        }));
    }
);