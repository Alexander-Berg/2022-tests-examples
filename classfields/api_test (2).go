package staff

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestNormalisePhone(t *testing.T) {
	s := Api{}
	m := map[string]string{
		"+79630495365": "+79630495365",
		"79630495365":  "+79630495365",
		"91230001122":  "+91230001122",
		"89630495365":  "+79630495365",
	}

	for phone, expected := range m {
		result := s.normalisePhone(phone)

		if result != expected {
			t.Errorf("Expected %s, got %s", expected, result)
		}
	}
}

func TestMakeQueryElements(t *testing.T) {
	s := Api{}
	what := []int{1, 2}
	result := s.makeQueryElements(what, "field", " sep ")
	expected := "field==1 sep field==2"

	if result != expected {
		t.Errorf("Expected %s, got %s", expected, result)
	}
}

func TestMakeQuery(t *testing.T) {
	s := Api{}
	orgs := []int{1, 2}
	depts := []int{3, 4}
	result := s.makeQuery(orgs, depts, "email", "sy@e1.ru")
	expected := "email==\"sy@e1.ru\" and official.is_dismissed==false and (official.organization.id==1 or official.organization.id==2 or department_group.department.id==3 or department_group.department.id==4)"

	assert.Equal(t, expected, result)

	result = s.makeQuery([]int{}, []int{}, "email", "sy@e1.ru")
	expected = "email==\"sy@e1.ru\" and official.is_dismissed==false"
	assert.Equal(t, expected, result)
}
