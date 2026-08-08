package com.train_ticket_booking;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.sql.PreparedStatement;
import java.util.UUID;

class Database{
    Connection conn;
    String userId;

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

    boolean addStationDetailsToSql(ArrayList<String> stationsBetween, int seatsToBook, String trainNumber, String date, String stationFrom,  String stationTo, String userId){
        try {
            // Use PreparedStatement to prevent SQL injection
            String sqlInsert = "insert into station_to_seat_mapping (train_id, station_id, seats, date) values(?, ?, ?, ?)";
            String sqlSelect = "select seats from station_to_seat_mapping where train_id = ? and station_id = ? and date = ?";
            String sqlUpdate = "update station_to_seat_mapping set seats = ? where train_id = ? and station_id = ? and date = ?";
            String sqlBookedSeats = "select seats from station_to_seat_mapping where station_id in ";
            //get all the booked seats and find the common free seats
            Statement pstmtBookedSeats = conn.createStatement();
            String stations = "('" + String.join("','", stationsBetween) + "')";

            ResultSet resSeats = pstmtBookedSeats.executeQuery(sqlBookedSeats + stations + " and train_id='" + trainNumber + "' and date='" + date + "'");
            ArrayList<Integer> bookedSeatsAcrossJourney = new ArrayList<>();

            while(resSeats.next()){
                String inputList = resSeats.getString("seats").replace("[", "").replace("]", "");
                ArrayList<Integer> seats = new ArrayList<>();
                if(!inputList.trim().isEmpty()){
                    for(String num : inputList.split(",")){
                        seats.add(Integer.parseInt(num.trim()));
                    }
                }

                //after for loop we have the seats booked at that station
                Set<Integer> uniqueSet = new LinkedHashSet<>(bookedSeatsAcrossJourney);
                uniqueSet.addAll(seats);

                bookedSeatsAcrossJourney = new ArrayList<>(uniqueSet);
            }

            // now we got the commonly booked seat along the entire journey, now allot seat to the user which is not booked
            int totalSeats = 15;
            if(seatsToBook >= (totalSeats - bookedSeatsAcrossJourney.size())){
                System.out.println("Not enough seats available");
                return false;
            }
            int seatsToBeBooked = seatsToBook;
            ArrayList<Integer> allotedSeats = new ArrayList<>();
            for(int seat = 1; seat <= totalSeats && seatsToBeBooked > 0; seat++){
                if(!bookedSeatsAcrossJourney.contains(seat)){
                    allotedSeats.add(seat);
                    seatsToBeBooked--;
                }
            }
            System.out.println("Seats alloted to user are: " + allotedSeats);

            //go through each station id and add it to sql
            for(int i = 0; i < stationsBetween.size() - 1; i++){
                //check if the row with station id and date is already created
                // if yes then just update it

                PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect);
                pstmtSelect.setString(1, trainNumber);
                pstmtSelect.setString(2, stationsBetween.get(i));
                pstmtSelect.setString(3, date);

                ResultSet res = pstmtSelect.executeQuery();

                if(res.next()){
                    //if it goes inside while, then we have to update the already created rows
                    PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate);

                    String prevSeats = res.getString("seats").replace("[", "").replace("]", "");
                    ArrayList<Integer> finalSeats = new ArrayList<>();
                    if(!prevSeats.trim().isEmpty()){
                        for(String num: prevSeats.split(",")){
                            finalSeats.add(Integer.parseInt(num.trim()));
                        }
                    }
                    finalSeats.addAll(allotedSeats);
                    Collections.sort(finalSeats);

                    pstmtUpdate.setString(1, finalSeats.toString());
                    pstmtUpdate.setString(2, trainNumber);
                    pstmtUpdate.setString(3, stationsBetween.get(i));
                    pstmtUpdate.setString(4, date);

                    //update the table feild
                    pstmtUpdate.executeUpdate();

                }else{
                    //if it comes into else part then it means we have to create a new row
                    PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert);
                    pstmtInsert.setString(1, trainNumber);
                    pstmtInsert.setString(2, stationsBetween.get(i));
                    pstmtInsert.setString(3, allotedSeats.toString());
                    pstmtInsert.setString(4, date);
                    //update the table
                    pstmtInsert.executeUpdate();
                }

            }

            addBookingDetails(trainNumber, stationFrom, stationTo, allotedSeats.toString(), "confirm", userId, date);
            // return true;
            return true;
        } catch (Exception e) {
            System.out.println("Error occurred: " + e);
            return false;
        }
    }

    String generatePnrNumber(){
        // 1. Generate a random UUID (Version 4)
        UUID uuid = UUID.randomUUID();

        // 2. Convert to string and remove hyphens
        String uuidString = uuid.toString().replace("-", "");

        // 3. Convert the 32-character hex string to a BigInteger (base 16)
        // This creates a massive unique decimal number
        java.math.BigInteger bigInt = new java.math.BigInteger(uuidString, 16);

        // 4. Convert to string and take the last 6 characters
        String numericCode = bigInt.toString().substring(bigInt.toString().length() - 8);

        return numericCode;
    }

    void addBookingDetails(String trainNumber, String stationFrom, String stationTo, String allotedSeats, String status, String userName, String date){
        String query = "insert into booking (booking_id, train_id, user_id, from_station, to_station, seats, status, date) values(?, ?, ?, ?, ?, ?, ?, ?)";
        String bookingId = generatePnrNumber();
        try {
            // add the data into the table
            PreparedStatement pstmtInsert = conn.prepareStatement(query);
            pstmtInsert.setString(1, bookingId);
            pstmtInsert.setString(2, trainNumber);
            pstmtInsert.setString(3, userName);
            pstmtInsert.setString(4, stationFrom);
            pstmtInsert.setString(5, stationTo);
            pstmtInsert.setString(6, allotedSeats);
            pstmtInsert.setString(7, status);
            pstmtInsert.setString(8, date);

            //update the table
            pstmtInsert.executeUpdate();
        } catch (Exception e) {
            System.out.println("Error occured while adding data to booking table: " + e);
        }
    }

    boolean login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet res = pstmt.executeQuery();
            if(res.next()){
                userId = res.getString("username");
                System.out.println(userId);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    boolean signup(String username, String password) {
        try {
            String check = "SELECT * FROM users WHERE username=?";
            PreparedStatement pstmt = conn.prepareStatement(check);
            pstmt.setString(1, username);

            ResultSet res = pstmt.executeQuery();

            if(res.next()){
                System.out.println("Username already exists.");
                return false;
            }

            String insert = "INSERT INTO users(username,password) VALUES(?,?)";
            pstmt = conn.prepareStatement(insert);
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            pstmt.executeUpdate();
            userId = username;
            return true;

        } catch(Exception e){
            System.out.println(e);
            return false;
        }
    }

    ResultSet fetchUserBookings(String userId){
        String query = "select * from booking where user_id = ?";
        try {
            int type = ResultSet.TYPE_SCROLL_INSENSITIVE;
            int concurrency = ResultSet.CONCUR_READ_ONLY;
            PreparedStatement pstmt = conn.prepareStatement(query, type, concurrency);
            pstmt.setString(1, userId);
            ResultSet res =  pstmt.executeQuery();
            return res;
        } catch (Exception e) {
            System.out.println("Error occured while fetching user booking: " + e);
            return null;
        }
    }
}
