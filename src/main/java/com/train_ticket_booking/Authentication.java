package com.train_ticket_booking;

import java.util.Scanner;

class Authentication {

    Database db = new Database();

    boolean authenticate() {

        Scanner sc = new Scanner(System.in);

        System.out.println("1. Login");
        System.out.println("2. Signup");
        System.out.print("Enter option: ");

        int opt = sc.nextInt();
        sc.nextLine();

        System.out.print("Username: ");
        String username = sc.nextLine();

        System.out.print("Password: ");
        String password = sc.nextLine();

        if(opt == 1){
            if(db.login(username,password)){
                System.out.println("Login Successful\n");
                return true;
            }else{
                System.out.println("Invalid Username or Password");
                return false;
            }
        }

        if(opt == 2){
            if(db.signup(username,password)){
                System.out.println("Signup Successful\n");
                return true;
            }
        }

        return false;
    }
}
