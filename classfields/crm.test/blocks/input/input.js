modules.define(
    'input',
    function(provide, input)
    {
        provide(input.decl(this.name, {

            onSetMod: {
                'js': {
                    'inited': function() {
                        this.__base.apply(this, arguments);

                        this.previousVal = this.getVal() || null;

                        var self = this;

                        this.elem('control').on('change', function() {
                            self._onBlur();
                        });
                    }
                }
            },

            _onBlur: function() {
                this.__base.apply(this, arguments);

                var val = this.getVal();
                if (val != this.previousVal) {
                    this._onSubmit();
                }

                this.previousVal = val;
            },

            _onSubmit: function() {
                if (!this.hasMod('ignore')) {
                    var item   = this.elem('control');
                    var params = {
                        name : item.attr('name')
                    };

                    $('#filter').trigger('update', params);
                }
            }

        }));
    }
);
