/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package co2emissionindicator;

import com.develco.amm.sequenceexecutertool.SequenceExecuterTool;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

/**
 *
 * @author ReneNilsson
 */
public class CO2EmissionIndicator {

    private static int co2EmissionLimit = 340;
    private static int co2EmissionLimitHysteresis = 5;
    private static InputStream input;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {        
        PipedOutputStream pos = new PipedOutputStream();
        System.setOut( new PrintStream(pos, true) );
        System.setErr( new PrintStream(pos, true) );
        
        try {
            input = new PipedInputStream(pos);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        
        CO2EmissionIndicator tool = new CO2EmissionIndicator();
        boolean running = true;
        int retryAttempts = 0;
        int currentCO2EmissionValue = -1;
        while(running){
            currentCO2EmissionValue = tool.downloadCO2Data();
            if(currentCO2EmissionValue == -1) {
                printLine("CO2 Emission level could not be retreived.");
            } else {
                if(currentCO2EmissionValue > co2EmissionLimit + co2EmissionLimitHysteresis) {
                    printLine("CO2 levels are high!");
                    while(!tool.runSequenceExecuterTool("Red") && retryAttempts < 5 ) {    
                        retryAttempts++;
                        printLine("Retrying to run Sequence Executer Tool: " + retryAttempts);
                    }
                    retryAttempts = 0;
                } else if (currentCO2EmissionValue < co2EmissionLimit - co2EmissionLimitHysteresis) {
                    printLine("CO2 levels are low!.");
                    while(!tool.runSequenceExecuterTool("Green") && retryAttempts < 5 ) {    
                        retryAttempts++;
                        printLine("Retrying to run Sequence Executer Tool: " + retryAttempts);
                    }
                    retryAttempts = 0;
                } else {
                    printLine("CO2 levels within hysteresis limits.");
                }
            }
            
            try {
                Thread.sleep(1000*45); // Sleep 4 minutes
            } catch (InterruptedException ex) {
                System.out.println(ex);
                running = false;
            }
        }
    }
    
    private int downloadCO2Data() {
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
            String remoteFile = "/Onlinedata/"+date+"_onlinedata.txt";
            InputStream inputStream = ftpClient.retrieveFileStream(remoteFile);
            
            int co2Emission = parseFtpFile(inputStream);
            boolean success = ftpClient.completePendingCommand();
            if (success) {
                printLine("Sucessfully retrieved and parsed CO2 data.");
            }
            inputStream.close();
            
            return co2Emission;
        } catch (IOException ex) {
            System.out.println("Error: " + ex.getMessage());
            ex.printStackTrace();
            return -1;
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
    
    private int parseFtpFile(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line = "";
        String dummy = "";
        int co2Emission = -1;
        
        while ((dummy = bufferedReader.readLine()) != null) {
            line = dummy; // this will eventually get the last line.
        }
        String[] entry = line.split(";");
        co2Emission = Integer.parseInt(entry[16].replaceAll("\\s", ""));
        return co2Emission;
    }
    
    private boolean runSequenceExecuterTool(String color){
        try {
            printLine("Running sequence: " + color);
            
            class OneShotTask implements Runnable {
                String color;
                OneShotTask(String s) { 
                    color = s; 
                }
                @Override
                public void run() {
                    SequenceExecuterTool seTool = new SequenceExecuterTool();
                    String[] args = new String[6];
                    args[0] = "-enableGUI";
                    args[1] = "false";
                    args[2] = "-toolSettingsPath"; 
                    args[3] = "settings.xml";
                    args[4] = "-XMLscript";
                    args[5] = "LedSet"+color+".xml"; 
                    seTool.main(args);
                }
            };
            Thread seThread=  new Thread(new OneShotTask(color));
            seThread.start();
            
            boolean success = false;
            
            String line = null;
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            while ((line = in.readLine()) != null) {
                /* Print lines in cmd line /*
                System.console().writer().print(line +"\n\r");
                System.console().writer().flush();  */
                if(line.contains("[Set blink pattern]") && line.contains("[Status: 0x00 [Success]]")) {
                    success = true;
                    seThread.destroy();
                    seThread = null;
                    printLine("Successfully ran Sequence Executer Tool.");
                    break;
                } else if(line.contains("Sequence Step [Set blink pattern] new status: [Done]")) {
                    success = true;
                    seThread.destroy();
                    seThread = null;
                    printLine("Successfully ran Sequence Executer Tool.");
                    break;
                } else if(line.contains("Sequence timed out when processing step [Set blink pattern]")) {
                    success = false;
                    seThread.destroy();
                    seThread = null;
                    printLine("Failed to run Sequence Executer Tool.");
                    break;
                } else if(line.contains("Sequence Step [Set blink pattern] new status: [TimedOut]")) {
                    success = false;
                    seThread.destroy();
                    seThread = null;
                    printLine("Failed to run Sequence Executer Tool.");
                    break;
                }
                //Should there be some timer on this to ensure that it is killed??
            }
            
            
            printLine("Status: "+success+"\n\r");
            
            
            return success;
            
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }
    
    private static void printLine(String str) {
        if(System.console() != null) {
            System.console().writer().printf(str + "\n\r");
            System.console().writer().flush();
        }
    }
}
