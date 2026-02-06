package com.epita.creeps;

import kong.unirest.core.Unirest;

public class Program {

    public static void main(String[] args) {
        System.out.println("Hello World!");
        String serverUri = "http://" + args[0] + ":" + args[1];
        String init = serverUri + "/init/" + args[2];
        Unirest.post(init).body("{}").asJson();
    }
}
