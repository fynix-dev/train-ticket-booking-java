package com.train_ticket_booking;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

class Main{
    public static void main(String args[]) throws Exception{

        Map<Integer, String> id_to_station = new HashMap<>();
        id_to_station.put(1, "Delhi");
        id_to_station.put(2, "Bhopal");
        id_to_station.put(3, "Agra");
        id_to_station.put(4, "Kolkatta");
        id_to_station.put(5, "Cherthala");
        id_to_station.put(6, "Ernakulam");

        BookingSystem bs = new BookingSystem();
        //menu for user
        System.out.print("\nEnter\n1 to get train details between 2 stations\n2 to book tickets\n3 to view your booked tickets\n");

        int opt;
        Scanner sc = new Scanner(System.in);

        System.out.print("\nEnter an option:");
        opt = sc.nextInt();

        if(opt == 1){
            //show the available stations and ask input from user
            System.out.print("\n1-Delhi\n2-Bhopal\n3-Agra\n4-Kolkatta\n5-Cherthala\n6-Eranakulam");
            System.out.print("\nEnter the respective number of From and To station: ");
            int f, t;
            f = sc.nextInt();
            t = sc.nextInt();
            bs.getTrainDetailsBtwTwoStations(id_to_station.get(f), id_to_station.get(t));
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
}
