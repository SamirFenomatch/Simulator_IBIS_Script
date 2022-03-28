/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.mycompany.structure.folder.simulator;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import org.ini4j.Ini;
import org.ini4j.Wini;

class StructureFolderSimulator
{
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        // dir variable contains path from where this file is called
        String dir = System.getProperty("user.dir");
         
        // Directory where the folder structure begins
        String dirIncubator = "/NAS/CCM-IBIS/name_patient";

        createPatient(1, 10, dir + dirIncubator);
        createPatient(2, 20, dir + dirIncubator);
        createPatient(3, 30, dir + dirIncubator);
        createPatient(4, 40, dir + dirIncubator);
        createPatient(5, 50, dir + dirIncubator);
        createPatient(6, 60, dir + dirIncubator);
        createPatient(7, 70, dir + dirIncubator);
        createPatient(8, 80, dir + dirIncubator);
        createPatient(9, 90, dir + dirIncubator);

    }

    public static void createPatient(int id, int minutes, String rootDirectory) {
        // ZonedDateTime together with a date formatter are responsible for creating the date of creation of the folder
        ZonedDateTime zdt = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "yyyyMMddHHmmss" );
        String patientDate = formatter.format(zdt);

        intervalPatient(id, minutes, rootDirectory, patientDate);  
    }

    public static void intervalPatient(int id, int minutes, String rootDirectory, String creationDate) {
        // CountDownLatch is a counter that is initialized to 1, for each time the countDown() method is called, it decrements the counter
        final CountDownLatch latch = new CountDownLatch(1);
         
        // Timer works to iterate everything inside the 'run()' method, this iteration only goes through milliseconds
        Timer timer = new Timer();

        // int milliseconds = minutes * 60000; // MINUTES TO MILI
        int milliseconds = minutes * 1000; // SECONDS TO MILI

        timer.schedule( new TimerTask() {
         
            public void run() {

                // The variable 'finalizePatient' will be used to know when the insertion of images to a patient (DISH) is finished
                Boolean finalizePatient = false;

                // getTime() function: Finds the difference of two dates in periods and returns the date in the mandatory format that the insertion time folder must carry
                // convertDateFolderToZonedDateTime() function: Gets the format or name of the folder (which is the creation date) and converts it to a ZonedDateTime object so that it can be used in getTime()
                String insertionDate = getTime(convertDateFolderToZonedDateTime(Paths.get(creationDate)), ZonedDateTime.now());

                // We create the directory with the insertion period calculated
                new File(rootDirectory + "/" + creationDate + "/DISH" +id+ "/" + insertionDate).mkdirs();
               
                createINI(id, rootDirectory + "/" + creationDate, creationDate);
               
                // Once the directory with insertion time we randomly add the images
                // And we calculate if the insertion time exceeds the period up to which it can be inserted
                finalizePatient = insertImageToFolderInsertion(Paths.get(rootDirectory + "/" + creationDate + "/DISH" +id+ "/" + insertionDate), Paths.get(creationDate));
               
                // If the last insert exceeds the maximum insert period, we call the countDown() method which decrements the counter by 0, and the iterable may terminate.
                if (finalizePatient) {
                  System.out.println("Se ha creado completamente el paciente con el dish: " + id);
                  latch.countDown();
                }
                
            }
        }, 0, milliseconds);

        // As long as the counter is not 0, it will keep waiting, if it is, it will end the iterable and continue with the next line
        try {
            latch.await();
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        timer.cancel();
    }

    public static Boolean insertImageToFolderInsertion(Path insertionDatePath, Path creationDate) {
        Boolean isFinalizePatient = false;

        // Location where all the images are
        String pathImage = "/home/samircabrera/Documents/images-script";

        // 'allImages' contains the path of all images
        List<Path> allImages = searchImages(Paths.get(pathImage));

        // We iterate, get the image and create it in a new directory; which would be the directory where the insertion date passed as a parameter is (insertionDatePath)
        for (Path images : allImages) {
            
            try {
               File initialImage = new File(images.toString());
               BufferedImage bImage = ImageIO.read(initialImage);

               String img = images.toString().substring(images.toString().lastIndexOf("/") + 1).trim();
               String directorySave = insertionDatePath.toString() + "/" + img;

               ImageIO.write(bImage, "jpg", new File(directorySave));
               
            } catch(Exception e) {
               System.out.println("Error agregando imagenes. " + e);
            }
            
        }

        // If the last insertion made is greater than the established number of days, it returns true: as a signal that no more insertions can be added to this patient
        int maxDay = getMaxDay(insertionDatePath);
        if (maxDay >= 2) {
            isFinalizePatient = true;
        }

        return isFinalizePatient;
    }

    public static Boolean verifyExistDirectory(String pathDirectory) {
        try {

            File file = new File(pathDirectory);
            return file.exists();

        } catch(Exception e) {

            System.out.println(e);
            return null;
        }
        
    }

    public static String getTime(ZonedDateTime pastDate, ZonedDateTime nowDate) {
        String result = "";
         
        // Get the duration difference between two dates
        Duration duration = Duration.between(pastDate, nowDate);
         
        // We convert the duration in seconds to days, hours, minutes and seconds
        int day = (int)TimeUnit.SECONDS.toDays(duration.toSeconds());    
        String hoursString = getValueString(TimeUnit.SECONDS.toHours(duration.toSeconds()) - (day *24));
        String minutesString = getValueString(TimeUnit.SECONDS.toMinutes(duration.toSeconds()) - (TimeUnit.SECONDS.toHours(duration.toSeconds())* 60));
        String secondString = getValueString(TimeUnit.SECONDS.toSeconds(duration.toSeconds()) - (TimeUnit.SECONDS.toMinutes(duration.toSeconds()) *60));

        result = "" + day + hoursString + minutesString + secondString;

        return result;
    }

    public static List<Path> searchFolder(Path path) {
        List<Path> folders = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(path, 1)) {

            folders = walk.filter(Files::isDirectory).filter(folder -> !folder.equals(path)).collect(Collectors.toList());

        } catch (Exception e) {
            System.out.println("Error looking for folders " + e);
            return null;
        }

        return folders;
    }

    public static List<Path> searchImages(Path path) {
        List<Path> images = new ArrayList<>();
 
        try (Stream<Path> walk = Files.walk(path, 1)) {
 
            images = walk.filter(image -> !image.equals(path)).collect(Collectors.toList());
 
        } catch (Exception e) {
            System.out.println("Error looking for images " + e);
            return null;
        }
 
        return images;
     }

    public static ZonedDateTime convertDateFolderToZonedDateTime(Path folders) {
        String folder = folders.toString().substring(folders.toString().lastIndexOf("/") + 1).trim();

        int days = Integer.parseInt(folder.substring(6,8));
        int hours = Integer.parseInt(folder.substring(8,10));
        int minutes = Integer.parseInt(folder.substring(10,12));
        int seconds = Integer.parseInt(folder.substring(12,14));

        ZonedDateTime result = ZonedDateTime.of(2022, 3, days, hours, minutes, seconds, 0, ZoneId.of("Europe/Paris"));
        
        return result;
    }

    public static int getMaxDay(Path folders) {
        // We get the last folder who has the insertion date
        String folder = folders.toString().substring(folders.toString().lastIndexOf("/") + 1).trim();

        // And we get the day that will be the first character
        // return Integer.parseInt(folder.substring(5,7)); // SECONDS

        return Integer.parseInt(folder.substring(3,5)); // MINUTES
        // return Integer.parseInt(folder.substring(0,1)); // DAYS
    }
      
    public static String getValueString(long value) {
        String result = "";

        if (value < 10) {
            result = "" + 0 + value;
        } else {
            result = "" + value;
        }

        return result;
    }
    
    public static void createINI(int id, String directory, String creationDate) {
        File newFile = new File(directory + "/DishInfo.ini");
                
        try {
            
            newFile.createNewFile();
            
        } catch (IOException ex) {
            
            Logger.getLogger(StructureFolderSimulator.class.getName()).log(Level.SEVERE, null, ex);
        
        }
               
        try {
        
            Wini ini = new Wini(new File(directory + "/DishInfo.ini"));
                    
            patterDataIni(ini, creationDate);
            timelapseDataIni(ini);
            incubatorInfoDataIni(ini);
            dishInfoDataIni(ini, id);
                    
        } catch (IOException ex) {
            
            Logger.getLogger(StructureFolderSimulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    public static void patterDataIni(Wini ini, String creationDate) throws IOException {
        String section = "Pattern";
        
        ini.put(section, "Pattern_count", 3);
        ini.put(section, "Pattern1_ZCount", 100);
        ini.put(section, "Pattern1_ZSliceUm", 3);
        ini.put(section, "Pattern1_StartTime", creationDate);
        ini.put(section, "Pattern1_EndTime", 0);
        ini.put(section, "Pattern2_ZCount", 0);
        ini.put(section, "Pattern2_ZSliceUm", 0);
        ini.put(section, "Pattern2_StartTime", 0);
        ini.put(section, "Pattern2_EndTime", 0);
        ini.put(section, "Pattern3_ZCount", 0);
        ini.put(section, "Pattern3_ZSliceUm", 0);
        ini.put(section, "Pattern3_StartTime", 0);
        ini.put(section, "Pattern3_EndTime", 0);
        
        ini.store();
        
    }
    
    public static void timelapseDataIni(Wini ini) throws IOException {
        String section = "Timelapse";
        
        ini.put(section, "StartTime", 0);
        ini.put(section, "DishCount", 9);
        ini.put(section, "WellCount", 25);
        ini.put(section, "TimelapseCount", 0);
        
        ini.store();
    }
    
    public static void incubatorInfoDataIni(Wini ini) throws IOException {
        String section = "IncubatorInfo";
        
        ini.put(section, "IncubatorName", "INCB1");
        
        ini.store();
    }
    
    public static void dishInfoDataIni(Wini ini, int id) throws IOException {
        String section = "Dish"+id+"Info";
        
        ini.put(section, "Avail", 1);
        ini.put(section, "PatientName", "nombre");
        ini.put(section, "PID1", id);
        ini.put(section, "PID2", "");
        ini.put(section, "CultivationNumber", 1);
        ini.put(section, "Terminate", 0);
        ini.put(section, "SoftwareVersion", 1.0);
        ini.put(section, "Comment", "");
        ini.put(section, "Reserve", "");
        
        for (int i = 1; i < 25; i++) {
            ini.put(section, "Well"+i+"Avail", 1);
            ini.put(section, "Well"+i+"ZCount", 11);
            ini.put(section, "Well"+i+"ZSliceUm", 10);
        }
        
        ini.store();
    }
}
