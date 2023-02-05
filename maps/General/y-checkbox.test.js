modules.define(
    'test',
    [
        'y-checkbox',
        'y-dom',
        'jquery',
        'y-test-utils'
    ],
    function (
        provide,
        YCheckbox,
        dom,
        $,
        testUtils
    ) {

    var should = chai.should();

    describe('YCheckbox', function () {
        var prevCheckbox;
        var checkbox;
        var box;
        var control;

        beforeEach(function () {
            prevCheckbox = $('<input type="checkbox" />').appendTo(document.body);
            checkbox = new YCheckbox();
            box = testUtils.findElement(checkbox, 'box');
            control = testUtils.findElement(checkbox, 'control');
        });

        afterEach(function () {
            prevCheckbox.remove();
            checkbox.destruct();
            checkbox = null;
            prevCheckbox = null;
            box = null;
            control = null;
        });

        it('should emit `change` event after change control', function () {
            var spy = sinon.spy();
            checkbox.on('change', spy);
            control.change();
            spy.callCount.should.equal(1);
        });

        it('should be determinate after change indeterminate control', function () {
            checkbox.setIndeterminate();
            control.change();
            checkbox.isIndeterminate().should.be.false;
        });

        it('should emit `determinate` event after change indeterminate control', function () {
            checkbox.setIndeterminate();
            var spy = sinon.spy();
            checkbox.on('determinate', spy);
            control.change();
            spy.callCount.should.equal(1);
        });

        describe('check()', function () {
            it('should set `checked` attribute', function () {
                control.is('[checked]').should.be.false;
                checkbox.check();
                control.is('[checked]').should.be.true;
            });

            it('should set `checked` state', function () {
                checkbox.getDomNode().hasClass('_checked').should.be.false;
                checkbox.check();
                checkbox.getDomNode().hasClass('_checked').should.be.true;
            });

            it('should emit `change` event', function () {
                var spy = sinon.spy();
                checkbox.on('change', spy);
                checkbox.check();
                spy.callCount.should.equal(1);
            });

            it('should not emit `change` on second call', function () {
                var spy = sinon.spy();
                checkbox.on('change', spy);
                checkbox.check();
                checkbox.check();
                spy.callCount.should.equal(1);
            });

            it('should make control determinate', function () {
                checkbox.setIndeterminate();
                checkbox.check();
                checkbox.isIndeterminate().should.be.false;
            });

            describe('when disabled', function () {
                beforeEach(function () {
                    checkbox.disable();
                });

                it('should not set `checked` attribute', function () {
                    checkbox.check();
                    control.is('[checked]').should.be.false;
                });

                it('should not set `checked` state', function () {
                    checkbox.check();
                    checkbox.getDomNode().hasClass('_checked').should.be.false;
                });

                it('should not emit `change` event', function () {
                    var spy = sinon.spy();
                    checkbox.on('change', spy);
                    checkbox.check();
                    spy.callCount.should.equal(0);
                });
            });

            describe('when checkbox indeterminate', function () {
                beforeEach(function () {
                    checkbox.setIndeterminate();
                });

                it('should determinate checkbox state', function () {
                    checkbox.check();
                    checkbox.isIndeterminate().should.be.false;
                });

                it('should emit `determinate` event', function () {
                    var spy = sinon.spy();
                    checkbox.on('determinate', spy);
                    checkbox.check();
                    spy.callCount.should.equal(1);
                });
            });
        });

        describe('uncheck()', function () {
            beforeEach(function () {
                checkbox.check();
            });

            it('should unset `checked` attribute', function () {
                checkbox.uncheck();
                control.is('[checked]').should.be.false;
            });

            it('should remove `checked` state', function () {
                checkbox.uncheck();
                checkbox.getDomNode().hasClass('_checked').should.be.false;
            });

            it('should emit `change` event', function () {
                var spy = sinon.spy();
                checkbox.on('change', spy);
                checkbox.uncheck();
                spy.callCount.should.equal(1);
            });

            it('should not emit `change` event on second call', function () {
                var spy = sinon.spy();
                checkbox.on('change', spy);
                checkbox.uncheck();
                checkbox.uncheck();
                spy.callCount.should.equal(1);
            });

            describe('when disabled', function () {
                beforeEach(function () {
                    checkbox.disable();
                });

                it('should not change `checked` attribute', function () {
                    checkbox.uncheck();
                    control.is('[checked]').should.be.true;
                });

                it('should not remove `checked` state', function () {
                    checkbox.uncheck();
                    checkbox.getDomNode().hasClass('_checked').should.be.true;
                });

                it('should not emit `change` event', function () {
                    var spy = sinon.spy();
                    checkbox.on('change', spy);
                    checkbox.uncheck();
                    spy.callCount.should.equal(0);
                });
            });

            describe('when checkbox indeterminate', function () {
                beforeEach(function () {
                    checkbox.setIndeterminate();
                });

                it('should determinate checkbox state', function () {
                    checkbox.uncheck();
                    checkbox.isIndeterminate().should.be.false;
                });

                it('should emit `determinate` event', function () {
                    var spy = sinon.spy();
                    checkbox.on('determinate', spy);
                    checkbox.check();
                    spy.callCount.should.equal(1);
                });
            });
        });

        describe('isChecked()', function () {
            it('should return true if checkbox checked', function () {
                checkbox.isChecked().should.be.false;
                checkbox.check();
                checkbox.isChecked().should.be.true;
                checkbox.uncheck();
                checkbox.isChecked().should.be.false;
            });
        });

        describe('setIndeterminate()', function () {
            it('should set `indeterminate` property', function () {
                control.prop('indeterminate').should.be.false;
                checkbox.setIndeterminate();
                control.prop('indeterminate').should.be.true;
            });

            it('should set `indeterminate` state', function () {
                var node = checkbox.getDomNode();
                node.hasClass('_indeterminate').should.be.false;
                checkbox.setIndeterminate();
                node.hasClass('_indeterminate').should.be.true;
            });

            it('should emit `indeterminate` event', function () {
                var spy = sinon.spy();
                checkbox.on('indeterminate', spy);
                checkbox.setIndeterminate();
                spy.callCount.should.equal(1);
            });

            describe('when checkbox disabled', function () {
                beforeEach(function () {
                    checkbox.disable();
                });

                it('should not set `indeterminate` property', function () {
                    checkbox.setIndeterminate();
                    control.prop('indeterminate').should.be.false;
                });

                it('should not set `indeterminate` state', function () {
                    checkbox.setIndeterminate();
                    checkbox.getDomNode().hasClass('_indeterminate').should.be.false;
                });

                it('should not emit `indeterminate` event', function () {
                    var spy = sinon.spy();
                    checkbox.on('indeterminate', spy);
                    checkbox.setIndeterminate();
                    spy.callCount.should.equal(0);
                });
            });
        });

        describe('setDeterminate()', function () {
            describe('when checkbox in indeterminate state', function () {
                beforeEach(function () {
                    checkbox.setIndeterminate();
                });

                it('should remove `indeterminate` property', function () {
                    checkbox.setDeterminate();
                    control.prop('indeterminate').should.be.false;
                });

                it('should remove `indeterminate` state', function () {
                    checkbox.setDeterminate();
                    checkbox.getDomNode().hasClass('_indeterminate').should.be.false;
                });

                it('should emit `determinate` event', function () {
                    var spy = sinon.spy();
                    checkbox.on('determinate', spy);
                    checkbox.setDeterminate();
                    spy.callCount.should.equal(1);
                });

                describe('when checkbox disabled', function () {
                    beforeEach(function () {
                        checkbox.disable();
                    });

                    it('should not remove `indeterminate` property', function () {
                        checkbox.setDeterminate();
                        control.prop('indeterminate').should.be.true;
                    });

                    it('should not remove `indeterminate` state', function () {
                        checkbox.setDeterminate();
                        checkbox.getDomNode().hasClass('_indeterminate').should.be.true;
                    });

                    it('should not emit `determinate` event', function () {
                        var spy = sinon.spy();
                        checkbox.on('determinate', spy);
                        checkbox.setDeterminate();
                        spy.callCount.should.equal(0);
                    });
                });
            });
        });

        describe('isIndeterminate()', function () {
            it('should return true for indeterminate checkbox', function () {
                checkbox.isIndeterminate().should.be.false;
                checkbox.setIndeterminate();
                checkbox.isIndeterminate().should.be.true;
                checkbox.setDeterminate();
                checkbox.isIndeterminate().should.be.false;
            });
        });

        describe('disable()', function () {
            it('should set `disable` attribute', function () {
                should.not.exist(control.attr('disabled'));
                checkbox.disable();
                control.attr('disabled').should.equal('disabled');
            });

            it('should set `disabled` state', function () {
                var node = checkbox.getDomNode();
                node.hasClass('_disabled').should.be.false;
                checkbox.disable();
                node.hasClass('_disabled').should.be.true;
            });

            it('should emit `disable` event', function () {
                var spy = sinon.spy();
                checkbox.on('disable', spy);
                checkbox.disable();
                spy.callCount.should.equal(1);
            });
        });

        describe('enable()', function () {
            describe('when checkbox disabled', function () {
                beforeEach(function () {
                    checkbox.disable();
                });

                it('should remove `disable` attribute', function () {
                    checkbox.enable();
                    should.not.exist(control.attr('disabled'));
                });

                it('should remove `disable` state', function () {
                    checkbox.enable();
                    checkbox.getDomNode().hasClass('_disabled').should.be.false;
                });

                it('should emit `enable` event', function () {
                    var spy = sinon.spy();
                    checkbox.on('enable', spy);
                    checkbox.enable();
                    spy.callCount.should.equal(1);
                });
            });
        });

        describe('isEnabled()', function () {
            it('should return true for enabled control', function () {
                checkbox.isEnabled().should.be.true;

                checkbox.disable();
                checkbox.isEnabled().should.be.false;

                checkbox.enable();
                checkbox.isEnabled().should.be.true;
            });
        });

        describe('focus()', function () {
            beforeEach(function () {
                checkbox.getDomNode().appendTo(document.body);
            });

            it('should set focus', function () {
                dom.focus.isFocused(control).should.be.false;
                checkbox.focus();
                dom.focus.isFocused(control).should.be.true;
            });

            it('should set `focused` state', function () {
                var node = checkbox.getDomNode();
                node.hasClass('_focused').should.be.false;
                checkbox.focus();
                node.hasClass('_focused').should.be.true;
            });

            it('should emit `focus` event', function () {
                var spy = sinon.spy();
                checkbox.on('focus', spy);
                checkbox.focus();
                spy.callCount.should.equal(1);
            });

            it('should emit `focus` event once', function () {
                var spy = sinon.spy();
                checkbox.on('focus', spy);
                checkbox.focus();
                checkbox.focus();
                spy.callCount.should.equal(1);
            });
        });

        describe('blur()', function () {
            describe('when checkbox has focus', function () {
                beforeEach(function () {
                    checkbox.getDomNode().appendTo(document.body);
                    checkbox.focus();
                });

                it('should remove focus', function () {
                    checkbox.blur();
                    dom.focus.isFocused(control).should.be.false;
                });

                it('should remove `focused` state', function () {
                    checkbox.blur();
                    checkbox.getDomNode().hasClass('_focused').should.be.false;
                });

                it('should emit `blur` event', function () {
                    var spy = sinon.spy();
                    checkbox.on('blur', spy);
                    checkbox.blur();
                    spy.callCount.should.equal(1);
                });

                it('should emit `blur` event once', function () {
                    var spy = sinon.spy();
                    checkbox.on('blur', spy);
                    checkbox.blur();
                    checkbox.blur();
                    spy.callCount.should.equal(1);
                });
            });
        });

        describe('isFocused()', function () {
            it('should return true if checkbox has focus', function () {
                checkbox.getDomNode().appendTo(document.body);
                checkbox.isFocused().should.be.false;
                control.focus();
                checkbox.isFocused().should.be.true;
            });

            it('should return false if checkbox lose focus', function () {
                checkbox.getDomNode().appendTo(document.body);
                control.focus();
                control.blur();
                checkbox.isFocused().should.be.false;
            });
        });

        describe('getName()', function () {
            it('should return undefined by default', function () {
                should.equal(checkbox.getName(), undefined);
            });

            it('should return value of `name` attribute', function () {
                var checkbox = new YCheckbox({name: 'test'});
                checkbox.getName().should.equal('test');
            });
        });
    });

    provide();
});
