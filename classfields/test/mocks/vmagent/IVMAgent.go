// Code generated by mockery 2.9.0. DO NOT EDIT.

package vmagent

import mock "github.com/stretchr/testify/mock"

// IVMAgent is an autogenerated mock type for the IVMAgent type
type IVMAgent struct {
	mock.Mock
}

// Manage provides a mock function with given fields: action, dc
func (_m *IVMAgent) Manage(action string, dc string) error {
	ret := _m.Called(action, dc)

	var r0 error
	if rf, ok := ret.Get(0).(func(string, string) error); ok {
		r0 = rf(action, dc)
	} else {
		r0 = ret.Error(0)
	}

	return r0
}
