package espero.jiofibatterynotifier.Classes;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;


/**
 * Created by parvesh_dhull on 9/2/17.
 */

public class SharedPrefMain {

    private static String engwingoSharedPrefMain = "esperoJioFiSharedPref";
    private static String uid ;
    Context context;
    SharedPreferences sharedpreferences;
    SharedPreferences.Editor editor;
    Gson gson;


    public SharedPrefMain(Context context) {


        try {
            this.context = context;
            sharedpreferences = context.getSharedPreferences(engwingoSharedPrefMain, Context.MODE_PRIVATE);
            editor = sharedpreferences.edit();
            gson = new Gson();
            uid = sharedpreferences.getString("uid", "");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public void setBoolean(String name,Boolean value){
        try {
            editor.putBoolean("myBooleanNew"+ name, value);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean getBoolean(String name){
        try {
            return sharedpreferences.getBoolean("myBooleanNew"+ name , true);
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
    public void setString(String name,String value){
        try {
            editor.putString("myStringNew"+ name, value);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String getString(String name) {
        try {
            return sharedpreferences.getString("myStringNew"+ name , "");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    public void setInt(String name,int value){
        try {
            editor.putInt("myIntNew"+ name, value);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public int getInt(String name) {
        try {
            return sharedpreferences.getInt("myIntNew"+ name , 0);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    public void setLong(String name,Long value){
        try {
            editor.putLong("myLongNew"+ name, value);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public long getLong(String name) {
        try {
            return sharedpreferences.getLong("myLongNew"+ name , 0L);
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        }
    }
}
