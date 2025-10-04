import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.io.*;

public class Main extends JFrame {
    private JLabel ayLabel, secilenLabel;
    private JPanel takvimPanel;
    private LocalDate gosterilenAy;
    private HashMap<LocalDate, NotBilgi> notlar;
    private JButton buguneDonButton;
    private final String DOSYA_ADI = "notlar_kategori.txt";

    // Not bilgisi sınıfı
    static class NotBilgi {
        String notMetni;
        String kategori;
        public NotBilgi(String metin, String kategori) { this.notMetni = metin; this.kategori = kategori; }
    }

    public Main () {
        setTitle("Eag Calendar");
        setSize(550, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        gosterilenAy = LocalDate.now();
        notlar = new HashMap<>();
        okuNotlar();

        // Üst panel
        JPanel ustPanel = new JPanel();
        JButton oncekiAy = new JButton("<<");
        JButton sonrakiAy = new JButton(">>");
        ayLabel = new JLabel("", JLabel.CENTER);
        ayLabel.setFont(new Font("Arial", Font.BOLD, 20));
        buguneDonButton = new JButton("Bugüne Dön");

        ustPanel.add(oncekiAy);
        ustPanel.add(ayLabel);
        ustPanel.add(sonrakiAy);
        ustPanel.add(buguneDonButton);
        add(ustPanel, BorderLayout.NORTH);

        // Takvim paneli
        takvimPanel = new JPanel(new GridLayout(0, 7, 5, 5));
        takvimPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        add(takvimPanel, BorderLayout.CENTER);

        // Alt panel
        JPanel altPanel = new JPanel();
        secilenLabel = new JLabel("Seçilen Tarih: ");
        altPanel.add(secilenLabel);
        add(altPanel, BorderLayout.SOUTH);

        // Buton olayları
        oncekiAy.addActionListener(e -> { gosterilenAy = gosterilenAy.minusMonths(1); guncelleTakvim(); });
        sonrakiAy.addActionListener(e -> { gosterilenAy = gosterilenAy.plusMonths(1); guncelleTakvim(); });
        buguneDonButton.addActionListener(e -> { gosterilenAy = LocalDate.now(); guncelleTakvim(); });

        guncelleTakvim();
        setVisible(true);
    }

    private void guncelleTakvim() {
        takvimPanel.removeAll();

        String ayIsim = gosterilenAy.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + gosterilenAy.getYear();
        ayLabel.setText(ayIsim);

        String[] gunler = {"Pzt","Sal","Çar","Per","Cum","Cmt","Paz"};
        for (String gun : gunler) {
            JLabel lbl = new JLabel(gun, JLabel.CENTER);
            lbl.setForeground(Color.BLUE.darker());
            lbl.setFont(new Font("Verdana", Font.BOLD, 14));
            takvimPanel.add(lbl);
        }

        LocalDate ilkGun = gosterilenAy.withDayOfMonth(1);
        int baslangicGun = ilkGun.getDayOfWeek().getValue();
        int toplamGun = gosterilenAy.lengthOfMonth();

        for (int i=1; i<baslangicGun; i++) { takvimPanel.add(new JLabel("")); }

        for (int i=1; i<=toplamGun; i++) {
            LocalDate tarih = gosterilenAy.withDayOfMonth(i);
            JButton btn = new JButton(String.valueOf(i));

            // Bugün ve hafta sonu
            if(tarih.equals(LocalDate.now())) { btn.setBackground(Color.ORANGE); btn.setOpaque(true); }
            DayOfWeek day = tarih.getDayOfWeek();
            if(day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) { btn.setForeground(Color.RED.darker()); }

            // Daha önce not varsa kategoriye göre renk
            if(notlar.containsKey(tarih)) {
                NotBilgi n = notlar.get(tarih);
                switch(n.kategori) {
                    case "Kişisel": btn.setBackground(new Color(144,238,144)); break;
                    case "İş": btn.setBackground(new Color(173,216,230)); break;
                    case "Okul": btn.setBackground(new Color(255,228,181)); break;
                    default: btn.setBackground(Color.LIGHT_GRAY);
                }
                btn.setOpaque(true);
                btn.setToolTipText(n.notMetni); // Tooltip ile kısa not göster
            }

            btn.addActionListener(e -> {
                secilenLabel.setText("Seçilen Tarih: " + tarih);
                String[] kategoriler = {"Kişisel","İş","Okul"};
                JComboBox<String> kategoriBox = new JComboBox<>(kategoriler);
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(new JLabel("Kategori:"), BorderLayout.NORTH);
                panel.add(kategoriBox, BorderLayout.CENTER);

                String mevcutNot = notlar.containsKey(tarih) ? notlar.get(tarih).notMetni : "";
                String not = JOptionPane.showInputDialog(this, panel, mevcutNot);
                String secilenKategori = (String) kategoriBox.getSelectedItem();
                if(not != null && !not.trim().isEmpty()) {
                    notlar.put(tarih, new NotBilgi(not, secilenKategori));
                } else { notlar.remove(tarih); }
                kaydetNotlar();
                guncelleTakvim();
            });

            takvimPanel.add(btn);
        }

        takvimPanel.revalidate();
        takvimPanel.repaint();
    }

    private void kaydetNotlar() {
        try(PrintWriter pw = new PrintWriter(new FileWriter(DOSYA_ADI))) {
            for(LocalDate tarih : notlar.keySet()) {
                NotBilgi n = notlar.get(tarih);
                pw.println(tarih + "=" + n.kategori + "=" + n.notMetni);
            }
        } catch(IOException e){ e.printStackTrace(); }
    }

    private void okuNotlar() {
        File dosya = new File(DOSYA_ADI);
        if(!dosya.exists()) return;
        try(Scanner sc = new Scanner(dosya)) {
            while(sc.hasNextLine()) {
                String[] parca = sc.nextLine().split("=", 3);
                if(parca.length==3) {
                    LocalDate tarih = LocalDate.parse(parca[0]);
                    String kategori = parca[1];
                    String not = parca[2];
                    notlar.put(tarih, new NotBilgi(not, kategori));
                }
            }
        } catch(IOException e){ e.printStackTrace(); }
    }

    public static void main(String[] args) {
        try {
            for(UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()){
                if("Nimbus".equals(info.getName())){
                    UIManager.setLookAndFeel(info.getClassName()); break;
                }
            }
        } catch(Exception e){ e.printStackTrace(); }

        new Main();
    }
}