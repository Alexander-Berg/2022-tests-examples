expect.Assertion.prototype.coordinates = function (expect) {
    // To deal with 0.999999999 and company.
    var ERROR = 1e-7;

    var ok = this.obj.length === expect.length &&
        Math.abs(this.obj[0] - expect[0]) < ERROR &&
        Math.abs(this.obj[1] - expect[1]) < ERROR;

    var actualStr = 'expected ' + JSON.stringify(this.obj);
    var expectedStr = 'be the same coordinates as ' + JSON.stringify(expect);

    this.assert(ok,
      function () { return actualStr + ' to ' + expectedStr; },
      function () { return actualStr + ' to not ' + expectedStr; });

    return this;
};

expect.Assertion.prototype.between = function (from, to) {
    var ok = from <= this.obj && this.obj <= to;

    var actualStr = 'expected ' + this.obj;
    var expectedStr = 'be between ' + from + ' and ' + to;

    this.assert(ok,
        function () { return actualStr + ' to ' + expectedStr; },
        function () { return actualStr + ' to not ' + expectedStr; });

    return this;
};
