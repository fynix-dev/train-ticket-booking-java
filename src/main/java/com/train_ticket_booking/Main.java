package com.train_ticket_booking;
import java.util.ArrayList;
import java.util.Scanner;
import java.sql.ResultSet;

class Main{
    public static void main(String args[]) throws Exception{
        BookingSystem bs = new BookingSystem();
        Authentication auth = new Authentication();

        if(!auth.authenticate()){
            return;
        }

        // Existing menu starts here
        System.out.print(
            "\nEnter\n1 to get train details between 2 stations\n" +
            "2 to book tickets\n" +
            "3 to view your booked tickets\n"
        );

        int opt;
        Scanner sc = new Scanner(System.in);

        System.out.print("\nEnter an option: ");
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
            //get the train number that user wants to book
            System.out.printf("\nEnter the Train Number to get the number of free seats and book tickets: ");
            String trainNumber = sc.next();
            System.out.print("\nEnter the date on which you want ticket in YYYY-MM-DD format: ");
            String date = sc.next();
            //show the number of free seats available
            int freeSeats = bs.getFreeSeats(trainNumber, date, stations.get(f-1));
            System.out.print("\nFree seats: " + freeSeats);
            //Get the number of seats to book
            System.out.print("\nEnter the number of seats to be booked: ");
            int seatsToBook = sc.nextInt();
            bs.bookTickets(seatsToBook, trainNumber, stations.get(f-1), stations.get(t-1), date);
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
            System.out.printf("\n====== Available Trains From %s to %s ======\n", from, to);
            System.out.printf("%-15s %-20s %-16s %-12s\n", "Train Number", "Train Name", "Departure Time", "Arrival Time\n");

            while(res.next()){
                String trainName = getTrainName(res.getString("train_id"));
                System.out.printf("%-15s %-20s %-16s %-12s\n",
                    res.getString("train_id"),
                    trainName,
                    res.getString("departure_time"),
                    res.getString("arrival_time")
                );
            }

        } catch (Exception e) {
            System.out.print("Error occured while getting train details: " + e);
        }
    }

    ArrayList<String> getListOfStations(){
        return db.getListOfStations();
    }

    int getFreeSeats(String trainNumber, String date, String stationId){
        System.out.print("\nSelected train number is " + trainNumber + ". To book on " + date);
        int freeSeats = db.getFreeSeats(trainNumber, date, stationId).get(0);
        return freeSeats;
    }

    void bookTickets(int seatsToBook, String trainNumber, String stationFrom, String stationTo, String date){
        //get the stations between from and to
        ArrayList<String> stationsBetween = db.getStationsBetween(trainNumber, stationFrom, stationTo);
        System.out.print(stationsBetween);

        //add each station
        boolean res = db.addStationDetailsToSql(stationsBetween, seatsToBook, trainNumber, date, stationTo);
        if(res){
            System.out.println("\nSuccessfully added the rows");
        }
    }
}
