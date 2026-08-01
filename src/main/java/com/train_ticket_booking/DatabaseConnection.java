package com.train_ticket_booking;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/train_ticket_booking";

    private static final String USER =
            "root";

    private static final String PASSWORD =
            "sqlismine@123";

    public static Connection getConnection() throws Exception {

        return DriverManager.getConnection(
                URL,
                USER,
                PASSWORD
        );
    }
}
