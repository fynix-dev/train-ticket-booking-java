package com.train_ticket_booking;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import java.sql.PreparedStatement;

class Database{
    Connection conn;

    Database() {
        try {
            conn = DatabaseConnection.getConnection();
        } catch (Exception e) {
            System.out.print("Error occurred while estabblishing connection with database : " + e);
        }
    }


    ResultSet getTrainDetailsBtwStations(String from, String to) {
        String query = "SELECT R1.train_id, R1.station_id, R1.departure_time, R2.arrival_time " +
                       "FROM routes AS R1 " +
                       "INNER JOIN routes AS R2 ON R1.train_id = R2.train_id " +
                       "WHERE R1.station_id = ? AND R2.station_id = ? AND CAST(SUBSTRING(R1.route_id FROM 2) AS UNSIGNED) < CAST(SUBSTRING(R2.route_id FROM 2) AS UNSIGNED)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, from);
            pstmt.setString(2, to);
            return pstmt.executeQuery();
        } catch (Exception e) {
            System.out.print("Error occurred: " + e);
            return null;
        }
    }


    ResultSet getTrainNameFromId(String train_id) {
        String query = "SELECT train_name FROM train WHERE train_id = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, train_id);
            return pstmt.executeQuery();
        } catch (Exception e) {
            System.out.print("Error occurred: " + e);
            return null;
        }
    }

    ArrayList<String> getListOfStations(){
        ArrayList<String> stations = new ArrayList<>();
        String query ="select distinct station.station_id from routes as station order by station.station_id";

        try {
            Statement stmt = conn.createStatement();
            ResultSet res =  stmt.executeQuery(query);
            while(res.next()){
                stations.add(res.getString("station_id"));
            }
            return stations;
        } catch (Exception e) {
            System.out.print("Error occurred: " + e);
            return null;
        }
    }

    ArrayList<Integer> getFreeSeats(String trainNumber, String date, String stationId){
        String query = "SELECT T1.seats, T2.seat_count FROM station_to_seat_mapping AS T1 JOIN train AS T2 ON T1.train_id = T2.train_id WHERE T1.train_id = ? AND T1.station_id = ? AND T1.date = ?";
        //first index free seats, second index booked seats
        ArrayList<Integer> result = new ArrayList<>();
        try {
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, trainNumber);
            pstmt.setString(2, stationId);
            pstmt.setString(3, date);
            ResultSet res = pstmt.executeQuery();

            while(res.next()){
                int totalSeats = Integer.parseInt(res.getString("seat_count"));
                if(res.getString("seats").replaceAll("^\\[|\\]$", "").trim().isEmpty()){
                    result.add(totalSeats);
                    result.add(0);
                    return result;
                }
                int count = res.getString("seats").split(",").length;
                result.add(totalSeats - count);
                result.add(count);
                return result;
            }
        } catch (Exception e) {
            System.out.print("Error occurred: " + e);
            result.add(0);
            result.add(0);
            return result;
        }

        try {
            // Use PreparedStatement to prevent SQL injection
            String sql = "SELECT seat_count FROM train WHERE train_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, trainNumber);

            ResultSet res = pstmt.executeQuery();

            if (res.next()) {
                result.add(res.getInt("seat_count"));
                result.add(0);
                return result;
            }
        } catch (Exception e) {
            System.out.println("Error occurred: " + e);
            result.add(0);
            result.add(0);
            return result;
        }
        result.add(0);
        result.add(0);
        return result; // Return 0 if no rows found
    }

    ArrayList<String> getStationsBetween(String trainNumber, String stationFrom,  String stationTo){
        String query = "select * from routes where train_id = ? order by cast(substring(route_id, 2) as unsigned);";

        try {
            ArrayList<String> stationsBetween = new ArrayList<>();
            ArrayList<String> allStations = new ArrayList<>();
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, trainNumber);
            ResultSet res =  pstmt.executeQuery();

            while(res.next()){
                allStations.add(res.getString("station_id"));
            }

            int startIndex = allStations.indexOf(stationFrom);
            int endIndex = allStations.indexOf(stationTo);

            if(startIndex == -1 || endIndex == -1){
                System.out.print("Start and end stations not found");
                return null;
            }

            if(startIndex <= endIndex){
                for(int i = startIndex; i <= endIndex; i++){
                    stationsBetween.add(allStations.get(i));
                }
            }else{
                for(int i = startIndex; i >= endIndex; i--){
                    stationsBetween.add(allStations.get(i));
                }
            }

            return stationsBetween;
        } catch (Exception e) {
            System.out.print("Error occurred: " + e);
            return null;
        }
    }

    boolean addStationDetailsToSql(ArrayList<String> stationsBetween, int seatsToBook, String trainNumber, String date, String stationTo){
        try {
            // Use PreparedStatement to prevent SQL injection
            String sqlInsert = "insert into station_to_seat_mapping (train_id, station_id, seats, date) values(?, ?, ?, ?)";
            String sqlSelect = "select seats from station_to_seat_mapping where train_id = ? and station_id = ? and date = ?";
            String sqlUpdate = "update station_to_seat_mapping set seats = ? where train_id = ? and station_id = ? and date = ?";

            //go through each station id and add it to sql
            for(int i = 0; i < stationsBetween.size() - 1; i++){
                int flag = 0;
                //check if the row with station id and date is already created
                // if yes then just update it
                try {
                    //get total seats to be added
                    int totalSeatsToBeAdded = getFreeSeats(trainNumber, date, stationsBetween.get(i)).get(1) + seatsToBook;
                    //add the seats numbers to the list
                    ArrayList<Integer> seats = new ArrayList<>();
                    for(int j = 1; j <= totalSeatsToBeAdded; j++){
                        seats.add(j);
                    }

                    PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect);
                    pstmtSelect.setString(1, trainNumber);
                    pstmtSelect.setString(2, stationsBetween.get(i));
                    pstmtSelect.setString(3, date);

                    ResultSet res = pstmtSelect.executeQuery();

                    while(res.next()){
                        //if it goes inside while, then we have to update the already created rows
                        PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate);

                        pstmtUpdate.setString(1, seats.toString());
                        pstmtUpdate.setString(2, trainNumber);
                        pstmtUpdate.setString(3, stationsBetween.get(i));
                        pstmtUpdate.setString(4, date);

                        //update the table feild
                        pstmtUpdate.executeUpdate();
                        System.out.print("\nFound the row already, so just updated it");
                        flag = 1;
                    }
                    //if already updated then do not create
                    if(flag == 1){
                        continue;
                    }

                    //if it comes out of while loop then it means we have to create a new row
                    PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert);
                    pstmtInsert.setString(1, trainNumber);
                    pstmtInsert.setString(2, stationsBetween.get(i));
                    pstmtInsert.setString(3, seats.toString());
                    pstmtInsert.setString(4, date);
                    //update the table
                    System.out.print("\nNo rows already found, so creating new one");
                    pstmtInsert.executeUpdate();
                } catch (Exception e) {
                    System.out.print("Error occured while checking if row is already created: " + e);
                    return false;
                }

            }
            return true;

        } catch (Exception e) {
            System.out.println("Error occurred: " + e);
            return false;
        }
    }
}
