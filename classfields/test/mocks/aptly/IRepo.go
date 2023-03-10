// Code generated by mockery v2.13.1. DO NOT EDIT.

package aptly

import (
	packages "aptly/pkg/packages"

	mock "github.com/stretchr/testify/mock"
)

// IRepo is an autogenerated mock type for the IRepo type
type IRepo struct {
	mock.Mock
}

// Update provides a mock function with given fields:
func (_m *IRepo) Update() (packages.Map, error) {
	ret := _m.Called()

	var r0 packages.Map
	if rf, ok := ret.Get(0).(func() packages.Map); ok {
		r0 = rf()
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).(packages.Map)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

type mockConstructorTestingTNewIRepo interface {
	mock.TestingT
	Cleanup(func())
}

// NewIRepo creates a new instance of IRepo. It also registers a testing interface on the mock and a cleanup function to assert the mocks expectations.
func NewIRepo(t mockConstructorTestingTNewIRepo) *IRepo {
	mock := &IRepo{}
	mock.Mock.Test(t)

	t.Cleanup(func() { mock.AssertExpectations(t) })

	return mock
}
