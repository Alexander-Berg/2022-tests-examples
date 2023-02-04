import tvm2
import tvmauth

from billing.hot.tests.config import config
from billing.hot.tests.lib.yav import yav


def get_tvm_client(cfg: config.TvmConfig) -> tvm2.TVM2:
    yav_client = yav.SecretProvider()
    secret = yav_client.get_secret(cfg.secret.id, cfg.secret.key)

    return tvm2.TVM2(
        client_id=cfg.src_id,
        secret=secret,
        blackbox_client=tvmauth.BlackboxTvmId.ProdYateam,
    )
