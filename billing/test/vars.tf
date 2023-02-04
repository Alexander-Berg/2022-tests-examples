variable "yc_token" {
    type = string
}

variable "yav_token" {
    type = string
}

variable "cnt" {
    description = "Hotbilling Cluster's count"
    type = number
}

variable "name" {
    description = "Hotbilling Cluster Name"
    type = string
}

variable "env" {
    description = "Hotbilling Cluster Environment (Prestable/Stable)"
    type = string
}

variable "resource_preset" {
    description = "Hotbilling Cluster resource preset"
    type = string
    default = "s3.micro"
}

variable "disk_size" {
    description = "Hotbilling Cluster SSD disk size"
    type = number
    default = 10
}

variable "dbname" {
    description = "Hotbilling PG database name"
    type = string
}

variable "dbowner" {
    description = "Hotbilling PG database owner"
    type = string
}

variable "username" {
    description = "Hotbilling PG database username"
    type = string
}

variable "hosts" {
    description = "Hotbilling PG cluster nodes hosts"
    type = list(object({
        zone = string
        priority = number
    }))
    default = [
        {
            zone = "vla",
            priority = 5
        }
    ]
}
