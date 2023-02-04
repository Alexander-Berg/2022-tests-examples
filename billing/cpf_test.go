package logbroker

import (
	"context"
	"errors"
	"fmt"
	"testing"

	"github.com/stretchr/testify/require"

	"a.yandex-team.ru/library/go/core/xerrors/multierr"
)

func TestLBCPFWriter_Start(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		ProducerStartErr       error
		ProducerDryRunStartErr error
		ExpectedErrs           []error
	}{
		{
			ProducerStartErr:       nil,
			ProducerDryRunStartErr: nil,
			ExpectedErrs:           nil,
		},
		{
			ProducerStartErr:       errors.New("foo"),
			ProducerDryRunStartErr: nil,
			ExpectedErrs: []error{
				errors.New("foo"),
			},
		},
		{
			ProducerStartErr:       nil,
			ProducerDryRunStartErr: errors.New("foo"),
			ExpectedErrs: []error{
				errors.New("foo"),
			},
		},
		{
			ProducerStartErr:       errors.New("foo"),
			ProducerDryRunStartErr: errors.New("bar"),
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

			var (
				p   = &MockLogBrokerWriter{Err: c.ProducerStartErr}
				pdr = &MockLogBrokerWriter{Err: c.ProducerDryRunStartErr}
			)

			w := LBCPFWriter{
				producer:       p,
				producerDryRun: pdr,
			}

			err := w.Start(context.Background())

			require.ElementsMatch(t, multierr.Errors(err), c.ExpectedErrs)
		})
	}
}

func TestLBCPFWriter_Disconnect(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		ProducerDisconnectErr       error
		ProducerDryRunDisconnectErr error
		ExpectedErrs                []error
	}{
		{
			ProducerDisconnectErr:       nil,
			ProducerDryRunDisconnectErr: nil,
			ExpectedErrs:                nil,
		},
		{
			ProducerDisconnectErr:       errors.New("foo"),
			ProducerDryRunDisconnectErr: nil,
			ExpectedErrs: []error{
				errors.New("foo"),
			},
		},
		{
			ProducerDisconnectErr:       nil,
			ProducerDryRunDisconnectErr: errors.New("foo"),
			ExpectedErrs: []error{
				errors.New("foo"),
			},
		},
		{
			ProducerDisconnectErr:       errors.New("foo"),
			ProducerDryRunDisconnectErr: errors.New("bar"),
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

			var (
				p   = &MockLogBrokerWriter{Err: c.ProducerDisconnectErr}
				pdr = &MockLogBrokerWriter{Err: c.ProducerDryRunDisconnectErr}
			)

			w := LBCPFWriter{
				producer:       p,
				producerDryRun: pdr,
			}

			err := w.Disconnect()

			require.ElementsMatch(t, multierr.Errors(err), c.ExpectedErrs)
		})
	}
}

func TestLBCPFWriter_Write(t *testing.T) {
	t.Parallel()

	testCases := []struct {
		GivenFacts             []any
		GivenDryRun            bool
		ExpectedMessages       [][]byte
		ExpectedDryRunMessages [][]byte
	}{
		{
			GivenFacts:             []any{},
			GivenDryRun:            false,
			ExpectedMessages:       nil,
			ExpectedDryRunMessages: nil,
		},
		{
			GivenFacts: []any{
				map[string]string{"foo": "a"},
				map[string]string{"bar": "b"},
			},
			GivenDryRun: false,
			ExpectedMessages: [][]byte{
				[]byte(`{"foo":"a"}`),
				[]byte(`{"bar":"b"}`),
			},
			ExpectedDryRunMessages: nil,
		},
		{
			GivenFacts: []any{
				map[string]string{"foo": "a"},
				map[string]string{"bar": "b"},
				map[string]string{"baz": "c"},
			},
			GivenDryRun:      true,
			ExpectedMessages: nil,
			ExpectedDryRunMessages: [][]byte{
				[]byte(`{"foo":"a"}`),
				[]byte(`{"bar":"b"}`),
				[]byte(`{"baz":"c"}`),
			},
		},
	}

	for i := range testCases {
		c := testCases[i]

		t.Run(fmt.Sprint(i), func(t *testing.T) {
			t.Parallel()

			var (
				p   = &MockLogBrokerWriter{}
				pdr = &MockLogBrokerWriter{}
			)

			w := LBCPFWriter{
				producer:       p,
				producerDryRun: pdr,
			}

			err := w.Write(context.Background(), c.GivenFacts, c.GivenDryRun)
			require.NoError(t, err)

			t.Run("messages", func(t *testing.T) {
				require.ElementsMatch(t, c.ExpectedMessages, p.Messages)
			})

			t.Run("dry_run_messages", func(t *testing.T) {
				require.ElementsMatch(t, c.ExpectedDryRunMessages, pdr.Messages)
			})
		})
	}
}
