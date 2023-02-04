modules.define(
    'form',
    ['i-bem__dom'],
    function(provide, bemDom)
    {
        provide(bemDom.decl(this.name, {
            onSetMod: {
                'js': {
                    'inited': function() {
                        this._initRadioGroup();
                        this._initAutocode();
                    }
                }
            },

            _initAutocode: function() {
                var form = this;

                this.findBlockInside({
                    block: 'dropdown', modName: 'role', modVal: 'geo'

                }).on('change', function() {
                    var listId = this.getSelectedRegionsId();
                    var visible = true;

                    if (listId.length) {
                        visible = listId.filter(function(id) {
                            return id == 1 || id == 87 || id == 38;
                        }).length > 0;
                    }

                    var autocodeBlock = form.findBlockInside('autocode');
                    if (autocodeBlock) {
                        if (visible) {
                            autocodeBlock.delMod('hidden');
                        } else {
                            autocodeBlock.setMod('hidden', true);
                            autocodeBlock.findBlockInside('checkbox').delMod('checked');
                        }
                    }
                });
            },

            _initRadioGroup: function() {
                var blocksSelect = this.findBlocksInside({
                    block: 'select', modName: 'disable', modVal: 'new'
                });

                var blocksInput = this.findBlocksInside({
                    block: 'input', modName: 'disable', modVal: 'new'
                });

                var blocksCheckbox = this.findBlocksInside({
                    block: 'checkbox', modName: 'disable', modVal: 'new'
                });

                var blocks = blocksSelect.concat(blocksInput, blocksCheckbox);

                var radioGroup = this.findBlockInside({
                    block: 'radio-group'
                });

                radioGroup.on('change', function() {

                    var focusedRadio = this.findBlockInside({
                        block : 'radio',
                        modName : 'focused',
                        modVal : true
                    });

                    var filter = $('#filter');
                    var action = focusedRadio.domElem.data('action');
                    var sectionId = focusedRadio.domElem.data('section-id');
                    var disabled;

                    if (sectionId == 2) {
                        disabled = true;
                    } else {
                        disabled = false;
                    }

                    blocks.forEach(function(select) {
                        if (disabled) {
                            select.setMod('disabled', true);
                        } else {
                            select.delMod('disabled');
                        }
                    });

                    var control = focusedRadio.elem('control');
                    var params  = {
                        name : control.attr('name'),
                        val  : this.getVal(),
                        nsv  : true
                    };

                    filter.attr('action', action);
                    filter.trigger('update', params);
                });

            }
        }));
    }
);
