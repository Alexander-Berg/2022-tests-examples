package timer

import (
	"context"
	"errors"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

var ErrStopRepeat = errors.New("stop repeat")

func TestDoEveryStopByError(t *testing.T) {
	// Проверем остановку работы при возврате ошибки из функции
	counter := 0
	ctx := context.Background()

	err := DoEvery(ctx, 10*time.Millisecond, func(_ time.Time) error {
		counter++
		if counter == 4 {
			return ErrStopRepeat
		}
		return nil
	})

	assert.Equal(t, 4, counter)
	assert.Error(t, err)
	assert.Equal(t, ErrStopRepeat, err)
}

func TestDoEveryStopByContextCancel(t *testing.T) {
	// Проверяем остановку работы при отмене контекта
	counter := 0
	ctx, cancel := context.WithTimeout(context.Background(), 1*time.Second)
	go func() {
		time.Sleep(100 * time.Millisecond)
		cancel()
	}()

	err := DoEvery(ctx, 10*time.Millisecond, func(_ time.Time) error {
		counter++
		return nil
	})

	assert.Greater(t, counter, 8)
	assert.Error(t, err)
	assert.Equal(t, context.Canceled, err)
}
