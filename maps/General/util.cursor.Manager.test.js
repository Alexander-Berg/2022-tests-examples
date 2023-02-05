ymaps.modules.define(util.testfile(), [
    'util.cursor.Manager'
], function (provide, CursorManager) {
    describe('cursor.Manager', function () {
        var element;
        var manager;
        beforeEach(function () {
            element = document.createElement('div');
            document.body.appendChild(element);

            manager = new CursorManager(element);
        });

        afterEach(function () {
            element.parentElement.removeChild(element);
        });

        it('should push new cursor', function () {
            var cursor = manager.push('move');
            expect(element.style.cursor.indexOf('move')).not.to.be(-1);
        });

        it('should set key on old cursor', function () {
            var cursor = manager.push('move');
            cursor.setKey('pointer');
            expect(element.style.cursor.indexOf('pointer')).not.to.be(-1);
        });

        it('should get key from cursor', function () {
            var cursor = manager.push('pointer');
            expect(cursor.getKey()).to.be('pointer');
        });

        it('should remove cursor', function () {
            manager.push('crosshair');
            var cursor = manager.push('pointer');
            cursor.remove();
            expect(element.style.cursor.indexOf('crosshair')).not.to.be(-1);
        });
    });
    provide({});
});
