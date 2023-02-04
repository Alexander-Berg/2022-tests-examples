var URL_MMM_DATA   = '/ajax/get_mmm_data/';
var URL_GET_GROUPS = '/ajax/get_groups/';

modules.define(
    'select',
    ['i-bem__dom', 'BEMHTML', 'jquery'],
    function(provide, bemDom, bemHtml, $, select)
    {
        provide(select.decl({ modName : 'role', modVal : 'folder' }, {

            onSetMod : {
                js : {
                    inited : function()
                    {
                        this.__base.apply(this, arguments);
                        this._defaultItems = this._menu.getItems();

                        this._setDefaultText();
                        this.on('change', this._onChange, this);
                    }
                }
            },

            _setDefaultText : function ()
            {
                if (this._isSelectedFirstItem()) {
                    this._button.setText(this.params.text);
                }
            },

            _getFirstItemVal : function ()
            {
                return this._menu
                    .getItems()[0]
                    .getVal();
            },

            _isSelectedFirstItem : function ()
            {
                return (this._getFirstItemVal() == this.getVal());
            },

            _onChange : function ()
            {
                var isSelectedFirstItem = (this._defaultItems[0].getVal() == this.getVal());

                this
                    ._button
                    .setMod('checked', !isSelectedFirstItem);

                if (isSelectedFirstItem) {
                    this._button.setText(this.params.text);
                }
            },

            replace : function (markId, callback)
            {
                var self = this;

                this._getMarkFolders(markId, function (folders) {
                    var options = [];

                    if (folders.length) {

                        folders.forEach(function (folder) {
                            var folderVal = [markId, folder.id].join('-');

                            options.push({
                                val:  folderVal,
                                text: folder.name
                            });

                            if (folder.hasOwnProperty('label')) {

                                folder.label.forEach(function (label) {
                                    var labelVal = folderVal + '_' + label.id;

                                    options.push({
                                        val:  labelVal,
                                        text: '&nbsp;&nbsp;&nbsp;' + label.name
                                    });
                                });
                            }
                        });

                        var html = bemHtml.apply(
                            self.getBemjson(markId, options)
                        );

                        bemDom.replace(self.domElem, html);
                    } else {
                        self.disable();
                    }

                    if (callback) {
                        callback();
                    }
                });
            },

            _getMarkFolders : function (markId, callback)
            {
                var url = this._getMarkFoldersUrl(markId);

                $.getJSON(url, function (result) {
                    var data = result.data.items || result.data;

                    callback(data);
                });
            },

            _getMarkFoldersUrl : function (markId)
            {
                var url = (filter_json.is_catalog)
                    ? URL_MMM_DATA
                    : URL_GET_GROUPS;

                return url
                    + '?'
                    + $.param({
                        category_id : 15,
                        section_id	: 1,
                        level       : 1,
                        type        : 'mark',
                        mark_id     : markId
                    })
            },

            disable : function ()
            {
                if (!this.getMod('disabled')) {
                    var firstItemVal = this._getFirstItemVal();

                    this
                        .setVal(firstItemVal)
                        .setMod('disabled');
                }
            },

            getBemjson : function (markId, options)
            {
                var mods = {
                    mode  : 'radio',
                    theme : 'islands',
                    size  : 's',
                    width : 'mmm',
                    role  : 'folder',
                    edit  : true
                };

                if (!options) {
                    options       = [];
                    mods.disabled = true;
                }

                if (!markId) {
                    markId = 0;
                }

                var firstItemVal = [markId, '0'].join('-');

                options.unshift({
                    val  : firstItemVal,
                    text : 'Любая'
                });

                return {
                    block : 'select',
                    name  : 'search[mark-folder][]',
                    val   : firstItemVal,
                    js    : {
                        text : 'Модель: любая',
                        placeholder : 'Поиск по модели'
                    },
                    options          : options,
                    mods             : mods,
                    optionsMaxHeight : 292
                };
            },

            _updateButton : function() {
                var text = this._getCheckedItems()[0].getText();

                text = text.replace(/^(&nbsp;|[\s]+)/g, '');

                this._button.setText(text);
            }
        }));
    }
);