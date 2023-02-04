# check apt configuration errors
apt-get install --fix-broken ${SIMULATE_APT_CHECK:+"--simulate"}
