modules.define(
    'select',
    function(provide, select)
    {
        provide(select.decl({modName : 'engine_type'}, {

            _updateControl : function() {
                this.elem('control').remove();
                this.dropElemCache('control');

                this.domElem.prepend(this._getCheckedItems().map(function(item) {

                    var name, val = item.getVal();

                    if (item.hasMod('group', 'turbo')) {
                        name = 'search[turbo_type][' + val + ']';
                    } else {
                        name = 'search[engine_type][' + val + ']';
                    }

                    return select._createControlHTML(name, '1');

                }));
            }

        }));
    }
);