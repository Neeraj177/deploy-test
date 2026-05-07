package com.springboot.swt.project.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.swt.project.ServiceImpl.BatchServiceImpl;
import com.springboot.swt.project.ServiceImpl.StudentServiceImpl;
import com.springboot.swt.project.ServiceImpl.UserServiceImpl;
import com.springboot.swt.project.entity.Batch;
import com.springboot.swt.project.entity.Student;
import com.springboot.swt.project.entity.TempUser;
import com.springboot.swt.project.entity.User;
import com.springboot.swt.project.repo.BatchRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RequestMapping("/user")
@Controller
public class
UserController {

	@Autowired
	private UserServiceImpl userserviceimpl;
	@Autowired
	private BatchServiceImpl batchservicesimpl;

	@Autowired
	private StudentServiceImpl studentServiceImpl;

	// Login Controller
	@PostMapping("/login")
	public ModelAndView login(HttpServletRequest request, @RequestParam String email, String password) {
		ModelAndView modal = new ModelAndView();
		User temp = userserviceimpl.login(email, password);
		if (temp != null && temp.getAllowed().equals("Allowed")) {
			HttpSession session = request.getSession();

			if (temp.getRole().equalsIgnoreCase("Student") || temp.getRole().equalsIgnoreCase("Volunteer")) {
				session.setAttribute("user", temp);
				Student student = studentServiceImpl.getActiveStudent(temp);
				session.setAttribute("activeStudentUser", student);
				modal.setViewName("redirect:dashboard");

				List<Integer> studentMarks = userserviceimpl.getMarksList(temp.getId());
				session.setAttribute("studentMarks", studentMarks);
				int x= studentMarks!=null?studentMarks.size():0;
				//System.out.println(studentMarks);
				return modal;
			}
			if (temp.getRole().equalsIgnoreCase("Admin")) {
				List<Student> allStudents = studentServiceImpl.findAllStudent("");
				List<Batch> allBatches = batchservicesimpl.sendAllBatches();
				List<User> notAllowedUsers = userserviceimpl.getNotAllowedUsers();
				List<User> volunteerList = userserviceimpl.getVolunteerList();
				Map<Batch, String> avgBatches = userserviceimpl.getAverage();

				session.setAttribute("allStudents", allStudents);
				session.setAttribute("allBatches", allBatches);
				session.setAttribute("notAllowedUsers", notAllowedUsers);
				session.setAttribute("volunteerList", volunteerList);
				session.setAttribute("avgBatches", avgBatches);
				session.setAttribute("admin", temp);
				modal.setViewName("redirect:/admin/dashboard");
				return modal;
			}
		}

		if (temp == null) {
			modal.addObject("error", "Incorrect Email or password ");
		} else if (temp.getAllowed().equals("Not Allowed")) {
			modal.addObject("error", "Wait For Approval");
		} else {
			modal.addObject("error", "Account Blocked Contact Admin Portal");
		}
		modal.setViewName("redirect:/swt/login");
		return modal;
	}

	@RequestMapping("/dashboard")
	public String getDashBoard(HttpServletRequest request) {
		if (request.getSession().getAttribute("user") == null)
			return "redirect:/swt/login";
		return "dashboard";
	}

	@RequestMapping("/regis")
	public ModelAndView registration(@ModelAttribute("user") User user, BindingResult bindingResult) {

		Map<String, Object> result = userserviceimpl.register(user); // Using the map returned from the service
		ModelAndView modal = new ModelAndView();

		String message = (String) result.get("message");
		User temp = (User) result.get("user");

		if (temp == null) {
			modal.addObject("error", message); // Using the message from the map
			modal.setViewName("redirect:/swt/regis");
			return modal;
		}
		modal.addObject("success", message);
		modal.setViewName("redirect:/swt/login");
		return modal;
	}

	@RequestMapping("/logout")
	public String logout(HttpServletRequest request) {
		request.getSession().invalidate();
		return "redirect:/swt/";
	}

	@RequestMapping("/dashboard/attendance")
	public String getAttendance(@RequestParam("student") String encodedJson, Model model, HttpServletRequest request) {
		if (request.getSession().getAttribute("user") == null)
			return "redirect:/swt/login";
		HttpSession session = request.getSession();
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			Student studentObject = objectMapper.readValue(encodedJson, Student.class);
			model.addAttribute("studentdecodedobject", studentObject);
		} catch (Exception e) {
		}
		return "attendance";
	}

	// finding user by email for otp verification
	@RequestMapping("/otp")
	public ModelAndView demo(@ModelAttribute("user") User user, String purpose) { // purpose string is added for otp and
																					// regis
		ModelAndView modal = new ModelAndView();

		if (purpose.equals("regis") || userserviceimpl.finder(user)) {
			userserviceimpl.otpSend(user.getEmail(), purpose); // otp sending according to purpose
			modal.addObject("user69", user);
			modal.addObject("purpose", purpose);
			modal.setViewName("forward:/views/Otp.jsp");
		} else {
			modal.setViewName("redirect:/views/forgetPassword.jsp");
			modal.addObject("error", "email is not registered");
		}

		return modal;
	}

	@RequestMapping("/verify")
	public ResponseEntity<Map<String, Object>> otpVerify(@ModelAttribute("user") User user1,
			@RequestParam String otp, String purpose) {

		Map<String, Object> response = new HashMap<>();
		Object user = userserviceimpl.getUser(user1.getEmail(), purpose); // retriving user for otp check using purpose
		String userOtp = (user instanceof TempUser) ? ((TempUser) user).getOtp() : ((User) user).getOtp(); // getting
																											// the otp
																											// from
																											// corresponding
																											// user
																											// weather
																											// it is
																											// tempuser
																											// of User

		if (userOtp.equals(otp)) {
			if (purpose.equals("regis")) {
				userserviceimpl.register(user1);
				response.put("success", "successfully registered");
				response.put("redirectUrl", "/swt/login?success=Email is registered");
			} else {

				response.put("success", true);
				response.put("redirectUrl", "/views/PasswordReset.jsp?email=" + user1.getEmail());
			}
			response.put("email", user1.getEmail());
			return ResponseEntity.ok(response);
		} else {
			response.put("success", false);
			response.put("errorMessage", "OTP is wrong");
			response.put("email", user1.getEmail());
			return ResponseEntity.badRequest().body(response);
		}
	}

	@RequestMapping("/resetpass")
	public ModelAndView resetPassword(@RequestParam("email") String email, String pass1, String pass2) {

		userserviceimpl.resetPassword(email, pass1);
		return new ModelAndView("redirect:/swt/login", "success", "password successfully is changed ");

	}

	@RequestMapping("/marks")
	public String getStudentMarks(HttpServletRequest request, @RequestParam String id) {
		if (request.getSession().getAttribute("user") == null)
			return "redirect:/swt/login";
		List<Integer> marksList = userserviceimpl.getMarksList(id);
		HttpSession session = request.getSession();
		session.setAttribute("marksList", marksList);

		return "studentmarks";
	}

	@RequestMapping("/allbatches")
	public ResponseEntity createBatchPage(HttpServletRequest request, Model model) {

		List<Batch> batches = batchservicesimpl.getAllBatches();
		return new ResponseEntity<>(batches, HttpStatus.OK);
	}

	@RequestMapping("/enrollstudent")
	public ResponseEntity enrollstudent(@RequestParam String batchId, HttpServletRequest request) {
		HttpSession session = request.getSession();
		User session1 = (User) session.getAttribute("user");
		if (session1 != null) {
			String cuurentstatus = userserviceimpl.enrollstudent(batchId, session1);
			return new ResponseEntity<>(cuurentstatus, HttpStatus.OK);
		}
		return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
	}

	@RequestMapping("/findVolunteer")
	public ResponseEntity findVolunteer(@RequestParam String name, HttpServletRequest request) {
		List<Student> list = (List<Student>) userserviceimpl.getAllStudent(name);
		return new ResponseEntity<>(list, HttpStatus.OK);
	}

	@RequestMapping("/find/student/batch")
	public ResponseEntity findStudentBatch(HttpServletRequest request, Model model) {
		HttpSession session = request.getSession();
		User session1 = (User) session.getAttribute("user");
		List<Student> batches = userserviceimpl.findStudentBatch(session1);
		return new ResponseEntity<>(batches, HttpStatus.OK);
	}

	@RequestMapping("/tempregis")
	public ModelAndView tempRegistration(@ModelAttribute("user") User user,
			RedirectAttributes redirectAttributes) {

//		String [] out =userserviceimpl.validation(user);
//		ModelAndView modalValid = new ModelAndView();
//		if(out[0].equals("false"))
//		{
//			modalValid.addObject("error", out[1]);
//			modalValid.setViewName("redirect:/swt/regis");
//			return modalValid;
//		}

		Map<String, Object> result = userserviceimpl.tempRegister(user); // saves the data in temprory table
		ModelAndView modal = new ModelAndView();

		String message = (String) result.get("message");
		User temp = (User) result.get("user");
		if (temp == null) {
			modal.addObject("error", message);
			modal.setViewName("redirect:/swt/regis");
			return modal;
		}
		modal.addObject("user", user);
		modal.addObject("success", message);

		redirectAttributes.addFlashAttribute("user", user); // used flash attributes to transfer the data from one
															// controller to other
		redirectAttributes.addFlashAttribute("success", message);
		return demo(user, "regis"); // another controller is called
	}

	@RequestMapping("/getmarks")
	public ResponseEntity getMarks(@RequestParam String id) {

		List<Integer> marksList = userserviceimpl.getMarksList(id);

		return new ResponseEntity(marksList, HttpStatus.OK);
	}

}
