kkt_sn=$1
fn_sn=$2
version=$3
use_virtual_fn=$4
password=666666

# Донастройка ККТ в соответствии с переданными параметрами.
cd /kkt
sed "s/\${superadmin_password}/$password/" templates/superadmin.json > /FR/superadmin.json
sed "s/\${kkt_sn}/$kkt_sn/" templates/serial_number.json > /FR/serial_number.json

if [[ "$use_virtual_fn" == "true" ]]; then
  sed "s/\${fn_sn}/$fn_sn/" templates/hardware-virtual.json > /FR/hardware.json
else
  success=0

  for port in {1884..1896}; do
      addr="zomb-fn-mgm-100.zombie.yandex.net:$port"
      echo "Trying to bind to $addr..."
      socat tcp:$addr pty,link=/dev/ttyUSB0,raw,echo=0 &

      sleep 3s

      pidof socat
      if [ $? == 0 ]; then
          echo "Binded successfully"
          success=1
          break
      else
          echo "Failed to bind in 3 seconds"
      fi

  done

  if  [[ $success == 0 ]]; then
      echo "Could not bind to any potential address! Stopping the container"
      exit 1
  fi

  cp templates/hardware-mgm.json /FR/hardware.json
fi

if [[ "$version" == "3_5_30" ]] || [[ "$version" == "3_5_84" ]] || [[ "$version" == "4_0_110" ]]; then
    mv /FR/FR_$version /FR/FR
else
    echo "Unknown version $version"
    exit 1
fi

service ssh start

/FR/start.sh
