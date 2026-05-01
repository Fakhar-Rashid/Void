package com.example.avoid.model;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;

public class Store implements Serializable {

    @DocumentId
    private String id;

    private String ownerId;
    private String name;
    private String description;
    private String location;
    private String contactEmail;
    private String phone;
    private String logoUrl;
    private long createdAt;

    public Store() {}

    public Store(String ownerId, String name, String description, String location,
                 String contactEmail, String phone, String logoUrl, long createdAt) {
        this.ownerId = ownerId;
        this.name = name;
        this.description = description;
        this.location = location;
        this.contactEmail = contactEmail;
        this.phone = phone;
        this.logoUrl = logoUrl;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
