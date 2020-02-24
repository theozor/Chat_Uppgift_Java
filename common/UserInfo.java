/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import java.io.Serializable;
import java.util.ArrayList;

/**
 *
 * @author Elev
 */
public class UserInfo implements Serializable {

    private String name;
    private static ArrayList<String> oldNames;

    public UserInfo() {
        name = "";
        oldNames = new ArrayList<>();
    }

    private UserInfo(UserInfo userInfo) {
        name = userInfo.getName();
        oldNames = userInfo.getOldNames();
    }

    public UserInfo(String name) {
        this.name = name;
        oldNames = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        oldNames.add(this.name);
        this.name = name;
    }

    public UserInfo getCopy() {
        return new UserInfo(this);
    }

    public ArrayList<String> getOldNames() {
        return oldNames;
    }

    @Override
    public String toString() {
        return name;
    }
}
