package logger

import (
	"net/url"
	"testing"

	"github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/shiva/common/logger/metrics"
	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/stretchr/testify/mock"

	"github.com/YandexClassifieds/shiva/common/logger/mocks"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/require"
)

func TestSanitizer(t *testing.T) {
	token := "TgToken12345"
	logger := NewLogger(logrus.NewLogger(), &metrics.Service{}).WithSanitizer(TgTokenSanitizer(token))
	err := &url.Error{
		Op:  "parse",
		URL: "wrong-url-" + token + "-wrong-url",
	}
	log := logger.WithError(err)
	log.Warn("")

	var urlErr *url.Error
	require.True(t, errors.As(log.(*Entry).params.err, &urlErr))
	require.NotContains(t, urlErr.URL, token)
}

func TestAutoError(t *testing.T) {
	loggerMock := &mocks.Logger{}
	logger := NewLogger(loggerMock, &metrics.Service{})
	err := errors.New("error")

	loggerMock.On("Error", mock.Anything).Once()
	loggerMock.On("Errorf", mock.Anything).Once()
	loggerMock.On("WithError", mock.Anything).Return(loggerMock).Twice()
	loggerMock.On("WithField", mock.Anything, mock.Anything).Return(loggerMock).Twice()
	logger.WithError(err).Auto("myMethod", "myError")
	logger.WithError(err).Autof("myMethod", "myError", "Error %d", 1)

	loggerMock.AssertExpectations(t)
}

func TestAutoWarn(t *testing.T) {
	testCases := []struct {
		name string
		err  error
	}{
		{
			name: "single userError",
			err:  user_error.NewUserError(errors.New("error"), "ошибка"),
		},
		{
			name: "userErrors",
			err:  user_error.NewUserErrors(user_error.NewUserError(errors.New("error"), "ошибка")),
		},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			loggerMock := &mocks.Logger{}
			logger := NewLogger(loggerMock, &metrics.Service{})

			loggerMock.On("Warn", mock.Anything).Once()
			loggerMock.On("Warnf", mock.Anything).Once()
			loggerMock.On("WithError", mock.Anything).Return(loggerMock).Twice()
			loggerMock.On("WithField", mock.Anything, mock.Anything).Return(loggerMock).Twice()
			logger.WithError(tc.err).Auto("myMethod", "myError")
			logger.WithError(tc.err).Autof("myMethod", "myError", "Error %d", 1)

			loggerMock.AssertExpectations(t)

		})
	}
}
