package com.android.attendance.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.attendance.bean.AttendanceBean;
import com.android.attendance.bean.StudentBean;
import com.android.attendance.context.ApplicationContext;
import com.android.attendance.db.DBAdapter;
import com.example.androidattendancesystem.R;

import java.util.ArrayList;
import java.util.HashMap;

public class AddAttendanceActivity extends Activity {

	private TextView studentNameTextView;
	private Button presentButton, absentButton, nextButton, backButton;

	private ArrayList<StudentBean> studentList;
	private int currentIndex = 0;
	private int sessionId;
	private DBAdapter dbAdapter;

	private boolean defaultAbsentMode = false;
	private String selectedStatus;

	// Track attendance manually to allow changes
	private HashMap<Integer, String> attendanceMap = new HashMap<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_attendance);

		studentNameTextView = findViewById(R.id.studentNameTextView);
		presentButton       = findViewById(R.id.presentButton);
		absentButton        = findViewById(R.id.absentButton);
		nextButton          = findViewById(R.id.nextButton);
		backButton          = findViewById(R.id.backButton);

		dbAdapter = new DBAdapter(this);
		sessionId = getIntent().getIntExtra("sessionId", -1);
		studentList = ((ApplicationContext) getApplicationContext()).getStudentBeanList();

		if (studentList == null || studentList.isEmpty()) {
			Toast.makeText(this, "No students available", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}

		// Disable buttons initially
		presentButton.setEnabled(false);
		absentButton.setEnabled(false);
		nextButton.setEnabled(false);
		backButton.setEnabled(false);

		showModeSelectionDialog();

		presentButton.setOnClickListener(v -> {
			selectedStatus = "P";
			attendanceMap.put(studentList.get(currentIndex).getStudent_id(), selectedStatus);
			currentIndex++;
			showStudent();
		});

		absentButton.setOnClickListener(v -> {
			selectedStatus = "A";
			attendanceMap.put(studentList.get(currentIndex).getStudent_id(), selectedStatus);
			currentIndex++;
			showStudent();
		});

		nextButton.setOnClickListener(v -> {
			attendanceMap.put(studentList.get(currentIndex).getStudent_id(), selectedStatus);
			currentIndex++;
			showStudent();
		});

		backButton.setOnClickListener(v -> {
			if (currentIndex > 0) {
				attendanceMap.put(studentList.get(currentIndex).getStudent_id(), selectedStatus);
				currentIndex--;
				showStudent();
			} else {
				Toast.makeText(this, "This is the first student", Toast.LENGTH_SHORT).show();
			}
		});
	}

	private void showModeSelectionDialog() {
		final String[] modes = {"Default Present", "Default Absent"};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select Attendance Mode")
				.setSingleChoiceItems(modes, defaultAbsentMode ? 1 : 0, (dialog, which) -> {
					defaultAbsentMode = (which == 1);
				})
				.setPositiveButton("Start", (dialog, which) -> {
					presentButton.setEnabled(true);
					absentButton.setEnabled(true);
					nextButton.setEnabled(true);
					backButton.setEnabled(true);

					if (defaultAbsentMode) {
						absentButton.setVisibility(View.GONE);
					} else {
						presentButton.setVisibility(View.GONE);
					}

					showStudent();
					dialog.dismiss();
				})
				.setCancelable(false)
				.show();
	}

	private void showStudent() {
		if (currentIndex < studentList.size()) {
			StudentBean student = studentList.get(currentIndex);
			studentNameTextView.setText("[" + (currentIndex + 1) + "/" + studentList.size() + "] "
					+ student.getStudent_firstname() + " " + student.getStudent_lastname());

			// Restore previously selected status if revisiting
			Integer studentId = student.getStudent_id();
			if (attendanceMap.containsKey(studentId)) {
				selectedStatus = attendanceMap.get(studentId);
			} else {
				selectedStatus = defaultAbsentMode ? "A" : "P";
			}

		} else {
			saveAllToDatabase();
			Toast.makeText(this, "Attendance completed", Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private void saveAllToDatabase() {
		for (StudentBean student : studentList) {
			int studentId = student.getStudent_id();
			String status = attendanceMap.getOrDefault(studentId, defaultAbsentMode ? "A" : "P");

			AttendanceBean attendance = new AttendanceBean();
			attendance.setAttendance_session_id(sessionId);
			attendance.setAttendance_student_id(studentId);
			attendance.setAttendance_status(status);
			dbAdapter.addNewAttendance(attendance);
		}
	}

	@Override
	public void onBackPressed() {
		saveAllToDatabase();
		Toast.makeText(this, "Attendance saved", Toast.LENGTH_SHORT).show();
		super.onBackPressed();
	}
}
