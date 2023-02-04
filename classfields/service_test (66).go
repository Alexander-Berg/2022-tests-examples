package rate_limiter

import (
	"errors"
	mocks "github.com/YandexClassifieds/shiva/pkg/rate_limiter/mock"
	"github.com/YandexClassifieds/shiva/test"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

type mockStorage struct {
	Keys map[string]int
}

func (s *mockStorage) Set(key string, value int) error {
	s.Keys[key] = value
	return nil

}

func (s *mockStorage) Get(key string) int {
	return s.Keys[key]
}

func TestRateLimiter_Check(t *testing.T) {

	mStorage := mockStorage{Keys: make(map[string]int)}
	storage := mocks.RateLimiterStorage{}
	storage.On("Get", mock.Anything).Return(mStorage.Get, nil)
	storage.On("Set", mock.Anything, mock.Anything).Return(mStorage.Set)
	l := test.NewLogger(t)
	conf := Conf{
		Delta: "1m",
		Rate:  3,
	}
	service := newRateLimiter(&storage, conf, l)
	err := service.Check("test1")
	require.NoError(t, err)

}

func TestRateLimiter_RateLimited(t *testing.T) {

	mStorage := mockStorage{Keys: make(map[string]int)}
	storage := mocks.RateLimiterStorage{}
	storage.On("Get", mock.Anything).Return(mStorage.Get, nil)
	storage.On("Set", mock.Anything, mock.Anything).Return(mStorage.Set)
	l := test.NewLogger(t)
	conf := Conf{
		Delta: "1m",
		Rate:  1,
	}
	service := newRateLimiter(&storage, conf, l)
	err := service.Check("test1")
	require.NoError(t, err)
	err = service.Check("test1")
	assert.True(t, errors.Is(err, RateLimitedErr))

}

func TestRateLimiter_Delta(t *testing.T) {

	mStorage := mockStorage{Keys: make(map[string]int)}
	storage := mocks.RateLimiterStorage{}
	storage.On("Get", mock.Anything).Return(mStorage.Get, nil)
	storage.On("Set", mock.Anything, mock.Anything).Return(mStorage.Set)
	l := test.NewLogger(t)
	conf := Conf{
		Delta: "1s",
		Rate:  1,
	}
	service := newRateLimiter(&storage, conf, l)
	err := service.Check("test1")
	require.NoError(t, err)
	err = service.Check("test1")
	assert.True(t, errors.Is(err, RateLimitedErr))
	time.Sleep(time.Second)
	err = service.Check("test1")
	require.NoError(t, err)
}

func TestRateLimiter_MultiService(t *testing.T) {

	mStorage := mockStorage{Keys: make(map[string]int)}
	storage := mocks.RateLimiterStorage{}
	storage.On("Get", mock.Anything).Return(mStorage.Get, nil)
	storage.On("Set", mock.Anything, mock.Anything).Return(mStorage.Set)
	l := test.NewLogger(t)
	conf := Conf{
		Delta: "1s",
		Rate:  1,
	}
	service := newRateLimiter(&storage, conf, l)
	err := service.Check("test1")
	require.NoError(t, err)
	err = service.Check("test1")
	assert.True(t, errors.Is(err, RateLimitedErr))
	err = service.Check("test2")
	require.NoError(t, err)
}
