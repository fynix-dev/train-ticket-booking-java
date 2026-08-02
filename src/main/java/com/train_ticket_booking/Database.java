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
                       "WHERE R1.station_id = ? AND R2.station_id = ?";
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

    int getFreeSeats(String trainNumber, String date, String stationId){
        String query = "SELECT T1.seats, T2.seat_count FROM station_to_seat_mapping AS T1 JOIN train AS T2 ON T1.train_id = T2.train_id WHERE T1.train_id = ? AND T1.station_id = ? AND T1.date = ?";
        try {
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, trainNumber);
            pstmt.setString(2, stationId);
            pstmt.setString(3, date);
            ResultSet res = pstmt.executeQuery();

            while(res.next()){
                int count = res.getString("seats").replaceAll("^\\[|\\]$", "").trim().isEmpty()
                    ? 0
                    : res.getString("seats").split(",").length;
                int totalSeats = Integer.parseInt(res.getString("seat_count"));
                return totalSeats - count;
            }
        } catch (Exception e) {
            System.out.print("Error occurred: " + e);
            return 0;
        }

        try {
            // Use PreparedStatement to prevent SQL injection
            String sql = "SELECT seat_count FROM train WHERE train_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, trainNumber);

            ResultSet res = pstmt.executeQuery();

            if (res.next()) {
                return res.getInt("seat_count");
            }
        } catch (Exception e) {
            System.out.println("Error occurred: " + e);
            return 0;
        }
        return 0; // Return 0 if no rows found
    }
}
