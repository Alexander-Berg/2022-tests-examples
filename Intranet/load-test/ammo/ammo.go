package main

import (
	"encoding/json"
	"os"

	"a.yandex-team.ru/intranet/ims/load-test/ammo/generator"
	zagLogger "a.yandex-team.ru/library/go/core/log/zap"
	"a.yandex-team.ru/library/go/core/log/zap/encoders"
	"go.uber.org/zap"
	"go.uber.org/zap/zapcore"
)

func main() {
	logger := initLogger()

	ammoGenerator, err := generator.NewAmmoGenerator()
	if err != nil {
		logger.Fatalf("Error on creating ammo generator: %+v", err)
	}
	ammo, err := ammoGenerator.GetAmmo()
	if err != nil {
		logger.Fatalf("Error on ammo generation: %+v", err)
	}
	for _, itm := range ammo {
		writeAmmo(itm, logger)
	}
}

func initLogger() *zagLogger.Logger {
	return zagLogger.Must(zap.Config{
		Level:            zap.NewAtomicLevelAt(zapcore.InfoLevel),
		Encoding:         encoders.EncoderNameTSKV,
		OutputPaths:      []string{"stdout"},
		ErrorOutputPaths: []string{"stderr"},
		EncoderConfig: zapcore.EncoderConfig{
			MessageKey:     "message",
			LevelKey:       "type",
			EncodeLevel:    zapcore.CapitalLevelEncoder,
			TimeKey:        "date",
			EncodeTime:     zapcore.EpochTimeEncoder,
			EncodeDuration: zapcore.StringDurationEncoder,
			NameKey:        "context",
		},
	})
}

func writeAmmo(ammo interface{}, logger *zagLogger.Logger) {
	str, err := json.Marshal(ammo)
	if err != nil {
		logger.Fatalf("Error on marshalling: %+v", err)
	}
	_, err = os.Stdout.Write(str)
	if err != nil {
		logger.Fatalf("Error on writing: %+v", err)
	}
	_, err = os.Stdout.WriteString("\n")
	if err != nil {
		logger.Fatalf("Error on writing EOL: %+v", err)
	}
}
