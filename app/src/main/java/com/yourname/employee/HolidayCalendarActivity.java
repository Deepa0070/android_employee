package com.yourname.employee;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.widget.TextView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.widget.Button;
import android.widget.ProgressBar;

public class HolidayCalendarActivity extends AppCompatActivity {

    private ListView listView;
    private TextView tvYear;
    private Button btnDownloadHolidayList;
    private ProgressBar progressBar;

    // Holiday data for 2026
    private ArrayList<Holiday> holidays2026;
    private String currentYear = "2026";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_holiday_calendar);

        initializeViews();
        setupHolidays2026();
        setupHolidayList();
        setupListeners();
    }

    private void initializeViews() {
        listView = findViewById(R.id.listView);
        tvYear = findViewById(R.id.tvYear);
        btnDownloadHolidayList = findViewById(R.id.btnDownloadHolidayList);
        progressBar = findViewById(R.id.progressBar);

        if (tvYear != null) {
            tvYear.setText("Holiday Calendar " + currentYear);
        }
    }

    private void setupHolidays2026() {
        holidays2026 = new ArrayList<>();

        // Format: Holiday name, date, day of week, description

        // January 2026
        holidays2026.add(new Holiday("New Year's Day", "1st January 2026", "Thursday", "New Year Celebration"));
        holidays2026.add(new Holiday("Republic Day", "26th January 2026", "Monday", "Constitution Day"));
        holidays2026.add(new Holiday("Makar Sankranti", "14th January 2026", "Wednesday", "Harvest Festival"));
        holidays2026.add(new Holiday("Guru Gobind Singh Jayanti", "17th January 2026", "Saturday", "Birth Anniversary of Guru Gobind Singh"));

        // February 2026
        holidays2026.add(new Holiday("Vasant Panchami", "22nd February 2026", "Sunday", "Goddess Saraswati Worship"));
        holidays2026.add(new Holiday("Shivaji Jayanti", "19th February 2026", "Thursday", "Birth Anniversary of Chhatrapati Shivaji Maharaj"));

        // March 2026
        holidays2026.add(new Holiday("Maha Shivaratri", "14th March 2026", "Saturday", "Night of Lord Shiva"));
        holidays2026.add(new Holiday("Holi", "3rd March 2026", "Tuesday", "Festival of Colors"));
        holidays2026.add(new Holiday("Good Friday", "3rd April 2026", "Friday", "Christian Holiday")); // Note: Good Friday is April 3 in 2026

        // April 2026
        holidays2026.add(new Holiday("Ugadi/Gudi Padwa", "19th April 2026", "Sunday", "New Year's Day in Karnataka/Maharashtra"));
        holidays2026.add(new Holiday("Ram Navami", "26th March 2026", "Thursday", "Birth of Lord Rama")); // Actually falls in March/April
        holidays2026.add(new Holiday("Mahavir Jayanti", "30th March 2026", "Monday", "Birth of Lord Mahavira"));

        // May 2026
        holidays2026.add(new Holiday("May Day", "1st May 2026", "Friday", "International Workers' Day"));
        holidays2026.add(new Holiday("Buddha Purnima", "4th May 2026", "Monday", "Birth of Gautam Buddha"));
        holidays2026.add(new Holiday("Eid al-Fitr", "14th May 2026", "Thursday", "End of Ramadan (Tentative)"));

        // June 2026
        holidays2026.add(new Holiday("Rath Yatra", "25th June 2026", "Thursday", "Chariot Festival"));

        // July 2026
        holidays2026.add(new Holiday("Eid al-Adha", "21st July 2026", "Tuesday", "Festival of Sacrifice (Tentative)"));

        // August 2026
        holidays2026.add(new Holiday("Independence Day", "15th August 2026", "Saturday", "India's Independence"));
        holidays2026.add(new Holiday("Raksha Bandhan", "28th August 2026", "Friday", "Bond of Protection"));
        holidays2026.add(new Holiday("Janmashtami", "5th August 2026", "Wednesday", "Birth of Lord Krishna"));

        // September 2026
        holidays2026.add(new Holiday("Ganesh Chaturthi", "19th September 2026", "Saturday", "Birth of Lord Ganesha"));
        holidays2026.add(new Holiday("Onam", "27th August 2026", "Thursday", "Harvest Festival of Kerala")); // Actually Aug/Sep

        // October 2026
        holidays2026.add(new Holiday("Gandhi Jayanti", "2nd October 2026", "Friday", "Birth of Mahatma Gandhi"));
        holidays2026.add(new Holiday("Dussehra", "30th October 2026", "Friday", "Victory of Good over Evil"));
        holidays2026.add(new Holiday("Milad-un-Nabi", "3rd October 2026", "Saturday", "Birth of Prophet Muhammad (Tentative)"));
        holidays2026.add(new Holiday("Karva Chauth", "31st October 2026", "Saturday", "Women's fasting festival"));

        // November 2026
        holidays2026.add(new Holiday("Diwali", "9th November 2026", "Monday", "Festival of Lights"));
        holidays2026.add(new Holiday("Govardhan Puja", "10th November 2026", "Tuesday", "Worship of Mount Govardhan"));
        holidays2026.add(new Holiday("Bhai Dooj", "11th November 2026", "Wednesday", "Sibling Bond"));
        holidays2026.add(new Holiday("Guru Nanak Jayanti", "15th November 2026", "Sunday", "Birth of Guru Nanak Dev"));

        // December 2026
        holidays2026.add(new Holiday("Christmas", "25th December 2026", "Friday", "Birth of Jesus Christ"));
        holidays2026.add(new Holiday("New Year's Eve", "31st December 2026", "Thursday", "Last day of the year"));
    }

    private void setupHolidayList() {
        // Create custom adapter for better display
        HolidayAdapter adapter = new HolidayAdapter(this, holidays2026);
        listView.setAdapter(adapter);
    }

    private void setupListeners() {
        // Set item click listener to show holiday details
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Holiday selected = holidays2026.get(position);
                String message = selected.getName() + "\n" +
                        selected.getDate() + " (" + selected.getDay() + ")\n" +
                        selected.getDescription();
                Toast.makeText(HolidayCalendarActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });

        // Download button listener
        if (btnDownloadHolidayList != null) {
            btnDownloadHolidayList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    generateHolidayListPdf();
                }
            });
        }
    }

    private void generateHolidayListPdf() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (btnDownloadHolidayList != null) {
            btnDownloadHolidayList.setEnabled(false);
        }

        String fileName = "Holiday_Calendar_2026.pdf";

        // Create PDF document
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        Paint headerPaint = new Paint();
        Paint boldPaint = new Paint();
        Paint monthPaint = new Paint();

        // Set colors and styles
        titlePaint.setColor(Color.parseColor("#6200EE"));
        titlePaint.setTextSize(24f);
        titlePaint.setFakeBoldText(true);

        headerPaint.setColor(Color.BLACK);
        headerPaint.setTextSize(16f);
        headerPaint.setFakeBoldText(true);

        monthPaint.setColor(Color.parseColor("#4CAF50"));
        monthPaint.setTextSize(18f);
        monthPaint.setFakeBoldText(true);

        boldPaint.setColor(Color.BLACK);
        boldPaint.setTextSize(12f);
        boldPaint.setFakeBoldText(true);

        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);

        int x = 50;
        int y = 80;
        int lineHeight = 30;

        // Draw title
        canvas.drawText("EMPLOYEE HOLIDAY CALENDAR 2026", x, y, titlePaint);
        y += lineHeight + 20;

        // Draw company info
        paint.setTextSize(14f);
        canvas.drawText("Your Company Name", x, y, paint);
        y += lineHeight - 10;
        canvas.drawText("Annual Holiday List", x, y, paint);
        y += lineHeight + 10;

        // Draw line
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawLine(x, y - 5, 545, y - 5, paint);
        paint.setStyle(Paint.Style.FILL);
        y += lineHeight;

        // Keep track of current month for grouping
        String currentMonth = "";
        int holidayCount = 0;

        for (Holiday holiday : holidays2026) {
            String month = holiday.getDate().split(" ")[1]; // Extract month from date string

            // Check if we need a new page
            if (y > 780) {
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 80;

                // Redraw header on new page
                canvas.drawText("EMPLOYEE HOLIDAY CALENDAR 2026 (Continued)", x, y, titlePaint);
                y += lineHeight + 20;
            }

            // Print month header if month changed
            if (!month.equals(currentMonth)) {
                currentMonth = month;
                y += 10;
                canvas.drawText(month.toUpperCase() + " 2026", x, y, monthPaint);
                y += lineHeight - 5;

                // Draw column headers
                canvas.drawText("Date", x, y, boldPaint);
                canvas.drawText("Day", x + 100, y, boldPaint);
                canvas.drawText("Holiday", x + 200, y, boldPaint);
                y += lineHeight - 10;

                // Draw separator line
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawLine(x, y - 5, 545, y - 5, paint);
                paint.setStyle(Paint.Style.FILL);
                y += 5;
            }

            // Draw holiday details
            String dateWithoutYear = holiday.getDate().replace("2026", "").trim();
            canvas.drawText(dateWithoutYear, x, y, paint);
            canvas.drawText(holiday.getDay(), x + 100, y, paint);
            canvas.drawText(holiday.getName(), x + 200, y, paint);
            y += lineHeight - 5;
            holidayCount++;
        }

        // Add summary at the end
        y += lineHeight;
        canvas.drawText("Total Holidays: " + holidayCount, x, y, boldPaint);
        y += lineHeight;

        // Add footer
        paint.setColor(Color.GRAY);
        paint.setTextSize(10f);
        String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(new java.util.Date());
        canvas.drawText("Generated on: " + dateStr, x, y, paint);
        y += lineHeight - 10;
        canvas.drawText("Note: Festival dates are tentative and subject to change based on moon sightings.", x, y, paint);
        y += lineHeight - 10;
        canvas.drawText("This is a system generated holiday calendar.", x, y, paint);

        // Finish the page
        pdfDocument.finishPage(page);

        // Save the PDF
        try {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs();
            }

            File file = new File(downloadsDir, fileName);
            FileOutputStream fos = new FileOutputStream(file);
            pdfDocument.writeTo(fos);
            fos.close();
            pdfDocument.close();

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (btnDownloadHolidayList != null) {
                btnDownloadHolidayList.setEnabled(true);
            }

            Toast.makeText(this, "Holiday calendar PDF saved to Downloads folder", Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            e.printStackTrace();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            if (btnDownloadHolidayList != null) {
                btnDownloadHolidayList.setEnabled(true);
            }
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Holiday model class
    private class Holiday {
        private String name;
        private String date;
        private String day;
        private String description;

        public Holiday(String name, String date, String day, String description) {
            this.name = name;
            this.date = date;
            this.day = day;
            this.description = description;
        }

        public String getName() { return name; }
        public String getDate() { return date; }
        public String getDay() { return day; }
        public String getDescription() { return description; }
    }

    // Custom ArrayAdapter for better holiday display
    private class HolidayAdapter extends android.widget.ArrayAdapter<Holiday> {
        public HolidayAdapter(HolidayCalendarActivity context, ArrayList<Holiday> holidays) {
            super(context, 0, holidays);
        }

        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            Holiday holiday = getItem(position);

            TextView text1 = convertView.findViewById(android.R.id.text1);
            TextView text2 = convertView.findViewById(android.R.id.text2);

            if (text1 != null) {
                text1.setText(holiday.getName() + " - " + holiday.getDate());
                text1.setTextSize(16);
                text1.setTextColor(Color.BLACK);
            }

            if (text2 != null) {
                text2.setText(holiday.getDay() + " • " + holiday.getDescription());
                text2.setTextSize(14);
                text2.setTextColor(Color.GRAY);
            }

            return convertView;
        }
    }
}