import os
from pathlib import Path

from pydantic import BaseSettings


class E2ESettings(BaseSettings):
    oauth: str
    oauth_b2b: str
    oauth_robot: str
    is_local: bool = False

    class Config:
        env_file = '.env'


# идем вверх от запущенного теста

env_location = None
candidate = Path(os.getcwd())
max_depth = 4

for i in range(4):
    if (candidate / '.env').exists():
        env_location = candidate / '.env'
        break
    candidate = candidate.parent

if env_location is None:
    e2e_settings = None
    print(".env file wasn't found; E2E tests will be disabled")
else:
    e2e_settings = E2ESettings(_env_file=str(env_location))
