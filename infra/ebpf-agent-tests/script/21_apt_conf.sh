umask 0022

tee etc/apt/apt.conf.d/80-retries-env-tmp <<EOF
APT::Acquire::Retries "3";
EOF
