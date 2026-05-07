package com.springboot.swt.project.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.springboot.swt.project.ServiceImpl.BatchServiceImpl;
import com.springboot.swt.project.ServiceImpl.StudentServiceImpl;
import com.springboot.swt.project.ServiceImpl.UserServiceImpl;
import com.springboot.swt.project.entity.Batch;
import com.springboot.swt.project.entity.Student;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/valunteer")

public class VolunteerController {

	@Autowired
	BatchServiceImpl batchservice;
	@Autowired
	UserServiceImpl userserviceimpl;
	@Autowired
	StudentServiceImpl StudentServiceImpl;

	@RequestMapping("/findActivebatches")
	public ResponseEntity findActivebatches(Model model, HttpServletRequest session) {
		List<Batch> batchlist = batchservice.findByCurrentStatus("Active");
		if (batchlist.size() == 0) {
			model.addAttribute("activebatch", batchlist);
			return null;
		}
		return new ResponseEntity(batchlist, HttpStatus.OK);
	}

	@RequestMapping("/findallstudent")
	public ResponseEntity findallstudent(@RequestParam("batchId") String batchId, Model model,
			HttpServletRequest session) {

		List<Student> studentlist = StudentServiceImpl.findByBatch(batchId);
		// List<Student> absentlist = StudentAttendanceServiceImpl.findByBatch(batchId);
		if (studentlist.size() != 0)
			model.addAttribute("studentlist", studentlist);
		return new ResponseEntity<>(studentlist, HttpStatus.OK);
	}

	@RequestMapping("/findallstudentmodel")
	public Model findallstudentModal(@RequestParam("batchId") String batchId, Model model,
			HttpServletRequest session) {

		List<Student> studentlist = StudentServiceImpl.findByBatch(batchId);
		if (studentlist.size() != 0)
			return model.addAttribute("studentlist", studentlist);
		return model;
	}

	@RequestMapping("/userattendance")
	public String userattendance(HttpServletRequest session) {

		return "userattendance";
	}

	@RequestMapping("/studentattendance/present")
	public ResponseEntity markAttendancepresent(@RequestParam("rollNo") String rollNo,
			@RequestParam("batchId") String batchId, HttpServletRequest session) {
		Student student = userserviceimpl.markAttendancepresent(rollNo, batchId);
		if (student != null)
			return new ResponseEntity<>(student, HttpStatus.OK);
		return new ResponseEntity<>(student, HttpStatus.FORBIDDEN);

	}

	@RequestMapping("/studentattendance/absent")
	public ResponseEntity markAttendanceAbsent(@RequestParam("rollNo") String rollNo,
			@RequestParam("batchId") String batchId, HttpServletRequest session) {
		Student student = userserviceimpl.markAttendanceAbsent(rollNo, batchId);
		if (student != null)
			return new ResponseEntity<>(student, HttpStatus.OK);
		return new ResponseEntity<>(student, HttpStatus.FORBIDDEN);

	}

	@RequestMapping("/volunteerMarks")
	public String volunteerMarks(HttpServletRequest request) {
		List<Batch> activebatches = batchservice.findByCurrentStatus("Active");
		HttpSession session = request.getSession();
		session.setAttribute("activebatch", activebatches);
		return "volunteerMarks";
	}

	@RequestMapping("/setmarks")
	public ResponseEntity setStudentMarks(@RequestParam String rollNo, String batchId, String marks, String testNo,
			HttpServletRequest request) {
		int test=0;
		int roll=0;
		int studentMarks=0;
				System.out.println(studentMarks+"105 vc ");
		try {
			studentMarks=Integer.parseInt(marks);
			test=Integer.parseInt(testNo);
			roll=Integer.parseInt(rollNo);
		}
		catch(Exception e)
		{
			return new ResponseEntity(null, HttpStatus.BAD_REQUEST);
		}
	
		Student student = StudentServiceImpl.setMarks(rollNo, batchId, test , studentMarks);
		if (student == null)
			return new ResponseEntity(student, HttpStatus.BAD_REQUEST);

		return new ResponseEntity(student, HttpStatus.OK);

	}
}
