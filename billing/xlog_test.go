package xlog

import (
	"context"
	"os"
	"testing"

	uberzap "go.uber.org/zap"
	"go.uber.org/zap/zapcore"

	"a.yandex-team.ru/library/go/core/log"
	"a.yandex-team.ru/library/go/core/log/zap"
)

const outputFileName = "./output.log"

func init() {
	SetGlobalLogger(logger())
}

func BenchmarkLogger(b *testing.B) {
	ctx := context.Background()
	defer os.Remove(outputFileName)

	b.RunParallel(func(pb *testing.PB) {
		for pb.Next() {
			Info(ctx, "Some log", log.String("key", "value"))
		}
	})
}

func logger() log.Logger {
	l, _ := zap.New(uberzap.Config{
		Level:            uberzap.NewAtomicLevelAt(uberzap.ErrorLevel),
		Encoding:         "json",
		OutputPaths:      []string{outputFileName},
		ErrorOutputPaths: []string{outputFileName},
		EncoderConfig: zapcore.EncoderConfig{
			MessageKey:     "msg",
			LevelKey:       "levelStr",
			StacktraceKey:  "stackTrace",
			TimeKey:        "@timestamp",
			CallerKey:      "",
			NameKey:        "loggerName",
			EncodeLevel:    zapcore.CapitalLevelEncoder,
			EncodeTime:     zapcore.ISO8601TimeEncoder,
			EncodeDuration: zapcore.StringDurationEncoder,
			EncodeCaller:   zapcore.ShortCallerEncoder,
		},
	})
	return l
}
