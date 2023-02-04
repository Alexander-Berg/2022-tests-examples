module.exports = {
    blocks: {
        'b-page': function() {
            return { block: 'b-page' };
        },
        'b-promo-page': function() {
            return { block: 'b-promo-page' };
        },
        'i-debug': function() {
            return { block: 'i-debug' };
        },
        'i-broken': function(x) {
            return x.hello();
        },
        'i-broke-bemhtml': function() {
            return { block: 'i-broke-bemhtml', broke: true };
        }
    },
    setData: function(data) {
    },
    BEMHTML: function() {
        if (this.broke) {
            throw new Error('broken block "' + this.block + '"!');
        }

        return this.block + ',';
    }
};
