package errors

import (
	"fmt"
	"testing"

	"github.com/stretchr/testify/assert"

	"a.yandex-team.ru/library/go/core/xerrors"
)

func TestAs(t *testing.T) {
	err := NewCodedError(123, "321", "HELLO!")
	wrappedErr := fmt.Errorf("fail: %w", err)

	var resError CodedError
	assert.True(t, xerrors.As(wrappedErr, &resError))
	assert.Equal(t, resError.HTTPCode(), 123)
	assert.Equal(t, resError.CharCode(), "321")
	assert.Equal(t, resError.Error(), "HELLO!")
	assert.Equal(t, wrappedErr.Error(), "fail: HELLO!")
}

type testError struct {
	msg string
}

func (e *testError) Error() string {
	return e.msg
}

func TestAsUnwrap(t *testing.T) {
	baseErr := &testError{msg: "welp"}
	err := WrapCode(baseErr, 321, "oopsies", "i'm sad =(")
	wrappedErr := fmt.Errorf("fail: %w", err)

	var resError *testError
	assert.True(t, xerrors.As(wrappedErr, &resError))
	assert.Equal(t, resError.Error(), "welp")
	assert.Equal(t, wrappedErr.Error(), "fail: i'm sad =(: welp")
}
