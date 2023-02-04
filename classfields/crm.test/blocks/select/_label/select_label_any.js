modules.define(
    'select',
    function(provide, select)
    {
        provide(select.decl({modName : 'label', modVal: 'any'}, {

            _updateButton : function() {
                var label = this.params.text,
                    prefix = this.params.prefix,
                    items = this._getCheckedItems(),
                    text;

                var anyVal = 0, emptyVal = 0;

                if (this.params['any_val'] != 0) {
                    anyVal = this.params['any_val'];
                }

                if (this.params['default_val'] != 0) {
                    emptyVal =  this.params['default_val'];
                }

                var checked = items.length > 0;

                if (checked && this.getVal() == emptyVal) {
                    checked = false;
                }

                var val = this.getVal();

                if (typeof val == 'object') {
                    val = val.length > 0 ? val[0] : emptyVal;
                }

                if (val == anyVal) {
                    var checkedItems = this._menu.getItems().filter(function(item) {
                        return item.getVal() == anyVal;
                    });

                    if (checkedItems.length > 0) {
                        text = label + ': ' + checkedItems[0].getText().toLowerCase();
                    } else {
                        text = label + ': ' + this._menu.getItems()[0].getText().toLowerCase();
                    }
                } else if (prefix) {
                    text = label + ': ' + items[0].getText().toLowerCase();
                } else {
                    text = items[0].getText();
                }

                this._button.setText(text);
                this._button.setMod('checked', checked);
            }

        }));
    }
);