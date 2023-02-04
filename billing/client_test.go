package client

import (
	"context"
	"errors"
	"fmt"
	"testing"
	"time"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/xerrors/multierr"
)

type senderFunc func(ctx context.Context) error

func (s senderFunc) Send(ctx context.Context, _ []any, _ bool) error {
	return s(ctx)
}

func TestSendUsingCombinedSenders(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		GivenSenders []Sender
		ExpectedErrs []error
	}{
		{
			GivenSenders: nil,
			ExpectedErrs: nil,
		},
		{
			GivenSenders: []Sender{
				senderFunc(func(_ context.Context) error {
					return nil
				}),
			},
			ExpectedErrs: nil,
		},
		{
			GivenSenders: []Sender{
				senderFunc(func(_ context.Context) error {
					return nil
				}),
				senderFunc(func(_ context.Context) error {
					return nil
				}),
			},
			ExpectedErrs: nil,
		},
		{
			GivenSenders: []Sender{
				senderFunc(func(_ context.Context) error {
					return errors.New("foo")
				}),
				senderFunc(func(_ context.Context) error {
					return nil
				}),
			},
			ExpectedErrs: []error{
				errors.New("foo"),
			},
		},
		{
			GivenSenders: []Sender{
				senderFunc(func(_ context.Context) error {
					return errors.New("foo")
				}),
				senderFunc(func(_ context.Context) error {
					return errors.New("bar")
				}),
			},
			ExpectedErrs: []error{
				errors.New("foo"),
				errors.New("bar"),
			},
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(fmt.Sprint(i), func(t *testing.T) {
			t.Parallel()

			sender := CombineSenders(c.GivenSenders...)

			err := sender.Send(context.Background(), nil, false)

			require.ElementsMatch(t, c.ExpectedErrs, multierr.Errors(err))
		})
	}
}

func TestCancelCombineSend(t *testing.T) {
	t.Parallel()

	firstDone := make(chan struct{})

	sender := CombineSenders(
		senderFunc(func(ctx context.Context) error {
			select {
			case <-ctx.Done():
				return ctx.Err()
			case <-time.After(10 * time.Millisecond):
				close(firstDone)

				return nil
			}
		}),
		senderFunc(func(ctx context.Context) error {
			select {
			case <-ctx.Done():
				return ctx.Err()
			case <-time.After(20 * time.Millisecond):
				return nil
			}
		}),
	)

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	go func() {
		<-firstDone

		cancel()
	}()

	err := sender.Send(ctx, nil, false)

	require.ErrorIs(t, err, context.Canceled)
	require.Len(t, multierr.Errors(err), 1)
}
