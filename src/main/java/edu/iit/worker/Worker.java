/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iit.worker;

import com.amazonaws.services.sqs.model.Message;
import static edu.iit.credentials.Credentials.THEPATH;
import edu.iit.doa.DOA;
import edu.iit.model.User_Jobs;
import edu.iit.rabbitmq.Receive;
import edu.iit.rabbitmq.Send;
import edu.iit.sendmail.SendEmail;
import edu.iit.sqs.SendQueue;
import edu.iit.walrus.Walrus;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author supramo
 */
public class Worker{
    Send sendqueue = new Send();
    DOA doa = new DOA();
    Walrus walrus = new Walrus();
    //String queuename = ;
    
    public Worker(){
        /*String ipaddress;
        try {
            ipaddress = Inet4Address.getLocalHost().getHostAddress();
            
            DOA doa = new DOA();
            //this.queuename = "sai3";
            this.queuename = doa.getEc2Queue(ipaddress);
            while (this.queuename.isEmpty() || this.queuename == null){
               this.queuename = doa.getEc2Queue(ipaddress);
               System.out.println("Queue name is not mapped, waiting for one");
               Thread.sleep(5000);
            }
                
            System.out.println(ipaddress+":"+this.queuename);
            
        } catch (Exception ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
            this.queuename = "sai3";
        }*/
        
    }
    
    /*
    public boolean checkForMessages(){
        
        //this.queuename = "https://sqs.us-east-1.amazonaws.com/961412573847/sai4";
        
        return receivequeue.getMessage();
        
    }*/
    
    
    
    
    public void getInputFile(String filelink) {
        try {
            Runtime r = Runtime.getRuntime();
            Logger.getLogger(Worker.class.getName()).log(Level.WARNING,filelink);
            walrus.downloadObject("sat-hadoop", filelink);
            r.exec("cp "+THEPATH+filelink+" " +THEPATH+"inputfile  ").waitFor();
        } catch (IOException|InterruptedException ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.WARNING,"Problem downloading the bucket");
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void getJar(String jar){
        try {
            Runtime r = Runtime.getRuntime();
            Logger.getLogger(Worker.class.getName()).log(Level.WARNING,jar);
            walrus.downloadObject("sat-jobs", jar);
            r.exec("cp "+THEPATH+jar+" /root/").waitFor();
        } catch (Exception ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.WARNING,"Problem downloading the bucket");
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public String renameAndUploadOutput(User_Jobs job){
        String filename = "";
        try {
            Runtime r = Runtime.getRuntime();
            filename = THEPATH+"output" + System.currentTimeMillis();
            r.exec("mv "+THEPATH+"output "+filename).waitFor();
            r.exec("/usr/bin/zip -r "+filename+".zip "+filename).waitFor();
            walrus.putObject("sat-hadoop", filename+".zip");
            job.setOutputurl(filename+".zip");
            job.setJobstatus("COMPLETE");
            doa.updateJob(job);
            
        } catch (IOException|InterruptedException ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.WARNING,"Problem downloading the bucket");
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }
        return filename+".zip";
    }
    
    
    /*
    public Message getMessages(){
        return sendq.getMessage();
    }
    */
    public User_Jobs getUserJob(String jobid){
        return doa.getUserJob(jobid);
    }
    
    
    /*
    public void deleteMessage(Message message,User_Jobs job){
        sendq.deleteMessage(message, this.queuename);
        job.setJobstatus("COMPLETE");
        doa.updateJob(job);
    }*/
    
    public void sendmail(User_Jobs job,String filename) {
        String to = job.getUserid();
        String message = "Your job is complete, The output is in the file "+filename;
        new SendEmail().sendmail("hajek@sat.iit.edu", to,message);
    }
    
    public List getSlaves(int i){
        return doa.getSlaves(i);
    }
    
    public void releaseSlaves(List slaves){
        for (int i=0;i<slaves.size();i++){
            doa.updateSlave((String)slaves.get(i), "a");
        }
    }
    
    public boolean makeSlaves(List slaves){
        Map<String, String> env = System.getenv();
        String home = env.get("HOME");
        try {
            String master = Inet4Address.getLocalHost().getHostAddress();
            for (int i=0;i<slaves.size();i++){
                Runtime r = Runtime.getRuntime();
                r.exec(home+"/BackendWorker/syncmaster "+(String)slaves.get(i) +" "+master).waitFor();
            }
            for (int i=0;i<slaves.size();i++){
                Runtime r = Runtime.getRuntime();
                r.exec("ssh /etc/hosts root@"+(String)slaves.get(i)+":/etc/hosts").waitFor();
            }
        }
        catch(Exception e){
                e.printStackTrace();
                return false;
        }
        return true;
    }
    
    public void addSlavesToCluster(List slaves){
        Map<String, String> env = System.getenv();
        String home = env.get("HOME");
        try{
            Runtime r = Runtime.getRuntime();
            System.out.println("adding master");
            File file = new File(home + "/hadoop-2.6.0/etc/hadoop/slaves");
            System.out.println(file.getAbsolutePath());
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write("master");
            //r.exec("/bin/echo master > "+ home + "/hadoop-2.6.0/etc/hadoop/slaves").waitFor();
            for (int i=0;i<slaves.size();i++){
                //r.exec("/bin/echo " +(String)slaves.get(i)+ ">> "+ home +"/hadoop-2.6.0/etc/hadoop/slaves").waitFor();
                output.write("\n"+(String)slaves.get(i));
            }
            output.close();
            System.out.println("added slaves");
        }
        catch(Exception e){
            System.out.println(" unable to add");
        }
        
    }
    
    public void makeCurrentNodeMaster(){
        Map<String, String> env = System.getenv();
        String home = env.get("HOME");
        try{
            Runtime r = Runtime.getRuntime();
            System.out.println("adding master");
            File file = new File(home + "/hadoop-2.6.0/etc/hadoop/masters");
            System.out.println(file.getAbsolutePath());
            BufferedWriter output = new BufferedWriter(new FileWriter(file));
            output.write("master");
            output.close();
        }
        catch(Exception e){
            System.out.println(" unable to add");
        }
    }
    
}

