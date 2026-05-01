package com.example.avoid.model;

import java.io.Serializable;

public class Color implements Serializable {

    private String name;
    private String hex;

    public Color() {}

    public Color(String name, String hex) {
        this.name = name;
        this.hex = hex;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHex() { return hex; }
    public void setHex(String hex) { this.hex = hex; }
}
