package com.evo.evo_backend.model;

public class Competitor {

    private String name;
    private double rating;
    private int reviewCount;
    private String address;
    private String website;
    private String type;
    private String city;
    private boolean isClaimed; // New: Helps identify unmanaged local threats

    public Competitor() {}

    public Competitor(String name, double rating, String address, String city, String website) {
        this.name = name;
        this.rating = rating;
        this.address = address;
        this.city = city;
        this.website = website;
    }

    // Getters
    public String getName()        { return name; }
    public double getRating()      { return rating; }
    public int getReviewCount()    { return reviewCount; }
    public String getAddress()     { return address; }
    public String getWebsite()     { return website; }
    public String getType()        { return type; }
    public String getCity()        { return city; }
    public boolean isClaimed()     { return isClaimed; }

    // Setters
    public void setName(String name)           { this.name = name; }
    public void setRating(double rating)       { this.rating = rating; }
    public void setReviewCount(int count)      { this.reviewCount = count; }
    public void setAddress(String address)     { this.address = address; }
    public void setWebsite(String website)     { this.website = website; }
    public void setType(String type)           { this.type = type; }
    public void setCity(String city)           { this.city = city; }
    public void setClaimed(boolean claimed)    { this.isClaimed = claimed; }
}