package com.springboot.swt.project.ServiceImpl;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springboot.swt.project.Service.UserService;
import com.springboot.swt.project.entity.Batch;
import com.springboot.swt.project.entity.Student;
import com.springboot.swt.project.entity.TempUser;
import com.springboot.swt.project.entity.User;
import com.springboot.swt.project.repo.BatchRepo;
import com.springboot.swt.project.repo.StudentRepo;
import com.springboot.swt.project.repo.TempUserRepo;
import com.springboot.swt.project.repo.UserRepo;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private TempUserRepo tempUserRepo;

	@Autowired
	private UserRepo userrepo;

	@Autowired
	private StudentRepo studentrepo;

	@Autowired
	private EmailSenderImpl emailSenderImpl;
	@Autowired
	private BatchRepo batchrepo;

	@Override
	public Map<String, Object> register(User user) {
		Map<String, Object> response = new HashMap<>();
		user.setRole("Student");
		user.setId(generateUserId(user));

			user.setPassword(encode(user.getPassword()));
			user.setContactNo(encode(user.getContactNo()));

		if (finder(user)) {
			response.put("message", "email or contact no already registered");
			response.put("user", null);
			return response;
		}
		userrepo.save(user);
		try {
			user.setContactNo(decode(user.getContactNo()));
			user.setPassword(decode(user.getPassword()));
		}
		catch(Exception e){}
		response.put("message", "User registered successfully");
		response.put("user", user);
		return response;
	}
	private String generateUserId(User user) {
		StringBuilder id = new StringBuilder();
		LocalDate local = LocalDate.now();
		id = id.append(("" + local.getYear()).substring(2));
		id = id.append("swt");
		id = id.append(user.getContactNo().substring(0, 3));
		Random rand = new Random();
		id = id.append(String.format("%04d", rand.nextInt(10000)));
		return id.toString();
	}

	@Override
	public String encode(String s) {
		return s;//Base64.getEncoder().encodeToString(s.getBytes());
	}

	@Override
	public String decode(String s) {
		return s;//new String(Base64.getDecoder().decode(s));
	}

	@Override
	public boolean finder(User user) {
		return userrepo.findByContactNoOrEmail(user.getContactNo(), user.getEmail()) != null;
	}

	@Override
	public User login(String email, String password) {
		User tempEmail = userrepo.findByEmailAndPassword(email, encode(password));
		if (tempEmail != null) {
			tempEmail.setContactNo(decode(tempEmail.getContactNo()));
			return tempEmail;
		}
		return null;
	}

	@Override
	public String allowOrBlockUserByID(String id, String allowed) {
		Optional<User> optional = userrepo.findById(id);
		User user = optional.get();
		user.setAllowed(allowed);
		userrepo.save(user);
		return allowed;
	}

	@Override
	public List<User> getNotAllowedUsers() {
		return userrepo.findByAllowed("Not Allowed").stream().map(user -> {
			user.setContactNo(decode(user.getContactNo()));
			return user;
		}).toList();
	}

	@Override
	@Transactional
	public String enrollstudent(String batchId, User user) {

		Random rand = new Random();
		Optional<Batch> optional = batchrepo.findById(batchId);
		Batch batch = optional.get();

		Student oldbatch = studentrepo.findByUserAndBatch(user, batch);
		List<Student> studybatch = studentrepo.findByuser(user);

		for (Student stud : studybatch) {
			if (stud.getBatch().getBatchTopic().equals(batch.getBatchTopic())
					&& stud.getBatch().getCurrentStatus().equals("Completed")) {
				return "Already studied that-:" + batch.getBatchTopic() + " batch";
			}
		}
		int oldbatchstatus = 0;
		String old = null;
		for (Student stud : studybatch) {
			if (stud.getBatch().getCurrentStatus().equals("Enroll")
					&& !stud.getBatch().getBatchTopic().equals(batch.getBatchTopic())) {
				old = "" + stud.getBatch().getBatchTopic();
				stud.setBatch(batch);
				studentrepo.save(stud);
				return "remove " + old + " batch And Enroll batch" + batch.getBatchTopic();
			}
		}

		if (oldbatch == null) {
			Student student = new Student();
			ArrayList<Integer> list = new ArrayList<Integer>(10);
			student.setBatch(batch);
			student.setUser(user);
			student.setMarks(list);
			user.setBatch(batch.getBatchTopic());
			userrepo.save(user);
			studentrepo.save(student);
			return "your are successfully Enroll " + batch.getBatchTopic() + " batch";
		} else {
			return " Student is Already Enrolled for " + batch.getBatchTopic();
		}

	}

	@Override
	public List<Integer> getMarksList(String id) {
		List<Student> studentList = studentrepo.findByuser(userrepo.findById(id).get());
		Student student = null;
		for (Student studentTemp : studentList) {
			if (studentTemp.getBatch().getCurrentStatus().equals("Active")) {
				student = studentTemp;
				break;
			}
		}
		if (student != null)
			return student.getMarks();
		return null;
	}

	@Override
	public void otpSend(String email, String purpose) {
		// it will generate 6 digit no and we will find the user by the email to set the
		// otp in the attribute
		int min = 100000; // Minimum 6-digit number
		int max = 999999; // Maximum 6-digit number

		Random random = new Random();
		int randomNumber = random.nextInt(max - min + 1) + min;

		if (purpose.equals("regis")) { // if the request is from regis it wil save otp in temprory table other wise in
										// primary table
			TempUser tempUser = tempUserRepo.findByEmail(email);
			tempUser.setOtp("" + randomNumber);
			tempUserRepo.save(tempUser); // we will update the otp in the database
			emailSenderImpl.sendEmail(email, "Password Reset OTP - Softwaves", "" + randomNumber);
		} else {
			User user = userrepo.findByContactNoOrEmail(null, email);
			user.setOtp("" + randomNumber);
			userrepo.save(user); // we will update the otp in the database
			emailSenderImpl.sendEmail(email, "Password Reset OTP - Softwaves", "" + randomNumber);
		}
	}

	public Object getUser(String email, String purpose) {
		if (purpose.equals("regis"))
			return tempUserRepo.findByEmail(email); // if the purpose is regis then we will find the user in temprory
													// table
		return userrepo.findByContactNoOrEmail(null, email); // otherwise in primary table
	}

	@Override
	public User resetPassword(String email, String password) {

		User user = userrepo.findByContactNoOrEmail(null, email);
		user.setPassword(encode(password));
		return userrepo.save(user);
	}

	public Student markAttendancepresent(String rollNo, String batchId) {

		Batch batch = (batchrepo.findById(batchId)).get();
		Student student = studentrepo.findByRollNoAndBatch(rollNo, batch);
		if (student == null)
			return null;
		LocalDate local = LocalDate.now();
		StringBuilder currenttime = new StringBuilder();
		currenttime = currenttime.append(local.getYear() + "-");
		int month = local.getMonthValue();
		if (month <= 9)
			currenttime = currenttime.append("0" + local.getMonthValue() + "-");
		else
			currenttime = currenttime.append(local.getMonthValue() + "-");
		currenttime = currenttime.append(local.getDayOfMonth());
		Date date = student.getBatch().getStartDate();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		// Correctly create LocalDate using month and day
		LocalDate sqllocalDate = LocalDate.of(calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1, // Month is 1-based in LocalDate
				calendar.get(Calendar.DAY_OF_MONTH));
		long totalday = ChronoUnit.DAYS.between(sqllocalDate, local) + 1;
		if (student.absent.contains(currenttime.toString()))// by chanc galti se agar absent lag to ye sahi kr dega
		{
			student.absent.remove(currenttime.toString());
			student.setAttendanceCount(student.getAttendanceCount() + 1);
			studentrepo.save(student);
			return student;
		} else if ((totalday == student.absent.size() + student.getAttendanceCount()
				&& student.getAttendanceCount() >= 1)) {
			studentrepo.save(student);
			return student;
		} else
			student.setAttendanceCount(student.getAttendanceCount() + 1);
		studentrepo.save(student);
		return student;
	}

	@Override
	public Student markAttendanceAbsent(String rollNo, String batchId) {
		Batch batch = (batchrepo.findById(batchId)).get();
		Student student = studentrepo.findByRollNoAndBatch(rollNo, batch);
		if (student == null)
			return null;
		LocalDate local = LocalDate.now();
		StringBuilder currenttime = new StringBuilder();
		currenttime = currenttime.append(local.getYear() + "-");
		int month = local.getMonthValue();
		if (month <= 9)
			currenttime = currenttime.append("0" + local.getMonthValue() + "-");
		else
			currenttime = currenttime.append(local.getMonthValue() + "-");
		currenttime = currenttime.append(local.getDayOfMonth());
		if (student.absent.contains(currenttime.toString()))// mana galti se pehle pesent lag gai to
			return student;
		Date date = student.getBatch().getStartDate();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		// Correctly create LocalDate using month and day
		LocalDate sqllocalDate = LocalDate.of(calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1, // Month is 1-based in LocalDate
				calendar.get(Calendar.DAY_OF_MONTH));
		long totalday = ChronoUnit.DAYS.between(sqllocalDate, local) + 1;
		if (totalday != 0 && (totalday == student.absent.size() + student.getAttendanceCount()
				&& student.getAttendanceCount() >= 1)) {
			student.setAttendanceCount(student.getAttendanceCount() - 1);
		}
		student.absent.add(currenttime.toString());
		studentrepo.save(student);

		return student;
	}

	@Override
	public List<Student> findStudentBatch(User user) {
		if(user!=null) {
		List<Student> studentlist = studentrepo.findByUserId(user.getId());
		return studentlist;
		}
		return null;
	}

	@Override
	public List getAllStudent(String name) {
		List<User> allStudentList = userrepo.findAll().stream()
				.filter(user -> user.getName().toLowerCase().startsWith(name.toLowerCase())
						&& !"Admin".equals(user.getRole()))
				.sorted(Comparator.comparing(User::getName)) // Sort by name
				.collect(Collectors.toList());

		return allStudentList;
	}

	@Override
	public String allowOrBlockVolunteerByID(String id, String allowed) {
		Optional<User> optional = userrepo.findById(id);
		User user = optional.get();
		user.setRole(allowed);
		userrepo.save(user);
		return allowed;
	}

	@Override
	public Map<String, Object> tempRegister(User user) {
		Map<String, Object> response = new HashMap<>();
		if (finder(user)) {
			response.put("message", "email or contact no already registered");
			response.put("user", null);
			return response;
		}

		tempUserRepo.save(new TempUser(user.getEmail(), null));

		response.put("message", "User registered successfully");
		response.put("user", user);
		return response;
	}
	
	@Override
	public List<User> getVolunteerList(){
		List<User> volunteerList = userrepo.findByRole("Volunteer");
		return volunteerList;
	}
	
	public Map getAverage() {
		List<Batch> activeBatches=batchrepo.findByCurrentStatus("Active");
		if(activeBatches == null || activeBatches.isEmpty())return null;
		Map<Batch, Integer> avgBatches= new HashMap<>();
		for(Batch batch : activeBatches)
		{
			int avgOfStudents=0;
			List<Student> studentList=studentrepo.findByBatch(batch);
			if(studentList!=null && !studentList.isEmpty()) {
			for(Student student : studentList)
			{
				LinkedList<Integer> marks=new LinkedList<>(student.getMarks());
				if(marks!=null && !marks.isEmpty())avgOfStudents=avgOfStudents+(int)marks.get(marks.size()-1);
			}
			}
			if(avgOfStudents != 0) avgBatches.put(batch,avgOfStudents/studentList.size());
			else avgBatches.put(batch,  0 );
		}
		
		return avgBatches;
	}

	@Override
	public List<Integer> getMarksListCompletedBatch(String studId) {
		Optional<Student> student = studentrepo.findById(Integer.parseInt(studId));
		if (student == null)
			return null;
		Student st = student.get();
		return st.getMarks();
	}

	@Override
	public String[] validation(User user) {
//		String [] patternString = {"^(\\w+[@](gmail|yahoo)\\.(com|in))$" , "^(([+]91)?[6-9]\\d{9})$" ,"^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$"};
//		String [] matcherString = {user.getEmail() , user.getContactNo() , user.getPassword()};
//		int count =0;
//		for (int i=0;i<patternString.length;i++)
//		{
//			Pattern pattern =Pattern.compile(patternString[i]);
//			Matcher matcher=pattern.matcher(matcherString[i]);
//			if (matcher.matches()) count++;
//		}
//
		Pattern emailValid=Pattern.compile("^(\\w+[@](gmail|yahoo)\\.(com|in))$");
		Matcher emailMatcher=emailValid.matcher(user.getEmail());
		if (!emailMatcher.matches())return new String[]{"false" , "email is not valid " };

		Pattern phoneValid=Pattern.compile("^(([+]91)?[6-9]\\d{9})$");
		Matcher phoneMatcher=phoneValid.matcher(user.getContactNo());
		if (!phoneMatcher.matches())return new String[]{"false", "phone no is invalid " };


		Pattern passValid=Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
		Matcher passMatcher=passValid.matcher(user.getPassword());
		if (!passMatcher.matches())return new String[]{"false" , "password is invalid " };


//		return passMatcher.matches() && emailMatcher.matches() && phoneMatcher.matches() ;
		return new String[]{"true" , "sucess"};
	}
}
