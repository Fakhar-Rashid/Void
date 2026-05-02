package com.example.avoid.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

public class Address implements Serializable {

    public static final String DEFAULT_COUNTRY = "Pakistan";

    /** Selectable provinces / federal territories — shown as a dropdown wherever an address is edited. */
    public static final String[] PAKISTAN_PROVINCES = new String[]{
            "Punjab",
            "Sindh",
            "Khyber Pakhtunkhwa",
            "Balochistan",
            "Gilgit-Baltistan",
            "Azad Jammu & Kashmir",
            "Islamabad Capital Territory"
    };

    private String houseNumber;
    private String streetNumber;
    private String area;
    private String province;
    private String country = DEFAULT_COUNTRY;

    public Address() {}

    public Address(String houseNumber, String streetNumber, String area,
                   String province, String country) {
        this.houseNumber = houseNumber;
        this.streetNumber = streetNumber;
        this.area = area;
        this.province = province;
        this.country = (country == null || country.trim().isEmpty()) ? DEFAULT_COUNTRY : country;
    }

    public String getHouseNumber() { return houseNumber; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }

    public String getStreetNumber() { return streetNumber; }
    public void setStreetNumber(String streetNumber) { this.streetNumber = streetNumber; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }

    public String getCountry() {
        return (country == null || country.trim().isEmpty()) ? DEFAULT_COUNTRY : country;
    }
    public void setCountry(String country) { this.country = country; }

    @Exclude
    public boolean isComplete() {
        return notBlank(houseNumber) && notBlank(streetNumber) && notBlank(area)
                && notBlank(province) && notBlank(getCountry());
    }

    /** True when every user-editable field is empty. Country isn't checked because it's auto-filled. */
    @Exclude
    public boolean isBlank() {
        return !notBlank(houseNumber) && !notBlank(streetNumber)
                && !notBlank(area) && !notBlank(province);
    }

    /** Single-line "House 12, Street 5, DHA Phase 4, Punjab, Pakistan". */
    @Exclude
    public String getOneLine() {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, "House " + nullToDash(houseNumber));
        appendPart(sb, "Street " + nullToDash(streetNumber));
        appendPart(sb, area);
        appendPart(sb, province);
        appendPart(sb, getCountry());
        return sb.toString();
    }

    /** "House 12, Street 5\nDHA Phase 4, Punjab\nPakistan" — for multi-line displays. */
    @Exclude
    public String getMultiLine() {
        StringBuilder sb = new StringBuilder();
        if (notBlank(houseNumber) || notBlank(streetNumber)) {
            sb.append("House ").append(nullToDash(houseNumber))
              .append(", Street ").append(nullToDash(streetNumber));
        }
        if (notBlank(area) || notBlank(province)) {
            if (sb.length() > 0) sb.append('\n');
            if (notBlank(area)) sb.append(area);
            if (notBlank(area) && notBlank(province)) sb.append(", ");
            if (notBlank(province)) sb.append(province);
        }
        if (sb.length() > 0) sb.append('\n');
        sb.append(getCountry());
        return sb.toString();
    }

    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    private static String nullToDash(String s) { return notBlank(s) ? s : "—"; }

    private static void appendPart(StringBuilder sb, String part) {
        if (!notBlank(part)) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(part);
    }
}
