modules.define(
    'menu-item',
    function(provide, menuItem)
    {
        provide(menuItem.decl({modName : 'role', modVal : 'reset'}, {

            beforeSetMod: {
                'checked': {
                    'true': function() {
                        return false;
                    }
                }
            }
        }));
    }
);