resource "time_sleep" "wait-sa" {
    lifecycle {
        prevent_destroy = true
    }
    create_duration = "10s"

    depends_on = [
        ycp_resource_manager_folder_iam_member.ig-sa-admin,
        ycp_resource_manager_folder_iam_member.ig-sa-image-user,
        ycp_resource_manager_folder_iam_member.ig-sa-dns-editor,
        ycp_resource_manager_folder_iam_member.vm-sa-kms,
        ycp_resource_manager_folder_iam_member.vm-sa-lockbox,
        ycp_resource_manager_folder_iam_member.vm-sa-certificates
    ]
}

resource "ycp_iam_service_account" "ig-sa" {
    lifecycle {
        prevent_destroy = true
    }
    folder_id = var.folder_id
    name = "${var.ig_name}-ig-sa"
    service_account_id = "yc.monitoring.${var.ig_name}-ig-sa"
}

resource "ycp_resource_manager_folder_iam_member" "ig-sa-admin" {
    lifecycle {
        prevent_destroy = true
    }
    folder_id = var.folder_id
    role = "admin"
    member = "serviceAccount:${ycp_iam_service_account.ig-sa.id}"
}

resource "ycp_resource_manager_folder_iam_member" "ig-sa-image-user" {
    lifecycle {
        prevent_destroy = true
    }
    folder_id = var.infra_folder_id[var.env]
    role = "compute.images.user"
    member = "serviceAccount:${ycp_iam_service_account.ig-sa.id}"
}

resource "ycp_resource_manager_folder_iam_member" "ig-sa-dns-editor" {
    lifecycle {
        prevent_destroy = true
    }
    folder_id = var.infra_folder_id[var.env]
    role = "dns.editor"
    member = "serviceAccount:${ycp_iam_service_account.ig-sa.id}"
}


resource "ycp_iam_service_account" "vm-sa" {
    lifecycle {
        prevent_destroy = true
    }
    folder_id = var.folder_id
    name = "${var.ig_name}-vm-sa"
    service_account_id = "yc.monitoring.${var.ig_name}-vm-sa"
}

resource "ycp_resource_manager_folder_iam_member" "vm-sa-kms" {
    lifecycle {
        prevent_destroy = true
    }
    folder_id = var.folder_id
    role = "kms.keys.encrypterDecrypter"
    member = "serviceAccount:${ycp_iam_service_account.vm-sa.id}"
}

resource "ycp_resource_manager_folder_iam_member" "vm-sa-lockbox" {
    lifecycle {
        prevent_destroy = true
    }
    folder_id = var.folder_id
    role = "lockbox.payloadViewer"
    member = "serviceAccount:${ycp_iam_service_account.vm-sa.id}"
}

resource "ycp_resource_manager_folder_iam_member" "vm-sa-certificates" {
    lifecycle {
        prevent_destroy = true
    }
    count = var.enable_certificates_downloader_binding == true ? 1 : 0
    folder_id = var.folder_id
    role = "certificate-manager.certificates.downloader"
    member = "serviceAccount:${ycp_iam_service_account.vm-sa.id}"
}
