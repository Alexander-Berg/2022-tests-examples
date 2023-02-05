ymaps.modules.define(util.testfile(), [
    'util.augment'
], function (provide, augment) {

    describe('util.augment', function () {

        it('Создает ссылку на суперкласс', function () {
            var parent = function () {};
            var child = function () {};
            augment(child, parent);

            expect(child.superclass).to.be(parent.prototype);
        });

        it('На потомке отображаются изменения прототипа родителя', function () {
            var parent = function () {};
            var child = function () {};
            augment(child, parent);

            var instance = new child();
            parent.prototype.method = function () { return 42 };
            expect(instance.method).to.be(parent.prototype.method);
        });

        it('Дает возможность вызывать методы родителя', function () {
            var parent = function (value) {
                this.prop = value;
            };
            parent.prototype = {
                get: function () {
                    return this.prop;
                }
            };

            var child = function (val) {
                child.superclass.constructor.call(this, val);
            };

            augment(child, parent);

            var instance = new child(42);
            expect(instance.get()).to.be(42);
        });

        it('Дает возможность вызывать методы прaродителя', function () {
            var grandParent = function (value) {
                this.prop = value;
            };
            grandParent.prototype = {
                get: function () {
                    return this.prop;
                }
            };

            var parent = function (value) {
                parent.superclass.constructor.call(this, value);
            };
            augment(parent, grandParent, {
                get: function () {
                    return parent.superclass.get.call(this) + 1;
                }
            });

            var child = function (val) {
                child.superclass.constructor.call(this, val);
            };

            augment(child, parent);

            var instance = new child(42);
            expect(instance.get()).to.be(43);
        });
    });

    provide();

});
