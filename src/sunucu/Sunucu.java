package sunucu;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

class anaSunucu extends Thread {

    public int kapasite = 10000;
    public int iKabul = 400;
    public int iYanit = 50;
    public int iKabulZaman = 500;
    public int iYanitZaman = 200;
    public int toplam = 0;
    public int ran;
    ReentrantLock kilit = new ReentrantLock();
    
    @Override
    public void run() {
        
        TimerTask tmtask = new TimerTask() {
            @Override
            public void run() {
                if(toplam < kapasite){
                    ran = 1 + (int)(Math.random()*iKabul);
                    kilit.lock();
                    try {
                        toplam = toplam + ran;
                        if(toplam > kapasite){
                            toplam = kapasite;
                        }
                        System.out.println("toplam " + toplam + " KABUL" + getName());
                    } finally {
                        kilit.unlock();
                    }
                }
            }
        };
        
        TimerTask tmtask2 = new TimerTask() {
            @Override
            public void run() {
                if(toplam > 0){ 
                    ran = 1 + (int)(Math.random()*iYanit);
                    kilit.lock();
                    try {
                        toplam = toplam - ran;
                        if(toplam < 0){
                            toplam = 0;
                        }
                        System.out.println("toplam " + ran + "YANIT -- Kalan istek " + toplam + " " + getName());
                    } finally {
                        kilit.unlock();
                    }
                }
            }
        };
        
        Timer tm = new Timer();
        tm.scheduleAtFixedRate(tmtask, iKabulZaman, iKabulZaman);
        tm.scheduleAtFixedRate(tmtask2, iYanitZaman, iYanitZaman);
    }
}

class altSunucuOlusturucu extends Thread {
    
    ReentrantLock kilit = new ReentrantLock();
    public anaSunucu mainT;

    public altSunucuOlusturucu(anaSunucu mainT) {
        this.mainT = mainT;
    }

    @Override
    public void run() {
        ArrayList<altSunucu> altTh = new ArrayList<>();
        ArrayList<JProgressBar> pBar = new ArrayList<>();
        
        
        
        altSunucu subT[] = {
             new altSunucu(mainT, 0),
             new altSunucu(mainT, 0)
        };
        
        altTh.add(0, subT[0]);
        altTh.add(1, subT[1]);
        subT[0].start();
        subT[1].start();
        
        System.out.println("");
        System.out.println("-----------------" + altTh.size() + " -------------------");
        System.out.println("");
        
        JFrame frame = new JFrame("Multithread'lerin anlık durum tablosu");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        JProgressBar bar1 = new JProgressBar();
        bar1.setMinimum(0);
        bar1.setMaximum(10000);
        bar1.setValue((int)(mainT.toplam));
        bar1.setStringPainted(true);
        panel.add(bar1, 0);
        pBar.add(bar1);
        JProgressBar bar2 = new JProgressBar();
        bar2.setMinimum(0);
        bar2.setMaximum(5000);
        bar2.setValue((int)(altTh.get(0).toplam));
        bar2.setStringPainted(true);
        panel.add(bar2, 1);
        pBar.add(bar2);
        JProgressBar bar3 = new JProgressBar();
        bar3.setMinimum(0);
        bar3.setMaximum(5000);
        bar3.setValue((int)(altTh.get(1).toplam));
        bar3.setStringPainted(true);
        panel.add(bar3, 2);
        pBar.add(bar3);
        frame.add(panel);
        frame.setSize(1024, 560);
        frame.setLocation(200, 150);
        frame.setVisible(true);
        
        sunucuTakip takip = new sunucuTakip(mainT, altTh, pBar, frame, panel);
        takip.start();
        
        int index = 0;
        double control;
        double oran = (1000*70)/100;
        double yarisi;
        while(!Thread.currentThread().isInterrupted()){
            kilit.lock();
            try {
                control = altTh.get(index).toplam;
                if(control >= oran){
                    yarisi = (altTh.get(index).toplam * 50)/100;
                    altTh.get(index).toplam = yarisi;
                    System.out.println("ALT TOPLAM &&&&&&&&&&&&& " + altTh.get(index).toplam + "&&&&&&&&&&&&&&&&&&&");
                    altSunucu sub = new altSunucu(mainT, yarisi);
                    int size = altTh.size();
                    altTh.add(size, sub);
                    System.out.println("");
                    System.out.println("-----------------" + altTh.size() + " -------------------");
                    System.out.println("");
                    sub.start();
                    JProgressBar bar = new JProgressBar();
                    bar.setMinimum(0);
                    bar.setMaximum(5000);
                    bar.setValue((int)(altTh.get(size).toplam));
                    bar.setStringPainted(true);
                    panel.add(bar, size + 1);
                    pBar.add(bar);
                    frame.setVisible(true);
                }
                if(control == 0.0 && index != 0 && index != 1){
                    System.out.println("??????????????????????????????????");
                    try {
                        altTh.get(index).tm.cancel();
                        if(altTh.get(index).isAlive()){
                            System.out.println("AaaaaaaaLlllllllIıııııııııVvvvvvvvvvEeeeeeeee");
                        } else {
                            System.out.println("DdddddddddddddddddIiiiiiiiiiiiiiiiEeeeeeeeeeeeeeee");
                        }
                        altTh.remove(index);
                        pBar.remove(index + 1);
                        panel.remove(index + 1);
                        panel.repaint();
                        System.out.println(pBar.size());
                    } catch (Exception ex) {
                        System.out.println("INTERRUPTED EXCEPTİON " + ex);
                    }
                }
                index = index + 1;
                if(index > altTh.size() - 1){
                    index = 0;
                }
            } finally {
                kilit.unlock();
            }
        }
    }
}

class sunucuTakip extends Thread {
    
    ReentrantLock kilit = new ReentrantLock();
    public anaSunucu mainT;
    public ArrayList<altSunucu> altTh;
    public ArrayList<JProgressBar> pBar;
    public JFrame frame;
    public JPanel panel;
    public JLabel label = new JLabel();

    public sunucuTakip(anaSunucu mainT, ArrayList<altSunucu> altTh, ArrayList<JProgressBar> pBar, JFrame frame, JPanel panel) {
        this.mainT = mainT;
        this.altTh = altTh;
        this.pBar = pBar;
        this.frame = frame;
        this.panel = panel;
    }

    @Override
    public void run() {
        panel.add(label);
        frame.setVisible(true);
        
        TimerTask tmtask = new TimerTask() {
            @Override
            public void run() {
                
                for(int index = 0; index < altTh.size(); index++){
                    kilit.lock();
                    try {
                        if(pBar.size() == altTh.size() + 1){
                            if(index == 0){
                                pBar.get(0).setValue(mainT.toplam);
                                pBar.get(0).setStringPainted(true);
                            }
                            System.out.println("INDEX " + index + " size pbar " + pBar.size() + " size altTh " + altTh.size());
                            pBar.get(index + 1).setValue((int)altTh.get(index).toplam);
                            pBar.get(index + 1).setStringPainted(true);
                        }
                    } finally {
                        kilit.unlock();
                    }
                }
                
                label.setText("Alt sunucu sayisi: " + String.valueOf(altTh.size()));
                frame.setVisible(true);
            }
        };
        
        Timer tm = new Timer();
        tm.scheduleAtFixedRate(tmtask, 100, 100);
    }
}

class altSunucu extends Thread {

    public int kapasite = 5000;
    public int iKabul = 300;
    public int iYanit = 100;
    public int iKabulZaman = 500;
    public int iYanitZaman = 300;
    public double toplam;
    public int ran;
    public anaSunucu mainT;
    Timer tm;
    
    public altSunucu(anaSunucu mainT, double oran) {
        this.mainT = mainT;
        this.toplam = oran;
    }
    
    @Override
    public void run() {
        
        TimerTask tmtask = new TimerTask() {
            @Override
            public void run() {
                if(toplam < kapasite && mainT.toplam > 0){
                    ran = 1 + (int)(Math.random()*iKabul);
                    mainT.toplam = mainT.toplam - ran;
                    if(mainT.toplam < 0){
                        mainT.toplam = 0;
                    }
                    System.out.println("");
                    System.out.println("Ana sunucudan " + ran + " tane iş yükü aldım. Ana sunucuda şu an " + mainT.toplam + " kadar iş yükü var." + getName());
                    toplam = toplam + ran;
                    if(toplam > kapasite){
                        toplam = kapasite;
                    }
                    System.out.println("toplam " + toplam + " KABUL --- " + getName());
                    System.out.println("");
                }
            }
        };
        
        TimerTask tmtask2 = new TimerTask() {
            @Override
            public void run() {
                if(toplam > 0){
                    ran = 1 + (int)(Math.random()*iYanit);
                    toplam = toplam - ran;
                    if(toplam < 0){
                        toplam = 0;
                    }
                    System.out.println("");
                    System.out.println("toplam " + ran + "YANIT -- Kalan istek " + toplam + " " + getName());
                    System.out.println("");
                }
            }
        };
        
        tm = new Timer();
        tm.scheduleAtFixedRate(tmtask, iKabulZaman, iKabulZaman);
        tm.scheduleAtFixedRate(tmtask2, iYanitZaman, iYanitZaman);
    }
}

public class Sunucu extends Thread {
    public static void main(String[] args) {
        anaSunucu mainT = new anaSunucu();
        mainT.start();
        altSunucuOlusturucu altT = new altSunucuOlusturucu(mainT);
        altT.start();
    }
}
