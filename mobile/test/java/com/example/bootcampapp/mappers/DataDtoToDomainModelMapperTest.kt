package com.example.bootcampapp.mappers

import com.example.bootcampapp.data.network.dto.ContainerOfStaffMembersInfoDto
import com.example.bootcampapp.data.network.dto.StaffMemberInfoDto
import com.example.bootcampapp.domain.models.StaffMemberInfo
import org.junit.Assert.*
import org.junit.Test

class DataDtoToDomainModelMapperTest() {
    val mapper = DataDtoToDomainModelMapper()

    @Test
    fun `testing staffMemberInfoDto - when mapper gets proper staff member information dto it works properly`() {
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, "$firstName $lastName")
            assertEquals(loginField, login)
            assertEquals(jobField, "$city, $department")
        }
    }

    @Test
    fun `testing staffMemberInfoDto - when mapper gets proper staff member information but file extension for photo url is different it works properly`() {
        val photo = "http::/photo.jpg"
        val betterQualityPhoto = "http::/photo/460.jpg"
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, "$firstName $lastName")
            assertEquals(loginField, login)
            assertEquals(jobField, "$city, $department")
        }
    }

    @Test
    fun `testing staffMemberInfoDto - when mapper gets null first name it works properly`() {
        val firstName = null
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, lastName)
            assertEquals(loginField, login)
            assertEquals(jobField, "$city, $department")
        }
    }

    @Test
    fun `testing staffMemberInfoDto - when mapper gets null last name it works properly`() {
        val lastName = null
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, firstName)
            assertEquals(loginField, login)
            assertEquals(jobField, "$city, $department")
        }
    }

    @Test
    fun `testing staffMemberInfoDto - when mapper gets both null last name and first name it works properly`() {
        val firstName = null
        val lastName = null
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, "")
            assertEquals(loginField, login)
            assertEquals(jobField, "$city, $department")
        }
    }

    @Test
    fun `testing staffMemberInfoDto - when mapper gets null login it works properly`() {
        val login = null
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, "$firstName $lastName")
            assertEquals(loginField, "")
            assertEquals(jobField, "$city, $department")
        }
    }

    @Test
    fun `testing staffMemberInfoDto - when mapper gets photo url without a dot it works properly`() {
        val photo = "http::/photo"
        val betterQualityPhoto = "http::/photo/460"
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, "$firstName $lastName")
            assertEquals(loginField, login)
            assertEquals(jobField, "$city, $department")
        }
    }

    @Test
    fun `testing staffMemberInfoDto - when mapper gets null city it works properly`() {
        val city = null
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, "$firstName $lastName")
            assertEquals(loginField, login)
            assertEquals(jobField, department)
        }
    }

    @Test
    fun `testing staffMemberInfoDto - when mapper gets null department it works properly`() {
        val department = null
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, "$firstName $lastName")
            assertEquals(loginField, login)
            assertEquals(jobField, city)
        }
    }

    @Test
    fun `testing staffMemberInfoDto - when mapper gets both null department and null city it works properly`() {
        val city = null
        val department = null
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, "$firstName $lastName")
            assertEquals(loginField, login)
            assertEquals(jobField, "")
        }
    }

    @Test
    fun `testing staffMemberInfoDto - when mapper gets null photo url it works properly`() {
        val photo = null
        val betterQualityPhoto = ""
        val properDto = StaffMemberInfoDto(
            firstName = firstName,
            lastName = lastName,
            login = login,
            city = city,
            photo = photo,
            department = department
        )
        val result = mapper.map(properDto)
        result.apply {
            assertEquals(photoUrl, betterQualityPhoto)
            assertEquals(nameField, "$firstName $lastName")
            assertEquals(loginField, login)
            assertEquals(jobField, "$city, $department")
        }
    }

    @Test
    fun `testing ContainerOfStaffMembersInfoDto - when mapper gets proper dto it works properly`() {
        val containerOfStaffMembersInfoDto = ContainerOfStaffMembersInfoDto(
            pages = pagesValue,
            currentPage = currentPageValue,
            listOfStaffMemberInfoDtos = listOfStaffMembersInfoDtoValue
        )
        val result = mapper.map(containerOfStaffMembersInfoDto)
        result.apply {
            assertEquals(currentPage, currentPageValue)
            assertEquals(pages, pagesValue)
            assertEquals(listOfStaffMembersInfo, listOfStaffMembersInfoValue)
        }
    }

    @Test
    fun `testing ContainerOfStaffMembersInfoDto - when mapper gets null pages it works properly`() {
        val pagesValue = null
        val containerOfStaffMembersInfoDto = ContainerOfStaffMembersInfoDto(
            pages = pagesValue,
            currentPage = currentPageValue,
            listOfStaffMemberInfoDtos = listOfStaffMembersInfoDtoValue
        )
        val result = mapper.map(containerOfStaffMembersInfoDto)
        result.apply {
            assertEquals(currentPage, currentPageValue)
            assertEquals(pages, 0)
            assertEquals(listOfStaffMembersInfo, listOfStaffMembersInfoValue)
        }
    }

    @Test
    fun `testing ContainerOfStaffMembersInfoDto - when mapper gets null current page it works properly`() {
        val currentPageValue = null
        val containerOfStaffMembersInfoDto = ContainerOfStaffMembersInfoDto(
            pages = pagesValue,
            currentPage = currentPageValue,
            listOfStaffMemberInfoDtos = listOfStaffMembersInfoDtoValue
        )
        val result = mapper.map(containerOfStaffMembersInfoDto)
        result.apply {
            assertEquals(currentPage, 0)
            assertEquals(pages, pagesValue)
            assertEquals(listOfStaffMembersInfo, listOfStaffMembersInfoValue)
        }
    }

    @Test
    fun `testing ContainerOfStaffMembersInfoDto - when mapper gets null as list it works properly`() {
        val containerOfStaffMembersInfoDto = ContainerOfStaffMembersInfoDto(
            pages = pagesValue,
            currentPage = currentPageValue,
            listOfStaffMemberInfoDtos = null
        )
        val result = mapper.map(containerOfStaffMembersInfoDto)
        result.apply {
            assertEquals(currentPage, currentPageValue)
            assertEquals(pages, pagesValue)
            assertEquals(listOfStaffMembersInfo, listOf<StaffMemberInfo>())
        }
    }

    companion object {
        private const val firstName = "first name"
        private const val lastName = "last name"
        private const val login = "login"
        private const val city = "San Andres"
        private const val photo = "http::/photo.pdf"
        private const val department = "boring department"
        private const val betterQualityPhoto = "http::/photo/460.pdf"
        private const val currentPageValue = 1
        private const val pagesValue = 2
        private val listOfStaffMembersInfoDtoValue = listOf(
            StaffMemberInfoDto(
                firstName = firstName,
                lastName = lastName,
                login = login,
                photo = photo,
                department = department,
                city = city
            )
        )
        private val listOfStaffMembersInfoValue = listOf(
            StaffMemberInfo(
                nameField = "$firstName $lastName",
                jobField = "$city, $department",
                loginField = login,
                photoUrl = betterQualityPhoto
            )
        )
    }
}
