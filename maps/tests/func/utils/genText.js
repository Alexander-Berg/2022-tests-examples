module.exports = function genText(n) {
    var s = '';
    var abd = 'abcdefghijklmnopqrstuvwxyz0123456789';
    var al = abd.length;

    while (s.length < n) {
        s += abd[Math.random() * al | 0];
    }
    return s;
};
