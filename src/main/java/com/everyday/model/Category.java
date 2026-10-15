package com.everyday.model;

public class Category {
    private int id;
    private String name;
    private String color;
    private boolean isDefault;

    public Category() {}

    public Category(int id, String name, String color, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.isDefault = isDefault;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    @Override
    public String toString() {
        return name;
    }
}
