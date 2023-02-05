ymaps.modules.define(util.testfile(), [
    'util.ContentSizeObserver',
    'util.dom.style',
    'util.ImageLoadObserver',
    'vow'
], function (provide, ContentSizeObserver, domStyle, ImageLoadObserver, vow) {
    var container;
    createStyles();
    createBlocks();

    describe('util.ContentSizeObserver', function () {
        this.timeout(10000);

        afterEach(function () {
            clearContainer(container);
        });

        it('Должен отследить все изменения размера контейнера', function () {
            var content = ' \
                <img src="' + util.apiUrlRoot + '/src/util/contentSizeObserver/w100.png?delay=100" /> \
                <img src="' + util.apiUrlRoot + '/src/util/contentSizeObserver/w150.png?delay=200" /> \
                <img src="' + util.apiUrlRoot + '/src/util/contentSizeObserver/w200.png?delay=400" /> \
                <img src="' + util.apiUrlRoot + '/src/util/contentSizeObserver/w300.png?delay=600" /> \
                <img src="' + util.apiUrlRoot + '/src/util/contentSizeObserver/w400.png?delay=800" /> \
                <img src="' + util.apiUrlRoot + '/src/util/contentSizeObserver/w500.png?delay=1000" />';

            var cso = new ContentSizeObserver(container, {});

            var sizeChangeCount = 0;
            cso.events.add('sizechange', function () { sizeChangeCount++; });

            container.innerHTML = content;
            cso.observe();

            var ilo = new ImageLoadObserver(container);
            return util.waitEventOnce(ilo.events, 'complete')
                .then(function() {
                    expect(sizeChangeCount).to.be(6);
                })
        });
    });

    function createBlocks() {
        container = document.createElement('div');
        container.setAttribute('id', 'container');
        document.body.appendChild(container);
    }

    function clearContainer(container) {
        container.innerHTML = '';
    }

    function createStyles() {
        var styles = document.createElement('style');
        styles.innerHTML = '\
            #container { \
                position: absolute; \
                top: 1px; \
                left: 1px; \
            }';
        document.head.appendChild(styles);
    }

    provide();
});
