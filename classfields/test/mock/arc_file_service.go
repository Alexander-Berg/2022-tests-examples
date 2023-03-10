// Code generated by mockery. DO NOT EDIT.

package mock

import (
	file "github.com/YandexClassifieds/shiva/pkg/arc/file"
	mock "github.com/stretchr/testify/mock"
)

// ArcFileService is an autogenerated mock type for the IService type
type ArcFileService struct {
	mock.Mock
}

// ActualRevision provides a mock function with given fields:
func (_m *ArcFileService) ActualRevision() (uint64, error) {
	ret := _m.Called()

	var r0 uint64
	if rf, ok := ret.Get(0).(func() uint64); ok {
		r0 = rf()
	} else {
		r0 = ret.Get(0).(uint64)
	}

	var r1 error
	if rf, ok := ret.Get(1).(func() error); ok {
		r1 = rf()
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// ListFiles provides a mock function with given fields: revision, path
func (_m *ArcFileService) ListFiles(revision uint64, path string) ([]*file.FileInfo, error) {
	ret := _m.Called(revision, path)

	var r0 []*file.FileInfo
	if rf, ok := ret.Get(0).(func(uint64, string) []*file.FileInfo); ok {
		r0 = rf(revision, path)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).([]*file.FileInfo)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(uint64, string) error); ok {
		r1 = rf(revision, path)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// ReadFile provides a mock function with given fields: revision, path
func (_m *ArcFileService) ReadFile(revision uint64, path string) ([]byte, error) {
	ret := _m.Called(revision, path)

	var r0 []byte
	if rf, ok := ret.Get(0).(func(uint64, string) []byte); ok {
		r0 = rf(revision, path)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).([]byte)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(uint64, string) error); ok {
		r1 = rf(revision, path)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

// ReadFileFromBranch provides a mock function with given fields: branch, path
func (_m *ArcFileService) ReadFileFromBranch(branch string, path string) ([]byte, error) {
	ret := _m.Called(branch, path)

	var r0 []byte
	if rf, ok := ret.Get(0).(func(string, string) []byte); ok {
		r0 = rf(branch, path)
	} else {
		if ret.Get(0) != nil {
			r0 = ret.Get(0).([]byte)
		}
	}

	var r1 error
	if rf, ok := ret.Get(1).(func(string, string) error); ok {
		r1 = rf(branch, path)
	} else {
		r1 = ret.Error(1)
	}

	return r0, r1
}

type mockConstructorTestingTNewArcFileService interface {
	mock.TestingT
	Cleanup(func())
}

// NewArcFileService creates a new instance of ArcFileService. It also registers a testing interface on the mock and a cleanup function to assert the mocks expectations.
func NewArcFileService(t mockConstructorTestingTNewArcFileService) *ArcFileService {
	mock := &ArcFileService{}
	mock.Mock.Test(t)

	t.Cleanup(func() { mock.AssertExpectations(t) })

	return mock
}
