class Discount(object):
    @staticmethod
    def return_qty_with_static_dicount(qty, discount_pct):
        return D((qty / (100 - discount)) * 100).quantize(D('.000001'))
