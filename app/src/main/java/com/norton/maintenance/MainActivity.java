package com.norton.maintenance;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private Spinner flatSpinner, monthSpinner, expenseSpinner;
    private RadioGroup paymentTypeRadioGroup, expensePaymentTypeRadioGroup;
    private EditText payAmount, expAmount, phoneInput, expenseDateInput;
    private String[] flats = {"1A", "1B", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "12A", "14", "15", "16", "17A", "17B", "18A", "18B", "19", "20"};
    private java.util.Calendar expenseCalendar = java.util.Calendar.getInstance();

    private static final String PREFS_NAME = "NortonPrefs";
    private static final String KEY_DEFAULT_AMOUNT = "default_amount";
    private static final String KEY_DEFAULT_SIGNATURE = "default_signature";
    private static final String KEY_BUILDING_NAME = "building_name";
    private static final String KEY_FLAT_LIST = "flat_list";

    private String buildingNameValue = "Norton Apartments";
    private String flatListValue = "1A, 1B, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 12A, 14, 15, 16, 17A, 17B, 18A, 18B, 19, 20";

    // Data mapping for WhatsApp
    private Map<String, String> flatContacts = new HashMap<>();

    private final ActivityResultLauncher<String> csvPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    importCsvFromUri(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        maintenanceManager = new MaintenanceManager();

        // Bind Views
        flatSpinner = findViewById(R.id.flatSpinner);
        monthSpinner = findViewById(R.id.monthSpinner);
        expenseSpinner = findViewById(R.id.expenseTypeSpinner);
        paymentTypeRadioGroup = findViewById(R.id.paymentTypeRadioGroup);
        expensePaymentTypeRadioGroup = findViewById(R.id.expensePaymentTypeRadioGroup);
        payAmount = findViewById(R.id.payAmount);
        expAmount = findViewById(R.id.expenseAmount);
        expenseDateInput = findViewById(R.id.expenseDate);
        phoneInput = findViewById(R.id.phoneInput);

        Button btnPay = findViewById(R.id.btnSavePayment);
        Button btnMultiPay = findViewById(R.id.btnMultiplePayments);
        Button btnExp = findViewById(R.id.btnSaveExpense);
        Button btnViewExp = findViewById(R.id.btnViewExpenses);
        Button btnExport = findViewById(R.id.btnExportData);
        Button btnStatus = findViewById(R.id.btnViewStatus);
        Button btnShareSum = findViewById(R.id.btnShareSummary);
        Button btnParams = findViewById(R.id.btnParameters);
        Button btnOpeningBal = findViewById(R.id.btnSetOpeningBalance);
        Button btnImportCsv = findViewById(R.id.btnImportCsv);
        Button btnGetTemplate = findViewById(R.id.btnGetCsvTemplate);

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
        btnOpeningBal.setOnClickListener(v -> showOpeningBalanceDialog());
        btnImportCsv.setOnClickListener(v -> csvPickerLauncher.launch("text/comma-separated-values|text/csv"));
        btnGetTemplate.setOnClickListener(v -> getCsvTemplate());

        // Load Defaults
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        payAmount.setText(prefs.getString(KEY_DEFAULT_AMOUNT, "2000"));
        buildingNameValue = prefs.getString(KEY_BUILDING_NAME, "Norton Apartments");
        flatListValue = prefs.getString(KEY_FLAT_LIST, "1A, 1B, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 12A, 14, 15, 16, 17A, 17B, 18A, 18B, 19, 20");

        // Update UI with loaded building name
        android.widget.TextView buildingTitle = findViewById(R.id.buildingNameText);
        if (buildingTitle != null) buildingTitle.setText(buildingNameValue);

        // Update flats array from prefs
        updateFlatsArray(flatListValue);
    }

    private void updateFlatsArray(String list) {
        if (list == null || list.isEmpty()) return;
        String[] parts = list.split(",");
        flats = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            flats[i] = parts[i].trim();
        }
        // Refresh spinners if they exist
        if (flatSpinner != null) {
            ArrayAdapter<String> flatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, flats);
            flatSpinner.setAdapter(flatAdapter);
        }
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
        
        int selectedPayTypeId = paymentTypeRadioGroup.getCheckedRadioButtonId();
        String payType = (selectedPayTypeId == R.id.radioPayGpay) ? "Gpay" : "Cash";
        
        String amountStr = payAmount.getText().toString();
        String phone = phoneInput.getText().toString();

        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String signature = prefs.getString(KEY_DEFAULT_SIGNATURE, "S.Sankarasubramanian");

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Enter WhatsApp phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        MaintenanceRecord record = new MaintenanceRecord(flat, month, currentYear, payType, amount, phone);

        // 1. Open WhatsApp PDF immediately
        maintenanceManager.sendWhatsApp(this, phone, flat, amount, signature, buildingNameValue);

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
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String defaultAmt = prefs.getString(KEY_DEFAULT_AMOUNT, "2000");

        maintenanceManager.getDb().collection("payments")
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
                        com.google.firebase.firestore.WriteBatch batch = maintenanceManager.getDb().batch();
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
                                    MaintenanceRecord record = new MaintenanceRecord(flat, selectedMonth, currentYear, type, amt, phone);
                                    batch.set(maintenanceManager.getDb().collection("payments").document(), record);
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

        android.widget.TextView labelBuild = new android.widget.TextView(this);
        labelBuild.setText("Building Name:");
        labelBuild.setPadding(0, 30, 0, 0);
        layout.addView(labelBuild);

        android.widget.EditText editBuild = new android.widget.EditText(this);
        editBuild.setText(prefs.getString(KEY_BUILDING_NAME, "Norton Apartments"));
        layout.addView(editBuild);

        android.widget.TextView labelFlats = new android.widget.TextView(this);
        labelFlats.setText("Flat List (comma separated):");
        labelFlats.setPadding(0, 30, 0, 0);
        layout.addView(labelFlats);

        android.widget.EditText editFlats = new android.widget.EditText(this);
        editFlats.setText(prefs.getString(KEY_FLAT_LIST, "1A, 1B, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 12A, 14, 15, 16, 17A, 17B, 18A, 18B, 19, 20"));
        layout.addView(editFlats);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("App Parameters")
            .setView(layout)
            .setPositiveButton("Save", (d, w) -> {
                android.content.SharedPreferences.Editor editor = prefs.edit();
                String newAmt = editAmt.getText().toString();
                String newSig = editSig.getText().toString();
                String newBuild = editBuild.getText().toString();
                String newFlats = editFlats.getText().toString();

                editor.putString(KEY_DEFAULT_AMOUNT, newAmt);
                editor.putString(KEY_DEFAULT_SIGNATURE, newSig);
                editor.putString(KEY_BUILDING_NAME, newBuild);
                editor.putString(KEY_FLAT_LIST, newFlats);
                editor.apply();
                
                payAmount.setText(newAmt);
                buildingNameValue = newBuild;
                flatListValue = newFlats;

                android.widget.TextView title = findViewById(R.id.buildingNameText);
                if (title != null) title.setText(newBuild);
                updateFlatsArray(newFlats);

                Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void getCsvTemplate() {
        String template = "TYPE,ID/FLAT,MONTH,AMOUNT,PAY_TYPE,DATE\n" +
                "PAYMENT,1A,January,2000,Cash,2025-01-01\n" +
                "EXPENSE,Watchman,January,8000,Cash,2025-01-01\n" +
                "OPENING,Cash,January,5000,Cash,\n" +
                "OPENING,Account,January,15000,Gpay,";
        maintenanceManager.shareCsv(this, "Norton_Template.csv", template);
        Toast.makeText(this, "Sharing CSV Template...", Toast.LENGTH_SHORT).show();
    }

    private void importCsvFromUri(Uri uri) {
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return;
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
            String line;
            com.google.firebase.firestore.WriteBatch batch = maintenanceManager.getDb().batch();
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int count = 0;

            // Skip header
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("-")) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 5) continue;

                String type = parts[0].trim();
                String id = parts[1].trim();
                String month = parts[2].trim();
                String amtStr = parts[3].trim();
                String payType = parts[4].trim();

                if (amtStr.isEmpty()) continue;
                double amount = Double.parseDouble(amtStr);

                if (type.equalsIgnoreCase("PAYMENT")) {
                    MaintenanceRecord record = new MaintenanceRecord(id, month, currentYear, payType, amount, "");
                    batch.set(maintenanceManager.getDb().collection("payments").document(), record);
                } else if (type.equalsIgnoreCase("EXPENSE")) {
                    Expense exp = new Expense(id, amount, payType, month, new java.util.Date(), "");
                    batch.set(maintenanceManager.getDb().collection("expenses").document(), exp);
                } else if (type.equalsIgnoreCase("OPENING")) {
                    String docId = currentYear + "_" + month;
                    Map<String, Object> data = new HashMap<>();
                    if (id.equalsIgnoreCase("Cash")) data.put("cash", amount);
                    else if (id.equalsIgnoreCase("Account")) data.put("account", amount);
                    batch.set(maintenanceManager.getDb().collection("opening_balances").document(docId), data, com.google.firebase.firestore.SetOptions.merge());
                }
                count++;
                
                // Commit every 400 records (Firestore limit is 500)
                if (count % 400 == 0) {
                    batch.commit();
                    batch = maintenanceManager.getDb().batch();
                }
            }
            batch.commit().addOnSuccessListener(aVoid -> Toast.makeText(this, "Import Successful!", Toast.LENGTH_SHORT).show());
            inputStream.close();
        } catch (Exception e) {
            Toast.makeText(this, "Import Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void showOpeningBalanceDialog() {
        String selectedMonth = monthSpinner.getSelectedItem().toString();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String docId = currentYear + "_" + selectedMonth;

        maintenanceManager.getDb().collection("opening_balances").document(docId).get()
            .addOnSuccessListener(doc -> {
                android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
                layout.setOrientation(android.widget.LinearLayout.VERTICAL);
                layout.setPadding(50, 40, 50, 10);

                android.widget.TextView labelCash = new android.widget.TextView(this);
                labelCash.setText("Opening Cash (Carry from prev month):");
                layout.addView(labelCash);

                android.widget.EditText editCash = new android.widget.EditText(this);
                editCash.setText(String.valueOf(doc.exists() ? doc.get("cash") : "0"));
                editCash.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                layout.addView(editCash);

                android.widget.TextView labelAcc = new android.widget.TextView(this);
                labelAcc.setText("Opening Account/Gpay Balance:");
                labelAcc.setPadding(0, 30, 0, 0);
                layout.addView(labelAcc);

                android.widget.EditText editAcc = new android.widget.EditText(this);
                editAcc.setText(String.valueOf(doc.exists() ? doc.get("account") : "0"));
                editAcc.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                layout.addView(editAcc);

                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Opening Balance: " + selectedMonth)
                    .setView(layout)
                    .setPositiveButton("Save", (d, w) -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("cash", Double.parseDouble(editCash.getText().toString()));
                        data.put("account", Double.parseDouble(editAcc.getText().toString()));
                        maintenanceManager.getDb().collection("opening_balances").document(docId).set(data)
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Balance Saved!", Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
    }

    private void shareSummaryToAdmin() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        
        // 1. Fetch ALL Payments for the year
        maintenanceManager.getDb().collection("payments")
            .whereEqualTo("year", currentYear)
            .get()
            .addOnSuccessListener(pSnapshot -> {
                // 2. Fetch ALL Expenses for the year
                maintenanceManager.getDb().collection("expenses")
                    .get()
                    .addOnSuccessListener(eSnapshot -> {
                        // 3. Fetch ALL Opening Balances
                        maintenanceManager.getDb().collection("opening_balances")
                            .get()
                            .addOnSuccessListener(bSnapshot -> {
                                StringBuilder csv = new StringBuilder("TYPE,ID/FLAT,MONTH,AMOUNT,PAY_TYPE,DATE\n");
                                
                                // Map balances for easy lookup
                                Map<String, com.google.firebase.firestore.DocumentSnapshot> balances = new HashMap<>();
                                for(com.google.firebase.firestore.DocumentSnapshot doc : bSnapshot.getDocuments()) {
                                    balances.put(doc.getId(), doc);
                                }

                                String[] monthsList = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
                                
                                for (String m : monthsList) {
                                    String balId = currentYear + "_" + m;
                                    boolean hasData = false;
                                    
                                    // Check if this month has any data
                                    List<com.google.firebase.firestore.DocumentSnapshot> monthPayments = new ArrayList<>();
                                    for(com.google.firebase.firestore.DocumentSnapshot d : pSnapshot.getDocuments()) {
                                        if (m.equals(d.getString("month"))) monthPayments.add(d);
                                    }
                                    
                                    List<com.google.firebase.firestore.DocumentSnapshot> monthExpenses = new ArrayList<>();
                                    for(com.google.firebase.firestore.DocumentSnapshot d : eSnapshot.getDocuments()) {
                                        if (m.equals(d.getString("month"))) monthExpenses.add(d);
                                    }

                                    if (monthPayments.isEmpty() && monthExpenses.isEmpty() && !balances.containsKey(balId)) continue;

                                    csv.append("--- ").append(m.toUpperCase()).append(" ---,,,,,\n");
                                    
                                    // Add Opening
                                    if (balances.containsKey(balId)) {
                                        com.google.firebase.firestore.DocumentSnapshot b = balances.get(balId);
                                        csv.append("OPENING,Cash,,").append(b.get("cash")).append(",Cash,\n");
                                        csv.append("OPENING,Account,,").append(b.get("account")).append(",Gpay,\n");
                                    }

                                    // Add Payments
                                    for (com.google.firebase.firestore.DocumentSnapshot d : monthPayments) {
                                        csv.append("PAYMENT,").append(d.getString("flatNo")).append(",")
                                           .append(m).append(",")
                                           .append(d.get("amount")).append(",")
                                           .append(d.getString("paymentType")).append(",")
                                           .append(d.get("date")).append("\n");
                                    }

                                    // Add Expenses
                                    for (com.google.firebase.firestore.DocumentSnapshot d : monthExpenses) {
                                        csv.append("EXPENSE,").append(d.getString("type")).append(",")
                                           .append(m).append(",")
                                           .append(d.get("amount")).append(",")
                                           .append(d.getString("paymentType")).append(",")
                                           .append(d.get("date")).append("\n");
                                    }
                                    csv.append(",,,,,\n");
                                }

                                maintenanceManager.shareCsv(this, "Full_Accounts_" + currentYear + ".csv", csv.toString(), "9962446732");
                                Toast.makeText(this, "Generating Full History CSV...", Toast.LENGTH_SHORT).show();
                            });
                    });
            });
    }

    private void viewExpensesAction() {
        String selectedMonth = monthSpinner.getSelectedItem().toString();
        maintenanceManager.getDb().collection("expenses")
            .whereEqualTo("month", selectedMonth)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                showBulkEditDialog("Expenses: " + selectedMonth, "expenses", querySnapshot.getDocuments());
            });
    }

    private void viewPaymentsAction(String month) {
        maintenanceManager.getDb().collection("payments")
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
            String name = collection.equals("payments") ? "Flat " + doc.getString("flatNo") : doc.getString("type");
            String payType = doc.getString("paymentType");
            if (payType == null) payType = "Cash"; // Default for old records
            String labelText = name + "\n[" + payType + "]";
            
            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(0, 15, 0, 15);

            android.widget.TextView label = new android.widget.TextView(this);
            label.setText(labelText);
            label.setTextSize(14);
            label.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0, -2, 1.2f));
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
                com.google.firebase.firestore.WriteBatch batch = maintenanceManager.getDb().batch();
                
                // 1. Process Deletions
                for (String id : docsToDelete) {
                    batch.delete(maintenanceManager.getDb().collection(collection).document(id));
                }

                // 2. Process Updates
                for (Map.Entry<String, EditText> entry : amountFields.entrySet()) {
                    if (!docsToDelete.contains(entry.getKey())) {
                        try {
                            double newAmt = Double.parseDouble(entry.getKey().startsWith("temp") ? "0" : entry.getValue().getText().toString());
                            batch.update(maintenanceManager.getDb().collection(collection).document(entry.getKey()), "amount", newAmt);
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
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String balDocId = currentYear + "_" + selectedMonth;

        // 1. Fetch Opening Balances
        maintenanceManager.getDb().collection("opening_balances").document(balDocId).get()
            .addOnSuccessListener(balDoc -> {
                double openingCash = balDoc.exists() ? (balDoc.getDouble("cash") != null ? balDoc.getDouble("cash") : 0) : 0;
                double openingGpay = balDoc.exists() ? (balDoc.getDouble("account") != null ? balDoc.getDouble("account") : 0) : 0;

                // 2. Fetch Payments
                maintenanceManager.getDb().collection("payments")
                    .whereEqualTo("month", selectedMonth)
                    .get()
                    .addOnSuccessListener(pSnapshot -> {
                        // 3. Fetch Expenses
                        maintenanceManager.getDb().collection("expenses")
                            .whereEqualTo("month", selectedMonth)
                            .get()
                            .addOnSuccessListener(eSnapshot -> {
                                StringBuilder csv = new StringBuilder("TYPE,ID/FLAT,MONTH,AMOUNT,PAY_TYPE,DATE\n");
                                
                                // Add Opening Balance Rows
                                csv.append("OPENING,Cash,,").append(openingCash).append(",Cash,\n");
                                csv.append("OPENING,Account,,").append(openingGpay).append(",Gpay,\n");
                                csv.append(",,,,,\n"); // Empty row separator

                                double totalCashPaid = 0;
                                double totalGpayPaid = 0;
                                double totalCashExp = 0;
                                double totalGpayExp = 0;

                                // Add Payments
                                for (com.google.firebase.firestore.DocumentSnapshot doc : pSnapshot.getDocuments()) {
                                    Double amt = doc.getDouble("amount");
                                    String type = doc.getString("paymentType");
                                    if (amt != null) {
                                        if ("Gpay".equals(type)) totalGpayPaid += amt; else totalCashPaid += amt;
                                    }
                                    csv.append("PAYMENT,").append(doc.getString("flatNo")).append(",")
                                       .append(doc.getString("month")).append(",")
                                       .append(amt).append(",")
                                       .append(type != null ? type : "Cash").append(",")
                                       .append(doc.get("date")).append("\n");
                                }

                                // Add Expenses
                                for (com.google.firebase.firestore.DocumentSnapshot doc : eSnapshot.getDocuments()) {
                                    Double amt = doc.getDouble("amount");
                                    String type = doc.getString("paymentType");
                                    if (amt != null) {
                                        if ("Gpay".equals(type)) totalGpayExp += amt; else totalCashExp += amt;
                                    }
                                    csv.append("EXPENSE,").append(doc.getString("type")).append(",")
                                       .append(doc.getString("month")).append(",")
                                       .append(amt).append(",")
                                       .append(type != null ? type : "Cash").append(",")
                                       .append(doc.get("date")).append("\n");
                                }

                                // Calculate Closings
                                double netCash = openingCash + totalCashPaid - totalCashExp;
                                double netGpay = openingGpay + totalGpayPaid - totalGpayExp;

                                csv.append(",,,,,\n"); // Separator
                                csv.append("CLOSING,Cash,,").append(netCash).append(",Cash,\n");
                                csv.append("CLOSING,Account,,").append(netGpay).append(",Gpay,\n");
                                csv.append("TOTAL,Net,,").append(netCash + netGpay).append(",,\n");
                                
                                maintenanceManager.shareCsv(this, "Accounts_" + selectedMonth + ".csv", csv.toString());
                            });
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
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String balDocId = currentYear + "_" + month;

        maintenanceManager.getDb().collection("opening_balances").document(balDocId).get()
            .addOnSuccessListener(balDoc -> {
                double openingCash = balDoc.exists() ? (balDoc.getDouble("cash") != null ? balDoc.getDouble("cash") : 0) : 0;
                double openingGpay = balDoc.exists() ? (balDoc.getDouble("account") != null ? balDoc.getDouble("account") : 0) : 0;

                maintenanceManager.getDb().collection("payments")
                .whereEqualTo("month", month)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalPaidSumStatus = 0;
                    double totalCashPaid = 0;
                    double totalGpayPaid = 0;
                    java.util.Map<String, String> paidFlatTypes = new java.util.HashMap<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String f = doc.getString("flatNo");
                        String type = doc.getString("paymentType");
                        if (f != null) paidFlatTypes.put(f, type != null ? type : "Cash");
                        
                        Double amt = doc.getDouble("amount");
                        if (amt != null) {
                            totalPaidSumStatus += amt;
                            if ("Gpay".equals(type)) totalGpayPaid += amt;
                            else totalCashPaid += amt;
                        }
                    }

                    StringBuilder pending = new StringBuilder("Pending (" + (flats.length - paidFlatTypes.size()) + "):\n");
                    StringBuilder paidStr = new StringBuilder("Paid (" + paidFlatTypes.size() + "):\n");

                    for (String f : flats) {
                        if (paidFlatTypes.containsKey(f)) {
                            paidStr.append(f).append(" (").append(paidFlatTypes.get(f)).append("), ");
                        } else {
                            pending.append(f).append(", ");
                        }
                    }

                    final double finalTotalPaid = totalPaidSumStatus;
                    final double finalCashPaid = totalCashPaid;
                    final double finalGpayPaid = totalGpayPaid;

                    maintenanceManager.getDb().collection("expenses")
                            .whereEqualTo("month", month)
                            .get()
                            .addOnSuccessListener(eSnapshot -> {
                                double totalExp = 0;
                                double totalCashExp = 0;
                                double totalGpayExp = 0;
                                for (com.google.firebase.firestore.DocumentSnapshot doc : eSnapshot.getDocuments()) {
                                    Double amt = doc.getDouble("amount");
                                    if (amt != null) {
                                        totalExp += amt;
                                        String type = doc.getString("paymentType");
                                        if ("Gpay".equals(type)) totalGpayExp += amt;
                                        else totalCashExp += amt;
                                    }
                                }

                                double netCash = openingCash + finalCashPaid - totalCashExp;
                                double netGpay = openingGpay + finalGpayPaid - totalGpayExp;

                                String statusText = "Summary for " + month + ":\n" +
                                        "Opening: Cash ₹" + (int)openingCash + ", Acc ₹" + (int)openingGpay + "\n" +
                                        "Collected: ₹" + (int) finalTotalPaid + " (Cash: " + (int)finalCashPaid + ", Gpay: " + (int)finalGpayPaid + ")\n" +
                                        "Expenses:  ₹" + (int) totalExp + " (Cash: " + (int)totalCashExp + ", Gpay: " + (int)totalGpayExp + ")\n" +
                                        "---------------------------\n" +
                                        "CLOSING: Cash ₹" + (int)netCash + ", Acc ₹" + (int)netGpay + "\n" +
                                        "TOTAL BALANCE: ₹" + (int)(netCash + netGpay) + "\n\n" +
                                        paidStr.toString() + "\n\n" +
                                        pending.toString();

                                statusView.setText(statusText);
                            });
                });
            });
    }

    private void saveExpenseAction() {
        String type = expenseSpinner.getSelectedItem().toString();
        
        int selectedExpPayTypeId = expensePaymentTypeRadioGroup.getCheckedRadioButtonId();
        String payType = (selectedExpPayTypeId == R.id.radioExpGpay) ? "Gpay" : "Cash";

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