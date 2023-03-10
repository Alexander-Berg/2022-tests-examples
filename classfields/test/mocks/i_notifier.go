// Code generated by mockery v2.9.3. DO NOT EDIT.

package mocks

import (
	email "github.com/YandexClassifieds/h2p/cmd/h2p-idm/email"
	mock "github.com/stretchr/testify/mock"
)

// INotifier is an autogenerated mock type for the INotifier type
type INotifier struct {
	mock.Mock
}

// Send provides a mock function with given fields: to, params
func (_m *INotifier) Send(to string, params email.Params) error {
	ret := _m.Called(to, params)

	var r0 error
	if rf, ok := ret.Get(0).(func(string, email.Params) error); ok {
		r0 = rf(to, params)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}
