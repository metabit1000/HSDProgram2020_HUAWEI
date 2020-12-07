package com.hsd.contest.spain.feelsafe;

import android.app.Application;

import com.huawei.hms.maps.model.LatLng;

import java.util.ArrayList;

public class GlobalVariables extends Application {
    private static String telf = "112"; //telefono para llamar en caso de emergencia

    public static class ListItem {
        String name;      //nombre del site buscado en SearchActivity
        LatLng location;  //latitud y longitud para ponerlo en el mapa

        public ListItem(String name, LatLng location) {
            this.name = name;
            this.location = location;
        }
    }

    public static ArrayList<ListItem> places = new ArrayList<ListItem>(); //arraylist para añadir los sites encontrados en la busqueda
    private static int selectedPosition = -1; //posicion seleccionada en Search Activity

    public static String getTelf() {
        return telf;
    }

    public static void setTelf(String telf) {
        GlobalVariables.telf = telf;
    }

    public static int getSelectedPosition() {
        return selectedPosition;
    }

    public static void setSelectedPosition(int selectedPosition) {
        GlobalVariables.selectedPosition = selectedPosition;
    }
}
