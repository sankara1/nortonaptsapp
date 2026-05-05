package com.norton.maintenance;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private MaintenanceManager maintenanceManager;
    private Spinner flatSpinner, monthSpinner, expenseSpinner, paymentTypeSpinner, expensePaymentTypeSpinner;
    private EditText payAmount, expAmount, phoneInput, signatureInput, expenseDateInput;
    private String[] flats = {"1A", "1B", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "12A", "14", "15", "16", "17A", "17B", "18A", "18B", "19", "20"};
    private java.util.Calendar expenseCalendar = java.util.Calendar.getInstance();

    private static final String PREFS_NAME = "NortonPrefs";
    private static final String KEY_DEFAULT_AMOUNT = "default_amount";
    private static final String KEY_DEFAULT_SIGNATURE = "default_signature";

    // Data mapping for WhatsApp
    private Map<String, String> flatContacts = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        maintenanceManager = new MaintenanceManager();

        // Bind Views
        flatSpinner = findViewById(R.id.flatSpinner);
        monthSpinner = findViewById(R.id.monthSpinner);
        expenseSpinner = findViewById(R.id.expenseTypeSpinner);
        expensePaymentTypeSpinner = findViewById(R.id.expensePaymentTypeSpinner);
        paymentTypeSpinner = findViewById(R.id.paymentTypeSpinner);
        payAmount = findViewById(R.id.payAmount);
        expAmount = findViewById(R.id.expenseAmount);
        expenseDateInput = findViewById(R.id.expenseDate);
        phoneInput = findViewById(R.id.phoneInput);
        signatureInput = findViewById(R.id.signatureInput);

        Button btnPay = findViewById(R.id.btnSavePayment);
        Button btnMultiPay = findViewById(R.id.btnMultiplePayments);
        Button btnExp = findViewById(R.id.btnSaveExpense);
        Button btnViewExp = findViewById(R.id.btnViewExpenses);
        Button btnExport = findViewById(R.id.btnExportData);
        Button btnStatus = findViewById(R.id.btnViewStatus);
        Button btnShareSum = findViewById(R.id.btnShareSummary);
        Button btnParams = findViewById(R.id.btnParameters);

        setupSpinners();
        setupDatePicker();

        loadContacts();

        btnPay.setOnClickListener(v -> savePaymentAction());
        btnMultiPay.setOnClickListener(v -> showMultiplePaymentsDialog());
        btnExp.setOnClickListener(v -> saveExpenseAction());
        btnViewExp.setOnClickListener(v -> viewExpensesAction());
        btnExport.setOnClickListener(v -> exportMonthlyData());
        btnStatus.setOnClickListener(v -> showPaymentStatus());
        btnShareSum.setOnClickListener(v -> shareSummaryToAdmin());
        btnParams.setOnClickListener(v -> showParametersDialog());

        // Load Defaults
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        payAmount.setText(prefs.getString(KEY_DEFAULT_AMOUNT, "2000"));
        signatureInput.setText(prefs.getString(KEY_DEFAULT_SIGNATURE, "S.Sankarasubramanian"));
    }

    private void loadContacts() {
        maintenanceManager.getAllContacts().addOnSuccessListener(querySnapshot -> {
            for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                String flat = doc.getId();
                String phone = doc.getString("phone");
                if (phone != null) {
                    flatContacts.put(flat, phone);
                }
            }
            // Refresh current flat's phone number after loading from DB
            String currentFlat = flatSpinner.getSelectedItem().toString();
            if (flatContacts.containsKey(currentFlat)) {
                phoneInput.setText(flatContacts.get(currentFlat));
            }
        }).addOnFailureListener(e -> {
            android.util.Log.e("MainActivity", "Error loading contacts", e);
        });
    }

    private void setupSpinners() {
        // 1. Flats List
        ArrayAdapter<String> flatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, flats);
        flatSpinner.setAdapter(flatAdapter);

        // Update phone input when flat selection changes
        flatSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String flat = flats[position];
                String phone = flatContacts.get(flat);
                phoneInput.setText(phone != null ? phone : "");
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Setup contact map
        flatContacts.put("1A", "9443773467");
        flatContacts.put("1B", "9445128146");
        // ... (You can add the rest later, or I can provide the full list)

        // 2. Months
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        monthSpinner.setAdapter(monthAdapter);

        // Set current month as default
        Calendar cal = Calendar.getInstance();
        int currentMonth = cal.get(Calendar.MONTH); // 0-indexed
        monthSpinner.setSelection(currentMonth);

        // 3. Expenses
        String[] expTypes = {"Watchman Salary", "Ravi Salary", "Kolam Salary", "TNEB", "Maintenance","Plumbing/Motor","Other","W Bonus","R bonus","K bonus","CCTV"};
        ArrayAdapter<String> expAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, expTypes);
        expenseSpinner.setAdapter(expAdapter);

        // 4. Payment Types
        String[] payTypes = {"Cash", "Gpay"};
        ArrayAdapter<String> payAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, payTypes);
        paymentTypeSpinner.setAdapter(payAdapter);
        expensePaymentTypeSpinner.setAdapter(payAdapter);
    }

    private void setupDatePicker() {
        expenseDateInput.setText(java.text.DateFormat.getDateInstance().format(expenseCalendar.getTime()));
        android.app.DatePickerDialog.OnDateSetListener dateSetListener = (view, year, month, dayOfMonth) -> {
            expenseCalendar.set(java.util.Calendar.YEAR, year);
            expenseCalendar.set(java.util.Calendar.MONTH, month);
            expenseCalendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth);
            expenseDateInput.setText(java.text.DateFormat.getDateInstance().format(expenseCalendar.getTime()));
        };

        expenseDateInput.setOnClickListener(v -> new android.app.DatePickerDialog(this, dateSetListener,
                expenseCalendar.get(java.util.Calendar.YEAR),
                expenseCalendar.get(java.util.Calendar.MONTH),
                expenseCalendar.get(java.util.Calendar.DAY_OF_MONTH)).show());
    }

    private void savePaymentAction() {
        String flat = flatSpinner.getSelectedItem().toString();
        String month = monthSpinner.getSelectedItem().toString();
        String payType = paymentTypeSpinner.getSelectedItem().toString();
        String amountStr = payAmount.getText().toString();
        String phone = phoneInput.getText().toString();
        String signature = signatureInput.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Enter WhatsApp phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        MaintenanceRecord record = new MaintenanceRecord(flat, month, 2026, payType, amount, phone);

        // 1. Open WhatsApp PDF immediately
        maintenanceManager.sendWhatsApp(this, phone, flat, amount, signature);

        // 2. Background database save
        maintenanceManager.savePayment(record);
        maintenanceManager.updateContact(flat, phone);
        flatContacts.put(flat, phone);

        // Clear fields for next entry
        payAmount.setText("2000"); // Reset to default

        Toast.makeText(this, "Generating PDF & Saving...", Toast.LENGTH_SHORT).show();
    }

    private void showMultiplePaymentsDialog() {
        String selectedMonth = monthSpinner.getSelectedItem().toString();
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String defaultAmt = prefs.getString(KEY_DEFAULT_AMOUNT, "2000");

        FirebaseFirestore.getInstance().collection("payments")
            .whereEqualTo("month", selectedMonth)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Map<String, com.google.firebase.firestore.DocumentSnapshot> existingPayments = new HashMap<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String f = doc.getString("flatNo");
                    if (f != null) existingPayments.put(f, doc);
                }

                android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
                android.widget.TableLayout table = new android.widget.TableLayout(this);
                table.setPadding(20, 20, 20, 20);
                table.setStretchAllColumns(true);
                scrollView.addView(table);

                // Header
                android.widget.TableRow header = new android.widget.TableRow(this);
                String[] titles = {"Flat", "Status", "Type", "Amount"};
                for (String t : titles) {
                    android.widget.TextView tv = new android.widget.TextView(this);
                    tv.setText(t);
                    tv.setPadding(10, 10, 10, 10);
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                    header.addView(tv);
                }
                table.addView(header);

                Map<String, android.widget.RadioButton> paidRadios = new HashMap<>();
                Map<String, android.widget.RadioButton> unpaidRadios = new HashMap<>();
                Map<String, android.widget.RadioButton> cashRadios = new HashMap<>();
                Map<String, android.widget.RadioButton> gpayRadios = new HashMap<>();
                Map<String, EditText> amountInputs = new HashMap<>();

                for (String flat : flats) {
                    android.widget.TableRow row = new android.widget.TableRow(this);
                    row.setPadding(0, 10, 0, 10);

                    // 1. Flat Number
                    android.widget.TextView tvFlat = new android.widget.TextView(this);
                    tvFlat.setText(flat);
                    row.addView(tvFlat);

                    // 2. Paid/Unpaid RadioGroup
                    android.widget.RadioGroup rgStatus = new android.widget.RadioGroup(this);
                    rgStatus.setOrientation(android.widget.RadioGroup.HORIZONTAL);
                    android.widget.RadioButton rbPaid = new android.widget.RadioButton(this);
                    rbPaid.setText("P");
                    android.widget.RadioButton rbUnpaid = new android.widget.RadioButton(this);
                    rbUnpaid.setText("U");
                    rgStatus.addView(rbPaid);
                    rgStatus.addView(rbUnpaid);
                    row.addView(rgStatus);
                    paidRadios.put(flat, rbPaid);
                    unpaidRadios.put(flat, rbUnpaid);

                    // 3. Gpay/Cash RadioGroup
                    android.widget.RadioGroup rgType = new android.widget.RadioGroup(this);
                    rgType.setOrientation(android.widget.RadioGroup.HORIZONTAL);
                    android.widget.RadioButton rbCash = new android.widget.RadioButton(this);
                    rbCash.setText("C");
                    android.widget.RadioButton rbGpay = new android.widget.RadioButton(this);
                    rbGpay.setText("G");
                    rgType.addView(rbCash);
                    rgType.addView(rbGpay);
                    row.addView(rgType);
                    cashRadios.put(flat, rbCash);
                    gpayRadios.put(flat, rbGpay);

                    // 4. Amount EditText
                    android.widget.EditText etAmount = new android.widget.EditText(this);
                    etAmount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                    row.addView(etAmount);
                    amountInputs.put(flat, etAmount);

                    // Logic: Enable Type/Amount only if Paid
                    rbPaid.setOnCheckedChangeListener((btn, isChecked) -> {
                        rgType.setEnabled(isChecked);
                        rbCash.setEnabled(isChecked);
                        rbGpay.setEnabled(isChecked);
                        etAmount.setEnabled(isChecked);
                    });

                    // Set initial state
                    if (existingPayments.containsKey(flat)) {
                        com.google.firebase.firestore.DocumentSnapshot doc = existingPayments.get(flat);
                        rbPaid.setChecked(true);
                        etAmount.setText(String.valueOf(doc != null ? doc.get("amount") : defaultAmt));
                        String type = doc != null ? doc.getString("paymentType") : "Cash";
                        if ("Gpay".equals(type)) rbGpay.setChecked(true); else rbCash.setChecked(true);
                    } else {
                        rbUnpaid.setChecked(true);
                        etAmount.setText(defaultAmt);
                        rbCash.setChecked(true);
                        rgType.setEnabled(false);
                        rbCash.setEnabled(false);
                        rbGpay.setEnabled(false);
                        etAmount.setEnabled(false);
                    }

                    table.addView(row);
                }

                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Multiple Payments: " + selectedMonth)
                    .setView(scrollView)
                    .setPositiveButton("Save All", (dialog, which) -> {
                        com.google.firebase.firestore.WriteBatch batch = FirebaseFirestore.getInstance().batch();
                        for (String flat : flats) {
                            boolean isPaid = paidRadios.get(flat).isChecked();
                            if (isPaid) {
                                String amtStr = amountInputs.get(flat).getText().toString();
                                double amt = Double.parseDouble(amtStr.isEmpty() ? defaultAmt : amtStr);
                                String type = gpayRadios.get(flat).isChecked() ? "Gpay" : "Cash";

                                if (existingPayments.containsKey(flat)) {
                                    com.google.firebase.firestore.DocumentSnapshot existingDoc = existingPayments.get(flat);
                                    if (existingDoc != null) {
                                        batch.update(existingDoc.getReference(), "amount", amt, "paymentType", type);
                                    }
                                } else {
                                    String phone = flatContacts.getOrDefault(flat, "");
                                    MaintenanceRecord record = new MaintenanceRecord(flat, selectedMonth, 2026, type, amt, phone);
                                    batch.set(FirebaseFirestore.getInstance().collection("payments").document(), record);
                                }
                            } else {
                                // If marked Unpaid, delete if existed
                                if (existingPayments.containsKey(flat)) {
                                    com.google.firebase.firestore.DocumentSnapshot existingDoc = existingPayments.get(flat);
                                    if (existingDoc != null) batch.delete(existingDoc.getReference());
                                }
                            }
                        }
                        batch.commit().addOnSuccessListener(aVoid -> Toast.makeText(this, "All Payments Updated!", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
    }

    private void showParametersDialog() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        android.widget.TextView labelAmt = new android.widget.TextView(this);
        labelAmt.setText("Default Maintenance Amount:");
        layout.addView(labelAmt);

        android.widget.EditText editAmt = new android.widget.EditText(this);
        editAmt.setText(prefs.getString(KEY_DEFAULT_AMOUNT, "2000"));
        editAmt.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(editAmt);

        android.widget.TextView labelSig = new android.widget.TextView(this);
        labelSig.setText("Default Signature Name:");
        labelSig.setPadding(0, 30, 0, 0);
        layout.addView(labelSig);

        android.widget.EditText editSig = new android.widget.EditText(this);
        editSig.setText(prefs.getString(KEY_DEFAULT_SIGNATURE, "S.Sankarasubramanian"));
        layout.addView(editSig);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("App Parameters")
            .setView(layout)
            .setPositiveButton("Save", (d, w) -> {
                android.content.SharedPreferences.Editor editor = prefs.edit();
                String newAmt = editAmt.getText().toString();
                String newSig = editSig.getText().toString();
                editor.putString(KEY_DEFAULT_AMOUNT, newAmt);
                editor.putString(KEY_DEFAULT_SIGNATURE, newSig);
                editor.apply();
                
                payAmount.setText(newAmt);
                signatureInput.setText(newSig);
                Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void shareSummaryToAdmin() {
        String selectedMonth = monthSpinner.getSelectedItem().toString();
        FirebaseFirestore.getInstance().collection("payments")
            .whereEqualTo("month", selectedMonth)
            .get()
            .addOnSuccessListener(pSnapshot -> {
                double[] totalPaidArr = {0};
                java.util.List<String> paidList = new java.util.ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : pSnapshot.getDocuments()) {
                    String f = doc.getString("flatNo");
                    if (f != null) paidList.add(f);
                    Double amt = doc.getDouble("amount");
                    if (amt != null) totalPaidArr[0] += amt;
                }

                StringBuilder pending = new StringBuilder();
                for (String f : flats) {
                    if (!paidList.contains(f)) pending.append(f).append(", ");
                }

                FirebaseFirestore.getInstance().collection("expenses")
                    .whereEqualTo("month", selectedMonth)
                    .get()
                    .addOnSuccessListener(eSnapshot -> {
                        double[] totalExpArr = {0};
                        for (com.google.firebase.firestore.DocumentSnapshot doc : eSnapshot.getDocuments()) {
                            Double amt = doc.getDouble("amount");
                            if (amt != null) totalExpArr[0] += amt;
                        }

                        String summary = "Norton Maintenance Summary [" + selectedMonth + "]\n" +
                                "---------------------------\n" +
                                "Flats Paid: " + paidList.size() + " / " + flats.length + "\n" +
                                "Collected: ₹" + (int) totalPaidArr[0] + "\n" +
                                "Expenses: ₹" + (int) totalExpArr[0] + "\n" +
                                "---------------------------\n" +
                                "Pending: " + pending.toString() + "\n" +
                                "---------------------------";
                        
                        maintenanceManager.sendTextWhatsApp(this, "9962446732", summary);
                    });
            });
    }

    private void viewExpensesAction() {
        String selectedMonth = monthSpinner.getSelectedItem().toString();
        FirebaseFirestore.getInstance().collection("expenses")
            .whereEqualTo("month", selectedMonth)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                showBulkEditDialog("Expenses: " + selectedMonth, "expenses", querySnapshot.getDocuments());
            });
    }

    private void viewPaymentsAction(String month) {
        FirebaseFirestore.getInstance().collection("payments")
            .whereEqualTo("month", month)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                showBulkEditDialog("Payments: " + month, "payments", querySnapshot.getDocuments());
            });
    }

    private void showBulkEditDialog(String title, String collection, List<com.google.firebase.firestore.DocumentSnapshot> documents) {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout mainLayout = new android.widget.LinearLayout(this);
        mainLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        mainLayout.setPadding(30, 30, 30, 30);
        scrollView.addView(mainLayout);

        Map<String, EditText> amountFields = new HashMap<>();
        List<String> docsToDelete = new ArrayList<>();

        for (com.google.firebase.firestore.DocumentSnapshot doc : documents) {
            String labelText = collection.equals("payments") ? "Flat " + doc.getString("flatNo") : doc.getString("type");
            
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 10, 0, 10);

            android.widget.TextView label = new android.widget.TextView(this);
            label.setText(labelText);
            label.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1f));
            row.addView(label);

            android.widget.EditText amountEdit = new android.widget.EditText(this);
            amountEdit.setText(String.valueOf(doc.get("amount")));
            amountEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            amountEdit.setLayoutParams(new android.widget.LinearLayout.LayoutParams(250, -2));
            row.addView(amountEdit);
            amountFields.put(doc.getId(), amountEdit);

            android.widget.ImageButton btnDel = new android.widget.ImageButton(this);
            btnDel.setImageResource(android.R.drawable.ic_menu_delete);
            btnDel.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnDel.setOnClickListener(v -> {
                docsToDelete.add(doc.getId());
                row.setVisibility(android.view.View.GONE);
            });
            row.addView(btnDel);

            mainLayout.addView(row);
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setView(scrollView)
            .setPositiveButton("Save All", (dialog, which) -> {
                com.google.firebase.firestore.WriteBatch batch = FirebaseFirestore.getInstance().batch();
                
                // 1. Process Deletions
                for (String id : docsToDelete) {
                    batch.delete(FirebaseFirestore.getInstance().collection(collection).document(id));
                }

                // 2. Process Updates
                for (Map.Entry<String, EditText> entry : amountFields.entrySet()) {
                    if (!docsToDelete.contains(entry.getKey())) {
                        try {
                            double newAmt = Double.parseDouble(entry.getKey().startsWith("temp") ? "0" : entry.getValue().getText().toString());
                            batch.update(FirebaseFirestore.getInstance().collection(collection).document(entry.getKey()), "amount", newAmt);
                        } catch (Exception e) {}
                    }
                }

                batch.commit().addOnSuccessListener(aVoid -> Toast.makeText(this, "Changes Saved!", Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void exportMonthlyData() {
        String selectedMonth = monthSpinner.getSelectedItem().toString();
        
        // 1. Fetch Payments
        FirebaseFirestore.getInstance().collection("payments")
            .whereEqualTo("month", selectedMonth)
            .get()
            .addOnSuccessListener(pSnapshot -> {
                // 2. Fetch Expenses for the same month
                FirebaseFirestore.getInstance().collection("expenses")
                    .whereEqualTo("month", selectedMonth)
                    .get()
                    .addOnSuccessListener(eSnapshot -> {
                        StringBuilder csv = new StringBuilder("TYPE,ID/FLAT,MONTH,AMOUNT,PAY_TYPE,DATE\n");
                        
                        // Add Payments
                        for (com.google.firebase.firestore.DocumentSnapshot doc : pSnapshot.getDocuments()) {
                            csv.append("PAYMENT,").append(doc.getString("flatNo")).append(",")
                               .append(doc.getString("month")).append(",")
                               .append(doc.get("amount")).append(",")
                               .append(doc.getString("paymentType")).append(",")
                               .append(doc.get("date")).append("\n");
                        }
                        
                        // Add Expenses
                        for (com.google.firebase.firestore.DocumentSnapshot doc : eSnapshot.getDocuments()) {
                            csv.append("EXPENSE,").append(doc.getString("type")).append(",")
                               .append(doc.getString("month")).append(",")
                               .append(doc.get("amount")).append(",")
                               .append(doc.getString("paymentType")).append(",")
                               .append(doc.get("date")).append("\n");
                        }
                        
                        maintenanceManager.shareCsv(this, "Accounts_" + selectedMonth + ".csv", csv.toString());
                    });
            });
    }

    private void showPaymentStatus() {
        showPaymentStatus(monthSpinner.getSelectedItem().toString());
    }

    private void showPaymentStatus(String initialMonth) {
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 20);
        scrollView.addView(layout);

        android.widget.Spinner dialogMonthSpinner = new android.widget.Spinner(this);
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, months);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogMonthSpinner.setAdapter(adapter);

        // Set to initial month
        for (int i = 0; i < months.length; i++) {
            if (months[i].equals(initialMonth)) {
                dialogMonthSpinner.setSelection(i);
                break;
            }
        }

        android.widget.TextView statusView = new android.widget.TextView(this);
        statusView.setPadding(0, 30, 0, 0);
        statusView.setTextSize(16);
        // Text color will now follow the device theme (White in Dark mode, Black in Light mode)

        layout.addView(dialogMonthSpinner);
        layout.addView(statusView);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Monthly Status")
                .setView(scrollView)
                .setPositiveButton("Close", null)
                .setNeutralButton("Copy & Share", null)
                .setNegativeButton("Edit Payments", null)
                .create();

        dialogMonthSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedMonth = months[position];
                updateStatusText(selectedMonth, statusView);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        dialog.show();

        // Re-set listeners to prevent dialog closing
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            String text = statusView.getText().toString();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Status", text);
            clipboard.setPrimaryClip(clip);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(sendIntent, "Share Status"));
            Toast.makeText(MainActivity.this, "Copied to clipboard!", Toast.LENGTH_SHORT).show();
        });

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            viewPaymentsAction(months[dialogMonthSpinner.getSelectedItemPosition()]);
        });
    }

    private void updateStatusText(String month, android.widget.TextView statusView) {
        FirebaseFirestore.getInstance().collection("payments")
                .whereEqualTo("month", month)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double[] totalPaidArr = {0};
                    java.util.List<String> paidFlats = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String f = doc.getString("flatNo");
                        if (f != null) paidFlats.add(f);
                        Double amt = doc.getDouble("amount");
                        if (amt != null) totalPaidArr[0] += amt;
                    }

                    StringBuilder pending = new StringBuilder("Pending (" + (flats.length - paidFlats.size()) + "):\n");
                    StringBuilder paidStr = new StringBuilder("Paid (" + paidFlats.size() + "):\n");

                    for (String f : flats) {
                        if (paidFlats.contains(f)) {
                            paidStr.append(f).append(", ");
                        } else {
                            pending.append(f).append(", ");
                        }
                    }

                    FirebaseFirestore.getInstance().collection("expenses")
                            .whereEqualTo("month", month)
                            .get()
                            .addOnSuccessListener(eSnapshot -> {
                                double[] totalExpArr = {0};
                                for (com.google.firebase.firestore.DocumentSnapshot doc : eSnapshot.getDocuments()) {
                                    Double amt = doc.getDouble("amount");
                                    if (amt != null) totalExpArr[0] += amt;
                                }

                                String statusText = "Summary for " + month + ":\n" +
                                        "Collected: ₹" + (int) totalPaidArr[0] + "\n" +
                                        "Expenses: ₹" + (int) totalExpArr[0] + "\n" +
                                        "Balance: ₹" + (int) (totalPaidArr[0] - totalExpArr[0]) + "\n\n" +
                                        paidStr.toString() + "\n\n" +
                                        pending.toString();

                                statusView.setText(statusText);
                            });
                });
    }

    private void saveExpenseAction() {
        String type = expenseSpinner.getSelectedItem().toString();
        String payType = expensePaymentTypeSpinner.getSelectedItem().toString();
        String amountStr = expAmount.getText().toString();
        String selectedMonth = monthSpinner.getSelectedItem().toString();

        if (amountStr.isEmpty()) return;

        double amount = Double.parseDouble(amountStr);
        Expense exp = new Expense(type, amount, payType, selectedMonth, expenseCalendar.getTime(), "");

        maintenanceManager.saveExpense(exp)
            .addOnSuccessListener(doc -> {
                Toast.makeText(this, "Expense Saved!", Toast.LENGTH_SHORT).show();
                expAmount.setText("");
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to save expense: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}