pid_pages="$(pidof pages-server)"
pid_lom="$(pidof lom-server)"
pid_rom="$(pidof rom-server)"

if [ -n "${pid_pages}" ]; then
  sudo kill ${pid_pages}
  echo "Pages is off"
fi

if [ -n "${pid_lom}" ]; then
  sudo kill ${pid_lom}
  echo "Lom server is off"
fi

if [ -n "${pid_rom}" ]; then
  sudo kill ${pid_rom}
  echo "Rom server is off"
fi

npm i express
cd public
screen -dmS pages node pages-server.js
screen -dmS lom node lom-server.js
screen -dmS rom node rom-server.js
exit
