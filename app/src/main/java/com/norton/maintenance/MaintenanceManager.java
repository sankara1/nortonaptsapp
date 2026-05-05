package com.norton.maintenance;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class MaintenanceManager {

    private final FirebaseFirestore db;

    public MaintenanceManager() {
        String databaseId = "ai-studio-d6118695-2420-491c-b526-71bb953d4e66";
        this.db = FirebaseFirestore.getInstance(databaseId);
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    public Task<DocumentReference> savePayment(MaintenanceRecord record) {
        return db.collection("payments").add(record);
    }

    public Task<DocumentReference> saveExpense(Expense expense) {
        return db.collection("expenses").add(expense);
    }

    public void updateContact(String flatNo, String phone) {
        Map<String, Object> data = new HashMap<>();
        data.put("phone", phone);
        db.collection("contacts").document(flatNo).set(data);
    }

    public Task<QuerySnapshot> getAllContacts() {
        return db.collection("contacts").get();
    }

    public void sendWhatsApp(Context context, String phone, String flatNo, double amount, String signature, String buildingName) {
        // Create PDF
        File pdfFile = createPdf(context, flatNo, amount, signature, buildingName);
        if (pdfFile == null) {
            return;
        }

        try {
            String cleanedPhone = cleanPhone(phone);
            Uri uri = FileProvider.getUriForFile(context, "com.norton.maintenance.fileprovider", pdfFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            // Pre-select contact if possible
            intent.putExtra("jid", cleanedPhone + "@s.whatsapp.net");
            intent.setPackage("com.whatsapp");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("MaintenanceManager", "PDF Share error", e);
        }
    }

    public void shareCsv(Context context, String filename, String content) {
        shareCsv(context, filename, content, null);
    }

    public void shareCsv(Context context, String filename, String content, String phone) {
        File file = new File(context.getCacheDir(), filename);
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(content.getBytes());
            Uri uri = FileProvider.getUriForFile(context, "com.norton.maintenance.fileprovider", file);
            
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            if (phone != null && !phone.isEmpty()) {
                intent.putExtra("jid", cleanPhone(phone) + "@s.whatsapp.net");
            }
            intent.setPackage("com.whatsapp");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendTextWhatsApp(Context context, String phone, String message) {
        try {
            String url = "https://wa.me/" + cleanPhone(phone) + "?text=" + URLEncoder.encode(message, "UTF-8");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setPackage("com.whatsapp");
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File createPdf(Context context, String flatNo, double amount, String signature, String buildingName) {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 450, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        
        paint.setTextSize(16f);
        paint.setFakeBoldText(true);
        canvas.drawText(buildingName.toUpperCase(), 50, 50, paint);
        
        paint.setFakeBoldText(false);
        paint.setTextSize(12f);
        canvas.drawText("Maintenance Receipt", 50, 80, paint);
        canvas.drawText("-----------------------------------", 50, 100, paint);
        canvas.drawText("Flat No: " + flatNo, 50, 130, paint);
        canvas.drawText("Amount Paid: ₹" + (int)amount, 50, 160, paint);
        canvas.drawText("Date: " + java.text.DateFormat.getDateInstance().format(new java.util.Date()), 50, 190, paint);
        
        canvas.drawText("-----------------------------------", 50, 240, paint);
        canvas.drawText("Regards,", 50, 270, paint);
        paint.setFakeBoldText(true);
        canvas.drawText(signature, 50, 290, paint);
        
        paint.setFakeBoldText(false);
        paint.setTextSize(10f);


        document.finishPage(page);

        File file = new File(context.getCacheDir(), "Receipt_" + flatNo + ".pdf");
        try {
            document.writeTo(new FileOutputStream(file));
            document.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String cleanPhone(String p) {
        String clean = p.replaceAll("\\D+", "");
        return clean.length() == 10 ? "91" + clean : clean;
    }
}
