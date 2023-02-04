define(["jquery"], function ($) {
    function TestCase (template) {
        this.name = "No name";
        this.axes =  [];

        var tests = [];
        for (var name in template) {
            if (template.hasOwnProperty(name) && !name.indexOf("test")) {
                tests.push(name);
            }
        }
        this.tests = tests;

        $.extend(this, template);

        this.___unitClass = function () {};
        this.___unitClass.prototype = this;
        this.___unitClass.units = [];
    }

    return TestCase;
});
