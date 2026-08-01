package com.train_ticket_booking;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

class Main{
    public static void main(String args[]) throws Exception{
        BookingSystem bs = new BookingSystem();
        //menu for user
        System.out.print("\nEnter\n1 to get train details between 2 stations\n2 to book tickets\n3 to view your booked tickets\n");

        int opt;
        Scanner sc = new Scanner(System.in);

        System.out.print("\nEnter an option:");
        opt = sc.nextInt();

        if(opt == 1){
            //show the available stations and ask input from user
            ArrayList<String> stations = bs.getListOfStations();
            System.out.println();
            for(int i = 0; i < stations.size(); i++){
                System.out.print(i+1 + " - " + stations.get(i) + "\n");
            }
            System.out.print("\nEnter the respective number of From and To station: ");
            int f, t;
            f = sc.nextInt();
            t = sc.nextInt();
            bs.getTrainDetailsBtwTwoStations(stations.get(f-1), stations.get(t-1));
        }else{
            System.out.print("Invalid input");
        }
        sc.close();
    }
}

class BookingSystem{
    Database db;
    BookingSystem(){
        db = new Database();
    }

    String getTrainName(String trainId){
        ResultSet res = db.getTrainNameFromId(trainId);
        if(res == null){
            return "";
        }
        try {
            while(res.next()){
                return res.getString("train_name");
            }
        } catch (Exception e) {
            System.out.print("Error occured while fetching the train name: " + e);
        }

        return "";
    }

    void getTrainDetailsBtwTwoStations(String from, String to){
        ResultSet res = db.getTrainDetailsBtwStations(from, to);
        if(res == null){
            return;
        }
        try {
            System.out.print("====== Avaible Trains From " + from + " to " + to + " ======\n");
            System.out.print("Train Name\tArrival Time\tDeparture Time\n");
            while(res.next()){
                String trainName = getTrainName(res.getString("train_id"));
                System.out.println(trainName + "\t" + res.getString("arrival_time") + "\t" + res.getString("departure_time"));
            }
        } catch (Exception e) {
            System.out.print("Error occured while getting train details: " + e);
        }
    }

    ArrayList<String> getListOfStations(){
        return db.getListOfStations();
    }
}
