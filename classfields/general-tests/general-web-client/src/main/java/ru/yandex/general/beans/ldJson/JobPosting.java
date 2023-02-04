package ru.yandex.general.beans.ldJson;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import static ru.yandex.general.beans.ldJson.BaseSalary.baseSalary;
import static ru.yandex.general.beans.ldJson.HiringOrganization.hiringOrganization;
import static ru.yandex.general.beans.ldJson.JobLocation.jobLocation;
import static ru.yandex.general.beans.ldJson.Value.value;

@Setter
@Getter
@Accessors(chain = true)
public class JobPosting {

    @SerializedName("@type")
    String type;
    String url;
    String datePosted;
    String title;
    String description;
    HiringOrganization hiringOrganization;
    JobLocation jobLocation;
    BaseSalary baseSalary;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public static JobPosting jobPosting() {
        return new JobPosting().setType("JobPosting");
    }

    public JobPosting withSallary(String sallary) {
        setBaseSalary(baseSalary(value(sallary)));
        return this;
    }

    public JobPosting withOrganizationName(String organizationName) {
        setHiringOrganization(hiringOrganization(organizationName));
        return this;
    }

    public JobPosting withAddress(Address address) {
        setJobLocation(jobLocation(address));
        return this;
    }

}
