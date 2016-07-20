/*
 * Copyright (C) 2016 ai
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jevis.emaildatasource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author ai
 */
public class EMailManager {

    private static void testMethod(InputStream inputStream, int number, String partName) {
        // only for test****************************************

        File f = new File("/home/bi/NetBeansProjects/JEDataCollector-Email-Driver/docs/" + partName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EMailManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte[] buf = new byte[4096];
        int bytesRead;
        try {
            while ((bytesRead = inputStream.read(buf)) != -1) {
                fos.write(buf, 0, bytesRead);
            }
        } catch (IOException ex) {
            Logger.getLogger(EMailManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            fos.close();
        } catch (IOException ex) {
            Logger.getLogger(EMailManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        

//        OutputStream outputStream = null; // only for test
//        try {
//            outputStream
//                    = new FileOutputStream(new File("/home/bi/NetBeansProjects/JEDataCollector-Email-Driver/docs/datei" + number));
//        } catch (FileNotFoundException ex) {
//            Logger.getLogger(EMailManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//        int read = 0;
//        byte[] bytes = new byte[1024];
//
//        try {
//            while ((read = inputStream.read()) != -1) {
//                outputStream.write(bytes, 0, read);
//            }
//        } catch (IOException ex) {
//            Logger.getLogger(EMailManager.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    public static List<InputStream> getAnswerList(MessageFilter filter, IEMailConnection conn) {
        List<InputStream> input = new ArrayList<>();

        Folder folder = conn.getFolder();
        List<Message> messages = getMessageList(folder, filter);

        for (Message message : messages) {
            try {
                if (message.isMimeType("multipart/*")) {
                    Multipart multiPart = (Multipart) message.getContent();
                    // For all multipart contents
                    for (int i = 0; i < multiPart.getCount(); i++) {
                        MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
                        // If multipart content is attachment
                        String disp = part.getDisposition();
                        
                        String partName = part.getFileName();
                        
                        if (!Part.ATTACHMENT.equalsIgnoreCase(disp)
                                && !StringUtils.isNotBlank(partName)) {
                            continue; // dealing with attachments only
                        }

                        if (Part.ATTACHMENT.equalsIgnoreCase(disp) || disp == null) {
                            System.out.println("EMail attach: " + " " + part.getFileName() + " !///! " + part.getContentType());
                            input.add(part.getInputStream());
                        }
                    }
                }
            } catch (MessagingException | IOException ex) {
                Logger.getLogger(EMailDataSource.class.getName()).log(Level.SEVERE, "could not process the attachment!", ex);
            }
        }

        ///*** to test
        for (int i = 0; i < input.size(); i++) {
            testMethod(input.get(i), i, "mydatei"+i+".csv");
        }
        /// end test
        return input;
    }

    private static List<Message> getMessageList(Folder folder, MessageFilter filter) {

        List<Message> messageList = null;
        try {
            folder.open(Folder.READ_ONLY);
            messageList = Arrays.asList(folder.search(filter.getSearchTerms()));
        } catch (MessagingException ex) {
            Logger.getLogger(EMailManager.class.getName()).log(Level.SEVERE, "Unable to search for messages", ex);
        }
        return messageList;
    }

    public static IEMailConnection createConnection(EMailServerParameters parameters) {

        Properties props = createProperties(parameters);
        Session session = Session.getInstance(props);
        if (parameters.getProtocol().equalsIgnoreCase(EMailConstants.Protocol.IMAP)) {
            IEMailConnection conn = new IMAPConnection();
            conn.setConnection(session, parameters);
            return conn;
        } else if (parameters.getProtocol().equalsIgnoreCase(EMailConstants.Protocol.IMAP)) {
            return null;//new POP3Connection(parameters);
        } else {
            Logger.getLogger(EMailConnection.class.getName()).log(Level.SEVERE, "EMail Connection failed");
            return null; //!!!!!!!!!!
        }
    }

    public static void terminate(IEMailConnection conn) {
        conn.terminate();
    }

    private static Properties createProperties(EMailServerParameters parameters) {

        Properties props = new Properties();
        String key = "mail." + parameters.getProtocol();
        System.out.println("Key is: " + key);
        props.put(key + ".host", parameters.getHost());
        props.put(key + ".port", parameters.getPort());
        props.put(key + ".connectiontimeout", parameters.getConnectionTimeout()); //*1000?ms
        props.put(key + ".timeout", parameters.getReadTimeout());    //*1000?ms

//        String ssl = parameters.getSsl();
//        if (ssl.equals(EMailConstants.ValidValues.CryptProtocols.SSL_TLS)) {
//            props.put(key + ".ssl.enable", true);
//        } else if (ssl.equals(EMailConstants.ValidValues.CryptProtocols.STARTTLS)) {
//            props.put(key + "starttls.enable", true);
//        }
        //_parameters.getAuthentication() usually not used in SSL connections
        props.put("mail.debug", "true");
        props.put("mail.store.protocol", "imaps");
        return props;
    }
}
