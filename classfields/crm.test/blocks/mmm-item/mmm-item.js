var CHANNEL_NAMESPACE = 'mmm-item';

modules.define(
    'mmm-item',
    ['i-bem__dom', 'BEMHTML', 'events__channels', 'jquery'],
    function(provide, bemDom, bemHtml, channels, $)
    {
        provide(bemDom.decl(this.name,
        {
            onSetMod : {
                js : {
                    inited : function()
                    {
                        this.__base.apply(this, arguments);

                        channels(CHANNEL_NAMESPACE).emit('inited');

                        this._initListeners();
                        this
                            ._getMarkBlock()
                            .setCountyGroup();
                    },

                    '' : function ()
                    {
                        this.__base.apply(this, arguments);

                        channels(CHANNEL_NAMESPACE).emit('delete');
                    }
                }
            },

            getBemjson : function ()
            {
                return {
                    block : 'mmm-item',
                    js    : true,
                    mix   : [
                        { block : 'g-row' },
                        { block : 'filter-row' }
                    ],
                    content : [
                        {
                            block: 'g-col-1',
                            mix: {
                                block: 'g-span-12'
                            },
                            content: [
                                this._getMarkBlock().getBemjson(),
                                this._getFolderBlock().getBemjson(),
                                this._getGenerationBlock().getBemjson(),
                                {
                                    block: 'button',
                                    mods: {
                                        theme: 'islands',
                                        size: 's',
                                        role: 'delete'
                                    },
                                    icon: {
                                        block: 'icon',
                                        mods: {
                                            role: 'delete'
                                        }
                                    }
                                },
                                {
                                    block: 'button',
                                    mods: {
                                        theme: 'islands',
                                        size: 's',
                                        role: 'add',
                                        hidden: true
                                    },
                                    icon: {
                                        block: 'icon',
                                        mods: {
                                            role: 'add'
                                        }
                                    }
                                }
                            ]
                        }
                    ]
                };
            },

            _initListeners : function ()
            {
                // Mark control
                this
                    ._getMarkBlock()
                    .on('change', this._onMarkChange, this);

                // Folder control
                this._initFolderListeners();

                // Add button
                this
                    .findBlockInside({
                        block   : 'button',
                        modName : 'role',
                        modVal  : 'add'
                    })
                    .on(
                        'click',
                        this._onAddClick,
                        this
                    );

                // Delete button
                this
                    .findBlockInside({
                        block   : 'button',
                        modName : 'role',
                        modVal  : 'delete'
                    })
                    .on(
                        'click',
                        this._onDeleteClick,
                        this
                    );
            },

            _getFolderBlock : function ()
            {
                return this.findBlockInside({
                    block   : 'select',
                    modName : 'role',
                    modVal  : 'folder'
                });
            },

            _getMarkBlock : function ()
            {
                return this.findBlockInside({
                    block   : 'select',
                    modName : 'role',
                    modVal  : 'mark'
                });
            },

            _getGenerationBlock : function ()
            {
                return this.findBlockInside({
                    block   : 'select',
                    modName : 'role',
                    modVal  : 'generation'
                });
            },

            _onAddClick : function ()
            {
                channels(CHANNEL_NAMESPACE).emit('add');
            },

            _onDeleteClick : function ()
            {
                var markId = this._getMarkBlock().getVal();

                bemDom.replace(this.domElem, '');

                if (markId) {
                    $('#filter')
                        .trigger('update')
                        .trigger('change');
                }
            },

            _onMarkChange : function(e)
            {
                var markId     = e.target.getVal();
                var folder     = this._getFolderBlock();
                var generation = this._getGenerationBlock();
                var self       = this;

                if (markId > 0) {
                    folder.replace(markId, function () {
                        generation.disable();
                        self._initFolderListeners();
                    });
                } else {
                    folder.disable();
                    generation.disable();
                }

                this
                    ._getMarkBlock()
                    .setCountyGroup();
            },

            _onFolderChange : function(e)
            {
                var folderId = this._getFolderId(
                    e.target.getVal()
                );
                var markId     = this._getMarkBlock().getVal();
                var generation = this._getGenerationBlock();

                (0 == folderId)
                    ? generation.disable()
                    : generation.replace(markId, folderId);
            },

            _getFolderId : function (val)
            {
                return (val)
                    ? val.split('-')[1]
                    : null;
            },

            _initFolderListeners : function ()
            {
                this
                    ._getFolderBlock()
                    .on('change', this._onFolderChange, this);
            }
        }));
    }
);