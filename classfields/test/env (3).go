package test

import (
	"github.com/spf13/viper"
	"os"
)

func SetEnv(key, value string) {
	err := os.Setenv(key, value)
	if err != nil {
		panic(err)
	}
}

func InitEnv() {

	SetEnv("LB_OAUTH_TOKEN", "_")
	SetEnv("LB_OAUTH", "false")
	SetEnv("LB_CLIENT_ID", "golf")
	SetEnv("LB_TOPICS", "vertis--vertis-backend-log")
	SetEnv("LB_IDLE_TIMEOUT_SEC", "3")
	SetEnv("LB_COMMIT_INTERVAL_MS", "0")
	SetEnv("LB_MAX_READ_MESSAGES_COUNT", "1000000")
	SetEnv("LB_MA_XREAD_SIZE", "1000000")
	SetEnv("LB_MAX_READ_PARTITIONS_COUNT", "0")
	SetEnv("LB_MAX_TIME_LAG_MS", "0")
	SetEnv("LB_READ_TIMESTAMP_MS", "1547627400000")
	SetEnv("LB_ADDRESS", "logbroker.yandex.net")

	SetEnv("TVM_TOKEN", "00000000000000000000000000000000")
	SetEnv("TVM_UPDATE_DURATION", "60")
	SetEnv("TVM_ADDRESS", "http://127.0.0.1:8085")

	SetEnv("LOG_LEVEL", "debug")
	SetEnv("_DEPLOY_DC", "sas")

	viper.SetDefault("ch_hosts", "localhost:9000")
	viper.SetDefault("ch_database", "logs")
	viper.SetDefault("ch_username", "")
	viper.SetDefault("ch_password", "")
	viper.SetDefault("ch_secure", false)
	viper.SetDefault("ch_open_circuit_timeout", "1s")
	viper.SetDefault("ch_buffer_time", "10s")
	viper.SetDefault("ch_buffer_size", 10)
	viper.SetDefault("ch_write_timeout", "5s")

	viper.AutomaticEnv()
}
