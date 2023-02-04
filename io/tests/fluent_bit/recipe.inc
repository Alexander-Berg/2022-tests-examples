DEPENDS(yandex_io/tests/fluent_bit)
USE_RECIPE(yandex_io/tests/fluent_bit/recipe)

DEPENDS(contrib/tools/fluent-bit-minimal/bin)
DATA(arcadia/yandex_io/tests/fluent_bit/data/test.cfg)
DATA(arcadia/yandex_io/tests/fluent_bit/data/test.log)
