TEST=$1
SHELL_PORT=1080
nc localhost $SHELL_PORT < $TEST
