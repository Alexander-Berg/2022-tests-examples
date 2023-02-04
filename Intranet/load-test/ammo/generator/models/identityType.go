package models

type IdentityType struct {
	ID      string `csv:"id" json:"id"`
	IsGroup bool   `csv:"is_group"  json:"is_group"`
}
