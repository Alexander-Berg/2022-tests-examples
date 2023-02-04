var DEFAULT_VALUE = 0;

modules.define(
    'select',
    function(provide, select)
    {
        provide(select.decl({ modName : 'label', modVal : true }, {

            _updateButton : function() {
                var label        = this.params.text;
                var defaultText  = label + ': ' + (this.params['default'] ? this.params['default'] : 'любой');
                var value        = this.getVal();
                var text;

                if (this.hasMod('tree', true)) {
                    var items           = this._menu.getItems();
                    var isParentChecked = false;

                    checkedItems = items.filter(function (item) {
                        var result    = false;
                        var isParent  = item.hasMod('type', 'parent');
                        var isChecked = item.hasMod('checked');

                        if (isParent) {
                            isParentChecked = (isChecked);
                        }

                        if (isChecked) {

                            if (isParent) {
                                result = true;
                            } else if (!isParentChecked) {
                                result = true;
                            }
                        }

                        return result;
                    });
                } else {
                    var checkedItems = this._getCheckedItems();
                }

                var numCheckedItems = checkedItems.length;

                if (0 == numCheckedItems) {
                    text = defaultText;
                } else if (1 == numCheckedItems) {

                    if (DEFAULT_VALUE == this.getVal()) {
                        text = defaultText;
                    } else {
                        text = checkedItems[0].getText().replace(/^(&nbsp;|[\s]+)/g, '');
                    }
                } else {
                    text = label + ' (' + numCheckedItems + ')';
                }

                var checked = (numCheckedItems > 0 && DEFAULT_VALUE != value);

                this._button.setText(text);
                this._button.setMod('checked', checked);
            }
        },
        {
            _createControlHTML : function(name, val) {
                var multiVal = '[' + DEFAULT_VALUE + ']';

                if (DEFAULT_VALUE != val
                    && (name.indexOf(multiVal) != name.length - multiVal.length)
                ) {
                    return this.__base.apply(this, arguments)
                }
            }
        }));
    }
);