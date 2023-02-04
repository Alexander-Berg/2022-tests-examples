package mock

import (
	"github.com/prometheus/common/model"
)

func (_m *PrometheusApi) GenerateMatrix(values ...float64) model.Matrix {
	answer := model.Matrix{
		{
			Values: []model.SamplePair{},
		},
	}
	for _, value := range values {
		answer[0].Values = append(answer[0].Values, model.SamplePair{Value: model.SampleValue(value)})
	}
	return answer
}

func (_m *PrometheusApi) GenerateVector(value float64) model.Vector {
	return model.Vector{
		{
			Value: model.SampleValue(value),
		},
	}
}
