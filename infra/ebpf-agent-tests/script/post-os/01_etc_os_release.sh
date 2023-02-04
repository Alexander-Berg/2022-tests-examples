printf 'YANDEX_BUILD_TIME="%s"\n' "${build_time}" >> /etc/os-release
printf 'YANDEX_VIRT_MODE="%s"\n' "${virt_mode}" >> /etc/os-release
printf 'YANDEX_ARCADIA_PATH="%s"\n' "${arcadia_path}" >> /etc/os-release
printf 'YANDEX_SCRIPT_DIGEST="%s"\n' "${script_digest}" >> /etc/os-release
