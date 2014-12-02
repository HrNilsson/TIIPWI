/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co2emissionindicator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

/**
 *
 * @author ReneNilsson
 */
public class CO2EmissionIndicator {

    private static Process processRun = null;
    private static int runningCO2Average = 0;
    private static int[] dailyCO2Values = new int[24];
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        CO2EmissionIndicator tool = new CO2EmissionIndicator();
        boolean running = true;
        
        while(running){
            dailyCO2Values = tool.downloadCO2Data();
            runningCO2Average = tool.updateRunningAverage(dailyCO2Values);
            if(tool.getCurrentCO2Value() > runningCO2Average + 10) {
                System.out.println("CO2 levels are high!");
                tool.runSequenceExecuterTool("Red");
            } else if (tool.getCurrentCO2Value() < runningCO2Average - 10) {
                System.out.println("CO2 levels are low!.");
                tool.runSequenceExecuterTool("Green");
            } else {
                System.out.println("CO2 levels somewhere in the middle. No need to change. Hysteresis.");
            }

            try {
                Thread.sleep(1000*60);//*15); // Sleep 15 minutes
            } catch (InterruptedException ex) {
                System.out.println(ex);
                running = false;
            }
        }
    }
    
    private int[] downloadCO2Data() {
        String server = "ftp.energinet.dk";
        int port = 21;
        String user = "endkftp";
        String pass = "martin$!aston";
 
        FTPClient ftpClient = new FTPClient();
        try {
 
            ftpClient.connect(server, port);
            ftpClient.login(user, pass);
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            
            String date = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
            String remoteFile = "/CO2Prognoser/"+date+"_CO2prognose.txt";
            InputStream inputStream = ftpClient.retrieveFileStream(remoteFile);
            int[] co2 = parseFtpFile(inputStream);
            boolean success = ftpClient.completePendingCommand();
            if (success) {
                System.out.println("Sucessfully retrieved and parsed CO2 data.");
            }
            inputStream.close();
            
            return co2;
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private int[] parseFtpFile(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        bufferedReader.readLine();
        bufferedReader.readLine();
        int co2[] = new int[24];
        String line = "";
        int i = 0;
        while ((line = bufferedReader.readLine()) != null) {
            if(i>24) {
                System.out.println("Parsing failed. More than 24 entries.");
                return null;
            }
            String[] entry = line.split(";");
            co2[i] = Integer.parseInt(entry[1]);
            i++;
            
        }
        return co2;
    }
    
    private int updateRunningAverage(int[] co2) {
        // This should probably be more fancy...
        int dailyAverage = 0;
        for(int value: co2) {
            dailyAverage += value;
        }
        return dailyAverage /= 24;
    }
    
    private int getCurrentCO2Value() {
        String hh = new SimpleDateFormat("HH").format(Calendar.getInstance().getTime());
        int hour = Integer.parseInt(hh);
        
        return dailyCO2Values[hour];
    }
    
    private void runSequenceExecuterTool(String color){
        try {
            if(processRun != null)
                processRun.destroy();
            
            System.out.println("Running sequence: " + color);
           // processRun = Runtime.getRuntime().exec("java -Dlogback.configurationFile=file:logback.xml -jar Sequence_Executer_Tool.jar -enableGUI false -toolSettingsPath settings.xml -XMLscript LedSet"+color+".xml");
            String jcommand = "java -Dlogback.configurationFile=file:logback.xml -jar Sequence_Executer_Tool.jar -enableGUI false -toolSettingsPath settings.xml -XMLscript LedSet"+color+".xml";
            processRun = Runtime.getRuntime().exec("cmd.exe /c cd \""+new File("newDir")+"\" & start cmd.exe /k "+jcommand);
  
           /* 
            printLines(" stdout:", processRun.getInputStream());
            printLines(" stderr:", processRun.getErrorStream());
            */
            /*
            File file = new File("Sequence_Executer_Tool.jar"); 
            URL[] urls = { file.toURI().toURL() };  
            URLClassLoader loader = new URLClassLoader(urls);  
            
            JarFile jarFile = new JarFile(file);  
            Manifest manifest = jarFile.getManifest(); // warning: can be null  
            Attributes attributes = manifest.getMainAttributes();  
            String className = attributes.getValue(Attributes.Name.MAIN_CLASS);
            
            Class<?> cls = loader.loadClass(className); // replace the complete class name with the actual main class  
            Method main;
            main = cls.getDeclaredMethod("main", String[].class);
            
            Object[] args = new String[1];//new String[1];
            args[0] = "false";
            //args[0] = "-enableGUI false -toolSettingsPath settings.xml -XMLscript LedSet"+color+".xml"; 
                   //"enableGUI false", "toolSettingsPath settings.xml", "XMLscript LedSet"+color+".xml"};
             
            main.invoke(null, args);*/
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private void printLines(String name, InputStream ins) throws Exception {
        String line = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(ins));
        while ((line = in.readLine()) != null) {
            System.out.println(name + " " + line);
        }
    }
    
}
