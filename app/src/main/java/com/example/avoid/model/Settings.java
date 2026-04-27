package com.example.avoid.model;

import java.io.Serializable;

public class Settings implements Serializable {

    private boolean notificationsEnabled = true;
    private boolean emailNotifications = true;
    private boolean pushNotifications = true;
    private boolean orderUpdates = true;
    private boolean promotionalAlerts = false;
    private boolean darkMode = false;
    private String language = "English";
    private String currency = "USD";

    public Settings() {}

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean v) { this.notificationsEnabled = v; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean v) { this.emailNotifications = v; }

    public boolean isPushNotifications() { return pushNotifications; }
    public void setPushNotifications(boolean v) { this.pushNotifications = v; }

    public boolean isOrderUpdates() { return orderUpdates; }
    public void setOrderUpdates(boolean v) { this.orderUpdates = v; }

    public boolean isPromotionalAlerts() { return promotionalAlerts; }
    public void setPromotionalAlerts(boolean v) { this.promotionalAlerts = v; }

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean v) { this.darkMode = v; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
