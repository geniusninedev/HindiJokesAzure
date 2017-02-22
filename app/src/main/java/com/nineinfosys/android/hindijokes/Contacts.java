package com.nineinfosys.android.hindijokes;

/**
 * Created by Dev on 22-02-2017.
 */

public class Contacts {

    //@com.google.gson.annotations.SerializedName("id")
    //private String id;
    @com.google.gson.annotations.SerializedName("firebaseid")
    private String firebaseid;
    @com.google.gson.annotations.SerializedName("contactname")
    private String contactname;
    @com.google.gson.annotations.SerializedName("contactnumber")
    private String contactnumber;

    public Contacts() {
    }


    /*public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }*/

    public String getFirebaseid() {
        return firebaseid;
    }

    public void setFirebaseid(String firebaseid) {
        this.firebaseid = firebaseid;
    }

    public String getContactname() {
        return contactname;
    }

    public void setContactname(String contactname) {
        this.contactname = contactname;
    }

    public String getContactnumber() {
        return contactnumber;
    }

    public void setContactnumber(String contactnumber) {
        this.contactnumber = contactnumber;
    }
}
