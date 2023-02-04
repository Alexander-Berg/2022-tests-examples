package ru.auto.tests.diff;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Generated;
/**
 * Created by vicdev on 27.10.17.
 */
@Generated("org.jsonschema2pojo")
public class TestObject {

    @SerializedName("confirmed")
    @Expose
    private Boolean confirmed;
    @SerializedName("added")
    @Expose
    private String added;
    /**
     * (Required)
     */
    @SerializedName("email_to")
    @Expose
    private String emailTo;

    protected final static Object NOT_FOUND_VALUE = new Object();

    /**
     * @return The confirmed
     */
    public Boolean getConfirmed() {
        return confirmed;
    }

    /**
     * @param confirmed The confirmed
     */
    public void setConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
    }

    public TestObject withConfirmed(Boolean confirmed) {
        this.confirmed = confirmed;
        return this;
    }

    /**
     * @return The added
     */
    public String getAdded() {
        return added;
    }

    /**
     * @param added The added
     */
    public void setAdded(String added) {
        this.added = added;
    }

    public TestObject withAdded(String added) {
        this.added = added;
        return this;
    }

    /**
     * (Required)
     *
     * @return The emailTo
     */
    public String getEmailTo() {
        return emailTo;
    }

    /**
     * (Required)
     *
     * @param emailTo The emailTo
     */
    public void setEmailTo(String emailTo) {
        this.emailTo = emailTo;
    }

    public TestObject withEmail(String email) {
        this.emailTo = email;
        return this;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    protected boolean declaredProperty(String name, Object value) {
        if ("confirmed".equals(name)) {
            if (value instanceof Boolean) {
                setConfirmed(((Boolean) value));
            } else {
                throw new IllegalArgumentException(("property \"confirmed\" is of type \"java.lang.Boolean\", but got " + value.getClass().toString()));
            }
            return true;
        } else {
            if ("added".equals(name)) {
                if (value instanceof String) {
                    setAdded(((String) value));
                } else {
                    throw new IllegalArgumentException(("property \"added\" is of type \"java.lang.String\", but got " + value.getClass().toString()));
                }
                return true;
            } else {
                if ("emailTo".equals(name)) {
                    if (value instanceof String) {
                        setEmailTo(((String) value));
                    } else {
                        throw new IllegalArgumentException(("property \"emailTo\" is of type \"java.lang.String\", but got " + value.getClass().toString()));
                    }
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    protected Object declaredPropertyOrNotFound(String name, Object notFoundValue) {
        if ("confirmed".equals(name)) {
            return getConfirmed();
        } else {
            if ("added".equals(name)) {
                return getAdded();
            } else {
                if ("emailTo".equals(name)) {
                    return getEmailTo();
                } else {
                    return notFoundValue;
                }
            }
        }
    }

    @SuppressWarnings({
            "unchecked"
    })
    public <T> T get(String name) {
        Object value = declaredPropertyOrNotFound(name, TestObject.NOT_FOUND_VALUE);
        if (TestObject.NOT_FOUND_VALUE != value) {
            return ((T) value);
        } else {
            throw new IllegalArgumentException((("property \"" + name) + "\" is not defined"));
        }
    }

    public void set(String name, Object value) {
        if (!declaredProperty(name, value)) {
            throw new IllegalArgumentException((("property \"" + name) + "\" is not defined"));
        }
    }

    public TestObject with(String name, Object value) {
        if (!declaredProperty(name, value)) {
            throw new IllegalArgumentException((("property \"" + name) + "\" is not defined"));
        }
        return this;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(confirmed).append(added).append(emailTo).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof TestObject) == false) {
            return false;
        }
        TestObject rhs = ((TestObject) other);
        return new EqualsBuilder().append(confirmed, rhs.confirmed).append(added, rhs.added).append(emailTo, rhs.emailTo).isEquals();
    }

}
