package ru.yoricya.privat.sota.sotaandradio;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        var a = new FileInputStream(filename);
        var b = a.readAllBytes();
        a.close();
        return new String(b);
    }
    public static boolean if_file_exs(String filename) {
        File file = new File(filename);
        return file.exists() && !file.isDirectory();
    }
    public static boolean if_dir_exs(String dir){
        Path path = Paths.get(dir);
        return  (Files.exists(path));
    }
    public static void mkdir(String dir){
        new File(dir).mkdirs();
    }
    public static boolean file_put_contents(String filename, String data){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    FileWriter fstream = new FileWriter(filename,false);
                    BufferedWriter out = new BufferedWriter(fstream);
                    out.write(data);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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