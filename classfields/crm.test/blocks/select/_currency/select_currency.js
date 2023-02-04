modules.define(
    'select',
    function(provide, select)
    {
        provide(select.decl({modName : 'currency'}, {

            _updateButton : function() {
                var checkedItem = this._getCheckedItems().pop();
                if (checkedItem) {
                    var html = $(checkedItem.domElem).html();
                    this._button.elem('text').html(html);
                } else {
                    this.__base.apply(this, arguments);
                }
            }

        }));
    }
);