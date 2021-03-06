/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.iit.sendmail;

/**
 *
 * @author supramo
 */
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class SendEmail
{
   public void sendmail(String from , String to, String messagetext)
   {    

      // Assuming you are sending email from localhost
      String host = "localhost";

      // Get system properties
      Properties properties = System.getProperties();

      // Setup mail server
      properties.setProperty("mail.smtp.host", host);

      // Get the default Session object.
      Session session = Session.getDefaultInstance(properties);

      try{
         // Create a default MimeMessage object.
         MimeMessage message = new MimeMessage(session);

         // Set From: header field of the header.
         message.setFrom(new InternetAddress("sat-hadoop@iit.edu"));

         // Set To: header field of the header.
         message.addRecipient(Message.RecipientType.TO,
                                  new InternetAddress(to));

         // Set Subject: header field
         message.setSubject("Job results");

         // Now set the actual message
         message.setText(messagetext);

         // Send message
         Transport.send(message);
         System.out.println("Sent message successfully...." + message.toString());
      }catch (MessagingException mex) {
         mex.printStackTrace();
      }
   }
}