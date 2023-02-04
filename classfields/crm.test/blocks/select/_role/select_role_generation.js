var URL_MMM_DATA   = '/ajax/get_mmm_data/';

modules.define(
    'select',
    ['i-bem__dom', 'BEMHTML', 'jquery'],
    function(provide, bemDom, bemHtml, $, select)
    {
        provide(select.decl({ modName : 'role', modVal : 'generation' }, {

            replace: function (markId, folderId, callback)
            {
                var self = this;

                this._getGenerations(markId, folderId, function (data) {
                    var options = [];

                    if (data && data.items && data.items.length) {

                        data.items.forEach(function (generation) {
                            var val  = [markId, folderId, generation.id].join('-');
                            var text = generation.name;

                            if (generation.start_year_production && generation.end_year_production) {
                                text += '<br>'
                                    + generation.start_year_production
                                    + '-'
                                    + generation.end_year_production;
                            }

                            options.push({
                                val         : val,
                                text        : text,
                                checkedText : generation.name
                            });
                        });

                        var html = bemHtml.apply(
                            self.getBemjson(options)
                        );

                        bemDom.replace(self.domElem, html);
                    } else {
                        self.disable();
                    }

                    if (callback) {
                        callback();
                    }
                })
            },

            _getGenerations: function (markId, folderId, callback)
            {
                var url = this._getGenerationsUrl(markId, folderId);

                $.getJSON(url, function (result) {
                    callback(result.data);
                });
            },

            _getGenerationsUrl : function (markId, folderId)
            {
                return URL_MMM_DATA
                    + '?'
                    + $.param({
                        category_id : 15,
                        level       : 2,
                        type        : 'folder',
                        mark_id     : markId,
                        id          : folderId
                    });
            },

            disable: function ()
            {
                if (!this.getMod('disabled')) {
                    var html = bemHtml.apply(
                        this.getBemjson()
                    );

                    bemDom.replace(
                        this.domElem,
                        html
                    );
                }
            },

            getBemjson : function (options)
            {
                var mods = {
                    mode  : 'check',
                    theme : 'islands',
                    size  : 's',
                    width : 'mmm',
                    role  : 'generation'
                };

                if (!options) {
                    options       = [];
                    mods.disabled = true;
                }

                return {
                    block            : 'select',
                    name             : 'search[mark-folder-generation]',
                    text             : 'Поколение: любое',
                    options          : options,
                    mods             : mods,
                    optionsMaxHeight : 292
                };
            },

            _updateButton : function() {
                var selectedItems = this._getCheckedItems();
                var text;

                var numSelectedItems = selectedItems.length;

                if (1 == numSelectedItems) {
                    text = selectedItems[0].params.checkedText;
                } else if (numSelectedItems > 1) {
                    text = 'Поколений (' + numSelectedItems + ')';
                } else if (0 == numSelectedItems) {
                    text = this.params.text;
                }

                var checked = (numSelectedItems > 0);

                this._button.setText(text);
                this._button.setMod('checked', checked);
            }
        }));
    }
);