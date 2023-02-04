# ${CURDIR} relative to ${ARCADIA_ROOT}
SET(ENV_LAYER_NAME layer.tar.zst)
SET(ENV_BASE_DIR infra/environments/ebpf-agent-tests)

# Enable nat64dns
SET_APPEND(ENV_VAR with_nat64=t)

# install recent kernel
DEFAULT(ENV_KERNEL_VERSION 4.19.119-30.2)
DEFAULT(ENV_KERNEL_PACKAGE linux-image-server=${ENV_KERNEL_VERSION}\ linux-image-extra-${ENV_KERNEL_VERSION}\ linux-tools=${ENV_KERNEL_VERSION})
SET_APPEND(ENV_VAR KERNEL_PACKAGE='\"${ENV_KERNEL_PACKAGE}\"')

# install recent porto
SET_APPEND(ENV_VAR PORTO_PACKAGE=yandex-porto=5.0.3)
