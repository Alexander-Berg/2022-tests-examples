package retry

import (
	"context"
	"database/sql/driver"
	"errors"
	"fmt"
	"math/rand"
	"time"
)

func ExampleUntilSuccess() {
	ctx, cancelF := context.WithTimeout(context.Background(), time.Second)
	defer cancelF()

	UntilSuccess(ctx, func(ctx context.Context) (retry bool, err error) {
		return rand.Intn(10) == 0, nil
	}, BackoffLinearWithJitter(50*time.Millisecond, 0.1))
}

func ExampleWithBackoff() {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*10)
	defer cancel()
	defaultBackoff := BackoffExponentialWithJitter(time.Second/10, time.Second)

	WithBackoff(ctx, func(ctx context.Context) (retry bool, backoff BackoffFunc, err error) {
		retry = true
		backoff = defaultBackoff
		err = someFunction()
		// we want to retry ErrBadConn instantly in this example
		if errors.Is(err, driver.ErrBadConn) {
			backoff = BackoffNone()
		}
		return
	})
}

func someFunction() error {
	return fmt.Errorf("some error")
}
