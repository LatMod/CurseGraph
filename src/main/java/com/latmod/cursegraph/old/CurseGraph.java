package com.latmod.cursegraph.old;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URL;

public class CurseGraph
{
    public static String version = "0.0.0";
    public static boolean updateAvailable = false;

    public static TrayIcon trayIcon = null;
    public static BufferedImage imageReady, imageBusy, imageSettings;
    public static ImageIcon iconSettings;

    public static final File folder = getFolder();
    public static File dataFolder, configFile;
    public static Config config;

    private static boolean firstRefresh = true;

    public static void main(String[] args) throws Exception
    {
        if(!Desktop.isDesktopSupported())
        {
            System.out.println("Desktop not supported!");
            System.exit(1);
            return;
        }

        System.out.println("Loading CurseGraph, Version: " + version + " @ " + Graph.getTimeString(System.currentTimeMillis()));

        {
            File versionFile = new File(folder, "CurseGraph.version");
            versionFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(versionFile);
            fos.write(("" + version).getBytes());
            fos.close();
        }

        configFile = new File(folder, "config.json");
        config = Utils.fromJsonFile(configFile, Config.class);
        if(!configFile.exists())
        {
            configFile.createNewFile();
        }

        if(config == null)
        {
            config = new Config();
        }
        config.setDefaults();
        config.save();

        dataFolder = new File(config.dataFolderPath);

        try
        {
            String s = Utils.toString(new URL("http://pastebin.com/raw.php?i=RyuQPm4f").openStream());
            if(s != null)
            {
                updateAvailable = !s.equals(version);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(1);
        }

        imageReady = loadImage("icons/tray_icon.png");
        imageBusy = loadImage("icons/tray_icon_busy.png");
        imageSettings = loadImage("icons/settings.png");
        iconSettings = new ImageIcon(imageSettings.getScaledInstance(32, 32, Image.SCALE_SMOOTH));

        trayIcon = new TrayIcon(imageReady);
        trayIcon.setImageAutoSize(true);
        trayIcon.setToolTip("Curse Graph");

        if(!Utils.contains(args, "notray"))
        {
            if(!SystemTray.isSupported())
            {
                System.out.println("System tray not supported!");
                System.exit(1);
                return;
            }

            trayIcon.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    CurseGraphFrame.inst.setVisible(!CurseGraphFrame.inst.isVisible());
                }
            });

            PopupMenu menu = new PopupMenu();

            {
                MenuItem m1 = new MenuItem("Curse Graph v" + version + (updateAvailable ? " (Update available)" : ""));

                m1.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        CurseGraphFrame.inst.setVisible(true);
                    }
                });

                menu.add(m1);
            }

            menu.addSeparator();

            {
                MenuItem m1 = new MenuItem("Exit");

                m1.addActionListener(new ActionListener()
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        SystemTray.getSystemTray().remove(trayIcon);
                        System.exit(0);
                    }
                });

                menu.add(m1);
            }

            trayIcon.setPopupMenu(menu);

            SystemTray.getSystemTray().add(trayIcon);
        }

        OldDataLoader.init();
        refresh();
        Projects.save();
        Graph.init();

        if(updateAvailable)
        {
            info("Update available!", false);
        }

        firstRefresh = false;
    }

    private static File getFolder()
    {
        File f = new File(System.getProperty("user.home"), "/LatMod/CurseGraph/");
        if(!f.exists())
        {
            f.mkdirs();
        }
        return f;
    }

    public static BufferedImage loadImage(String s) throws Exception
    {
        return ImageIO.read(CurseGraph.class.getResource("/latmod/cursegraph/" + s));
    }

    public static BufferedImage loadImageURL(String s) throws Exception
    {
        return ImageIO.read(new URL(s));
    }

    public static void refresh()
    {
        trayIcon.setImage(imageBusy);
        if(!firstRefresh)
        {
            CurseGraphFrame.inst.setIconImage(imageBusy);
        }
        Projects.load();

        trayIcon.setImage(imageReady);
        if(!firstRefresh)
        {
            CurseGraphFrame.inst.setIconImage(imageReady);
        }
        CurseGraphFrame.inst.refresh();
    }

    public static void info(String string, boolean silent)
    {
        if(!silent)
        {
            JOptionPane.showMessageDialog(null, string, "Info", JOptionPane.INFORMATION_MESSAGE);
        }
        System.out.println(string);
    }

    public static void error(String string, boolean silent)
    {
        if(!silent)
        {
            JOptionPane.showMessageDialog(null, string, "Error!", JOptionPane.ERROR_MESSAGE);
        }
        System.out.println(string);
    }

    public static boolean showYesNo(String title, String question)
    {
        int i = JOptionPane.showConfirmDialog(null, question, title, JOptionPane.YES_NO_OPTION);
        return i == JOptionPane.YES_OPTION;
    }

    public static void openURL(String s)
    {
        try
        {
            Desktop.getDesktop().browse(new URI(s));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
}