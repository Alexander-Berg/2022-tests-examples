modules.define(
    'select',
    function(provide, select)
    {
        provide(select.decl({modName : 'tree'}, {

            onSetMod : {
                'js': {
                    'inited': function () {
                        this.__base.apply(this, arguments);

                        this._menu.on('item-click', this._updateCheckedItems, this);
                    }
                }
            },

            _updateChildren: function(parent) {
                var checked = !parent.hasMod('checked');
                var itemAlias = $(parent.domElem).data('body-alias') || parent.getVal();

                var subItems = this._menu.getItems().filter(function(item) {
                    var alias = $(item.domElem).data('body-alias');
                    return item.hasMod('type', 'child') && alias == itemAlias;
                });

                subItems.forEach(function(item) {
                    if (checked) {
                        item.setMod('checked', true);
                    } else {
                        item.delMod('checked');
                    }
                });
            },

            _updateParent: function(child) {
                var checked = !child.hasMod('checked');
                var itemAlias = $(child.domElem).data('body-alias') || child.getVal();

                var parents = this._menu.getItems().filter(function(item) {
                    var alias = $(item.domElem).data('body-alias');
                    return item.hasMod('type', 'parent') && alias == itemAlias;
                });

                if (!parents.length) {
                    return;
                }

                var parent = parents[0];

                var children = this._menu.getItems().filter(function(item) {
                    if (item.getVal() == child.getVal()) {
                        return false;
                    }
                    var alias = $(item.domElem).data('body-alias');
                    return item.hasMod('type', 'child') && alias == itemAlias;
                });

                var count = children.length;

                var checkedCount = children.filter(function(item) {
                    return item.hasMod('checked');
                }).length;

                if (checked && checkedCount == count) {
                    parent.setMod('checked', true);
                } else {
                    parent.delMod('checked');
                }
            },

            _updateCheckedItems : function(params, target) {
                if (target.item.hasMod('type', 'parent')) {
                    this._updateChildren(target.item);

                } else if (target.item.hasMod('type', 'child')) {
                    this._updateParent(target.item);
                }
            }
        }));
    }
);