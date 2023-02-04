modules.define(
    'select',
    function(provide, select)
    {
        provide(select.decl({modName : 'mode', modVal : 'check'}, {

            _isResetItemClick: false,

            _anyItem: null,

            onSetMod : {
                'js': {
                    'inited': function () {
                        this.__base.apply(this, arguments);

                        this._anyItem = this.findBlockInside({
                            block: 'menu-item', modName: 'role', modVal: 'any'
                        });

                        if (this._anyItem) {
                            this._anyItem.on('click', this._clear, this);
                        }
                    }
                }
            },

            _clear: function() {
                var items = this._getCheckedItems();

                items.forEach(function (item) {
                    item.delMod('checked');
                });

                this._button.delMod('checked');
            },

            _onMenuChange: function () {

                if (this._anyItem) {
                    this._updateAnyItem();
                }

                this.__base.apply(this, arguments);
            },

            _updateAnyItem : function() {
                var checkedItems    = this._getCheckedItems();
                var numCheckedItems = checkedItems.length;

                if (numCheckedItems > 0) {

                    if (!(1 == numCheckedItems
                        && this._getCheckedItems()[0].hasMod('role', 'any'))
                    ) {
                        this._anyItem.delMod('checked');
                    }
                } else {
                    this._anyItem.setMod('checked', true);
                }
            },

            _updateControl : function() {
                this.elem('control').remove();
                this.dropElemCache('control');

                var baseName = this.getName();

                this.domElem.prepend(
                    this.getVal()
                        .map(function(val) {
                            var name = baseName + '[' + val + ']';
                            return select._createControlHTML(name, '1');
                        }));
            }

        }));
    }
);