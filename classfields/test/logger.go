package test

import (
	"testing"

	"github.com/YandexClassifieds/shiva/common/logger"
	"github.com/sirupsen/logrus"
)

type Logger struct {
	log logrus.FieldLogger
}

type logCapture struct {
	*testing.T
}

func (c logCapture) Write(p []byte) (n int, err error) {
	c.Logf((string)(p))
	return len(p), nil
}

func NewLogger(t *testing.T) logger.Logger {
	l := logrus.New()
	lc := logCapture{T: t}
	l.SetOutput(lc)
	return newLogger(l.WithField("test", "mock"))
}

func NewLoggerWithoutTest() logger.Logger {

	return newLogger(logrus.New().WithField("test", "mock"))
}

func newLogger(log logrus.FieldLogger) logger.Logger {

	return &Logger{
		log: log,
	}
}

func (l Logger) NewContext(module, service string, mType logger.Type) logger.Logger {
	return newLogger(l.log.WithFields(logrus.Fields{
		"module":  module,
		"service": service,
		"type":    mType.String(),
	}))
}

func (l Logger) WithSanitizer(sanitizeError ...logger.ISanitizer) logger.Logger {
	return l
}

func (l Logger) WithField(key string, value interface{}) logger.Logger {
	return newLogger(l.log.WithField(key, value))
}

func (l Logger) WithFields(fields map[string]interface{}) logger.Logger {
	return newLogger(l.log.WithFields(fields))
}

func (l Logger) WithError(err error) logger.Logger {
	return newLogger(l.log.WithError(err))
}

func (l Logger) RateInc() {
	// skip
}

func (l Logger) WarnM(reason string) {
	// skip
}

func (l Logger) Debug(args ...interface{}) {
	l.log.Debug(args...)
}

func (l Logger) Info(args ...interface{}) {
	l.log.Info(args...)
}

func (l Logger) Warn(reason string, args ...interface{}) {
	l.log.WithFields(logrus.Fields{
		"reason": reason,
	}).Warn(args...)
}

func (l Logger) Error(args ...interface{}) {
	l.log.Error(args...)
}

func (l Logger) Auto(reason string, args ...interface{}) {
	// skip
}

func (l Logger) Fatal(args ...interface{}) {
	l.log.Panic(args...)
}

func (l Logger) Panic(args ...interface{}) {
	l.log.Panic(args...)
}

func (l Logger) Debugf(format string, args ...interface{}) {
	l.log.Debugf(format, args...)
}

func (l Logger) Infof(format string, args ...interface{}) {
	l.log.Infof(format, args...)
}

func (l Logger) Warnf(reason, format string, args ...interface{}) {
	l.log.WithFields(logrus.Fields{
		"reason": reason,
	}).Warnf(format, args...)
}

func (l Logger) Errorf(format string, args ...interface{}) {
	l.log.Errorf(format, args...)
}

func (l Logger) Autof(reason, format string, args ...interface{}) {
	//skip
}

func (l Logger) Fatalf(format string, args ...interface{}) {
	l.log.Panicf(format, args...)
}

func (l Logger) Panicf(format string, args ...interface{}) {
	l.log.Panicf(format, args...)
}

func (l Logger) Print(args ...interface{}) {
	l.log.Print(args...)
}

func (l Logger) Printf(format string, args ...interface{}) {
	l.log.Printf(format, args...)
}
