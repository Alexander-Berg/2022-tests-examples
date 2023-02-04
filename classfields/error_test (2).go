package common

import (
	"errors"
	"fmt"
	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/stretchr/testify/assert"
	"testing"
)

func errNil() error {

	return nil
}

func errFmt() error {

	return fmt.Errorf("error2")
}

func errUserErr() error {

	return user_error.NewUserError(fmt.Errorf("error3"), "", "")
}

func errUserErrs() *user_error.UserErrors {

	return user_error.NewUserErrors(user_error.NewUserError(fmt.Errorf("error4"), "", ""))
}

func TestError(t *testing.T) {
	assertError(t, errNil(), false, false)
	assertError(t, errUserErrs(), true, false)
	assertError(t, errFmt(), true, false)
	assertError(t, errUserErr(), true, true)
}

func TestGenericErr(t *testing.T) {

	StorageError := user_error.GenericError()
	ApiError := user_error.GenericError()
	CommonNotFoundError := user_error.GenericError()
	err1 := user_error.NewUserError(fmt.Errorf("error"), "", "")
	err1.Mark(StorageError, CommonNotFoundError)
	assert.True(t, errors.Is(err1, StorageError))
	assert.True(t, errors.Is(err1, CommonNotFoundError))
	assert.False(t, errors.Is(err1, ApiError))
	assert.False(t, errors.Is(err1, fmt.Errorf("test error")))
	assert.False(t, errors.Is(err1, ErrNotFound))
}

func assertError(t *testing.T, errI interface{}, isError, isUserError bool) {

	err, ok := errI.(error)
	if !ok && !isError {
		return
	}

	var uErr *user_error.UserError
	if err != nil {
		if !isError {
			t.Fatal("Not error!", err)
			return
		}
		isUr := errors.As(err, &uErr)
		assert.Equal(t, isUserError, isUr)
		return
	}
	if isError {
		t.Fatal("Not error!", err)
		return
	}
}

func TestUserError_Unwrap(t *testing.T) {

	err := fmt.Errorf("my error")
	uErr := user_error.NewUserError(err, "текст", "docs")
	assert.Equal(t, err, errors.Unwrap(uErr))
}
