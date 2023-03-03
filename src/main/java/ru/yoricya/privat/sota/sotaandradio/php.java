package ru.yoricya.zslogin.srv;

import org.json.JSONObject;

import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ThreadLocalRandom;

public class php {
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);
            String hashtext = no.toString(16);
            while (hashtext.length() < 32) {
                hashtext = "0" + hashtext;
            }
            return hashtext;
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    public static String file_get_contents(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        StringBuilder builder = new StringBuilder();
        String line;

        // For every line in the file, append it to the string builder
        while((line = reader.readLine()) != null)
        {
            if(line.equalsIgnoreCase("\n")){
                builder.append("\n");
            } else if (line.equalsIgnoreCase("")) {
                builder.append("\n");
            }else{
                builder.append(line);
            }
        }

        reader.close();
        return builder.toString();
    }
    public static boolean file_put_contents(String filename, String data){
        try {
            FileWriter fstream = new FileWriter(filename,false);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(data);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    public static void echo(String msg){
        System.out.print(msg);
    }
    public static void echo(int msg){
        System.out.print(msg);
    }
    public static boolean file_put_contents(String filename, String data, boolean apn){
        try {
            FileWriter fstream = new FileWriter(filename,apn);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(data);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    public static int rand(int min, int max){
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    public static boolean rand(){
        if(ThreadLocalRandom.current().nextInt(1, 2 + 1) == 1){
            return true;
        }
        return false;
    }
}