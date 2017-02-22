package com.nineinfosys.android.hindijokes;

/**
 * Created by Dev on 22-02-2017.
 */

public class HindiJokesCategory {

    @com.google.gson.annotations.SerializedName("category")
    private String category;
    @com.google.gson.annotations.SerializedName("id")
    private  String id;
    @com.google.gson.annotations.SerializedName("image")
    private String image;

    public HindiJokesCategory() {
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


}
