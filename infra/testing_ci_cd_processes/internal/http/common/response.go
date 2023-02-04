package common

import (
	"encoding/json"
	"log"
	"net/http"
)

type Response struct {
	Result interface{} `json:"result,omitempty"`
	Error  string      `json:"error,omitempty"`
}

func WriteResponse(w http.ResponseWriter, status int, result interface{}, respErr error) {
	w.Header().Add("Content-Type", "application/json;charset=utf8")
	w.WriteHeader(status)

	response := Response{Result: result}
	if respErr != nil {
		response.Error = respErr.Error()
	}

	b, err := json.Marshal(response)
	if err != nil {
		log.Println("can't json.Marshal response")
	}

	_, err = w.Write(b)
	if err != nil {
		log.Println("can't write response")
	}
}
