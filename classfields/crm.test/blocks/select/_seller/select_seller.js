modules.define(
    'select',
    function(provide, select)
    {
        var OWNER_VAL_ID = 1;

        provide(select.decl({modName : 'seller'}, {

            _blockAvailability: null,

            onSetMod : {
                'js': {
                    'inited': function () {
                        this.__base.apply(this, arguments);
                        this._initBlockForm();
                    }
                }
            },

            _initBlockForm: function() {
                var blockForm = this.findBlockOutside('form');

                if (blockForm) {
                    this._blockAvailability = blockForm.findBlockInside({
                        block: 'select', modName: 'role', modVal: 'availability'
                    });
                }

                this.findBlockInside('menu').on('change', this._menuChange, this);
            },

            _menuChange : function() {
                if (this._blockAvailability) {
                    if (this.getVal() == OWNER_VAL_ID) {
                        this._blockAvailability.setMod('disabled', true);
                    } else {
                        this._blockAvailability.delMod('disabled');
                    }
                }
            }
        }));
    }
);