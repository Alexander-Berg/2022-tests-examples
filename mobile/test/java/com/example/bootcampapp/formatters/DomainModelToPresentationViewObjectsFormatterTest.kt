package com.example.bootcampapp.formatters

import com.example.bootcampapp.domain.models.StaffMemberInfo
import org.junit.Assert.*
import org.junit.Test

class DomainModelToPresentationViewObjectsFormatterTest() {

    val formatter = DomainModelToPresentationViewObjectsFormatter()

    @Test
    fun `testing DomainModelToPresentationViewObjectsFormatter - when formatter gets proper model it works properly`() {
        val result = formatter.map(model)
        result.apply {
            assertEquals(jobField, jobFieldValue)
            assertEquals(nameField, nameFieldValue)
            assertEquals(photoUrl, photoValue)
            assertEquals(loginField, loginFieldValue)
        }
    }

    companion object {
        private const val nameFieldValue = "first name last name"
        private const val loginFieldValue = "login"
        private const val jobFieldValue = "San Andres, boring department"
        private const val photoValue = "http::/photo.pdf"
        private val model = StaffMemberInfo(
            loginField = loginFieldValue,
            nameField = nameFieldValue,
            photoUrl = photoValue,
            jobField = jobFieldValue
        )
    }
}
