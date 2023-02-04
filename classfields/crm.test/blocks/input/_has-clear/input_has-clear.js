modules.define(
    'input',
    function(provide, input)
    {
        provide(input.decl({ modName : 'has-clear', modVal : true }, {

            _updateClear: function(e, data) {
                this.__base.apply(this, arguments);

                if (data && data.source == 'clear') {
                    if (!this.hasMod('search-ignore')) {
                        this._onSubmit.apply(this, arguments);

                        $('#filter').trigger('change');
                    }
                }
            }

        }));
    }
);